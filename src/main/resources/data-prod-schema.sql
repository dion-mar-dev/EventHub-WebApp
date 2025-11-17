-- BACKUP SCHEMA - Manual MySQL Setup Only
--
-- Purpose: Emergency fallback schema if Flyway migrations fail
-- Normal usage: Flyway handles schema automatically (devprod & prod profiles)
--
-- Use this only if:
-- - Flyway migrations are broken and need manual intervention
-- - Setting up database outside Spring Boot context
--
-- Warning: Schema must match Java entity classes and db/migration/ files
--
-- To reset database (WARNING: deletes all data):
-- DROP DATABASE IF EXISTS webapp; CREATE DATABASE webapp; USE webapp;

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

-- Keywords table
CREATE TABLE IF NOT EXISTS keywords (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(16) NOT NULL UNIQUE,
    color VARCHAR(7)
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

-- Event-Keyword relationship
CREATE TABLE IF NOT EXISTS event_keywords (
    event_id BIGINT NOT NULL,
    keyword_id BIGINT NOT NULL,
    PRIMARY KEY (event_id, keyword_id),
    CONSTRAINT fk_event_keywords_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    CONSTRAINT fk_event_keywords_keyword FOREIGN KEY (keyword_id) REFERENCES keywords(id) ON DELETE CASCADE
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
-- Payments Table
-- ========================================

-- Payments table (tracks payment transactions for RSVPs)
CREATE TABLE IF NOT EXISTS payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rsvp_id BIGINT NOT NULL,
    stripe_payment_intent_id VARCHAR(255) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payments_rsvp FOREIGN KEY (rsvp_id) REFERENCES rsvp(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- Cancelled RSVP Table
-- ========================================

-- Cancelled RSVPs table (tracks all cancelled/blocked RSVPs with payment refund capability)
CREATE TABLE IF NOT EXISTS cancelled_rsvps (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- Original RSVP reference (can be null if RSVP already deleted)
    rsvp_id BIGINT,

    -- User and Event references (required for audit trail)
    user_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,

    -- Cancellation metadata
    cancelled_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    initiated_by VARCHAR(20) NOT NULL, -- 'admin', 'organiser', or 'attendee'
    cancelled_by_user_id BIGINT NOT NULL,

    -- Payment information (copied from original RSVP at time of cancellation)
    payment_status VARCHAR(20),
    amount_paid DECIMAL(10,2),
    stripe_payment_intent_id VARCHAR(255),

    -- Refund tracking
    refund_status VARCHAR(20), -- null, 'refunded', or 'failed'
    refunded_at TIMESTAMP,
    stripe_refund_id VARCHAR(255),
    refunded_by_user_id BIGINT,

    -- Foreign keys
    CONSTRAINT fk_cancelled_rsvp_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_cancelled_rsvp_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    CONSTRAINT fk_cancelled_by_user FOREIGN KEY (cancelled_by_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_refunded_by_user FOREIGN KEY (refunded_by_user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- Blocked RSVP Table
-- ========================================

-- Blocked RSVPs table (tracks users blocked from RSVPing to events)
CREATE TABLE IF NOT EXISTS blocked_rsvps (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    blocked_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    blocked_by_id BIGINT NOT NULL,
    CONSTRAINT unique_event_user_block UNIQUE (event_id, user_id),
    CONSTRAINT fk_blocked_rsvps_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    CONSTRAINT fk_blocked_rsvps_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_blocked_rsvps_blocked_by FOREIGN KEY (blocked_by_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- Reviews Table
-- ========================================

-- Reviews table (tracks user reviews for events)
CREATE TABLE IF NOT EXISTS reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rating INT NOT NULL,
    comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    CONSTRAINT unique_user_event_review UNIQUE (user_id, event_id),
    CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_event FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    CONSTRAINT chk_rating CHECK (rating >= 1 AND rating <= 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- Spring Security Tables
-- ========================================

-- Persistent logins table for "Remember Me" functionality
CREATE TABLE IF NOT EXISTS persistent_logins (
    username VARCHAR(64) NOT NULL,
    series VARCHAR(64) PRIMARY KEY,
    token VARCHAR(64) NOT NULL,
    last_used TIMESTAMP NOT NULL
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

-- Indexes on keywords table
CREATE INDEX idx_keywords_name ON keywords(name);

-- Indexes on rsvp table
CREATE INDEX idx_rsvp_user_id ON rsvp(user_id);
CREATE INDEX idx_rsvp_event_id ON rsvp(event_id);
CREATE INDEX idx_rsvp_stripe_payment_intent_id ON rsvp(stripe_payment_intent_id);

-- Indexes on payments table
CREATE INDEX idx_payments_rsvp_id ON payments(rsvp_id);
CREATE INDEX idx_payments_stripe_payment_intent_id ON payments(stripe_payment_intent_id);

-- Indexes on cancelled_rsvps table
CREATE INDEX idx_cancelled_rsvps_user_id ON cancelled_rsvps(user_id);
CREATE INDEX idx_cancelled_rsvps_event_id ON cancelled_rsvps(event_id);
CREATE INDEX idx_cancelled_rsvps_refund_status ON cancelled_rsvps(refund_status);

-- Indexes on blocked_rsvps table
CREATE INDEX idx_blocked_rsvps_user_id ON blocked_rsvps(user_id);
CREATE INDEX idx_blocked_rsvps_event_id ON blocked_rsvps(event_id);
CREATE INDEX idx_blocked_rsvps_blocked_by_id ON blocked_rsvps(blocked_by_id);

-- Indexes on reviews table
CREATE INDEX idx_reviews_user_id ON reviews(user_id);
CREATE INDEX idx_reviews_event_id ON reviews(event_id);
CREATE INDEX idx_reviews_created_at ON reviews(created_at);

-- ========================================
-- End of Schema Creation
-- ========================================