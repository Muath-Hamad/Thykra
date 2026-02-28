import { useState, useMemo } from 'react';
import { useNavigate } from '@tanstack/react-router';

// ─── i18n ────────────────────────────────────────────────
type Lang = 'en' | 'ar';

const t = {
  en: {
    about: 'About',
    features: 'Features',
    signIn: 'Sign In',
    tagline: 'Shared Photo Albums',
    heroLine1: 'Travel Together.',
    heroLine2: 'Remember',
    heroLine3: 'Forever.',
    heroDesc: 'A cinematic way to collect, curate, and relive your most cherished moments — together with the people who were there.',
    heroCta: 'Begin Your Story',
    scrollHint: 'Scroll to explore',
    feat1Title: 'Shared Albums',
    feat1Desc: 'Create collaborative albums and invite your travel companions. Every perspective, every angle — one beautiful collection.',
    feat2Title: 'Instant Upload',
    feat2Desc: 'Drag, drop, done. Queue-based uploads with automatic retries so you never lose a single shot.',
    feat3Title: 'Cross-Platform',
    feat3Desc: 'Android, iOS, and Web. Your memories sync everywhere, beautifully presented on every screen.',
    showcaseH1: 'Every Trip',
    showcaseH2: 'Deserves',
    showcaseH3: 'Its Own',
    showcaseH4: 'Gallery',
    showcaseDesc: 'Organize by journey, share by link, and relive through a full-screen immersive viewer. Your photos, presented like they deserve.',
    statAlbums: 'Albums',
    statRes: 'Resolution',
    statPlat: 'Platforms',
    ctaH1: 'Start Collecting',
    ctaH2: 'Moments',
    ctaDesc: 'Free to use. No ads. Just your memories.',
    ctaCta: 'Get Started',
    privacy: 'Privacy',
    terms: 'Terms',
    verticalText: 'Shared memories — since 2024',
    albumView: 'Album view',
    detail: 'Detail',
  },
  ar: {
    about: 'عن التطبيق',
    features: 'المميزات',
    signIn: 'تسجيل الدخول',
    tagline: 'ألبومات صور مشتركة',
    heroLine1: 'سافروا معاً.',
    heroLine2: 'تذكّروا',
    heroLine3: 'للأبد.',
    heroDesc: 'طريقة سينمائية لجمع وتنظيم واستعادة أجمل لحظاتكم — مع الأشخاص الذين كانوا هناك.',
    heroCta: 'ابدأ قصتك',
    scrollHint: 'مرر للاستكشاف',
    feat1Title: 'ألبومات مشتركة',
    feat1Desc: 'أنشئ ألبومات تعاونية وادعُ رفاق السفر. كل منظور، كل زاوية — مجموعة واحدة جميلة.',
    feat2Title: 'رفع فوري',
    feat2Desc: 'اسحب وأفلت وانتهى. رفع تلقائي مع إعادة المحاولة حتى لا تفقد أي لقطة.',
    feat3Title: 'جميع المنصات',
    feat3Desc: 'أندرويد وآيفون والويب. ذكرياتك متزامنة في كل مكان، معروضة بشكل جميل على كل شاشة.',
    showcaseH1: 'كل رحلة',
    showcaseH2: 'تستحق',
    showcaseH3: 'معرضها',
    showcaseH4: 'الخاص',
    showcaseDesc: 'نظّم حسب الرحلة، شارك برابط، واستعد اللحظات عبر عارض غامر بملء الشاشة. صورك، معروضة كما تستحق.',
    statAlbums: 'ألبومات',
    statRes: 'الدقة',
    statPlat: 'منصات',
    ctaH1: 'ابدأ بجمع',
    ctaH2: 'اللحظات',
    ctaDesc: 'مجاني للاستخدام. بدون إعلانات. فقط ذكرياتك.',
    ctaCta: 'ابدأ الآن',
    privacy: 'الخصوصية',
    terms: 'الشروط',
    verticalText: 'ذكريات مشتركة — منذ ٢٠٢٤',
    albumView: 'عرض الألبوم',
    detail: 'التفاصيل',
  },
};

// ─── Image pools per location (Unsplash) ─────────────────
interface LocationImage {
  url: string;
  label: { en: string; ar: string };
}

const imagePool: LocationImage[][] = [
  // Pool A — Mediterranean / Middle East
  [
    { url: 'https://images.unsplash.com/photo-1570077188670-e3a8d69ac5ff?w=800&q=80', label: { en: 'Santorini, Greece', ar: 'سانتوريني، اليونان' } },
    { url: 'https://images.unsplash.com/photo-1533105079780-92b9be482077?w=800&q=80', label: { en: 'Santorini — 2024', ar: 'سانتوريني — ٢٠٢٤' } },
    { url: 'https://images.unsplash.com/photo-1613395877344-13d4a8e0d49e?w=800&q=80', label: { en: 'Oia, Santorini', ar: 'أويا، سانتوريني' } },
  ],
  // Pool B — East Asia
  [
    { url: 'https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?w=800&q=80', label: { en: 'Kyoto, Japan', ar: 'كيوتو، اليابان' } },
    { url: 'https://images.unsplash.com/photo-1528360983277-13d401cdc186?w=800&q=80', label: { en: 'Fushimi Inari, Kyoto', ar: 'فوشيمي إناري، كيوتو' } },
    { url: 'https://images.unsplash.com/photo-1545569341-9eb8b30979d9?w=800&q=80', label: { en: 'Tokyo at Night', ar: 'طوكيو ليلاً' } },
  ],
  // Pool C — South America
  [
    { url: 'https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=800&q=80', label: { en: 'Patagonia, Argentina', ar: 'باتاغونيا، الأرجنتين' } },
    { url: 'https://images.unsplash.com/photo-1476514525535-07fb3b4ae5f1?w=800&q=80', label: { en: 'Lake Pehoé, Chile', ar: 'بحيرة بيهوي، تشيلي' } },
    { url: 'https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=800&q=80', label: { en: 'Torres del Paine', ar: 'توريس ديل باينه' } },
  ],
  // Pool D — Middle East / North Africa
  [
    { url: 'https://images.unsplash.com/photo-1539768942893-daf53e736b68?w=800&q=80', label: { en: 'Marrakech, Morocco', ar: 'مراكش، المغرب' } },
    { url: 'https://images.unsplash.com/photo-1548018560-c7196c4d79c6?w=800&q=80', label: { en: 'Chefchaouen, Morocco', ar: 'شفشاون، المغرب' } },
    { url: 'https://images.unsplash.com/photo-1558642452-9d2a7deb7f62?w=800&q=80', label: { en: 'Dubai Creek', ar: 'خور دبي' } },
  ],
  // Pool E — Europe
  [
    { url: 'https://images.unsplash.com/photo-1502602898657-3e91760cbb34?w=800&q=80', label: { en: 'Paris, France', ar: 'باريس، فرنسا' } },
    { url: 'https://images.unsplash.com/photo-1523531294919-4bcd7c65e216?w=800&q=80', label: { en: 'Amalfi Coast, Italy', ar: 'ساحل أمالفي، إيطاليا' } },
    { url: 'https://images.unsplash.com/photo-1516483638261-f4dbaf036963?w=800&q=80', label: { en: 'Tuscany, Italy', ar: 'توسكانا، إيطاليا' } },
  ],
  // Pool F — Southeast Asia
  [
    { url: 'https://images.unsplash.com/photo-1552465011-b4e21bf6e79a?w=800&q=80', label: { en: 'Bali, Indonesia', ar: 'بالي، إندونيسيا' } },
    { url: 'https://images.unsplash.com/photo-1506665531195-3566af2b4dfa?w=800&q=80', label: { en: 'Ha Long Bay, Vietnam', ar: 'خليج ها لونغ، فيتنام' } },
    { url: 'https://images.unsplash.com/photo-1537996194471-e657df975ab4?w=800&q=80', label: { en: 'Ubud Rice Terraces', ar: 'مدرجات أرز أوبود' } },
  ],
];

function shuffle<T>(arr: T[]): T[] {
  const a = [...arr];
  for (let i = a.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [a[i], a[j]] = [a[j], a[i]];
  }
  return a;
}

function pickRandomImages(): { hero: LocationImage[]; showcase: LocationImage[] } {
  const shuffled = shuffle(imagePool);
  // Pick one random image from each of 5 pools
  const hero = [0, 1, 2].map(i => {
    const pool = shuffled[i];
    return pool[Math.floor(Math.random() * pool.length)];
  });
  const showcase = [3, 4].map(i => {
    const pool = shuffled[i];
    return pool[Math.floor(Math.random() * pool.length)];
  });
  return { hero, showcase };
}

// ─── Component ───────────────────────────────────────────
export function LandingPage1() {
  const navigate = useNavigate();
  const [lang, setLang] = useState<Lang>('en');
  const s = t[lang];
  const isRTL = lang === 'ar';

  const images = useMemo(() => pickRandomImages(), []);

  const toggleLang = () => setLang(prev => prev === 'en' ? 'ar' : 'en');

  return (
    <>
      <style>{`
        :root {
          --l1-bg: #FDF8EF;
          --l1-fg: #1A1A2E;
          --l1-accent: #1B7FCC;
          --l1-accent-hover: #4FA3E0;
          --l1-muted: #5C6B7A;
          --l1-surface: #F5E6C8;
          --l1-orange: #F47820;
        }

        .l1-page {
          background: var(--l1-bg);
          color: var(--l1-fg);
          font-family: var(--font-body);
          min-height: 100vh;
          overflow-x: hidden;
          position: relative;
        }

        .l1-page.rtl {
          direction: rtl;
          font-family: 'Noto Kufi Arabic', var(--font-body);
        }

        .l1-page.rtl .l1-hero-title,
        .l1-page.rtl .l1-feature-title,
        .l1-page.rtl .l1-showcase-content h2,
        .l1-page.rtl .l1-footer-cta h2 {
          font-family: 'Noto Kufi Arabic', serif;
        }

        .l1-nav {
          position: fixed;
          top: 0;
          left: 0;
          right: 0;
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: 0 3rem;
          height: 64px;
          z-index: 50;
          background: rgba(253, 248, 239, 0.92);
          backdrop-filter: blur(12px);
          -webkit-backdrop-filter: blur(12px);
          box-shadow: 0 1px 8px rgba(27, 127, 204, 0.06);
        }

        .l1-logo {
          font-family: var(--font-display);
          font-size: 1.4rem;
          font-weight: 800;
          letter-spacing: 0.02em;
          color: var(--l1-fg);
        }

        .l1-nav-links {
          display: flex;
          gap: 2rem;
          align-items: center;
        }

        .l1-nav-link {
          font-size: 0.85rem;
          font-weight: 500;
          color: var(--l1-muted);
          text-decoration: none;
          transition: color 0.3s;
          cursor: pointer;
          background: none;
          border: none;
          font-family: inherit;
        }

        .l1-nav-link:hover { color: var(--l1-accent); }

        /* LANG TOGGLE */
        .l1-lang-toggle {
          display: flex;
          align-items: center;
          gap: 0.5rem;
          background: var(--l1-surface);
          border: 1px solid rgba(27,127,204,0.15);
          border-radius: 100px;
          padding: 0.35rem 0.5rem;
          cursor: pointer;
          transition: all 0.3s;
        }
        .l1-lang-toggle:hover {
          background: rgba(27,127,204,0.08);
          border-color: var(--l1-accent);
        }

        .l1-lang-opt {
          font-size: 0.65rem;
          font-weight: 600;
          letter-spacing: 0.1em;
          text-transform: uppercase;
          color: var(--l1-muted);
          padding: 0.25rem 0.6rem;
          border-radius: 100px;
          transition: all 0.3s;
          line-height: 1;
        }
        .l1-lang-opt.active {
          background: var(--l1-accent);
          color: #fff;
        }

        /* HERO */
        .l1-hero {
          min-height: 100vh;
          display: grid;
          grid-template-columns: 1fr 1fr;
          position: relative;
        }

        .l1-page.rtl .l1-hero { direction: rtl; }

        .l1-hero-left {
          display: flex;
          flex-direction: column;
          justify-content: center;
          padding: 8rem 4rem 4rem 6rem;
          position: relative;
          z-index: 2;
        }
        .l1-page.rtl .l1-hero-left {
          padding: 8rem 6rem 4rem 4rem;
        }

        .l1-hero-tagline {
          font-size: 0.75rem;
          letter-spacing: 0.2em;
          text-transform: uppercase;
          color: var(--l1-accent);
          margin-bottom: 2rem;
          font-weight: 600;
          animation: l1FadeUp 1s ease both;
        }
        .l1-page.rtl .l1-hero-tagline { letter-spacing: 0.05em; }

        .l1-hero-title {
          font-family: var(--font-display);
          font-size: clamp(3.5rem, 6vw, 6rem);
          font-weight: 900;
          line-height: 1.05;
          margin: 0 0 2rem;
          color: var(--l1-fg);
          animation: l1FadeUp 1s ease 0.15s both;
        }

        .l1-page.rtl .l1-hero-title {
          font-size: clamp(2.8rem, 5vw, 5rem);
          line-height: 1.3;
        }

        .l1-hero-title em {
          font-style: italic;
          font-weight: 400;
          color: var(--l1-accent);
        }
        .l1-page.rtl .l1-hero-title em {
          font-style: normal;
          color: var(--l1-accent);
        }

        .l1-hero-desc {
          font-size: 1rem;
          line-height: 1.8;
          color: var(--l1-muted);
          max-width: 420px;
          margin-bottom: 3rem;
          font-weight: 400;
          animation: l1FadeUp 1s ease 0.3s both;
        }
        .l1-page.rtl .l1-hero-desc { line-height: 2; }

        .l1-hero-cta {
          display: inline-flex;
          align-items: center;
          gap: 0.8rem;
          background: var(--l1-accent);
          border: none;
          color: #fff;
          padding: 1rem 2.5rem;
          font-size: 0.85rem;
          font-weight: 600;
          letter-spacing: 0.02em;
          cursor: pointer;
          font-family: var(--font-display);
          border-radius: var(--radius-md);
          transition: all 0.3s;
          animation: l1FadeUp 1s ease 0.45s both;
          box-shadow: var(--shadow-sm);
        }
        .l1-hero-cta:hover {
          background: var(--l1-accent-hover);
          box-shadow: var(--shadow-md);
          transform: translateY(-2px);
        }
        .l1-page.rtl .l1-hero-cta {
          font-family: 'Noto Kufi Arabic', sans-serif;
          letter-spacing: 0;
          font-size: 0.9rem;
        }

        .l1-hero-cta svg { transition: transform 0.3s; }
        .l1-hero-cta:hover svg { transform: translateX(4px); }
        .l1-page.rtl .l1-hero-cta:hover svg { transform: translateX(-4px) scaleX(-1); }
        .l1-page.rtl .l1-hero-cta svg { transform: scaleX(-1); }

        /* Footer CTA uses orange */
        .l1-footer-cta .l1-hero-cta {
          background: var(--l1-orange);
          box-shadow: var(--shadow-orange);
        }
        .l1-footer-cta .l1-hero-cta:hover {
          background: #e06910;
        }

        .l1-hero-right {
          position: relative;
          overflow: hidden;
          animation: l1FadeIn 1.5s ease 0.3s both;
        }

        .l1-hero-img-stack {
          position: absolute;
          inset: 6rem 3rem 3rem 0;
          display: grid;
          grid-template: 1fr 1fr / 1fr 1fr;
          gap: 1rem;
        }
        .l1-page.rtl .l1-hero-img-stack {
          inset: 6rem 0 3rem 3rem;
        }

        .l1-hero-img {
          border-radius: var(--radius-md);
          overflow: hidden;
          position: relative;
          background: var(--l1-surface);
          box-shadow: var(--shadow-sm);
        }

        .l1-hero-img img {
          width: 100%;
          height: 100%;
          object-fit: cover;
          display: block;
          transition: transform 0.6s;
        }

        .l1-hero-img:hover img {
          transform: scale(1.05);
        }

        .l1-hero-img::after {
          content: '';
          position: absolute;
          inset: 0;
          background: linear-gradient(180deg, transparent 60%, rgba(26,26,46,0.25) 100%);
          pointer-events: none;
        }

        .l1-hero-img:nth-child(1) { grid-row: 1 / 3; }
        .l1-hero-img:nth-child(2) { transform: translateY(2rem); }
        .l1-hero-img:nth-child(3) { transform: translateY(-1rem); }

        .l1-hero-img-label {
          position: absolute;
          bottom: 1rem;
          left: 1rem;
          font-size: 0.65rem;
          font-weight: 500;
          letter-spacing: 0.1em;
          text-transform: uppercase;
          color: rgba(255,255,255,0.85);
          z-index: 2;
          text-shadow: 0 1px 4px rgba(0,0,0,0.4);
        }
        .l1-page.rtl .l1-hero-img-label {
          left: auto;
          right: 1rem;
          letter-spacing: 0.05em;
        }

        /* SCROLL LINE */
        .l1-scroll-line {
          position: absolute;
          bottom: 2rem;
          left: 6rem;
          display: flex;
          align-items: center;
          gap: 1rem;
          animation: l1FadeUp 1s ease 0.6s both;
        }
        .l1-page.rtl .l1-scroll-line {
          left: auto;
          right: 6rem;
        }

        .l1-scroll-line span {
          font-size: 0.7rem;
          letter-spacing: 0.15em;
          text-transform: uppercase;
          color: var(--l1-muted);
          font-weight: 500;
        }
        .l1-page.rtl .l1-scroll-line span { letter-spacing: 0.05em; }

        .l1-scroll-bar {
          width: 60px;
          height: 2px;
          background: rgba(27,127,204,0.2);
          position: relative;
          overflow: hidden;
          border-radius: 1px;
        }

        .l1-scroll-bar::after {
          content: '';
          position: absolute;
          top: 0;
          left: -100%;
          width: 100%;
          height: 100%;
          background: var(--l1-accent);
          animation: l1ScrollPulse 2s ease infinite;
        }

        /* FEATURES */
        .l1-features {
          padding: 8rem 6rem;
          display: grid;
          grid-template-columns: 1fr 1fr 1fr;
          gap: 3rem;
          position: relative;
        }

        .l1-features::before {
          content: '';
          position: absolute;
          top: 0;
          left: 6rem;
          right: 6rem;
          height: 1px;
          background: linear-gradient(90deg, transparent, rgba(27,127,204,0.2), transparent);
        }

        .l1-feature {
          animation: l1FadeUp 0.8s ease both;
          background: var(--l1-surface);
          border-radius: var(--radius-lg);
          padding: 2.5rem 2rem;
          box-shadow: var(--shadow-sm);
          transition: box-shadow 0.3s, transform 0.3s;
        }
        .l1-feature:hover {
          box-shadow: var(--shadow-md);
          transform: translateY(-3px);
        }
        .l1-feature:nth-child(2) { animation-delay: 0.15s; }
        .l1-feature:nth-child(3) { animation-delay: 0.3s; }

        .l1-feature-num {
          font-family: var(--font-display);
          font-size: 3rem;
          font-weight: 900;
          color: rgba(27,127,204,0.15);
          line-height: 1;
          margin-bottom: 1.5rem;
        }

        .l1-feature-title {
          font-family: var(--font-display);
          font-size: 1.4rem;
          font-weight: 700;
          margin: 0 0 1rem;
          color: var(--l1-fg);
        }

        .l1-feature-desc {
          font-size: 0.9rem;
          line-height: 1.8;
          color: var(--l1-muted);
          font-weight: 400;
        }
        .l1-page.rtl .l1-feature-desc { line-height: 2; }

        /* SHOWCASE */
        .l1-showcase {
          padding: 0 6rem 8rem;
          display: grid;
          grid-template-columns: 1.2fr 1fr;
          gap: 6rem;
          align-items: center;
        }

        .l1-showcase-visual {
          position: relative;
          height: 500px;
        }

        .l1-showcase-frame {
          position: absolute;
          overflow: hidden;
          border: 1px solid rgba(27,127,204,0.1);
          background: var(--l1-surface);
          border-radius: var(--radius-lg);
          box-shadow: var(--shadow-md);
        }

        .l1-showcase-frame img {
          width: 100%;
          height: 100%;
          object-fit: cover;
          display: block;
          transition: transform 0.8s;
        }

        .l1-showcase-frame:hover img {
          transform: scale(1.05);
        }

        .l1-showcase-frame:nth-child(1) {
          width: 65%;
          height: 80%;
          top: 0;
          left: 0;
          z-index: 2;
        }
        .l1-page.rtl .l1-showcase-frame:nth-child(1) { left: auto; right: 0; }

        .l1-showcase-frame:nth-child(2) {
          width: 55%;
          height: 70%;
          bottom: 0;
          right: 0;
          z-index: 1;
        }
        .l1-page.rtl .l1-showcase-frame:nth-child(2) { right: auto; left: 0; }

        .l1-showcase-frame-label {
          position: absolute;
          top: 1.5rem;
          left: 1.5rem;
          font-size: 0.65rem;
          font-weight: 500;
          letter-spacing: 0.1em;
          text-transform: uppercase;
          color: rgba(255,255,255,0.75);
          z-index: 3;
          text-shadow: 0 1px 4px rgba(0,0,0,0.4);
        }
        .l1-page.rtl .l1-showcase-frame-label {
          left: auto;
          right: 1.5rem;
          letter-spacing: 0.05em;
        }

        .l1-showcase-accent {
          position: absolute;
          width: 120px;
          height: 120px;
          border: 2px solid var(--l1-accent);
          top: 50%;
          left: 50%;
          transform: translate(-50%, -50%) rotate(45deg);
          opacity: 0.15;
          border-radius: 4px;
        }

        .l1-showcase-content h2 {
          font-family: var(--font-display);
          font-size: clamp(2rem, 3.5vw, 3rem);
          font-weight: 900;
          line-height: 1.15;
          margin: 0 0 1.5rem;
          color: var(--l1-fg);
        }
        .l1-page.rtl .l1-showcase-content h2 { line-height: 1.5; }

        .l1-showcase-content h2 em {
          font-style: italic;
          font-weight: 400;
          color: var(--l1-accent);
        }
        .l1-page.rtl .l1-showcase-content h2 em { font-style: normal; }

        .l1-showcase-content p {
          font-size: 0.95rem;
          line-height: 1.9;
          color: var(--l1-muted);
          margin-bottom: 2.5rem;
          font-weight: 400;
        }
        .l1-page.rtl .l1-showcase-content p { line-height: 2.1; }

        .l1-showcase-stats {
          display: flex;
          gap: 3rem;
        }

        .l1-stat-num {
          font-family: var(--font-display);
          font-size: 2.5rem;
          font-weight: 800;
          color: var(--l1-accent);
          line-height: 1;
        }

        .l1-stat-label {
          font-size: 0.72rem;
          font-weight: 500;
          letter-spacing: 0.1em;
          text-transform: uppercase;
          color: var(--l1-muted);
          margin-top: 0.5rem;
        }
        .l1-page.rtl .l1-stat-label { letter-spacing: 0.05em; }

        /* FOOTER CTA */
        .l1-footer-cta {
          padding: 6rem;
          text-align: center;
          position: relative;
        }

        .l1-footer-cta::before {
          content: '';
          position: absolute;
          top: 0;
          left: 6rem;
          right: 6rem;
          height: 1px;
          background: linear-gradient(90deg, transparent, rgba(27,127,204,0.2), transparent);
        }

        .l1-footer-cta h2 {
          font-family: var(--font-display);
          font-size: clamp(2.5rem, 5vw, 4.5rem);
          font-weight: 900;
          line-height: 1.1;
          margin: 0 0 1.5rem;
          color: var(--l1-fg);
        }
        .l1-page.rtl .l1-footer-cta h2 {
          font-family: 'Noto Kufi Arabic', serif;
          line-height: 1.4;
        }

        .l1-footer-cta h2 em {
          font-style: italic;
          font-weight: 400;
          color: var(--l1-accent);
        }
        .l1-page.rtl .l1-footer-cta h2 em { font-style: normal; }

        .l1-footer-cta p {
          color: var(--l1-muted);
          font-size: 1rem;
          margin-bottom: 3rem;
          font-weight: 400;
        }

        .l1-footer-bottom {
          padding: 2rem 6rem;
          display: flex;
          justify-content: space-between;
          align-items: center;
          border-top: 1px solid rgba(27,127,204,0.08);
        }

        .l1-footer-copy {
          font-size: 0.72rem;
          color: var(--l1-muted);
          letter-spacing: 0.05em;
        }

        .l1-footer-links {
          display: flex;
          gap: 2rem;
        }

        .l1-footer-link {
          font-size: 0.72rem;
          color: var(--l1-muted);
          text-decoration: none;
          font-weight: 500;
          letter-spacing: 0.05em;
          text-transform: uppercase;
          transition: color 0.3s;
        }

        .l1-footer-link:hover { color: var(--l1-accent); }

        /* VERTICAL TEXT */
        .l1-vertical-text {
          position: fixed;
          right: 2rem;
          top: 50%;
          transform: translateY(-50%) rotate(90deg);
          font-size: 0.6rem;
          font-weight: 500;
          letter-spacing: 0.2em;
          text-transform: uppercase;
          color: var(--l1-muted);
          z-index: 50;
          white-space: nowrap;
          opacity: 0.5;
        }
        .l1-page.rtl .l1-vertical-text {
          right: auto;
          left: 2rem;
          transform: translateY(-50%) rotate(-90deg);
          letter-spacing: 0.1em;
        }

        @keyframes l1FadeUp {
          from { opacity: 0; transform: translateY(30px); }
          to { opacity: 1; transform: translateY(0); }
        }

        @keyframes l1FadeIn {
          from { opacity: 0; }
          to { opacity: 1; }
        }

        @keyframes l1ScrollPulse {
          0% { left: -100%; }
          100% { left: 100%; }
        }

        @media (max-width: 768px) {
          .l1-nav { padding: 0 1.5rem; }
          .l1-hero { grid-template-columns: 1fr; min-height: auto; }
          .l1-hero-left { padding: 7rem 2rem 3rem !important; }
          .l1-hero-right { display: none; }
          .l1-features { grid-template-columns: 1fr; padding: 4rem 2rem; gap: 1.5rem; }
          .l1-showcase { grid-template-columns: 1fr; padding: 0 2rem 4rem; gap: 3rem; }
          .l1-showcase-visual { height: 300px; }
          .l1-footer-cta { padding: 4rem 2rem; }
          .l1-footer-bottom { padding: 2rem; flex-direction: column; gap: 1rem; }
          .l1-scroll-line { left: 2rem !important; right: auto !important; }
          .l1-vertical-text { display: none; }
        }
      `}</style>

      <div className={`l1-page${isRTL ? ' rtl' : ''}`}>
        <div className="l1-vertical-text">{s.verticalText}</div>

        {/* NAV */}
        <nav className="l1-nav">
          <div className="l1-logo">Thykra</div>
          <div className="l1-nav-links">
            <div className="l1-lang-toggle" onClick={toggleLang}>
              <span className={`l1-lang-opt${lang === 'en' ? ' active' : ''}`}>EN</span>
              <span className={`l1-lang-opt${lang === 'ar' ? ' active' : ''}`}>عر</span>
            </div>
            <span className="l1-nav-link">{s.about}</span>
            <span className="l1-nav-link">{s.features}</span>
            <button className="l1-nav-link" onClick={() => navigate({ to: '/login' })}>
              {s.signIn}
            </button>
          </div>
        </nav>

        {/* HERO */}
        <section className="l1-hero">
          <div className="l1-hero-left">
            <div className="l1-hero-tagline">{s.tagline}</div>
            <h1 className="l1-hero-title">
              {s.heroLine1}<br />
              <em>{s.heroLine2}</em><br />
              {s.heroLine3}
            </h1>
            <p className="l1-hero-desc">{s.heroDesc}</p>
            <button className="l1-hero-cta" onClick={() => navigate({ to: '/login' })}>
              <span>{s.heroCta}</span>
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.5">
                <path d="M3 8h10M9 4l4 4-4 4" />
              </svg>
            </button>
          </div>

          <div className="l1-hero-right">
            <div className="l1-hero-img-stack">
              {images.hero.map((img, i) => (
                <div className="l1-hero-img" key={i}>
                  <img src={img.url} alt={img.label[lang]} loading={i === 0 ? 'eager' : 'lazy'} />
                  <div className="l1-hero-img-label">{img.label[lang]}</div>
                </div>
              ))}
            </div>
          </div>

          <div className="l1-scroll-line">
            <div className="l1-scroll-bar" />
            <span>{s.scrollHint}</span>
          </div>
        </section>

        {/* FEATURES */}
        <section className="l1-features">
          <div className="l1-feature">
            <div className="l1-feature-num">01</div>
            <h3 className="l1-feature-title">{s.feat1Title}</h3>
            <p className="l1-feature-desc">{s.feat1Desc}</p>
          </div>
          <div className="l1-feature">
            <div className="l1-feature-num">02</div>
            <h3 className="l1-feature-title">{s.feat2Title}</h3>
            <p className="l1-feature-desc">{s.feat2Desc}</p>
          </div>
          <div className="l1-feature">
            <div className="l1-feature-num">03</div>
            <h3 className="l1-feature-title">{s.feat3Title}</h3>
            <p className="l1-feature-desc">{s.feat3Desc}</p>
          </div>
        </section>

        {/* SHOWCASE */}
        <section className="l1-showcase">
          <div className="l1-showcase-visual">
            <div className="l1-showcase-frame">
              <img src={images.showcase[0].url} alt={images.showcase[0].label[lang]} loading="lazy" />
              <div className="l1-showcase-frame-label">{images.showcase[0].label[lang]}</div>
            </div>
            <div className="l1-showcase-frame">
              <img src={images.showcase[1].url} alt={images.showcase[1].label[lang]} loading="lazy" />
              <div className="l1-showcase-frame-label">{images.showcase[1].label[lang]}</div>
            </div>
            <div className="l1-showcase-accent" />
          </div>
          <div className="l1-showcase-content">
            <h2>
              {s.showcaseH1}<br />
              {s.showcaseH2} <em>{s.showcaseH3}</em><br />
              {s.showcaseH4}
            </h2>
            <p>{s.showcaseDesc}</p>
            <div className="l1-showcase-stats">
              <div>
                <div className="l1-stat-num">&infin;</div>
                <div className="l1-stat-label">{s.statAlbums}</div>
              </div>
              <div>
                <div className="l1-stat-num">4K</div>
                <div className="l1-stat-label">{s.statRes}</div>
              </div>
              <div>
                <div className="l1-stat-num">3</div>
                <div className="l1-stat-label">{s.statPlat}</div>
              </div>
            </div>
          </div>
        </section>

        {/* FOOTER CTA */}
        <section className="l1-footer-cta">
          <h2>
            {s.ctaH1}<br />
            <em>{s.ctaH2}</em>
          </h2>
          <p>{s.ctaDesc}</p>
          <button className="l1-hero-cta" onClick={() => navigate({ to: '/login' })}>
            <span>{s.ctaCta}</span>
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.5">
              <path d="M3 8h10M9 4l4 4-4 4" />
            </svg>
          </button>
        </section>

        <footer className="l1-footer-bottom">
          <div className="l1-footer-copy">&copy; 2024 Thykra</div>
          <div className="l1-footer-links">
            <a href="#" className="l1-footer-link">{s.privacy}</a>
            <a href="#" className="l1-footer-link">{s.terms}</a>
          </div>
        </footer>
      </div>
    </>
  );
}
