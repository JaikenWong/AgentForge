#!/usr/bin/env bash
# 将本地 Nginx(5173) 暴露到公网，供飞书事件订阅回调。
# 依赖: ngrok (https://ngrok.com/download)
set -euo pipefail

PORT="${1:-5173}"

if ! command -v ngrok >/dev/null 2>&1; then
  echo "未找到 ngrok。安装: brew install ngrok/ngrok/ngrok"
  exit 1
fi

echo "▶ 请确保 Docker 栈已启动: cd docker && docker compose --env-file ../.env up -d"
echo "▶ 隧道端口: ${PORT} (web + /api + /webhook 反代)"
echo ""
echo "飞书事件订阅 URL 填:"
echo "  https://<ngrok域名>/webhook/feishu"
echo ""
echo "并在 .env 设置:"
echo "  PUBLIC_BASE_URL=https://<ngrok域名>"
echo "  FEISHU_ENCRYPT_KEY=<飞书后台 Encrypt Key>"
echo ""

exec ngrok http "$PORT"
