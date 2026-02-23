import { UploadState } from '../hooks/useUploadManager';

interface UploadProgressProps {
  uploads: UploadState[];
  albumId: string;
}

function statusText(status: UploadState['status']): string {
  switch (status) {
    case 'QUEUED': return 'Queued';
    case 'UPLOADING': return 'Uploading...';
    case 'CONFIRMING': return 'Processing...';
    case 'DONE': return 'Done';
    case 'FAILED': return 'Failed';
  }
}

function statusIcon(status: UploadState['status']): string {
  switch (status) {
    case 'QUEUED': return '\u25CB';    // ○
    case 'UPLOADING': return '\u25D4'; // ◔
    case 'CONFIRMING': return '\u25D4';
    case 'DONE': return '\u2713';      // ✓
    case 'FAILED': return '\u2717';    // ✗
  }
}

export function UploadProgress({ uploads, albumId }: UploadProgressProps) {
  const albumUploads = uploads.filter((u) => u.albumId === albumId);
  const active = albumUploads.filter((u) => u.status !== 'DONE');

  if (active.length === 0) return null;

  return (
    <div style={{ marginBottom: '1rem' }}>
      {active.map((upload) => (
        <div
          key={upload.id}
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '0.75rem',
            padding: '0.5rem 0.75rem',
            borderBottom: '1px solid #eee',
            fontSize: '0.875rem',
          }}
        >
          <span style={{ color: upload.status === 'FAILED' ? '#d32f2f' : upload.status === 'DONE' ? '#2e7d32' : '#666' }}>
            {statusIcon(upload.status)}
          </span>
          <span style={{ flex: 1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            {upload.filename}
          </span>
          <span style={{ color: '#999', fontSize: '0.75rem' }}>
            {statusText(upload.status)}
            {upload.attempt > 1 && ` (attempt ${upload.attempt}/3)`}
          </span>
        </div>
      ))}
    </div>
  );
}
