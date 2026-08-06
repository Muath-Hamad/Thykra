import React from 'react';
import ReactDOM from 'react-dom/client';
import { RouterProvider } from '@tanstack/react-router';
import { GoogleOAuthProvider } from '@react-oauth/google';
import './styles/tokens.css';
import './styles/base.css';
import { AuthProvider } from './auth/AuthContext';
import { ThemeProvider } from './theme/ThemeProvider';
import { LocaleProvider } from './i18n/LocaleProvider';
import { ToastProvider } from './ui/Toast';
import { UploadProvider } from './upload/UploadProvider';
import { router } from './router';

const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID ?? '';

const rootElement = document.getElementById('root');
if (!rootElement) throw new Error('Failed to find the root element');

ReactDOM.createRoot(rootElement).render(
  <React.StrictMode>
    <GoogleOAuthProvider clientId={GOOGLE_CLIENT_ID}>
      <ThemeProvider>
        <LocaleProvider>
          <AuthProvider>
            <ToastProvider>
              <UploadProvider>
                <RouterProvider router={router} />
              </UploadProvider>
            </ToastProvider>
          </AuthProvider>
        </LocaleProvider>
      </ThemeProvider>
    </GoogleOAuthProvider>
  </React.StrictMode>
);
