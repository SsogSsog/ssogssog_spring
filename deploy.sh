#!/bin/bash
set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

log() { echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"; }
warn() { echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')] WARNING:${NC} $1"; }
error() { echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] ERROR:${NC} $1"; }

# Working directory
DEPLOY_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$DEPLOY_DIR"

# Check .env file
if [ ! -f ".env" ]; then
    error ".env file not found. Aborting deployment."
    exit 1
fi

log "Starting deployment..."

# Pull latest image
log "Pulling latest Docker image..."
docker compose pull app

# Stop and remove app container only (keep DB/Redis running)
log "Stopping app container..."
docker compose stop app || true
docker compose rm -f app || true

# Start all services
log "Starting services..."
docker compose up -d

# Wait for health check
log "Waiting for app to start (max 120 seconds)..."
MAX_RETRIES=24
RETRY_INTERVAL=5
RETRY_COUNT=0

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if curl -s -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
        log "App started successfully!"
        break
    fi
    RETRY_COUNT=$((RETRY_COUNT + 1))
    warn "Health check failed ($RETRY_COUNT/$MAX_RETRIES), retrying in ${RETRY_INTERVAL}s..."
    sleep $RETRY_INTERVAL
done

if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
    error "App failed to start. Showing logs:"
    docker compose logs --tail=50 app
    exit 1
fi

# Restart Grafana to apply provisioning changes (dashboards, datasources)
log "Restarting Grafana for provisioning updates..."
docker compose restart grafana || true

# Cleanup unused images
log "Cleaning up unused Docker images..."
docker image prune -f

# Show status
log "Deployment completed!"
echo ""
log "Container status:"
docker compose ps

log "Deploy script finished"
