#!/usr/bin/env bash
# AgentForge 本地开发（当前主栈：Spring + MySQL + Redis + Web）
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "Starting AgentForge (Docker)..."
docker compose -f docker/docker-compose.yml --env-file .env up -d --build

echo ""
echo "Ready:"
echo "  Web:     http://localhost:5173"
echo "  API:     http://localhost:5173/api"
echo "  Webhook: http://localhost:5173/webhook/feishu"
echo ""
echo "飞书公网回调: ./scripts/tunnel.sh  然后配置 PUBLIC_BASE_URL + FEISHU_ENCRYPT_KEY"
echo "查看 Runtime: GET http://localhost:5173/api/runtime/status (需登录)"
