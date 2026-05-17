#!/bin/bash

echo "🚀 Starting AgentForge development environment..."

# Start Docker services
echo "📦 Starting PostgreSQL and Redis..."
cd docker
docker compose up -d
cd ..

# Wait for services to be ready
echo "⏳ Waiting for services..."
sleep 5

# Run Prisma migrations
echo "🗄️ Running database migrations..."
cd apps/api
pnpm prisma migrate dev --name init
cd ..

# Start all services
echo "🌟 Starting all services..."
pnpm dev

echo "✅ AgentForge is ready!"
echo "   - API: http://localhost:3000"
echo "   - Web: http://localhost:5173"