#!/bin/bash
# VM Demo Launcher Script (Debian/Ubuntu)
#
# Purpose: Single command to prepare and launch demo on Google Cloud VM
# - Performs pre-flight checks (Docker, port forwarding, git status)
# - Manages volumes and image updates
# - Starts containers and health checks public URL
# - Handles cleanup when demo is complete
#
# Note: Can be run serially after validate-cicd-locally.sh - picks up existing
# containers seamlessly with no manual cleanup required.
#
# Prerequisites:
# - Running on the Google Cloud VM via SSH
# - Docker and docker-compose installed
# - Port forwarding configured (80 → 8080)
# - Git credentials configured
# - Run from ~/team-project-group-p02-06 directory
#
# Usage:
# chmod +x vm-demo-launcher-(GCS).sh
# ./vm-demo-launcher-(GCS).sh

set -e  # Exit on any error

# Trap Ctrl+C for graceful exit
trap 'echo ""; echo "⚠️  Script interrupted. Use commands above to cleanup if needed."; exit 130' INT

echo "========================================================================"
echo "QUICK REFERENCE (copy/paste if needed)"
echo "========================================================================"
echo "Check containers: sudo docker ps"
echo "View logs:        sudo docker-compose -f docker-compose.remote.yml logs webapp"
echo "Stop containers:  sudo docker-compose -f docker-compose.remote.yml stop"
echo "Remove (keep DB): sudo docker-compose -f docker-compose.remote.yml down"
echo "Clean all:        sudo docker-compose -f docker-compose.remote.yml down -v"
echo "========================================================================"
echo ""

echo "=== VM Demo Setup Starting ==="
echo ""

# Check 1: Docker daemon running
echo "→ Checking Docker daemon status..."
if ! sudo systemctl is-active --quiet docker; then
    echo "❌ Docker is not running"
    echo "   Starting Docker: sudo systemctl start docker"
    sudo systemctl start docker
    sleep 2
fi
echo "✓ Docker daemon is running"

# Check 2: Port forwarding rule exists
echo ""
echo "→ Checking port forwarding (80 → 8080)..."
if ! sudo iptables -t nat -L PREROUTING -n | grep -q "tcp dpt:80 redir ports 8080"; then
    echo "❌ Port forwarding rule missing"
    echo "   To fix: sudo iptables -t nat -A PREROUTING -p tcp --dport 80 -j REDIRECT --to-port 8080"
    echo "           sudo netfilter-persistent save"
    exit 1
fi
echo "✓ Port forwarding configured"

# Check 3: Git credentials saved
echo ""
echo "→ Checking Git credentials..."
if [ ! -f ~/.git-credentials ]; then
    echo "⚠️  Git credentials not found at ~/.git-credentials"
    echo "   You may need to enter credentials for git operations"
else
    echo "✓ Git credentials configured"
fi

# Check 4: Git status (fetch and compare)
echo ""
echo "→ Checking repository status..."
git fetch origin main --quiet 2>/dev/null || echo "⚠️  Could not fetch from remote (network issue?)"
LOCAL=$(git rev-parse HEAD)
REMOTE=$(git rev-parse origin/main 2>/dev/null || echo "unknown")
if [ "$LOCAL" != "$REMOTE" ] && [ "$REMOTE" != "unknown" ]; then
    echo "⚠️  WARNING: Local repository is behind remote"
    echo "   Current commit: ${LOCAL:0:7}"
    echo "   Remote commit:  ${REMOTE:0:7}"
    echo "   Consider running: git pull"
else
    echo "✓ Repository is up to date"
fi

# Check 5: Existing volumes prompt
echo ""
echo "→ Checking for existing database volumes..."
if sudo docker volume ls 2>/dev/null | grep -q "mysql_data_remote"; then
    echo "⚠️  Pre-existing mysql_data_remote volume detected"
    read -p "Reuse existing volumes? (Enter=yes, n=clean): " REUSE
    if [ "$REUSE" = "n" ]; then
        echo "→ Cleaning volumes..."
        sudo docker-compose -f docker-compose.remote.yml down -v 2>/dev/null || true
        echo "✓ Volumes cleaned"
    else
        echo "→ Reusing existing volumes"
    fi
else
    echo "✓ No existing volumes (fresh start)"
fi

# Check 6: Pull latest image (auto-pull if missing, prompt if exists)
echo ""
echo "→ Docker image check..."
if ! sudo docker image inspect adfaon3343cz/eventhub:latest > /dev/null 2>&1; then
    echo "⚠️  Image not found locally - pulling..."
    sudo docker pull adfaon3343cz/eventhub:latest
    echo "✓ Image downloaded"
else
    echo "✓ Image exists locally"
    read -p "Pull latest version? (Enter=skip, y=pull): " PULL_UPDATE
    if [ "$PULL_UPDATE" = "y" ]; then
        echo "→ Pulling latest image..."
        sudo docker pull adfaon3343cz/eventhub:latest
        echo "✓ Image updated"
    else
        echo "→ Using cached image"
    fi
fi

# Start containers
echo ""
echo "→ Starting Docker containers..."
sudo docker-compose -f docker-compose.remote.yml up -d
echo "✓ Containers started"

# Health check - wait for app to respond
echo ""
echo "→ Performing health check on public URL..."
echo "  Initial startup delay (15 seconds)..."
sleep 15

# Get external IP from VM metadata or use hardcoded
VM_IP=$(curl -s -H "Metadata-Flavor: Google" http://metadata.google.internal/computeMetadata/v1/instance/network-interfaces/0/access-configs/0/external-ip 2>/dev/null || echo "34.129.54.215")

MAX_ATTEMPTS=30
ATTEMPT=0
while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
    # Check if Spring Boot container crashed
    if ! sudo docker ps | grep -q webapp-springboot-remote; then
        echo "❌ Error: Spring Boot container crashed"
        echo "→ Container logs:"
        sudo docker-compose -f docker-compose.remote.yml logs webapp
        echo ""
        echo "⚠️  Use quick reference commands above to cleanup"
        exit 1
    fi

    # Check if application responds (use curl with VM's external IP)
    if curl -s http://$VM_IP > /dev/null 2>&1; then
        echo "✓ Application is responding"
        break
    fi
    ATTEMPT=$((ATTEMPT + 1))
    echo "  Attempt $ATTEMPT/$MAX_ATTEMPTS - waiting..."
    sleep 2
done

if [ $ATTEMPT -eq $MAX_ATTEMPTS ]; then
    echo "❌ Error: Application failed to respond after $MAX_ATTEMPTS attempts"
    echo "→ Check logs: sudo docker-compose -f docker-compose.remote.yml logs webapp"
    echo ""
    echo "⚠️  Use quick reference commands above to cleanup"
    exit 1
fi

# Stripe reminder
echo ""
echo "========================================================================"
echo "💳 STRIPE PAYMENT TESTING"
echo "========================================================================"
echo "✓ Webhook configured in Stripe Dashboard"
echo "  Destination: http://$VM_IP/api/payments/webhook"
echo "  Event: checkout.session.completed"
echo ""
echo "Test card for successful payments:"
echo "  Card number: 4242 4242 4242 4242"
echo "  Expiry:      Any future date (e.g., 12/34)"
echo "  CVC:         Any 3 digits (e.g., 123)"
echo "  ZIP:         Any 5 digits (e.g., 12345)"
echo ""
echo "Stripe Dashboard (verify real webhook integration):"
echo "  https://dashboard.stripe.com/test/payments"
echo "  - Shows live payment events from your demo"
echo "  - Confirms real Stripe backend integration (not superficial)"
echo "========================================================================"

# Demo ready
echo ""
echo "========================================================================"
echo "✅ DEMO READY!"
echo "========================================================================"
echo ""
echo "Application URL: http://$VM_IP"
echo "GCS Bucket:      https://console.cloud.google.com/storage/browser/eventhub-photos-prod"
echo "Monitor logs:    sudo docker-compose -f docker-compose.remote.yml logs -f webapp"
echo ""
echo "When demo is complete:"
echo "  - Type 'exit' to cleanup (preserve volumes)"
echo "  - Type 'exit -v' to cleanup and delete volumes"
echo "  - Press Ctrl+C to exit script and leave containers running"
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
echo "→ Starting cleanup process..."
if [ "$COMMAND" = "exit -v" ]; then
    echo "  Stopping containers and deleting volumes..."
    sudo docker-compose -f docker-compose.remote.yml down -v
    echo "✓ Cleanup complete - containers stopped and volumes deleted"
else
    echo "  Stopping containers, preserving volumes..."
    sudo docker-compose -f docker-compose.remote.yml down
    echo "✓ Cleanup complete - containers stopped (volumes preserved)"
fi

# Clean up dangling images
echo ""
echo "→ Removing dangling Docker images..."
PRUNE_OUTPUT=$(sudo docker image prune -f 2>&1)
SPACE_RECLAIMED=$(echo "$PRUNE_OUTPUT" | grep "Total reclaimed space" | sed 's/Total reclaimed space: //')
if [ -z "$SPACE_RECLAIMED" ]; then
    echo "✓ No dangling images found"
else
    echo "✓ Reclaimed: $SPACE_RECLAIMED"
fi

echo ""
echo "=== Demo cleanup complete! ==="

# ========================================
# VM SETUP GUIDE
# ========================================
# If setting up a fresh VM to replicate this environment, run these commands:
#
# 1. Install Docker and Docker Compose:
#    sudo apt update
#    sudo apt install -y docker.io docker-compose
#
# 2. Start Docker and enable on boot:
#    sudo systemctl start docker
#    sudo systemctl enable docker
#
# 3. Configure port forwarding (80 → 8080):
#    sudo iptables -t nat -A PREROUTING -p tcp --dport 80 -j REDIRECT --to-port 8080
#    sudo apt install -y iptables-persistent
#    sudo netfilter-persistent save
#
# 4. Configure Git credentials (for private repo access):
#    git config --global credential.helper store
#    # Then clone repo - you'll be prompted for username + Personal Access Token once
#
# 5. Clone repository:
#    cd ~
#    git clone https://github.com/cosc2299-2025/team-project-group-p02-06.git
#    cd team-project-group-p02-06
#
# 6. SSH Key setup (for CI/CD deployment):
#    # Generate SSH key pair on local machine:
#    ssh-keygen -t ed25519 -f gce-deploy-key -N ""
#
#    # Add PUBLIC key to VM:
#    # - Go to Google Cloud Console → Compute Engine → VM instances
#    # - Click your VM → Edit
#    # - Scroll to "SSH Keys" → Add item
#    # - Paste contents of gce-deploy-key.pub
#    # - Save
#
#    # Add PRIVATE key to GitHub secrets:
#    # - Go to GitHub repo → Settings → Secrets and variables → Actions
#    # - Add secret: GCE_SSH_KEY (paste contents of gce-deploy-key)
#    # - Add secret: GCE_USERNAME (your VM username, e.g., dion_marinovic)
#    # - Add secret: GCE_VM_IP (your VM external IP, e.g., 34.129.54.215)
#
# 7. Verify setup:
#    sudo docker --version
#    sudo docker-compose --version
#    sudo systemctl status docker
#    sudo iptables -t nat -L PREROUTING -n -v
#    git config --global credential.helper
#
# 8. Make this script executable:
#    chmod +x vm-demo-launcher-(GCS).sh
#
# 9. Optional - Reserve static IP (prevents IP change on VM stop/start):
#    # Google Cloud Console → VPC Network → IP addresses
#    # Find your VM's external IP → Change "Type" from Ephemeral to Static
#
# 10. Security updates (run periodically):
#     sudo apt update && sudo apt upgrade -y
#
# ========================================
#
# VM INSTANCE CONFIGURATION (Google Cloud Compute Engine)
# ========================================
# When creating the VM instance in Google Cloud Console, use these settings:
#
# Machine Configuration:
#   - Machine type: e2-medium (2 vCPU, 1 core, 4 GB memory)
#   - Architecture: x86/64
#   - Why: Sufficient for Docker + MySQL + Spring Boot app with concurrent demo users.
#     e2-small (2GB) works for solo testing, e2-medium recommended for group demos.
#
# Boot Disk:
#   - Operating System: Debian GNU/Linux 12 (bookworm)
#   - Boot disk type: Balanced persistent disk
#   - Size: 10 GB (default, sufficient for app + Docker images)
#   - Why: Debian is stable and Docker-friendly. Any major Linux distro works.
#
# Networking:
#   - Network interface: Default VPC, default subnet
#   - Internal IP: Automatic (e.g., 10.192.0.0/20 range)
#   - External IP: Ephemeral (or reserve as Static to prevent IP changes)
#   - Hostname: Default
#   - Firewall: Allow HTTP traffic (checkbox enabled)
#     - This opens port 80, which forwards to 8080 via iptables
#     - SSH is automatically allowed by default
#
# Access:
#   - Service account: Default Compute Engine service account
#   - Access scopes: Allow default access
#   - SSH: Automatically configured via Google Cloud Console browser SSH
#
# Cost Estimate (as of 2025):
#   - e2-medium: ~$25-30/month running 24/7
#   - Static IP: Free when attached to running VM, ~$3-5/month if reserved but unattached
#   - Network egress: Minimal for demo usage (covered by free tier)
#   - Free tier: Includes 1 e2-micro instance (0.25-2 vCPU, 1GB RAM) if eligible
#
# ========================================
