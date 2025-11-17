#!/bin/bash
# Demo Setup Script - Docker Hub Pull Version (macOS only)
#
# Purpose: Quick setup for demonstrations using pre-built Docker Hub image
# - Pulls latest image from Docker Hub (skips if already up-to-date)
# - Starts Docker containers
# - Prompts for Stripe CLI setup (payment demos)
# - Opens browser automatically
# - Handles cleanup when demo is done
#
# Prerequisites:
# - Docker Desktop installed (script will prompt if not running)
# - Stripe CLI installed (if demoing payments)
# - Run from project root directory
# - All authentication already configured
#
# Differences from v-demo-setup-(local).sh:
# - Pulls image from Docker Hub instead of building locally
# - No Maven/Java required
# - Uses devprod profile (local MySQL + localhost Stripe URLs)

# Docker Hub Authentication:
# To authenticate Docker CLI (if needed for private images):
# 1. Generate access token at: https://hub.docker.com/settings/security
# 2. Run: docker login -u <your-username>
# 3. Use token as password when prompted

set -e  # Exit on any error

echo "=== Demo Setup Starting (Docker Hub Pull Mode) ==="
echo ""

# Check for existing volumes and prompt to clean
echo "### Checking for existing database volumes..."
if docker volume ls 2>/dev/null | grep -q "mysql_data"; then
    echo "⚠️  Pre-existing mysql_data volume detected"
    read -p "Reuse pre-existing volumes? (y/n): " REUSE
    if [ "$REUSE" = "n" ]; then
        echo "→ Cleaning volumes..."
        docker compose down -v 2>/dev/null || true
        echo "✓ Volumes cleaned"
    else
        echo "→ Reusing pre-existing volumes"
    fi
fi
echo "### Volume check complete"
echo ""

# Pull latest image from Docker Hub
echo "### Pulling latest image from Docker Hub..."
echo "    Image: adfaon3343cz/eventhub:latest"
echo "    (Will skip download if already up-to-date)"
echo ""
docker pull adfaon3343cz/eventhub:latest
echo ""
echo "### Image pull complete"
echo ""

# Check Docker Desktop is running (loop until confirmed)
echo "### Verifying Docker Desktop status..."
while ! docker info > /dev/null 2>&1; do
    echo "❌ Docker Desktop is not running"
    echo "   Launch with: open -a Docker"
    read -p "Press Enter when Docker Desktop is running..."
done
echo "✓ Docker Desktop is running"
echo "### Docker verification complete"
echo ""

# Start containers in detached mode
echo "### Starting Docker containers (Command: docker compose up -d)"
echo "    Launching MySQL and Spring Boot containers in background..."
echo ""
docker compose up -d
echo ""
echo "### Containers started successfully"
echo ""

# Stripe CLI setup prompt
echo "=========================================="
echo "STRIPE CLI SETUP (for payment demos)"
echo "=========================================="
echo ""
echo "1. Press Cmd+N to open a new terminal window, copy and run this command:"
echo " ------> stripe listen --forward-to localhost:8080/api/payments/webhook"
echo "   then return to this window and press 'y' then Enter when Stripe CLI is running"
echo "   (or just press Enter to skip if not demoing payments)"
echo ""
read -p "Stripe CLI ready? (y/Enter to skip): " STRIPE_READY

if [ "$STRIPE_READY" = "exit" ]; then
    echo ""
    echo "### Starting cleanup process..."
    echo "    Stopping containers, preserving volumes (Command: docker compose down)..."
    docker compose down
    echo "### Cleanup complete - containers stopped (volumes preserved)"
    echo ""
    echo "⚠️  Don't forget to stop Stripe CLI (if running): Press Ctrl+C in Stripe terminal"
    echo ""
    TOTAL_IMAGES=$(docker images -q | wc -l | tr -d ' ')
    DANGLING_IMAGES=$(docker images -f "dangling=true" -q | wc -l | tr -d ' ')
    echo "Docker images: $TOTAL_IMAGES total, $DANGLING_IMAGES dangling"
    echo ""
    docker images
    echo ""
    echo "### Demo cleanup complete!"
    exit 0
fi

echo "💳 Test card for successful payments: 4242 4242 4242 4242"
echo ""

# Wait for application to be ready
echo "### Performing application health check..."
echo "    Initial startup delay (10 seconds)..."
sleep 10  # Give app time to initialize

MAX_ATTEMPTS=20
ATTEMPT=0
while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
    if curl -s http://localhost:8080 > /dev/null 2>&1; then
        echo "✓ Application is ready"
        echo "### Health check passed - app responding at http://localhost:8080"
        break
    fi
    ATTEMPT=$((ATTEMPT + 1))
    echo "    Attempt $ATTEMPT/$MAX_ATTEMPTS - waiting..."
    sleep 2
done

if [ $ATTEMPT -eq $MAX_ATTEMPTS ]; then
    echo "❌ Application failed to start - check logs with: docker compose logs webapp"
    echo "### Health check failed"
    docker compose down
    exit 1
fi

# Open browser
echo ""
echo "### Opening browser to http://localhost:8080"
sleep 3
open http://localhost:8080
echo "### Browser launched"
echo ""

# Demo ready - wait for exit command
echo "=========================================="
echo "✅ DEMO READY!"
echo "=========================================="
echo ""
echo "Application: http://localhost:8080"
echo "View logs:   docker compose logs -f webapp"
echo ""
echo "When demo is complete:"
echo "  - Type 'exit' to cleanup (preserve volumes)"
echo "  - Type 'exit -v' to cleanup and delete volumes"
echo ""

# Wait for exit command
while true; do
    read -p "> " COMMAND
    if [ "$COMMAND" = "exit" ] || [ "$COMMAND" = "exit -v" ]; then
        break
    else
        echo "Type 'exit' or 'exit -v' to cleanup and stop the demo"
    fi
done

# Cleanup
echo ""
echo "### Starting cleanup process..."
if [ "$COMMAND" = "exit -v" ]; then
    echo "    Stopping containers and deleting volumes (Command: docker compose down -v)..."
    docker compose down -v
    echo "### Cleanup complete - containers stopped and volumes deleted"
else
    echo "    Stopping containers, preserving volumes (Command: docker compose down)..."
    docker compose down
    echo "### Cleanup complete - containers stopped (volumes preserved)"
fi
echo ""
echo "=========================================="
echo "⚠️  MANUAL CLEANUP REQUIRED"
echo "=========================================="
echo ""
echo "Don't forget to stop Stripe CLI:"
echo "  - Switch to the Stripe CLI terminal"
echo "  - Press Ctrl+C to stop the webhook listener"
echo ""
TOTAL_IMAGES=$(docker images -q | wc -l | tr -d ' ')
DANGLING_IMAGES=$(docker images -f "dangling=true" -q | wc -l | tr -d ' ')
echo "Docker images: $TOTAL_IMAGES total, $DANGLING_IMAGES dangling"
echo ""
docker images
echo ""
echo "### Demo cleanup complete!"

# Note: This script pulls the pre-built image from Docker Hub and uses the default
# docker-compose.yml with devprod profile. No local Maven build or JAR compilation required.
