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

function statusColor(status: UploadState['status']): string {
  switch (status) {
    case 'QUEUED': return 'var(--color-muted-slate)';
    case 'UPLOADING': return 'var(--color-sky-blue)';
    case 'CONFIRMING': return 'var(--color-sky-blue)';
    case 'DONE': return '#2e9e50';
    case 'FAILED': return 'var(--color-soft-red)';
  }
}

export function UploadProgress({ uploads, albumId }: UploadProgressProps) {
  const albumUploads = uploads.filter((u) => u.albumId === albumId);
  const active = albumUploads.filter((u) => u.status !== 'DONE');

  if (active.length === 0) return null;

  return (
    <div style={{ marginBottom: '1.5rem' }}>
      {active.map((upload) => (
        <div
          key={upload.id}
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '0.75rem',
            padding: '0.6rem 0.75rem',
            borderBottom: '1px solid rgba(27,127,204,0.06)',
            fontFamily: 'var(--font-body)',
            fontSize: '0.85rem',
          }}
        >
          <span style={{ color: statusColor(upload.status), fontSize: '1rem' }}>
            {statusIcon(upload.status)}
          </span>
          <span style={{
            flex: 1,
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            whiteSpace: 'nowrap',
            color: 'var(--color-deep-navy)',
          }}>
            {upload.filename}
          </span>
          <span style={{
            color: 'var(--color-muted-slate)',
            fontSize: '0.75rem',
            fontWeight: 500,
          }}>
            {statusText(upload.status)}
            {upload.attempt > 1 && ` (attempt ${upload.attempt}/3)`}
          </span>
        </div>
      ))}
    </div>
  );
}
