#!/usr/bin/env bash
# ============================================================
#  智慧云脑诊疗平台 — 演示环境一键部署脚本
#  用法: bash scripts/setup-demo.sh
#  前提: 已安装 Docker Desktop 并处于运行状态
# ============================================================
set -euo pipefail

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
DIFY_DIR="$HOME/dify/docker"
DIFY_WORKFLOWS="$ROOT_DIR/deploy/dify-workflows"
DIFY_NETWORK="${DIFY_DOCKER_NETWORK:-docker_default}"
PROJECT_ENV="$ROOT_DIR/deploy/env/.env"
PROJECT_ENV_TEMPLATE="$ROOT_DIR/deploy/env/.env.dev.example"

info()  { printf '\033[0;32m[✓]\033[0m %s\n' "$*"; }
warn()  { printf '\033[1;33m[!]\033[0m %s\n' "$*"; }
fail()  { printf '\033[0;31m[✗]\033[0m %s\n' "$*"; exit 1; }

echo "=========================================="
echo "  智慧云脑诊疗平台 — 演示环境部署"
echo "=========================================="
echo ""

# ----------------------------------------------------------
#  0. 前置检查
# ----------------------------------------------------------
command -v docker >/dev/null 2>&1 || fail "未检测到 Docker，请安装 Docker Desktop: https://www.docker.com/products/docker-desktop/"
docker info >/dev/null 2>&1       || fail "Docker Desktop 未运行，请先启动它"
info "Docker 已就绪"

case "$(uname -m)" in
    arm64|aarch64)
        KINGBASE_IMAGE="kingbase_v009r001c010b0004_single_arm:v1"
        ;;
    *)
        KINGBASE_IMAGE="kingbase_v009r001c010b0004_single_x86:v1"
        ;;
esac
info "Kingbase 镜像: $KINGBASE_IMAGE"

# ----------------------------------------------------------
#  1. 启动 Dify
# ----------------------------------------------------------
echo ""
echo ">>> 步骤 1/5: 启动 Dify"
echo ""

if [ -d "$DIFY_DIR" ] && [ -f "$DIFY_DIR/docker-compose.yaml" ]; then
    info "Dify 目录已存在"
else
    warn "正在克隆 Dify (约 1 分钟)..."
    git clone --depth 1 https://github.com/langgenius/dify.git "$HOME/dify"
    info "Dify 已克隆"
fi

cd "$DIFY_DIR"
if docker compose ps --format '{{.Names}}' 2>/dev/null | grep -q 'api-1'; then
    info "Dify 已在运行"
else
    warn "首次启动 Dify 约需 2-3 分钟..."
    docker compose up -d
    printf '  等待 Dify 就绪 '
    for i in $(seq 1 60); do
        if curl -fsS http://localhost/health 2>/dev/null | grep -q 'ok'; then
            echo ""
            info "Dify 已就绪"
            break
        fi
        if [ "$i" -eq 60 ]; then
            echo ""
            fail "Dify 启动超时，请检查 Docker Desktop"
        fi
        printf '.'
        sleep 3
    done
    echo ""
fi

# ----------------------------------------------------------
#  2. 导入 Workflow
# ----------------------------------------------------------
echo ""
echo ">>> 步骤 2/5: 导入 Workflow"
echo ""

if [ ! -d "$DIFY_WORKFLOWS" ] || [ -z "$(ls "$DIFY_WORKFLOWS"/*.yml 2>/dev/null)" ]; then
    fail "未找到 Workflow 文件，请先将 DSL 文件放到 $DIFY_WORKFLOWS/"
fi

WARN_COUNT=$(ls "$DIFY_WORKFLOWS"/*.yml 2>/dev/null | wc -l | tr -d ' ')
info "检测到 $WARN_COUNT 个 Workflow 文件"

warn "请在浏览器中手动导入 Workflow（Dify 暂不支持 API 自动导入）"
echo ""
echo "  操作步骤："
echo "  1. 浏览器打开 http://localhost/apps"
echo "  2. 注册/登录 Dify（首次需要创建管理员账号）"
echo "  3. 点击「创建应用」->「导入 DSL 文件」"
echo "  4. 依次导入以下文件："
echo ""
for f in "$DIFY_WORKFLOWS"/*.yml; do
    fname=$(basename "$f")
    echo "     - $fname"
done
echo ""
echo "  5. 导入完成后，进入每个应用 ->「API 访问」-> 复制 API Key"
echo ""

if command -v open >/dev/null 2>&1; then
    open "http://localhost/apps"
elif command -v xdg-open >/dev/null 2>&1; then
    xdg-open "http://localhost/apps"
fi

# ----------------------------------------------------------
#  3. 获取 3 个 API Key
# ----------------------------------------------------------
echo ""
echo ">>> 步骤 3/5: 配置 API Key"
echo ""

TRIAGE_KEY=""
MEDICAL_KEY=""
PRESCRIPTION_KEY=""

if [ -f "$PROJECT_ENV" ]; then
    TRIAGE_KEY=$(grep '^DIFY_TRIAGE_API_KEY=' "$PROJECT_ENV" 2>/dev/null | head -1 | cut -d'=' -f2-)
    MEDICAL_KEY=$(grep '^DIFY_MEDICAL_RECORD_API_KEY=' "$PROJECT_ENV" 2>/dev/null | head -1 | cut -d'=' -f2-)
    PRESCRIPTION_KEY=$(grep '^DIFY_PRESCRIPTION_CHECK_API_KEY=' "$PROJECT_ENV" 2>/dev/null | head -1 | cut -d'=' -f2-)
fi

if [ -n "$TRIAGE_KEY" ] && [ -n "$MEDICAL_KEY" ] && [ -n "$PRESCRIPTION_KEY" ]; then
    info "使用已有 API Key"
else
    warn "请从 Dify 控制台复制 3 个应用的 API Key"
    echo ""

    if [ -z "$TRIAGE_KEY" ]; then
        read -r -s -p "  分诊 Workflow API Key: " TRIAGE_KEY
        echo ""
        [ -z "$TRIAGE_KEY" ] && fail "未输入分诊 Key，中止"
    fi
    if [ -z "$MEDICAL_KEY" ]; then
        read -r -s -p "  病历生成 Workflow API Key: " MEDICAL_KEY
        echo ""
        [ -z "$MEDICAL_KEY" ] && fail "未输入病历 Key，中止"
    fi
    if [ -z "$PRESCRIPTION_KEY" ]; then
        read -r -s -p "  处方审核 Workflow API Key: " PRESCRIPTION_KEY
        echo ""
        [ -z "$PRESCRIPTION_KEY" ] && fail "未输入处方 Key，中止"
    fi
    info "已获取 3 个 API Key"
fi

# ----------------------------------------------------------
#  4. 生成项目 .env
# ----------------------------------------------------------
echo ""
echo ">>> 步骤 4/5: 环境配置"
echo ""

if [ ! -f "$PROJECT_ENV" ]; then
    cp "$PROJECT_ENV_TEMPLATE" "$PROJECT_ENV"
    info "已从 .env.dev.example 生成 .env"
else
    info "使用已有 .env"
fi

# sed 兼容 macOS / Linux
if sed --version 2>/dev/null | grep -q GNU; then
    SED_I="sed -i"
else
    SED_I="sed -i ''"
fi

eval "$SED_I" "'s/^AI_PROVIDER=.*/AI_PROVIDER=dify/'" "$PROJECT_ENV"
eval "$SED_I" "'s|^DIFY_BASE_URL=.*|DIFY_BASE_URL=http://docker-nginx-1/v1|'" "$PROJECT_ENV"

# 删除旧的单一 DIFY_API_KEY（如果有）
grep -v '^DIFY_API_KEY=' "$PROJECT_ENV" > "$PROJECT_ENV.tmp" && mv "$PROJECT_ENV.tmp" "$PROJECT_ENV"

# 写入 3 个独立 Key
grep -q '^DIFY_TRIAGE_API_KEY=' "$PROJECT_ENV" 2>/dev/null || echo "DIFY_TRIAGE_API_KEY=$TRIAGE_KEY" >> "$PROJECT_ENV"
grep -q '^DIFY_MEDICAL_RECORD_API_KEY=' "$PROJECT_ENV" 2>/dev/null || echo "DIFY_MEDICAL_RECORD_API_KEY=$MEDICAL_KEY" >> "$PROJECT_ENV"
grep -q '^DIFY_PRESCRIPTION_CHECK_API_KEY=' "$PROJECT_ENV" 2>/dev/null || echo "DIFY_PRESCRIPTION_CHECK_API_KEY=$PRESCRIPTION_KEY" >> "$PROJECT_ENV"

eval "$SED_I" "'s|^DIFY_TRIAGE_API_KEY=.*|DIFY_TRIAGE_API_KEY=$TRIAGE_KEY|'" "$PROJECT_ENV"
eval "$SED_I" "'s|^DIFY_MEDICAL_RECORD_API_KEY=.*|DIFY_MEDICAL_RECORD_API_KEY=$MEDICAL_KEY|'" "$PROJECT_ENV"
eval "$SED_I" "'s|^DIFY_PRESCRIPTION_CHECK_API_KEY=.*|DIFY_PRESCRIPTION_CHECK_API_KEY=$PRESCRIPTION_KEY|'" "$PROJECT_ENV"

info "AI_PROVIDER -> dify"
info "DIFY_BASE_URL -> http://docker-nginx-1/v1"
info "3 个 Workflow API Key 已写入"

# ----------------------------------------------------------
#  5. 启动主项目
# ----------------------------------------------------------
echo ""
echo ">>> 步骤 5/5: 启动智慧云脑诊疗平台"
echo ""

cd "$ROOT_DIR"

KINGBASE_IMAGE="$KINGBASE_IMAGE" docker compose \
    --env-file "$PROJECT_ENV" \
    -f deploy/docker-compose.yml \
    up -d --build

docker network inspect "$DIFY_NETWORK" >/dev/null 2>&1 \
    || fail "Dify 网络 $DIFY_NETWORK 不存在，请确认 Dify 已启动"
if ! docker network inspect "$DIFY_NETWORK" --format '{{range .Containers}}{{println .Name}}{{end}}' \
    | grep -qx 'scb-ai-service'; then
    docker network connect "$DIFY_NETWORK" scb-ai-service
fi
info "AI 服务已连接 Dify 网络: $DIFY_NETWORK"

info "所有服务启动中，等待就绪..."

for i in $(seq 1 40); do
    if curl -fsS http://localhost:18080/actuator/health 2>/dev/null | grep -q 'UP'; then
        info "网关服务已就绪"
        break
    fi
    if [ "$i" -eq 40 ]; then
        warn "网关启动较慢，请稍后手动检查"
    fi
    sleep 3
done

echo ""
echo "=========================================="
printf '\033[0;32m  部署完成！\033[0m\n'
echo "=========================================="
echo ""
echo "  患者端:  http://localhost:5173"
echo "  医生端:  http://localhost:5174"
echo "  管理端:  http://localhost:5175"
echo "  统一入口: http://localhost:18000"
echo "  Dify:    http://localhost"
echo ""
echo "  验证 AI: bash scripts/verify-dify-ai.sh"
echo "  停止:    docker compose -f deploy/docker-compose.yml down"
echo ""
