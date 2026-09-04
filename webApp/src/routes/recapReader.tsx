/**
 * /r/:shareToken — the recap reader (Doc 3 § s-recaps, "The recap reader").
 *
 * Public, unauthenticated, no app chrome: a full-bleed cover with the title
 * over its lower third, then one plate per screen, then a closing panel with
 * everyone who contributed. It is a document, so it also just scrolls —
 * the keys are a courtesy, not the only way through.
 */

import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from '@tanstack/react-router';
import { getPublicRecap, type RecapViewDto } from '../api/recaps';
import { useLocale } from '../i18n/LocaleProvider';
import { formatDayShort } from '../lib/format';
import { AvatarStack } from '../ui/Avatar';
import { Button } from '../ui/Button';
import { Plate } from '../ui/Plate';
import { Delayed, Skeleton } from '../ui/Skeleton';
import { useToast } from '../ui/Toast';
import { useDocumentTitle, usePrefersReducedMotion } from '../ui/hooks';
import styles from './recapReader.module.css';

export function RecapReaderPage() {
  const { shareToken } = useParams({ strict: false }) as { shareToken: string };
  const navigate = useNavigate();
  const { t, lang } = useLocale();
  const { toast } = useToast();
  const reducedMotion = usePrefersReducedMotion();

  const [view, setView] = useState<RecapViewDto | null>(null);
  const [status, setStatus] = useState<'loading' | 'ready' | 'missing'>('loading');

  const sectionsRef = useRef<(HTMLElement | null)[]>([]);
  const indexRef = useRef(0);

  useDocumentTitle(view ? `${view.recap.title} · ${t('app.name')}` : null);

  useEffect(() => {
    let cancelled = false;
    void getPublicRecap(shareToken)
      .catch(() => null)
      .then((resp) => {
        if (cancelled) return;
        if (resp?.success && resp.data) {
          setView(resp.data);
          setStatus('ready');
        } else {
          setStatus('missing');
        }
      });
    return () => {
      cancelled = true;
    };
  }, [shareToken]);

  // The section in view is the one the keys move from.
  useEffect(() => {
    if (status !== 'ready') return;
    const sections = sectionsRef.current.filter((el): el is HTMLElement => !!el);
    if (sections.length === 0) return;
    const observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (!entry.isIntersecting) continue;
          const i = sections.indexOf(entry.target as HTMLElement);
          if (i >= 0) indexRef.current = i;
        }
      },
      { threshold: 0.5 },
    );
    sections.forEach((el) => observer.observe(el));
    return () => observer.disconnect();
  }, [status, view]);

  useEffect(() => {
    if (status !== 'ready') return;
    const onKey = (event: KeyboardEvent) => {
      if (event.metaKey || event.ctrlKey || event.altKey) return;
      const target = event.target as HTMLElement | null;
      if (target && (target.isContentEditable || /^(INPUT|TEXTAREA|SELECT)$/.test(target.tagName))) {
        return;
      }
      if (event.key === 'Escape') {
        event.preventDefault();
        if (window.history.length > 1) window.history.back();
        else void navigate({ to: '/' });
        return;
      }
      let step = 0;
      if (event.key === 'ArrowDown' || event.key === ' ') step = 1;
      else if (event.key === 'ArrowUp') step = -1;
      else return;
      const sections = sectionsRef.current.filter((el): el is HTMLElement => !!el);
      if (sections.length === 0) return;
      event.preventDefault();
      const next = Math.min(Math.max(indexRef.current + step, 0), sections.length - 1);
      indexRef.current = next;
      sections[next].scrollIntoView({
        behavior: reducedMotion ? 'auto' : 'smooth',
        block: 'start',
      });
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [status, reducedMotion, navigate]);

  const share = useCallback(async () => {
    try {
      await navigator.clipboard.writeText(window.location.href);
      toast({ kind: 'success', title: t('recaps.reader.copied') });
    } catch {
      toast({ kind: 'error', title: t('common.errorGeneric') });
    }
  }, [t, toast]);

  if (status === 'loading') {
    return (
      <Delayed>
        <Skeleton height="100dvh" radius={0} />
      </Delayed>
    );
  }

  if (status === 'missing' || !view) {
    return (
      <div className={styles.notFound}>
        <h1 className="t-display-s">{t('recaps.notFound')}</h1>
        <p>
          <Link to="/">{t('notFound.cta.anon')}</Link>
        </p>
      </div>
    );
  }

  const { recap, media, contributors, ownerDisplayName } = view;
  const setSection = (i: number) => (el: HTMLElement | null) => {
    sectionsRef.current[i] = el;
  };

  return (
    <div className={styles.reader}>
      {/* The cover carries its own darkroom so the title is bone on the plate. */}
      <section ref={setSection(0)} className={styles.cover} data-theme="dark">
        {recap.coverUrl && <img src={recap.coverUrl} alt="" className={styles.coverImg} />}
        <div className={styles.coverScrim} aria-hidden="true" />
        <div className={styles.coverText}>
          <h1 className={`${styles.coverTitle} t-display-xl`}>{recap.title}</h1>
          <p className={`${styles.coverMeta} t-meta`}>{recap.albumTitle}</p>
        </div>
      </section>

      {media.map((item, i) => {
        const ratio = item.width && item.height ? item.width / item.height : 4 / 3;
        return (
          <section key={item.id} ref={setSection(i + 1)} className={styles.plateSection}>
            <figure className={styles.figure}>
              <Plate
                src={item.type === 'VIDEO' ? (item.thumbnailUrl ?? item.url) : item.url}
                alt=""
                isVideo={item.type === 'VIDEO'}
                aspectRatio={
                  item.width && item.height ? `${item.width} / ${item.height}` : '4 / 3'
                }
                style={{ maxInlineSize: `calc(88vh * ${ratio})` }}
              />
              {/* The public payload carries no uploader, so the caption is the day. */}
              <figcaption className={`${styles.caption} t-meta`}>
                {formatDayShort(item.takenAt ?? item.uploadedAt, lang)}
              </figcaption>
            </figure>
          </section>
        );
      })}

      <section ref={setSection(media.length + 1)} className={styles.closing}>
        <span className="t-label">{t('recaps.reader.contributors')}</span>
        <AvatarStack
          people={contributors}
          totalCount={contributors.length}
          size="lg"
          asDecoration
        />
        <p className={`${styles.owner} t-body`}>{ownerDisplayName}</p>
        <Button variant="secondary" onClick={() => void share()}>
          {t('recaps.reader.share')}
        </Button>
      </section>
    </div>
  );
}
