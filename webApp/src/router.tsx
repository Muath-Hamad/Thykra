import {
  createRootRoute,
  createRoute,
  createRouter,
  Outlet,
  redirect,
} from '@tanstack/react-router';
import { AppShell } from './app/AppShell';
import { getAccessToken } from './api/client';
import { LandingPage } from './routes/landing';
import { LoginPage } from './routes/login';
import { TripsPage } from './routes/trips';
import { TripPage } from './routes/trip';
import { TripSettingsPage } from './routes/tripSettings';
import { TripActivityPage } from './routes/tripActivity';
import { TripRecapsPage } from './routes/tripRecaps';
import { ActivityPage } from './routes/activity';
import { RecapsPage } from './routes/recaps';
import { RecapReaderPage } from './routes/recapReader';
import { MePage } from './routes/me';
import { InvitePage } from './routes/invite';
import { PublicTripPage } from './routes/publicTrip';
import { NotFoundPage } from './routes/notFound';

const rootRoute = createRootRoute({
  component: () => <Outlet />,
  notFoundComponent: NotFoundPage,
});

function requireAuth({ location }: { location: { href: string } }) {
  if (!getAccessToken()) {
    throw redirect({ to: '/login', search: { next: location.href } });
  }
}

// ── Public routes ──

// Landing at / — the front door. Signed-in visitors never see it.
const landingRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/',
  beforeLoad: () => {
    if (getAccessToken()) throw redirect({ to: '/trips' });
  },
  component: LandingPage,
});

const loginRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/login',
  validateSearch: (search: Record<string, unknown>): { next?: string; error?: string } => {
    const out: { next?: string; error?: string } = {};
    if (typeof search.next === 'string') out.next = search.next;
    if (typeof search.error === 'string') out.error = search.error;
    return out;
  },
  beforeLoad: () => {
    // Already signed in: the loader redirects out before render.
    if (getAccessToken()) throw redirect({ to: '/trips' });
  },
  component: LoginPage,
});

// Invite landing — path unchanged, live links in the wild.
const inviteRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/invite/$token',
  component: InvitePage,
});

// Public trip — path unchanged; /s/:id is the short alias for new shares.
const publicAlbumRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/public/$albumId',
  component: PublicTripPage,
});
const shortShareRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/s/$albumId',
  component: PublicTripPage,
});

// Public recap reader.
const recapReaderRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/r/$shareToken',
  component: RecapReaderPage,
});

// ── Authenticated shell ──

const authedRoute = createRoute({
  getParentRoute: () => rootRoute,
  id: 'authed',
  beforeLoad: requireAuth,
  component: AppShell,
});

const tripsRoute = createRoute({
  getParentRoute: () => authedRoute,
  path: '/trips',
  validateSearch: (search: Record<string, unknown>): { new?: string } => {
    const out: { new?: string } = {};
    if (typeof search.new === 'string') out.new = search.new;
    return out;
  },
  component: TripsPage,
});

const tripRoute = createRoute({
  getParentRoute: () => authedRoute,
  path: '/trips/$albumId',
  validateSearch: (search: Record<string, unknown>): { m?: string; joined?: string } => {
    const out: { m?: string; joined?: string } = {};
    if (typeof search.m === 'string') out.m = search.m;
    if (typeof search.joined === 'string') out.joined = search.joined;
    return out;
  },
  component: TripPage,
});

const tripSettingsRoute = createRoute({
  getParentRoute: () => authedRoute,
  path: '/trips/$albumId/settings',
  component: TripSettingsPage,
});

const tripActivityRoute = createRoute({
  getParentRoute: () => authedRoute,
  path: '/trips/$albumId/activity',
  component: TripActivityPage,
});

const tripRecapsRoute = createRoute({
  getParentRoute: () => authedRoute,
  path: '/trips/$albumId/recaps',
  component: TripRecapsPage,
});

const activityRoute = createRoute({
  getParentRoute: () => authedRoute,
  path: '/activity',
  component: ActivityPage,
});

const recapsRoute = createRoute({
  getParentRoute: () => authedRoute,
  path: '/recaps',
  component: RecapsPage,
});

const meRoute = createRoute({
  getParentRoute: () => authedRoute,
  path: '/me',
  component: MePage,
});

// ── Aliases — kept indefinitely; links in the wild must not break ──

const legacyLandingRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/1',
  beforeLoad: () => {
    throw redirect({ to: '/' });
  },
});

const legacyAlbumsRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/albums',
  beforeLoad: () => {
    throw redirect({ to: '/trips' });
  },
});

const legacyAlbumDetailRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/albums/$albumId',
  beforeLoad: ({ params }) => {
    throw redirect({ to: '/trips/$albumId', params: { albumId: params.albumId } });
  },
});

const legacyProfileRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/profile',
  beforeLoad: () => {
    throw redirect({ to: '/me' });
  },
});

const routeTree = rootRoute.addChildren([
  landingRoute,
  loginRoute,
  inviteRoute,
  publicAlbumRoute,
  shortShareRoute,
  recapReaderRoute,
  authedRoute.addChildren([
    tripsRoute,
    tripRoute,
    tripSettingsRoute,
    tripActivityRoute,
    tripRecapsRoute,
    activityRoute,
    recapsRoute,
    meRoute,
  ]),
  legacyLandingRoute,
  legacyAlbumsRoute,
  legacyAlbumDetailRoute,
  legacyProfileRoute,
]);

export const router = createRouter({ routeTree });

declare module '@tanstack/react-router' {
  interface Register {
    router: typeof router;
  }
}
