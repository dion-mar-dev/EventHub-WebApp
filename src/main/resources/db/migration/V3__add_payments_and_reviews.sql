-- Add Payment Processing and Reviews Features
-- MySQL/MariaDB Compatible
-- Managed by Flyway
-- V3: Payments, cancelled RSVPs with refunds, and event reviews

-- ========================================
-- Payment Processing Feature
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
-- Reviews Feature
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
-- Indexes for Performance Optimization
-- ========================================

-- Indexes on payments table
CREATE INDEX idx_payments_rsvp_id ON payments(rsvp_id);
CREATE INDEX idx_payments_stripe_payment_intent_id ON payments(stripe_payment_intent_id);

-- Indexes on cancelled_rsvps table
CREATE INDEX idx_cancelled_rsvps_user_id ON cancelled_rsvps(user_id);
CREATE INDEX idx_cancelled_rsvps_event_id ON cancelled_rsvps(event_id);
CREATE INDEX idx_cancelled_rsvps_refund_status ON cancelled_rsvps(refund_status);

-- Indexes on reviews table
CREATE INDEX idx_reviews_user_id ON reviews(user_id);
CREATE INDEX idx_reviews_event_id ON reviews(event_id);
CREATE INDEX idx_reviews_created_at ON reviews(created_at);
