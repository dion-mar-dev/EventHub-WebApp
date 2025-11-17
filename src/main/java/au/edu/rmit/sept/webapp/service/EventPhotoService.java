package au.edu.rmit.sept.webapp.service;

import au.edu.rmit.sept.webapp.model.Event;
import au.edu.rmit.sept.webapp.model.EventPhoto;
import au.edu.rmit.sept.webapp.model.User;
import au.edu.rmit.sept.webapp.repository.EventPhotoRepository;
import au.edu.rmit.sept.webapp.repository.EventRepository;
import au.edu.rmit.sept.webapp.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * EventPhotoService
 * Handles business logic for event photo gallery feature.
 *
 * FEATURES:
 * - Upload photos (organizers/admins only, past events only)
 * - Delete photos (organizers/admins only)
 * - Get photos for display (with URLs)
 *
 * BUSINESS RULES:
 * - Maximum 20 photos per event
 * - Only organizers and admins can upload/delete
 * - Photos can only be added after event has ended
 * - File validation: JPEG/PNG only, <5MB each
 */
@Service
@Transactional(readOnly = true)
public class EventPhotoService {

    private static final int MAX_PHOTOS_PER_EVENT = 20;

    private final EventPhotoRepository eventPhotoRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final PhotoStorageService photoStorageService;
    private final UserService userService;

    public EventPhotoService(EventPhotoRepository eventPhotoRepository,
                             EventRepository eventRepository,
                             UserRepository userRepository,
                             PhotoStorageService photoStorageService,
                             UserService userService) {
        this.eventPhotoRepository = eventPhotoRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.photoStorageService = photoStorageService;
        this.userService = userService;
    }

    /**
     * Uploads multiple photos to an event gallery.
     *
     * @param eventId The event ID
     * @param files Array of photo files to upload
     * @param uploaderId The user ID of the uploader (must be organizer or admin)
     * @return Map of upload results (filename -> success/error message)
     * @throws AccessDeniedException if user is not organizer or admin
     * @throws IllegalStateException if event hasn't ended yet
     * @throws IllegalArgumentException if adding photos would exceed 20-photo limit
     */
    @Transactional
    public Map<String, String> uploadEventPhotosAsOrganiser(Long eventId, MultipartFile[] files, Long uploaderId) {
        // Validate event exists
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found"));

        // Validate user exists
        User uploader = userRepository.findById(uploaderId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        // Verify user is organizer or admin
        if (!event.getCreatedBy().getId().equals(uploaderId) && !isUserAdmin(uploaderId)) {
            throw new AccessDeniedException("Only the event organizer or admin can upload photos");
        }

        // Verify event has ended
        if (!hasEventEnded(event)) {
            throw new IllegalStateException("Photos can only be uploaded after the event has ended");
        }

        // Check current photo count
        long currentPhotoCount = eventPhotoRepository.countByEventId(eventId);

        // Validate total count won't exceed limit
        if (currentPhotoCount + files.length > MAX_PHOTOS_PER_EVENT) {
            long remaining = MAX_PHOTOS_PER_EVENT - currentPhotoCount;
            throw new IllegalArgumentException(
                String.format("Cannot upload %d photos. Current: %d, Maximum: %d, Remaining: %d",
                    files.length, currentPhotoCount, MAX_PHOTOS_PER_EVENT, remaining)
            );
        }

        // Upload each file
        Map<String, String> results = new HashMap<>();

        for (MultipartFile file : files) {
            String originalFilename = file.getOriginalFilename();

            try {
                // Upload file (validates type and size)
                String savedFilename = photoStorageService.uploadPhoto(eventId, file, originalFilename);

                // Create database record
                EventPhoto photo = new EventPhoto(
                    savedFilename,
                    originalFilename,
                    file.getSize(),
                    event,
                    uploader
                );
                eventPhotoRepository.save(photo);

                results.put(originalFilename, "Success");

            } catch (IllegalArgumentException | IOException e) {
                // Upload failed - log error but continue with other files
                results.put(originalFilename, "Failed: " + e.getMessage());
            }
        }

        return results;
    }

    /**
     * Deletes a photo from an event gallery.
     *
     * @param eventId The event ID
     * @param photoId The photo ID to delete
     * @param deleterId The user ID of the requester (must be organizer or admin)
     * @throws AccessDeniedException if user is not organizer or admin
     * @throws EntityNotFoundException if photo or event not found
     */
    @Transactional
    public void deleteEventPhotoAsOrganiser(Long eventId, Long photoId, Long deleterId) {
        // Validate event exists
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found"));

        // Validate photo exists
        EventPhoto photo = eventPhotoRepository.findById(photoId)
                .orElseThrow(() -> new EntityNotFoundException("Photo not found"));

        // Verify photo belongs to this event
        if (!photo.getEvent().getId().equals(eventId)) {
            throw new IllegalArgumentException("Photo does not belong to this event");
        }

        // Verify user is organizer or admin
        if (!event.getCreatedBy().getId().equals(deleterId) && !isUserAdmin(deleterId)) {
            throw new AccessDeniedException("Only the event organizer or admin can delete photos");
        }

        // Delete file from storage
        try {
            photoStorageService.deletePhoto(eventId, photo.getFilename());
        } catch (IOException e) {
            // Log error but continue with database deletion
            // Database deletion ensures UI consistency even if file delete fails
            System.err.println("Failed to delete photo file: " + e.getMessage());
        }

        // Delete database record
        eventPhotoRepository.delete(photo);
    }

    /**
     * Gets all photos for an event with their URLs.
     *
     * @param eventId The event ID
     * @return List of photo data maps (id, originalFilename, photoUrl, thumbnailUrl, uploadedAt)
     */
    public List<Map<String, Object>> getEventPhotos(Long eventId) {
        // Validate event exists
        if (!eventRepository.existsById(eventId)) {
            throw new EntityNotFoundException("Event not found");
        }

        List<EventPhoto> photos = eventPhotoRepository.findByEventIdOrderByUploadedAtDesc(eventId);

        return photos.stream()
                .map(photo -> {
                    Map<String, Object> photoData = new HashMap<>();
                    photoData.put("id", photo.getId());
                    photoData.put("filename", photo.getFilename());
                    photoData.put("originalFilename", photo.getOriginalFilename());
                    photoData.put("photoUrl", photoStorageService.getPhotoUrl(eventId, photo.getFilename()));
                    photoData.put("thumbnailUrl", photoStorageService.getThumbnailUrl(eventId, photo.getFilename()));
                    photoData.put("uploadedAt", photo.getUploadedAt().toString());
                    photoData.put("uploadedBy", photo.getUploadedBy().getUsername());
                    return photoData;
                })
                .collect(Collectors.toList());
    }

    /**
     * Gets the current photo count for an event.
     *
     * @param eventId The event ID
     * @return Number of photos for the event
     */
    public long getPhotoCount(Long eventId) {
        return eventPhotoRepository.countByEventId(eventId);
    }

    /**
     * Checks if a user is an admin.
     */
    private boolean isUserAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        return userService.hasRole(userId, "ROLE_ADMIN");
    }

    /**
     * Checks if an event has ended.
     * An event is considered ended if the current date/time is past the event date/time.
     */
    private boolean hasEventEnded(Event event) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        // Event has ended if:
        // 1. Event date is in the past, OR
        // 2. Event date is today AND event time has passed
        return event.getEventDate().isBefore(today) ||
               (event.getEventDate().isEqual(today) && event.getEventTime().isBefore(now));
    }
}
