# syntax=docker/dockerfile:1.7

# ---- Build stage --------------------------------------------------------
FROM node:20-alpine AS build
WORKDIR /app

# Dependencies first so the layer caches while only sources change.
# NOTE: webApp has no lockfile of its own (the repo root owns one, via npm
# workspaces), so this deliberately uses `npm install` rather than `npm ci`.
COPY package.json ./
RUN npm install --no-audit --no-fund

COPY tsconfig.json tsconfig.node.json vite.config.ts index.html ./
COPY src ./src

# Vite inlines VITE_* at build time, so the OAuth client id has to arrive as a
# build arg — setting it at container runtime would have no effect.
ARG VITE_GOOGLE_CLIENT_ID=""
ENV VITE_GOOGLE_CLIENT_ID=$VITE_GOOGLE_CLIENT_ID

RUN npm run build

# ---- Runtime stage ------------------------------------------------------
FROM nginx:1.27-alpine AS runtime

RUN apk add --no-cache curl

COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD curl -fsS http://localhost/ >/dev/null || exit 1
