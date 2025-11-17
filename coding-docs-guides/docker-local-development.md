# Docker Setup Guide - Event Management System

## Overview
This project uses Docker to containerize the Spring Boot application and MySQL database. The entire stack runs locally with a single command, eliminating manual MySQL setup.

## Architecture
- **MySQL Container**: Official `mysql:8.0` image with empty database initialization
- **Spring Boot Container**: Custom image built from our Dockerfile with Flyway integration
- **Docker Compose**: Orchestrates both containers, handles networking and startup order
- **Flyway**: Manages database schema via versioned migration scripts (V1, V2, V3...)

## Prerequisites
- Docker Desktop installed and running
- Java 17 (for building JAR locally)
- Maven (for building JAR locally)

## File Structure
```
project-root/
├── Dockerfile              # Builds Spring Boot container
├── docker-compose.yml      # Orchestrates both services
├── .dockerignore          # Excludes unnecessary files from build
├── pom.xml
├── src/
│   └── main/
│       └── resources/
│           ├── application-prod.properties    # Production config
│           └── data-prod-schema.sql          # Auto-runs in MySQL
└── target/
    └── webapp-*.jar       # Built by Maven
```

## How It Works

### Startup Flow
1. **MySQL container starts first**
   - Creates empty `webapp` database (via `MYSQL_DATABASE` environment variable)
   - No schema initialization (Flyway handles this)
   - Health check ensures MySQL is ready before Spring Boot starts

2. **Spring Boot container starts**
   - Waits for MySQL health check to pass
   - Connects to MySQL using service name `mysql` as hostname
   - Runs with `prod` profile (set via environment variable)
   - **Flyway runs BEFORE Hibernate validation**
     - Checks `flyway_schema_history` table for applied migrations
     - Executes pending migrations in order: V1 → V2 → V3
     - V1: Core tables (users, events, categories, rsvp)
     - V2: Keywords and blocking features
     - V3: Payments and reviews
     - Records each migration in `flyway_schema_history`
   - Hibernate `validate` mode verifies schema matches entities
   - **DataInitializer runs** (only if database is empty)
     - Checks: `if (userRepository.count() == 0)`
     - Creates 35 users, 8 categories, 33 events, 11 keywords, 41 RSVPs
     - Safe to run on every restart (idempotent)

### Key Configuration Details

**Profile Management:**
- Docker Compose sets `SPRING_PROFILES_ACTIVE=prod` via environment variable
- Your local `application.properties` should have `spring.profiles.active` commented out
- This allows Docker to control the profile without code changes

**Database Connection:**
- Spring Boot uses environment variables: `DB_HOST=mysql`, `DB_USER=root`, `DB_NAME=webapp`
- No password (blank) - matches local development setup
- MySQL container allows empty password via `MYSQL_ALLOW_EMPTY_PASSWORD: yes`

**Flyway Configuration (Spring Boot 3.x):**
- Circular dependency fix applied in `application-prod.properties`:
  - `spring.jpa.defer-datasource-initialization=false` - Prevents initialization deadlock
  - `spring.main.allow-circular-references=true` - Allows Spring to resolve circular dependencies
  - `spring.flyway.depends-on=entityManagerFactory` - Controls initialization order
- These properties are **required** for Spring Boot 3.x + Flyway + Hibernate compatibility

**Data Persistence:**
- MySQL data stored in Docker volume: `mysql_data`
- Data survives container restarts (`docker compose down` then `up`)
- Only deleted with `docker compose down -v`
- **Volume reuse with Flyway is safe**: Flyway checks history and skips already-applied migrations

**DataInitializer Behavior:**
- Runs on **every** Spring Boot startup
- Checks if data exists before creating (idempotent)
- First run: Creates all sample data
- Subsequent runs: Skips creation, prints "Database already contains data"
- MySQL persistence means data stays even after restarts

## Commands

### First Time Setup

**Build JAR (skip outdated tests):**
```bash
# Mac/Linux
mvn clean package -DskipTests

# Windows (PowerShell/CMD)
mvn clean package -DskipTests
```

**Start Docker Desktop:**
- Mac: Open Docker.app from Applications
- Windows: Open Docker Desktop from Start Menu
- Verify: `docker --version`

**Start containers:**
```bash
# Mac/Linux/Windows
docker compose up
```

**To get your terminal back:** Use `docker compose up -d` (detached mode) or open a new terminal tab.

### Day-to-Day Usage

**Start the stack:**
```bash
docker compose up
# Or detached: docker compose up -d
```

**Stop the stack (keeps data):**
```bash
docker compose down
```

**View logs (if running detached):**
```bash
docker compose logs -f
# Or specific service: docker compose logs -f webapp
```

**Rebuild after code changes:**
```bash
mvn clean package -DskipTests
docker compose up --build
```

**Access the application:**
- Web app: `http://localhost:8080`
- MySQL: `localhost:3306` (use any MySQL client)

### Troubleshooting

**Complete reset (wipe database):**
```bash
docker compose down -v  # -v deletes volumes (all data)
mvn clean package -DskipTests
docker compose up
```

**Check container status:**
```bash
docker compose ps
```

**View specific service logs:**
```bash
docker compose logs webapp
docker compose logs mysql
```

**Connect to MySQL directly:**
```bash
docker exec -it webapp-mysql mysql -u root webapp
```

**Force rebuild (if Dockerfile changed):**
```bash
docker compose up --build
```

## Important Notes

### Schema Management
- **Flyway-managed approach**: Schema defined in versioned migration files
  - `V1__initial_schema.sql`: Core tables (users, events, categories, rsvp)
  - `V2__add_keywords_and_blocking.sql`: Keywords and blocking features
  - `V3__add_payments_and_reviews.sql`: Payments and reviews
- **Validation mode**: Hibernate uses `validate` (read-only, ensures schema matches entities)
- **Entity changes require**: Create new migration file (e.g., `V4__add_new_field.sql` with `ALTER TABLE`)
- **Migration execution**: Flyway runs pending migrations automatically on startup
- **Migration tracking**: `flyway_schema_history` table tracks which migrations have been applied
- **Volume reuse**: Safe with Flyway - checks history and skips already-applied migrations
- **Legacy file**: `data-prod-schema.sql` is now orphaned/backup code (not used by Docker)

### When DataInitializer Runs
- ✅ First startup with empty database → Creates all sample data
- ✅ Container restart with existing data → Skips creation
- ✅ After `docker compose down` (keeps volume) → Skips creation
- ✅ After `docker compose down -v` (deletes volume) → Creates data again

### Profile Behavior
- **Dev profile** (default): H2 in-memory database, not used in Docker
- **Prod profile** (Docker): MySQL database, set by Docker Compose
- Docker overrides profile via environment variable, no code changes needed

### Common Pitfalls
- ❌ Forgot to build JAR: Docker copies old JAR → rebuild with `mvn clean package`
- ❌ Docker Desktop not running: `docker compose up` fails → start Docker Desktop
- ❌ Port conflicts: Another app using 3306 or 8080 → change ports in `docker-compose.yml`
- ❌ Schema drift: Entities don't match database → create new migration file (e.g., `V4__fix_schema.sql`)
- ❌ Modified migration file after applied: Flyway checksum mismatch → never modify applied migrations, create new ones
- ❌ Circular dependency error: Missing Spring Boot 3.x compatibility properties in `application-prod.properties` (see Flyway Configuration section above)

## Clean Slate Workflow

If you need to start completely fresh:
```bash
# Stop everything and delete all data
docker compose down -v

# Rebuild JAR
mvn clean package -DskipTests

# Start fresh (schema + sample data created)
docker compose up
```

## Assignment Requirements Met
✅ Docker images created (Dockerfile for Spring Boot)  
✅ Docker Compose orchestrates multiple services (webapp + MySQL)  
✅ Services boot locally with `docker compose up`  
✅ Production-grade database (MySQL 8.0)  
✅ Containerized application with proper networking

## Questions?
- Check Docker Desktop for container logs and status
- Use `docker compose logs` for debugging
- Verify MySQL schema: `docker exec -it webapp-mysql mysql -u root webapp`