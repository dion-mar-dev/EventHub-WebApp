package au.edu.rmit.sept.webapp.repository;

import au.edu.rmit.sept.webapp.model.EventPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * EventPhotoRepository
 * Data access layer for event photo gallery feature.
 *
 * KEY QUERIES:
 * - findByEventIdOrderByUploadedAtDesc: Get all photos for an event (newest first)
 * - countByEventId: Check photo count to enforce 20-photo limit
 */
public interface EventPhotoRepository extends JpaRepository<EventPhoto, Long> {

    /**
     * Get all photos for a specific event, ordered by upload time (newest first).
     * Used to display photo gallery on event details page.
     *
     * @param eventId The event ID
     * @return List of event photos ordered by uploadedAt descending
     */
    List<EventPhoto> findByEventIdOrderByUploadedAtDesc(Long eventId);

    /**
     * Count total photos for a specific event.
     * Used to enforce 20-photo limit before upload.
     *
     * @param eventId The event ID
     * @return Number of photos for the event
     */
    long countByEventId(Long eventId);
}
