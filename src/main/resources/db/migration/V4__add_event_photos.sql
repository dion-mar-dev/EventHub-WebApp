-- ========================================
-- Flyway Migration V4: Event Photo Gallery
-- ========================================
-- FEATURE: Photo Gallery for Past Events
-- Allows event organizers to upload up to 20 photos after an event has ended.
-- RSVP'd attendees can view and download photos. Admins have organizer privileges.
--
-- STORAGE:
-- - dev/devprod profiles: Local filesystem (uploads/events/{eventId}/)
-- - prod profile: Google Cloud Storage
-- - This table stores metadata and filename references only
--
-- SECURITY:
-- - UUID-based filenames prevent path traversal attacks
-- - Access control enforced at service layer (organizers/admins upload, RSVP'd users view)
-- - Maximum 20 photos per event enforced in application logic
-- ========================================

-- ========================================
-- Event Photos Table
-- ========================================

CREATE TABLE IF NOT EXISTS event_photos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- Event reference
    event_id BIGINT NOT NULL,

    -- File metadata
    filename VARCHAR(255) NOT NULL,                -- UUID-based filename (e.g., abc123-def456.jpg)
    original_filename VARCHAR(255) NOT NULL,       -- Original user upload name (for display)
    file_size BIGINT NOT NULL,                     -- File size in bytes (for validation tracking)

    -- Upload metadata
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    uploaded_by_user_id BIGINT NOT NULL,           -- Organizer or admin who uploaded

    -- Foreign keys with CASCADE delete
    CONSTRAINT fk_event_photos_event FOREIGN KEY (event_id)
        REFERENCES events(id) ON DELETE CASCADE,
    CONSTRAINT fk_event_photos_uploader FOREIGN KEY (uploaded_by_user_id)
        REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- Indexes for Performance Optimization
-- ========================================

-- Primary lookup: Get all photos for an event (most common query)
CREATE INDEX idx_event_photos_event_id ON event_photos(event_id);

-- Secondary lookup: Track photos uploaded by specific user
CREATE INDEX idx_event_photos_uploaded_by ON event_photos(uploaded_by_user_id);

-- Composite index for ordered retrieval
CREATE INDEX idx_event_photos_event_uploaded_at ON event_photos(event_id, uploaded_at DESC);
