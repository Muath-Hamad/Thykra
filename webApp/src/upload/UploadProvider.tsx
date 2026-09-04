import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react';
import {
  confirmUpload,
  requestUploadUrl,
  uploadFileWithProgress,
  type MediaDto,
} from '../api/media';

export type UploadStatus = 'QUEUED' | 'UPLOADING' | 'CONFIRMING' | 'DONE' | 'FAILED';

export interface UploadItem {
  id: string;
  albumId: string;
  albumTitle: string;
  filename: string;
  previewUrl?: string;
  status: UploadStatus;
  attempt: number;
  /** Real bytes from xhr.upload.onprogress — only while UPLOADING. */
  progress: number | null;
  mediaId?: string;
  media?: MediaDto;
  error?: string;
}

export interface UploadBatchSummary {
  albumId: string;
  albumTitle: string;
  added: MediaDto[];
  failed: number;
  total: number;
}

interface UploadContextType {
  items: UploadItem[];
  /** Enqueue files; non-media files are rejected via onRejected. */
  enqueue: (files: File[], albumId: string, albumTitle: string) => File[];
  retry: (id: string) => void;
  retryAll: () => void;
  dismiss: (id: string) => void;
  clearFinished: () => void;
  /** Fires once when a batch fully settles (all DONE/terminal-FAILED). */
  onBatchSettled: (fn: (summary: UploadBatchSummary) => void) => () => void;
  /** Fires per confirmed media — the trip screen inserts into its chapter. */
  onMediaConfirmed: (fn: (media: MediaDto) => void) => () => void;
}

const UploadContext = createContext<UploadContextType | null>(null);

let nextId = 1;

const MAX_ATTEMPTS = 3;

function isMediaFile(file: File): boolean {
  return file.type.startsWith('image/') || file.type.startsWith('video/');
}

export function UploadProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<UploadItem[]>([]);
  const queueRef = useRef<{ item: UploadItem; file: File; reported?: boolean }[]>([]);
  const processingRef = useRef(false);
  const settledListeners = useRef(new Set<(s: UploadBatchSummary) => void>());
  const confirmedListeners = useRef(new Set<(m: MediaDto) => void>());

  const patch = useCallback((id: string, changes: Partial<UploadItem>) => {
    setItems((prev) => prev.map((u) => (u.id === id ? { ...u, ...changes } : u)));
    const entry = queueRef.current.find((q) => q.item.id === id);
    if (entry) Object.assign(entry.item, changes);
  }, []);

  const checkSettled = useCallback(() => {
    // A batch = everything not yet reported in the queue for one album.
    const byAlbum = new Map<string, typeof queueRef.current>();
    for (const entry of queueRef.current) {
      if (entry.reported) continue;
      (byAlbum.get(entry.item.albumId) ??
        byAlbum.set(entry.item.albumId, []).get(entry.item.albumId)!).push(entry);
    }
    for (const [albumId, entries] of byAlbum) {
      const albumItems = entries.map((e) => e.item);
      const unfinished = albumItems.some(
        (i) =>
          i.status === 'QUEUED' ||
          i.status === 'UPLOADING' ||
          i.status === 'CONFIRMING' ||
          (i.status === 'FAILED' && i.attempt < MAX_ATTEMPTS),
      );
      if (!unfinished && albumItems.length > 0) {
        const summary: UploadBatchSummary = {
          albumId,
          albumTitle: albumItems[0].albumTitle,
          added: albumItems.filter((i) => i.media).map((i) => i.media!),
          failed: albumItems.filter((i) => i.status === 'FAILED').length,
          total: albumItems.length,
        };
        if (summary.added.length > 0 || summary.failed > 0) {
          settledListeners.current.forEach((fn) => fn(summary));
        }
        // DONE entries no longer need their File; FAILED entries keep it so
        // Retry can re-upload. `reported` stops the batch from re-reporting.
        queueRef.current = queueRef.current.filter(
          (q) => q.item.albumId !== albumId || q.item.status === 'FAILED',
        );
        for (const e of entries) {
          if (e.item.status === 'FAILED') e.reported = true;
        }
      }
    }
  }, []);

  const processQueue = useCallback(async () => {
    if (processingRef.current) return;
    processingRef.current = true;

    for (;;) {
      const next = queueRef.current.find(
        (q) =>
          q.item.status === 'QUEUED' ||
          (q.item.status === 'FAILED' && q.item.attempt < MAX_ATTEMPTS),
      );
      if (!next) break;

      const { item, file } = next;
      const attempt = item.attempt + 1;
      patch(item.id, { status: 'UPLOADING', attempt, progress: 0, error: undefined });

      try {
        const presigned = await requestUploadUrl(item.albumId, {
          filename: file.name,
          contentType: file.type || 'application/octet-stream',
          fileSize: file.size,
        });
        if (!presigned.success || !presigned.data) {
          throw new Error(presigned.error || 'Failed to get upload URL');
        }
        const { mediaId, uploadUrl } = presigned.data;
        patch(item.id, { mediaId });

        const put = await uploadFileWithProgress(
          uploadUrl,
          file,
          file.type || 'application/octet-stream',
          (loaded, total) => patch(item.id, { progress: total > 0 ? loaded / total : null }),
        );
        if (!put.ok) throw new Error(`Upload failed: ${put.status}`);

        patch(item.id, { status: 'CONFIRMING', progress: null });
        const confirmed = await confirmUpload(item.albumId, mediaId, {});
        if (!confirmed.success || !confirmed.data) {
          throw new Error(confirmed.error || 'Confirm failed');
        }
        const media = confirmed.data as MediaDto;
        patch(item.id, { status: 'DONE', media });
        confirmedListeners.current.forEach((fn) => fn(media));
      } catch (err) {
        const message = err instanceof Error ? err.message : 'Upload failed';
        patch(item.id, { status: 'FAILED', error: message, progress: null });
        if (attempt < MAX_ATTEMPTS) {
          // Exponential backoff: 1s, 2s.
          await new Promise((r) => setTimeout(r, 1000 * 2 ** (attempt - 1)));
        }
      }
    }

    processingRef.current = false;
    checkSettled();
  }, [checkSettled, patch]);

  const enqueue = useCallback(
    (files: File[], albumId: string, albumTitle: string): File[] => {
      const rejected = files.filter((f) => !isMediaFile(f));
      const accepted = files.filter(isMediaFile);
      const entries = accepted.map((file) => {
        const item: UploadItem = {
          id: `upload-${nextId++}`,
          albumId,
          albumTitle,
          filename: file.name,
          previewUrl: file.type.startsWith('image/') ? URL.createObjectURL(file) : undefined,
          status: 'QUEUED',
          attempt: 0,
          progress: null,
        };
        return { item, file };
      });
      queueRef.current.push(...entries);
      setItems((prev) => {
        // A new batch replaces finished rows — otherwise the dock's counts
        // and progress bar start inflated by the previous batch's DONE rows.
        prev
          .filter((u) => u.status === 'DONE' && u.previewUrl)
          .forEach((u) => URL.revokeObjectURL(u.previewUrl!));
        return [...prev.filter((u) => u.status !== 'DONE'), ...entries.map((e) => e.item)];
      });
      void processQueue();
      return rejected;
    },
    [processQueue],
  );

  const retry = useCallback(
    (id: string) => {
      const entry = queueRef.current.find((q) => q.item.id === id);
      if (entry && entry.item.status === 'FAILED') {
        entry.item.attempt = 0;
        entry.reported = false;
        patch(id, { status: 'QUEUED', attempt: 0, error: undefined });
        void processQueue();
      } else {
        // No file to re-upload (defensive — Retry only renders for FAILED).
        setItems((prev) => prev.filter((u) => u.id !== id));
      }
    },
    [patch, processQueue],
  );

  const retryAll = useCallback(() => {
    let any = false;
    for (const entry of queueRef.current) {
      if (entry.item.status === 'FAILED') {
        entry.item.attempt = 0;
        entry.reported = false;
        patch(entry.item.id, { status: 'QUEUED', attempt: 0, error: undefined });
        any = true;
      }
    }
    if (any) void processQueue();
  }, [patch, processQueue]);

  const dismiss = useCallback((id: string) => {
    queueRef.current = queueRef.current.filter((q) => q.item.id !== id);
    setItems((prev) => {
      const item = prev.find((u) => u.id === id);
      if (item?.previewUrl) URL.revokeObjectURL(item.previewUrl);
      return prev.filter((u) => u.id !== id);
    });
  }, []);

  const clearFinished = useCallback(() => {
    setItems((prev) => {
      prev
        .filter((u) => u.status === 'DONE' && u.previewUrl)
        .forEach((u) => URL.revokeObjectURL(u.previewUrl!));
      return prev.filter((u) => u.status !== 'DONE');
    });
  }, []);

  const onBatchSettled = useCallback((fn: (s: UploadBatchSummary) => void) => {
    settledListeners.current.add(fn);
    return () => settledListeners.current.delete(fn);
  }, []);

  const onMediaConfirmed = useCallback((fn: (m: MediaDto) => void) => {
    confirmedListeners.current.add(fn);
    return () => confirmedListeners.current.delete(fn);
  }, []);

  const value = useMemo(
    () => ({ items, enqueue, retry, retryAll, dismiss, clearFinished, onBatchSettled, onMediaConfirmed }),
    [items, enqueue, retry, retryAll, dismiss, clearFinished, onBatchSettled, onMediaConfirmed],
  );

  return <UploadContext.Provider value={value}>{children}</UploadContext.Provider>;
}

export function useUploads(): UploadContextType {
  const ctx = useContext(UploadContext);
  if (!ctx) throw new Error('useUploads must be used within UploadProvider');
  return ctx;
}
