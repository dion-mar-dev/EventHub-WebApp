-- Add Keywords and Event Blocking Features
-- MySQL/MariaDB Compatible
-- Managed by Flyway
-- V2: Keywords, event-keyword relationships, blocked RSVPs, and persistent logins

-- ========================================
-- Keywords Feature
-- ========================================

-- Keywords table
CREATE TABLE IF NOT EXISTS keywords (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(16) NOT NULL UNIQUE,
    color VARCHAR(7)
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
-- Blocking Feature
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
-- Spring Security Enhancement
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

-- Indexes on keywords table
CREATE INDEX idx_keywords_name ON keywords(name);

-- Indexes on blocked_rsvps table
CREATE INDEX idx_blocked_rsvps_user_id ON blocked_rsvps(user_id);
CREATE INDEX idx_blocked_rsvps_event_id ON blocked_rsvps(event_id);
CREATE INDEX idx_blocked_rsvps_blocked_by_id ON blocked_rsvps(blocked_by_id);
