# syntax=docker/dockerfile:1

FROM node:22-alpine AS builder
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

ARG VITE_API_BASE_URL
ARG VITE_WS_URL
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
ENV VITE_WS_URL=${VITE_WS_URL}

# Not `npm run build`: that chains `check:i18n`, which reads sibling
# `gateway/src/main/resources/i18n/*.properties` by relative path — those
# aren't in this build context (only `frontend/` is). The cross-module i18n
# key check belongs to CI/local dev against the full repo, not an isolated
# frontend image build.
RUN npx tsc -b && npx vite build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
