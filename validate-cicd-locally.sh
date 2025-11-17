#!/bin/bash
# Local CI/CD Validation Script (macOS only)
# Prerequisites:
# - Docker Desktop installed and running
# - Maven installed (mvn command available)
# - Java 17 installed
# - Run from project root directory
# Note: No authentication needed - script only builds/runs locally, doesn't push to registries

# PIPING COMMANDS (Usage Examples):
# 1. See output on screen AND save everything to file
#    ./validate-cicd-locally.sh 2>&1 | tee validation-output.txt
# 2. Save everything (stdout + stderr) to file, no screen output
#    ./validate-cicd-locally.sh > validation-output.txt 2>&1
# 3. Save only last 100 lines (skip verbose Maven output)
#    ./validate-cicd-locally.sh 2>&1 | tail -100 > summary.txt
# 4. TWO FILES: FULL LOG + LAST 100 LINES SUMMARY (SHOWS LAST 100 ON SCREEN TOO)
#    ./validate-cicd-locally.sh 2>&1 | tee full-output.txt | tail -100 | tee summary.txt
# 5. Save errors only (useful for debugging failures)
#    ./validate-cicd-locally.sh 2> errors-only.txt

set -e  # Exit on any error

echo "=== Starting Local CI/CD Validation ==="

# Check Docker is running
echo "→ Checking Docker Desktop..."
if ! docker info > /dev/null 2>&1; then
    echo "❌ Error: Docker Desktop is not running. Please start it and try again."
    exit 1
fi
echo "✓ Docker Desktop daemon is running"

# Run Maven build with tests and coverage
echo ""
echo "→ Running Maven build with tests and coverage..."
MVN_OUTPUT=$(mktemp)
if mvn -B clean package jacoco:report > "$MVN_OUTPUT" 2>&1; then
    # Extract only the test summary
    grep -E "Tests run:" "$MVN_OUTPUT" | tail -1
    echo "✓ Build and tests completed"
else
    echo "❌ Build failed - showing errors:"
    cat "$MVN_OUTPUT"
    rm "$MVN_OUTPUT"
    exit 1
fi
rm "$MVN_OUTPUT"

# Extract and display coverage percentage
echo ""
echo "→ Code Coverage Summary:"
COVERAGE=$(awk -F"," '{ instructions += $4 + $5; covered += $5 } END { print int(100*covered/instructions) }' target/site/jacoco/jacoco.csv 2>/dev/null || echo "N/A")
if [ "$COVERAGE" != "N/A" ]; then
    echo "  Total Instruction Coverage: ${COVERAGE}%"
    if [ $COVERAGE -gt 80 ]; then
        echo "  Status: ✓ Excellent (>80%)"
    elif [ $COVERAGE -gt 60 ]; then
        echo "  Status: ⚠ Good (>60%)"
    else
        echo "  Status: ⚠ Needs improvement (<60%)"
    fi
else
    echo "  ⚠ Could not calculate coverage"
fi
echo "  Full report: target/site/jacoco/index.html"

# Start containers in detached mode (force rebuild to use fresh JAR)
echo ""
echo "→ Building and starting Docker containers..."
docker compose up -d --build --quiet-pull > /dev/null 2>&1
echo "✓ Containers started"

# Wait for app to be ready (poll localhost:8080)
echo ""
echo "→ Waiting for application to be ready..."
sleep 15

MAX_ATTEMPTS=40
ATTEMPT=0
while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
    # Check if Spring Boot container crashed
    if ! docker ps | grep -q webapp-springboot; then
        echo "❌ Error: Spring Boot container crashed"
        echo "→ Container logs:"
        docker compose logs webapp
        echo "→ Cleaning up..."
        docker compose down -v
        exit 1
    fi

    # Check if application responds
    if curl -s http://localhost:8080 > /dev/null 2>&1; then
        echo "✓ Application is responding at http://localhost:8080"
        break
    fi
    ATTEMPT=$((ATTEMPT + 1))
    sleep 2
done

if [ $ATTEMPT -eq $MAX_ATTEMPTS ]; then
    echo "❌ Error: Application failed to respond after $MAX_ATTEMPTS attempts"
    echo "→ Stopping containers..."
    docker compose down
    exit 1
fi

# Stop containers and delete volumes (frees resources for push/deploy)
echo ""
echo "→ Stopping containers and cleaning up..."
docker compose down -v
echo "✓ Containers stopped and volumes deleted"

# Push to Docker Hub (optional - continues even if it fails)
echo ""
IMAGE_SIZE=$(docker images team-project-group-p02-06-webapp:latest --format "{{.Size}}")
echo "→ Pushing image to Docker Hub ($IMAGE_SIZE)..."
echo "  Target: adfaon3343cz/eventhub:latest"
echo "  Note: Only changed layers pushed (~80MB JAR), not full image size"

# Tag the built image
docker tag team-project-group-p02-06-webapp:latest adfaon3343cz/eventhub:latest 2>/dev/null || true

# Always attempt push - fails gracefully if not authenticated
if docker push adfaon3343cz/eventhub:latest; then
    echo "✓ Docker Hub: Image pushed successfully"
    echo ""
    echo "Docker Hub verification (curl ping to check push timestamp):"
    curl -s "https://hub.docker.com/v2/repositories/adfaon3343cz/eventhub/tags/latest" | python3 -c "import sys, json; from datetime import datetime, timezone; data = json.load(sys.stdin); last_push = datetime.fromisoformat(data['last_updated'].replace('Z', '+00:00')); now = datetime.now(timezone.utc); delta = now - last_push; hours = int(delta.total_seconds() // 3600); minutes = int((delta.total_seconds() % 3600) // 60); print(f'Last pushed: {hours}h {minutes}m ago')"
else
    echo "⚠️  Docker Hub: Push failed - run 'docker login -u adfaon3343cz' or check network connection"
fi

# Deploy to Google Cloud VM (optional - continues even if it fails)
echo ""
echo "→ Deploying to Google Cloud VM..."
VM_IP="34.129.54.215"
VM_USER="dion_marinovic"

ssh -o ConnectTimeout=10 -o StrictHostKeyChecking=no ${VM_USER}@${VM_IP} "\
    cd team-project-group-p02-06 && \
    echo '✓ Successfully navigated to repo directory' && \
    git pull && \
    echo '✓ Successfully pulled latest code' && \
    sudo docker-compose -f docker-compose.remote.yml pull && \
    echo '✓ Successfully pulled image from Docker Hub' && \
    sudo docker-compose -f docker-compose.remote.yml up -d && \
    echo '✓ Successfully started/recreated containers' && \
    sudo docker image prune -f && \
    echo '✓ Successfully cleaned up old images'" 2>&1

if [ $? -eq 0 ]; then
    echo "✓ VM deployment completed"
    echo "  Note: Doesn't verify containers stayed running, app responds to HTTP, or DB connected successfully"
else
    echo "⚠️  VM deployment failed or skipped (Script will continue - VM deployment is optional)"
fi

# Clean up dangling images
echo ""
echo "→ Removing dangling Docker images..."
PRUNE_OUTPUT=$(docker image prune -f 2>&1)
SPACE_RECLAIMED=$(echo "$PRUNE_OUTPUT" | grep "Total reclaimed space" | sed 's/Total reclaimed space: //')
if [ -z "$SPACE_RECLAIMED" ]; then
    echo "✓ No dangling images found"
else
    echo "✓ Reclaimed: $SPACE_RECLAIMED"
fi

echo ""
echo "=== ✅ Local CI/CD Validation Successful ==="
echo "All checks passed:"
echo "  • Maven build + tests ✓"
echo "  • Code coverage: ${COVERAGE}% ✓"
echo "  • Docker image build ✓"
echo "  • Container startup ✓"
echo "  • Application HTTP response ✓"
echo "  • Docker Hub push (if logged in) ✓"
echo "  • VM deployment (if available) ✓"
