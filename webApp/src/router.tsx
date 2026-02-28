import {
  createRootRoute,
  createRoute,
  createRouter,
  Outlet,
  redirect,
} from '@tanstack/react-router';
import { LoginPage } from './routes/login';
import { HomePage } from './routes/index';
import { ProfilePage } from './routes/profile';
import { AlbumsPage } from './routes/albums';
import { AlbumDetailPage } from './routes/albums/detail';
import { LandingPage1 } from './routes/landing1';
import { getAccessToken } from './api/client';

const rootRoute = createRootRoute({
  component: () => <Outlet />,
});

function requireAuth() {
  if (!getAccessToken()) {
    throw redirect({ to: '/login' });
  }
}

const loginRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/login',
  component: LoginPage,
});

const indexRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/',
  beforeLoad: requireAuth,
  component: HomePage,
});

const profileRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/profile',
  beforeLoad: requireAuth,
  component: ProfilePage,
});

const albumsRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/albums',
  beforeLoad: requireAuth,
  component: AlbumsPage,
});

const albumDetailRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/albums/$albumId',
  beforeLoad: requireAuth,
  component: AlbumDetailPage,
});

const landing1Route = createRoute({
  getParentRoute: () => rootRoute,
  path: '/1',
  component: LandingPage1,
});

const routeTree = rootRoute.addChildren([
  loginRoute,
  indexRoute,
  profileRoute,
  albumsRoute,
  albumDetailRoute,
  landing1Route,
]);

export const router = createRouter({ routeTree });

declare module '@tanstack/react-router' {
  interface Register {
    router: typeof router;
  }
}
