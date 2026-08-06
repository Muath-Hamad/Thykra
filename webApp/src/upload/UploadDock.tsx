import { useMemo, useState } from 'react';
import { useT } from '../i18n/LocaleProvider';
import { useUploads, type UploadItem } from './UploadProvider';
import styles from './UploadDock.module.css';

/**
 * The dock — pinned bottom-trailing (bottom-centre above the tab bar on
 * mobile). Collapsed it is one summary row; expanded it lists files, six
 * visible, the rest scrolling. Lives at layout level so it survives
 * navigation.
 */
export function UploadDock() {
  const t = useT();
  const { items, retry, retryAll, dismiss } = useUploads();
  const [expanded, setExpanded] = useState(false);

  const active = items;
  const counts = useMemo(() => {
    const done = active.filter((i) => i.status === 'DONE').length;
    const failed = active.filter((i) => i.status === 'FAILED' && i.attempt >= 3).length;
    const inFlight = active.length - done - failed;
    return { done, failed, inFlight, total: active.length };
  }, [active]);

  if (active.length === 0) return null;

  const allSettled = counts.inFlight === 0;
  const fraction = counts.total > 0 ? counts.done / counts.total : 0;

  const summaryTitle = allSettled
    ? counts.failed > 0
      ? t('upload.partial', { failed: counts.failed, total: counts.total })
      : t('upload.done.count', { n: counts.done })
    : counts.total === 1
      ? t('upload.adding.one')
      : t('upload.adding', { n: counts.total });

  const summarySub = allSettled
    ? active[0]?.albumTitle
    : counts.failed > 0
      ? t('upload.progressLine', {
          done: counts.done,
          failed: counts.failed,
          left: counts.inFlight,
        })
      : t('upload.progressLine.noFail', { done: counts.done, left: counts.inFlight });

  return (
    <div className={styles.dock} role="status" aria-label={summaryTitle}>
      <button
        type="button"
        className={styles.summary}
        onClick={() => setExpanded((e) => !e)}
        aria-expanded={expanded}
      >
        {allSettled ? (
          counts.failed > 0 ? (
            <span className={styles.failMark} aria-hidden="true">!</span>
          ) : (
            <span className={styles.doneMark} aria-hidden="true">✓</span>
          )
        ) : (
          <span className={styles.spinner} aria-hidden="true" />
        )}
        <span className={styles.summaryText}>
          <span className={styles.summaryTitle}>{summaryTitle}</span>
          <span className={styles.summarySub} style={{ display: 'block' }}>
            {summarySub}
          </span>
        </span>
        <span
          className={[styles.chev, expanded ? styles.chevOpen : ''].filter(Boolean).join(' ')}
          aria-hidden="true"
        >
          ▾
        </span>
      </button>

      {/* Queue bar — a real fraction: completed ÷ total. */}
      {!allSettled && (
        <div className={styles.queueBar}>
          <div className={styles.queueFill} style={{ width: `${fraction * 100}%` }} />
        </div>
      )}

      {expanded && (
        <ul className={styles.list}>
          {active.map((item) => (
            <UploadRow key={item.id} item={item} onRetry={retry} onDismiss={dismiss} />
          ))}
        </ul>
      )}

      <div className={styles.footer}>
        <span className={styles.footerNote}>
          {allSettled ? active[0]?.albumTitle : t('upload.keepTabOpen')}
        </span>
        {counts.failed > 0 ? (
          <button type="button" className={styles.footerAction} onClick={retryAll}>
            {t('common.retryAll')}
          </button>
        ) : allSettled ? (
          <button
            type="button"
            className={styles.footerAction}
            onClick={() => active.forEach((i) => dismiss(i.id))}
          >
            {t('upload.dismiss')}
          </button>
        ) : null}
      </div>
    </div>
  );
}

function UploadRow({
  item,
  onRetry,
  onDismiss,
}: {
  item: UploadItem;
  onRetry: (id: string) => void;
  onDismiss: (id: string) => void;
}) {
  const t = useT();
  const failedFinal = item.status === 'FAILED' && item.attempt >= 3;
  const rowClass = [
    styles.row,
    item.status === 'UPLOADING' ? styles.rowUploading : '',
    failedFinal ? styles.rowFailed : '',
  ]
    .filter(Boolean)
    .join(' ');

  return (
    <li className={rowClass}>
      {item.previewUrl ? (
        <img className={styles.thumb} src={item.previewUrl} alt="" />
      ) : (
        <span className={styles.thumb} aria-hidden="true" />
      )}
      <span className={styles.rowMain}>
        <span className={styles.rowName}>{item.filename}</span>
        {item.status === 'UPLOADING' && item.progress !== null ? (
          <span className={styles.fileBar}>
            <span className={styles.fileFill} style={{ width: `${item.progress * 100}%` }} />
          </span>
        ) : item.status === 'QUEUED' || item.status === 'CONFIRMING' ? (
          <span className={styles.indeterminate} />
        ) : failedFinal ? (
          <span className={styles.rowError}>{t('upload.failedAfter')}</span>
        ) : item.status === 'FAILED' ? (
          <span className={styles.rowAttempt}>{t('upload.retryOf', { n: item.attempt + 1 })}</span>
        ) : null}
      </span>
      {item.status === 'DONE' ? (
        <span className={`${styles.rowStatus} ${styles.stDone}`}>{t('upload.status.done')}</span>
      ) : item.status === 'UPLOADING' ? (
        <span className={`${styles.rowStatus} ${styles.stUploading}`}>
          {t('upload.status.uploading')}
        </span>
      ) : item.status === 'CONFIRMING' ? (
        <span className={`${styles.rowStatus} ${styles.stConfirming}`}>
          {t('upload.status.confirming')}
        </span>
      ) : item.status === 'QUEUED' ? (
        <span className={`${styles.rowStatus} ${styles.stQueued}`}>{t('upload.status.queued')}</span>
      ) : failedFinal ? (
        <button type="button" className={styles.rowRetry} onClick={() => onRetry(item.id)}>
          {t('common.retry')}
        </button>
      ) : null}
      {(item.status === 'DONE' || failedFinal) && (
        <button
          type="button"
          className={styles.dismiss}
          onClick={() => onDismiss(item.id)}
          aria-label={t('upload.dismiss')}
        >
          ✕
        </button>
      )}
    </li>
  );
}

/** Full-viewport dragover affordance. */
export function DropOverlay({ albumTitle }: { albumTitle: string }) {
  const t = useT();
  return (
    <div className={styles.dropOverlay} aria-hidden="true">
      <div className={styles.dropPanel}>
        <div className={styles.dropTitle}>{t('upload.dropTitle', { title: albumTitle })}</div>
        <div className={styles.dropSub}>{t('upload.dropBody')}</div>
      </div>
    </div>
  );
}
