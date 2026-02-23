import { useState, useRef, useCallback } from 'react';
import { requestUploadUrl, uploadFile, confirmUpload } from '../api/media';

export type UploadStatus = 'QUEUED' | 'UPLOADING' | 'CONFIRMING' | 'DONE' | 'FAILED';

export interface UploadState {
  id: string;
  albumId: string;
  filename: string;
  status: UploadStatus;
  attempt: number;
  mediaId?: string;
  error?: string;
}

interface QueueItem {
  state: UploadState;
  file: File;
}

let nextId = 1;

export function useUploadManager() {
  const [uploads, setUploads] = useState<UploadState[]>([]);
  const processingRef = useRef(false);
  const queueRef = useRef<QueueItem[]>([]);

  function updateUpload(id: string, patch: Partial<UploadState>) {
    setUploads((prev) => prev.map((u) => (u.id === id ? { ...u, ...patch } : u)));
    const item = queueRef.current.find((q) => q.state.id === id);
    if (item) {
      Object.assign(item.state, patch);
    }
  }

  async function processQueue() {
    if (processingRef.current) return;
    processingRef.current = true;

    while (true) {
      const next = queueRef.current.find(
        (q) => q.state.status === 'QUEUED' || (q.state.status === 'FAILED' && q.state.attempt < 3)
      );
      if (!next) break;

      const { state, file } = next;
      const attempt = state.attempt + 1;
      updateUpload(state.id, { status: 'UPLOADING', attempt });

      try {
        // Step 1: Request presigned upload URL
        const presignedResp = await requestUploadUrl(state.albumId, {
          filename: file.name,
          contentType: file.type || 'application/octet-stream',
          fileSize: file.size,
        });

        if (!presignedResp.success || !presignedResp.data) {
          throw new Error(presignedResp.message || 'Failed to get upload URL');
        }

        const { mediaId, uploadUrl } = presignedResp.data;
        updateUpload(state.id, { mediaId });

        // Step 2: Upload file to presigned URL
        const uploadResp = await uploadFile(uploadUrl, file, file.type || 'application/octet-stream');
        if (!uploadResp.ok) {
          throw new Error(`Upload failed: ${uploadResp.status}`);
        }

        // Step 3: Confirm upload
        updateUpload(state.id, { status: 'CONFIRMING' });
        const confirmResp = await confirmUpload(state.albumId, mediaId, {});
        if (!confirmResp.success) {
          throw new Error(confirmResp.message || 'Confirm failed');
        }

        updateUpload(state.id, { status: 'DONE' });
      } catch (err: any) {
        const message = err?.message || 'Upload failed';
        if (attempt >= 3) {
          updateUpload(state.id, { status: 'FAILED', error: message });
        } else {
          updateUpload(state.id, { status: 'FAILED', error: message });
          // Exponential backoff: 1s, 2s, 4s
          await new Promise((r) => setTimeout(r, 1000 * Math.pow(2, attempt - 1)));
        }
      }
    }

    processingRef.current = false;
  }

  const enqueue = useCallback((files: File[], albumId: string) => {
    const newItems: QueueItem[] = files.map((file) => {
      const id = `upload-${nextId++}`;
      return {
        state: { id, albumId, filename: file.name, status: 'QUEUED' as UploadStatus, attempt: 0 },
        file,
      };
    });

    queueRef.current.push(...newItems);
    setUploads((prev) => [...prev, ...newItems.map((item) => item.state)]);
    processQueue();
  }, []);

  const clearCompleted = useCallback(() => {
    queueRef.current = queueRef.current.filter((q) => q.state.status !== 'DONE');
    setUploads((prev) => prev.filter((u) => u.status !== 'DONE'));
  }, []);

  return { uploads, enqueue, clearCompleted };
}
