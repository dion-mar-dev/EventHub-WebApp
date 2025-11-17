# MySQL Production Setup Guide (Flyway)

**Note**: This guide reflects the Flyway-managed approach. For legacy manual setup, see `data-prod-schema.sql` (now orphaned/backup code).

## 1. Initial Setup

- Start MySQL service:
  ```bash
  # MacOS
  brew services start mysql  # start server
  brew services stop mysql   # stop server

  # Windows
  net start MySQL80  # start server (or your MySQL service name)
  net stop MySQL80   # stop server
  ```

- Connect to MySQL:
  ```bash
  mysql -u root
  # Or with password: mysql -u root -p
  ```

- Create empty database (Flyway will create tables):
  ```sql
  CREATE DATABASE webapp;
  ```

- Verify database created (should be empty):
  ```sql
  USE webapp;
  SHOW TABLES;  -- Should show: Empty set (0.00 sec)
  ```

## 2. First Application Run

- Run application with prod profile:
  ```bash
  mvn spring-boot:run -Dspring-boot.run.profiles=prod
  # Or set environment variable: SPRING_PROFILES_ACTIVE=prod
  ```

- **What happens automatically:**
  1. Flyway connects to MySQL via JDBC
  2. Creates `flyway_schema_history` table to track migrations
  3. Runs pending migrations in order:
     - V1: Core tables (users, categories, events, rsvp)
     - V2: Keywords and blocking features
     - V3: Payments and reviews
  4. Hibernate validates schema matches entities
  5. DataInitializer creates sample data (if database empty)

- Verify tables created:
  ```sql
  USE webapp;
  SHOW TABLES;
  -- Should show: blocked_rsvps, cancelled_rsvps, categories, event_keywords,
  --              events, flyway_schema_history, keywords, payments,
  --              persistent_logins, reviews, rsvp, user_categories, users
  ```

- **DataInitializer Profile Management:**
  ```java
  @Profile("prod")  // for prod environment (when doing MySQL db only)
  // OR
  @Profile("dev")   // for dev environment (normal standard use)
  // OR
  // remove @Profile entirely, which is just the same as dev
  ```

- After first successful run, change to:
  ```java
  @Profile("nonexistent-profile")  // prevents duplicate data creation, just disables the script basically
  ```

## 3. Production Configuration

- **Flyway Configuration** (in `application-prod.properties`):
  ```properties
  spring.flyway.enabled=true
  spring.flyway.baseline-on-migrate=true
  spring.flyway.locations=classpath:db/migration
  spring.flyway.validate-on-migrate=false
  spring.flyway.depends-on=entityManagerFactory

  # Circular Dependency Fix for Spring Boot 3.x
  spring.jpa.defer-datasource-initialization=false
  spring.main.allow-circular-references=true
  ```

  **Note**: The circular dependency properties are required for Spring Boot 3.x compatibility:
  - `defer-datasource-initialization=false` prevents DataSource from waiting for EntityManagerFactory
  - `allow-circular-references=true` allows Spring to resolve circular bean references
  - `flyway.depends-on=entityManagerFactory` explicitly controls initialization order

- **Migration Files** (in `src/main/resources/db/migration/`):
  - `V1__initial_schema.sql`: Core tables
  - `V2__add_keywords_and_blocking.sql`: Keywords and blocking
  - `V3__add_payments_and_reviews.sql`: Payments and reviews

- **MySQL config** in `application-prod.properties`
- **Validation mode**: Hibernate uses `validate` (read-only, never changes schema)
- **Schema management**: Flyway executes migrations automatically on startup

## 4. Schema Changes (Flyway Workflow)

- **When updating entity classes, create a new migration file:**
  ```java
  // Example: Adding field to User entity
  @Column(name = "phone")
  private String phone;
  ```

- **Create new migration file** (e.g., `V4__add_user_phone.sql`):
  ```sql
  -- V4: Add phone field to users table
  ALTER TABLE users ADD COLUMN phone VARCHAR(20);
  ```

- **Flyway workflow:**
  1. Modify entity class (e.g., add new field)
  2. Create new migration file: `V4__description.sql` in `src/main/resources/db/migration/`
  3. Start app → Flyway detects V4 not in history → executes V4 → updates `flyway_schema_history`
  4. Hibernate validates schema matches entities

- **Important rules:**
  - **NEVER modify** migration files after they've been applied (checksum mismatch error)
  - **ALWAYS create new** migration files for schema changes
  - Migration files execute in version order: V1 → V2 → V3 → V4 → ...
  - Flyway tracks applied migrations in `flyway_schema_history` table

## Notes
- **MySQL persists data** (unlike H2 which resets on every restart)
- **Flyway manages schema** (automatically runs migrations on startup)
- **DataInitializer** only runs once for initial sample data (checks if data exists)
- **Schema must exactly match Java entities** (Hibernate validates on startup)
- **Profiles:**
  - Default profile (no profile) and dev profile: H2 in-memory database
  - Prod profile: MySQL production settings + Flyway enabled
- **To run with prod profile:** `mvn spring-boot:run -Dspring-boot.run.profiles=prod` or set `SPRING_PROFILES_ACTIVE=prod`
- **Always stop MySQL service** when not needed to save resources

## Flyway Migration History

Check which migrations have been applied:
```sql
USE webapp;
SELECT * FROM flyway_schema_history;
```

Output shows:
- `installed_rank`: Execution order
- `version`: Migration version (1, 2, 3, ...)
- `description`: Migration description from filename
- `script`: Migration filename
- `checksum`: Hash of file content (detects modifications)
- `installed_on`: When migration was executed
- `success`: Whether migration succeeded

## Legacy Files
- **data-prod-schema.sql**: Now orphaned/backup code (Flyway manages schema)
- Keep as reference but do NOT run manually
- Docker Compose no longer auto-runs this file (commented out)