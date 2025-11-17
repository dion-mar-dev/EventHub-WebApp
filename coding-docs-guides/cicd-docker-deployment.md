# CI/CD Pipeline & Docker Hub Deployment Guide

Quick guide for understanding our automated deployment pipeline and testing remote builds.
```bash
# QUICK CLI COMMANDS
# ============================================
# LOCAL DEVELOPMENT (Build from Source)
# ============================================
# Start containers (builds image from local code)
docker compose up
# Stop containers (keep database volume)
docker compose down
# Stop containers and doesn't delete database volume ( no fresh start)
docker compose down
# ============================================
# REMOTE TESTING (Pull from Docker Hub)
# ============================================
# Pull latest image from Docker Hub (shouldn't be protected/require auth, should be public)
docker pull adfaon3343cz/eventhub:latest
# Start containers (uses pre-built image from Docker Hub)
docker compose -f docker-compose.remote.yml up
# Stop containers (keep database volume)
docker compose -f docker-compose.remote.yml down
# Stop containers and  doesn't delete database volume (no fresh start)
docker compose -f docker-compose.remote.yml down
# Note: The -v flag deletes volumes, forcing a fresh database on next startup.
#       Without it, your database persists across restarts.
# Tip:  Add -d flag to run containers in detached mode (frees up terminal).
#       Example: docker compose up -d

# ============================================
# QUICK PAUSE/RESUME (FOR DEMOS)
# ============================================
# Quick pause/resume (for demos - faster than down/up, keeps containers)
docker compose stop                                    # Pause containers (no removal)
docker compose start                                   # Resume stopped containers
docker compose -f docker-compose.remote.yml stop       # Pause remote containers
docker compose -f docker-compose.remote.yml start      # Resume remote containers
# stop/start = pause/resume existing containers (fast, keeps state)
# down/up = remove/recreate containers (slower, fresh state)
```

## How CD Works

### Trigger
- **PR to main** → Runs CI only (tests, coverage, build)
- **PR merge** → Runs CI + CD automatically (builds image, pushes to Docker Hub)
- **Direct push to main**:
  - `[run CI]` in commit message → CI only, skip CD
  - `[run CD]` in commit message → CI + CD (builds image, pushes to Docker Hub)
  - No flags → Nothing runs (saves GitHub Actions VM minutes)

### What Happens on Merge
1. GitHub Actions spins up Ubuntu VM
2. Builds JAR file (`mvn clean package -DskipTests`)
3. Builds Docker image from `Dockerfile`
4. Pushes to Docker Hub:
   - `adfaon3343cz/eventhub:latest` (latest version)
   - `adfaon3343cz/eventhub:<commit-sha>` (specific version)

### Where Images Live
- **Registry**: Docker Hub
- **Repository**: `adfaon3343cz/eventhub`
- **URL**: https://hub.docker.com/r/adfaon3343cz/eventhub

---

## Testing Remote Builds

### Option 1: Quick Test (Pull & Run Container Only)

**Prerequisites**: Docker Desktop running

**Pull the image:**
```bash
docker pull adfaon3343cz/eventhub:latest
```

**Run standalone** (will fail - needs MySQL):
```bash
docker run -p 8080:8080 adfaon3343cz/eventhub:latest
# Expected: crashes looking for MySQL at localhost:3306
```

---

### Option 2: Full Stack Test (App + MySQL)

**Use the remote compose file:**
```bash
docker compose -f docker-compose.remote.yml up
```

**What this does:**
- Pulls `adfaon3343cz/eventhub:latest` from Docker Hub (NOT local build)
- Starts MySQL container
- Connects app to MySQL
- Runs full production setup

**Access app:**
- Open: http://localhost:8080

**Stop containers:**
```bash
docker compose -f docker-compose.remote.yml down
```

---

## Local vs Remote Development

### Local Development (Build from Source)
```bash
docker compose up
```
- Uses `docker-compose.yml`
- Builds image from local code (`build: .`)
- Good for testing changes before committing

### Remote Testing (Use CD Output)
```bash
docker compose -f docker-compose.remote.yml up
```
- Uses `docker-compose.remote.yml`
- Pulls pre-built image from Docker Hub (`image: adfaon3343cz/eventhub:latest`)
- Tests what actually gets deployed
- Verifies CD pipeline worked

---

## Common Commands

**Check Docker is running:**
```bash
docker --version
```

**Login to Docker Hub** (if needed for private images):
```bash
docker login
# Username: adfaon3343cz
# Password: <Docker Hub access token>
```

**View downloaded images:**
```bash
docker images
```

**View running containers:**
```bash
docker ps
```

**Stop all containers:**
```bash
docker compose -f docker-compose.remote.yml down
# or
docker compose down  # for local
```

**Force re-pull latest image:**
```bash
docker compose -f docker-compose.remote.yml pull
docker compose -f docker-compose.remote.yml up
```

---

## Troubleshooting

**"Cannot connect to Docker daemon"**
- Start Docker Desktop app
- Wait for whale icon in menu bar (Mac)

**"Image not found" or "pull access denied"**
- Check image name: `adfaon3343cz/eventhub:latest`
- Image is public - no login needed

**App crashes with "missing table [something]"**
- Schema mismatch - rebuild image after schema changes
- Commit + push → triggers CD → new image built

**Port 8080 already in use**
- Stop other running instances: `docker compose down`
- Or kill local Spring Boot: `lsof -ti:8080 | xargs kill`

**MySQL fails to start**
- First startup takes 30s (initializing database)
- Check health: `docker ps` (should show "healthy")

---

## File Reference

- **`.github/workflows/ci-cd.yml`** - CI/CD pipeline config
- **`Dockerfile`** - Image build instructions (what gets packaged)
- **`docker-compose.yml`** - Local dev (builds from source)
- **`docker-compose.remote.yml`** - Remote test (pulls from Docker Hub)
- **`src/main/resources/data-prod-schema.sql`** - Database schema (must match entities)

---

## Key Differences: Local vs Remote Compose

| Feature | `docker-compose.yml` | `docker-compose.remote.yml` |
|---------|---------------------|----------------------------|
| **Image source** | Builds locally | Pulls from Docker Hub |
| **Use case** | Development | Testing CD output |
| **Speed** | Slower (rebuilds) | Faster (pre-built) |
| **Code changes** | Immediate | Need commit → merge → CD |
| **Container names** | `webapp-springboot` | `webapp-springboot-remote` |
| **Volumes** | `mysql_data` | `mysql_data_remote` |

---

## Verifying CD Worked

After merging to main:

1. **Check GitHub Actions**:
   - Go to repo → Actions tab
   - Look for green checkmark on workflow run
   - Click into `build-docker-image` job
   - Should see: "Docker image pushed to adfaon3343cz/eventhub:latest"

2. **Pull and test:**
   ```bash
   docker pull adfaon3343cz/eventhub:latest
   docker compose -f docker-compose.remote.yml up
   ```

3. **Verify version:**
   - Check image digest/tags on Docker Hub
   - Or inspect locally: `docker inspect adfaon3343cz/eventhub:latest`

---

## Notes

- Images are **public** - anyone can pull without credentials (but very hard to pull image if you don't already know the docker hub username adfaon3343cz)
- CD only runs on **main branch** (not PRs)
- Each commit gets tagged with SHA for rollback capability
- MySQL uses official `mysql:8.0` image (not built by us)
- **GHCR (GitHub Container Registry)** - GitHub's own Docker registry, similar to Docker Hub but integrated with GitHub. We use Docker Hub instead due to organisation permission restrictions ("installation not allowed to Create organization package" error)
