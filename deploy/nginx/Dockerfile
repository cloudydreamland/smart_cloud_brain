FROM node:24-alpine AS frontend-build

ARG PNPM_VERSION=9.15.0
ARG NPM_REGISTRY=https://registry.npmmirror.com

WORKDIR /workspace
ENV npm_config_registry=${NPM_REGISTRY}
ENV VITE_API_BASE=/api

COPY frontend/package.json ./
COPY frontend/pnpm-lock.yaml ./
COPY frontend/pnpm-workspace.yaml ./
COPY frontend/tsconfig.base.json ./
COPY frontend/apps apps
COPY frontend/packages packages

RUN npm config set registry ${NPM_REGISTRY} \
    && (npm install -g pnpm@${PNPM_VERSION} --registry=${NPM_REGISTRY} \
        || npm install -g pnpm@${PNPM_VERSION} --registry=https://registry.npmjs.org) \
    && pnpm config set registry ${NPM_REGISTRY} \
    && pnpm config set fetch-retries 5 \
    && pnpm config set fetch-retry-factor 2 \
    && pnpm config set fetch-timeout 120000 \
    && (pnpm install --frozen-lockfile \
        || (pnpm config set registry https://registry.npmjs.org \
            && pnpm install --frozen-lockfile))

RUN pnpm --filter @smart-cloud-brain/patient-web build \
    && pnpm --filter @smart-cloud-brain/doctor-web build \
    && pnpm --filter @smart-cloud-brain/admin-web build

FROM nginx:1.27-alpine

COPY deploy/nginx/multi-frontend.conf /etc/nginx/conf.d/default.conf
COPY deploy/nginx/html /usr/share/nginx/html
COPY --from=frontend-build /workspace/apps/patient-web/dist /usr/share/nginx/html/patient
COPY --from=frontend-build /workspace/apps/doctor-web/dist /usr/share/nginx/html/doctor
COPY --from=frontend-build /workspace/apps/admin-web/dist /usr/share/nginx/html/admin
