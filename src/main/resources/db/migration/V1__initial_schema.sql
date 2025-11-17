-- ========================================
-- Flyway Migration V1: Initial Schema
-- ========================================
-- WHAT IS FLYWAY:
-- Flyway is a database migration tool that automatically applies versioned SQL scripts
-- on application startup. It tracks which migrations have been applied in the
-- 'flyway_schema_history' table to avoid re-running them.
--
-- HOW IT WORKS IN THIS CODEBASE:
-- 1. On startup (prod profile only), Flyway connects to MySQL via JDBC
-- 2. Checks 'flyway_schema_history' to see which migrations already ran
-- 3. Executes pending migrations in version order: V1 → V2 → V3 → V4 → etc.
-- 4. Records each successful migration in 'flyway_schema_history'
-- 5. Hibernate then validates schema matches entity classes
--
-- MIGRATION FILES:
-- - Located in: src/main/resources/db/migration/
-- - Naming: V{version}__{description}.sql (e.g., V1__initial_schema.sql)
-- - V1 (this file): Core tables (users, categories, events, rsvp)
-- - V2: Keywords and blocking features
-- - V3: Payments and reviews
-- - Future: V4, V5, etc. for incremental schema changes (ALTER TABLE, etc.)
--
-- PREREQUISITES:
-- - MySQL server running
-- - Database 'webapp' created manually: CREATE DATABASE webapp;
-- - App started with: --spring.profiles.active=prod
--
-- NOTE: Never modify migration files after they've been applied to production.
-- Always create new migration files (V4, V5, etc.) for schema changes.
--
-- ========================================
-- V1: Core Tables (Event Management Foundation)
-- ========================================

-- ========================================
-- Core Entity Tables
-- ========================================

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    deactivated BOOLEAN DEFAULT FALSE,
    deactivated_by_admin_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    role VARCHAR(50) DEFAULT 'ROLE_USER'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Categories table
CREATE TABLE IF NOT EXISTS categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    colour_code VARCHAR(20)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Events table
CREATE TABLE IF NOT EXISTS events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(36) NOT NULL UNIQUE,
    title VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    event_date DATE NOT NULL,
    event_time TIME NOT NULL,
    location VARCHAR(200) NOT NULL,
    capacity INT,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    category_id BIGINT NOT NULL,
    is_deactivated BOOLEAN NOT NULL DEFAULT FALSE,
    deactivated_by_admin_id BIGINT,
    price DECIMAL(10,2),
    requires_payment BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_events_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_events_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- Join Tables (Many-to-Many Relationships)
-- ========================================

-- User-Category relationship (user preferences)
CREATE TABLE IF NOT EXISTS user_categories (
    user_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, category_id),
    CONSTRAINT fk_user_categories_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_categories_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- RSVP Table
-- ========================================

-- RSVP table (event attendance tracking)
CREATE TABLE IF NOT EXISTS rsvp (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    rsvp_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    payment_status VARCHAR(20),
    stripe_payment_intent_id VARCHAR(255),
    amount_paid DECIMAL(10,2),
    CONSTRAINT unique_user_event_rsvp UNIQUE (user_id, event_id),
    CONSTRAINT fk_rsvp_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_rsvp_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- Indexes for Performance Optimization
-- ========================================

-- Indexes on users table
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);

-- Indexes on events table
CREATE INDEX idx_events_event_date ON events(event_date);
CREATE INDEX idx_events_created_by ON events(created_by);
CREATE INDEX idx_events_category_id ON events(category_id);
CREATE INDEX idx_events_uid ON events(uid);

-- Indexes on categories table
CREATE INDEX idx_categories_name ON categories(name);

-- Indexes on rsvp table
CREATE INDEX idx_rsvp_user_id ON rsvp(user_id);
CREATE INDEX idx_rsvp_event_id ON rsvp(event_id);
CREATE INDEX idx_rsvp_stripe_payment_intent_id ON rsvp(stripe_payment_intent_id);
