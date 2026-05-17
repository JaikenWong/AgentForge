#!/bin/bash

# Build and start AgentForge
echo "🚀 Building AgentForge images..."
docker compose build --parallel

echo "✅ Starting services..."
docker compose up -d

echo "📊 Service status:"
docker compose ps
