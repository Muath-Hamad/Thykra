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

const routeTree = rootRoute.addChildren([loginRoute, indexRoute, profileRoute]);

export const router = createRouter({ routeTree });

declare module '@tanstack/react-router' {
  interface Register {
    router: typeof router;
  }
}
