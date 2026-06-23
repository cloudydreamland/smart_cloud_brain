FROM node:24-alpine AS frontend-build

ARG PNPM_VERSION=9.15.0
ARG NPM_REGISTRY=https://registry.npmmirror.com

WORKDIR /workspace
ENV npm_config_registry=${NPM_REGISTRY}
ENV VITE_API_BASE=/api

# ── Step 1: 复制依赖描述文件（只复制 package.json + lock 文件）─────────
COPY frontend/package.json ./frontend/
COPY frontend/pnpm-lock.yaml ./frontend/
COPY frontend/pnpm-workspace.yaml ./frontend/
COPY frontend/tsconfig.base.json ./frontend/

# 复制每个 workspace 包的 package.json（不复制源码）
COPY frontend/apps/patient-web/package.json ./frontend/apps/patient-web/
COPY frontend/apps/doctor-web/package.json ./frontend/apps/doctor-web/
COPY frontend/apps/admin-web/package.json ./frontend/apps/admin-web/
COPY frontend/apps/patient-mobile/package.json ./frontend/apps/patient-mobile/

COPY frontend/packages/shared-ui/package.json ./frontend/packages/shared-ui/
COPY frontend/packages/shared-api/package.json ./frontend/packages/shared-api/
COPY frontend/packages/shared-types/package.json ./frontend/packages/shared-types/

# ── Step 2: 安装依赖（这一层在 package.json 不变时永远命中缓存）────────
RUN cd frontend \
    && npm config set registry ${NPM_REGISTRY} \
    && (npm install -g pnpm@${PNPM_VERSION} --registry=${NPM_REGISTRY} \
        || npm install -g pnpm@${PNPM_VERSION} --registry=https://registry.npmjs.org) \
    && pnpm config set registry ${NPM_REGISTRY} \
    && pnpm config set fetch-retries 5 \
    && pnpm config set fetch-retry-factor 2 \
    && pnpm config set fetch-timeout 120000 \
    && (pnpm install --frozen-lockfile \
        || (pnpm config set registry https://registry.npmjs.org \
            && pnpm install --frozen-lockfile))

# ── Step 3: 复制源码（改代码只重跑这一步和 build）────────────────────
COPY frontend/apps ./frontend/apps
COPY frontend/packages ./frontend/packages

# ── Step 4: 构建三端 ─────────────────────────────────────────────
RUN cd frontend \
    && pnpm --filter @smart-cloud-brain/patient-web build \
    && pnpm --filter @smart-cloud-brain/doctor-web build \
    && pnpm --filter @smart-cloud-brain/admin-web build

# ── Step 5: Nginx 镜像 ──────────────────────────────────────────
FROM nginx:1.27-alpine

COPY deploy/nginx/multi-frontend.conf /etc/nginx/conf.d/default.conf
COPY deploy/nginx/html /usr/share/nginx/html
COPY --from=frontend-build /workspace/frontend/apps/patient-web/dist /usr/share/nginx/html/patient
COPY --from=frontend-build /workspace/frontend/apps/doctor-web/dist /usr/share/nginx/html/doctor
COPY --from=frontend-build /workspace/frontend/apps/admin-web/dist /usr/share/nginx/html/admin
