package au.edu.rmit.sept.webapp.controller;

import au.edu.rmit.sept.webapp.service.EventPhotoService;
import au.edu.rmit.sept.webapp.service.PhotoStorageService;
import au.edu.rmit.sept.webapp.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * EventPhotoController
 * REST API endpoints for event photo gallery feature.
 *
 * ENDPOINTS:
 * - POST /api/events/{eventId}/photos/upload - Upload photos (organizer/admin only)
 * - DELETE /api/events/{eventId}/photos/{photoId} - Delete photo (organizer/admin only)
 * - GET /api/events/{eventId}/photos - Get all photos for event
 *
 * SECURITY:
 * - Upload/delete require authentication and organizer/admin role
 * - Get photos is public (access control handled at page level)
 */
@RestController
@RequestMapping("/api/events")
public class EventPhotoController {

    private final EventPhotoService eventPhotoService;
    private final UserService userService;
    private final PhotoStorageService photoStorageService;

    public EventPhotoController(EventPhotoService eventPhotoService, UserService userService, PhotoStorageService photoStorageService) {
        this.eventPhotoService = eventPhotoService;
        this.userService = userService;
        this.photoStorageService = photoStorageService;
    }

    /**
     * Uploads one or more photos to an event gallery.
     *
     * @param eventId Event ID
     * @param files Array of photo files to upload
     * @param authentication Current authenticated user
     * @return Upload results (success/failure per file)
     */
    @PostMapping("/{eventId}/photos/upload")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> uploadPhotos(
            @PathVariable Long eventId,
            @RequestParam("files") MultipartFile[] files,
            Authentication authentication) {

        try {
            // Get current user ID
            Long userId = userService.getUserIdByUsername(authentication.getName());

            // Validate files array
            if (files == null || files.length == 0) {
                return ResponseEntity.badRequest().body(
                    Map.of("error", "No files provided")
                );
            }

            // Upload photos
            Map<String, String> results = eventPhotoService.uploadEventPhotosAsOrganiser(
                eventId, files, userId
            );

            // Check if all uploads succeeded
            boolean allSuccess = results.values().stream()
                .allMatch(result -> result.equals("Success"));

            if (allSuccess) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "All photos uploaded successfully",
                    "count", files.length,
                    "results", results
                ));
            } else {
                // Partial success
                long successCount = results.values().stream()
                    .filter(result -> result.equals("Success"))
                    .count();

                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", String.format("%d of %d photos uploaded successfully", successCount, files.length),
                    "results", results
                ));
            }

        } catch (AccessDeniedException e) {
            return ResponseEntity.status(403).body(
                Map.of("error", "Only event organizers and admins can upload photos")
            );
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(
                Map.of("error", e.getMessage())
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                Map.of("error", e.getMessage())
            );
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(404).body(
                Map.of("error", e.getMessage())
            );
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("error", "An unexpected error occurred: " + e.getMessage())
            );
        }
    }

    /**
     * Deletes a photo from an event gallery.
     *
     * @param eventId Event ID
     * @param photoId Photo ID to delete
     * @param authentication Current authenticated user
     * @return Success/failure response
     */
    @DeleteMapping("/{eventId}/photos/{photoId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deletePhoto(
            @PathVariable Long eventId,
            @PathVariable Long photoId,
            Authentication authentication) {

        try {
            // Get current user ID
            Long userId = userService.getUserIdByUsername(authentication.getName());

            // Delete photo
            eventPhotoService.deleteEventPhotoAsOrganiser(eventId, photoId, userId);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Photo deleted successfully"
            ));

        } catch (AccessDeniedException e) {
            return ResponseEntity.status(403).body(
                Map.of("error", "Only event organizers and admins can delete photos")
            );
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(404).body(
                Map.of("error", e.getMessage())
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                Map.of("error", e.getMessage())
            );
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("error", "An unexpected error occurred: " + e.getMessage())
            );
        }
    }

    /**
     * Gets all photos for an event with their URLs.
     *
     * @param eventId Event ID
     * @return List of photo data (id, originalFilename, photoUrl, thumbnailUrl, uploadedAt)
     */
    @GetMapping("/{eventId}/photos")
    public ResponseEntity<?> getEventPhotos(@PathVariable Long eventId) {
        try {
            List<Map<String, Object>> photos = eventPhotoService.getEventPhotos(eventId);
            long photoCount = eventPhotoService.getPhotoCount(eventId);

            return ResponseEntity.ok(Map.of(
                "photos", photos,
                "count", photoCount,
                "maxPhotos", 20
            ));

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(404).body(
                Map.of("error", e.getMessage())
            );
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                Map.of("error", "An unexpected error occurred: " + e.getMessage())
            );
        }
    }

    /**
     * Downloads a photo file from an event gallery.
     * Proxies the download through the server to ensure same-origin and proper Content-Disposition headers.
     * Public access - photos are already publicly viewable, download should be too.
     *
     * @param eventId Event ID
     * @param photoId Photo ID
     * @return Photo file with download headers
     */
    @GetMapping("/{eventId}/photos/{photoId}/download")
    public ResponseEntity<byte[]> downloadPhoto(
            @PathVariable Long eventId,
            @PathVariable Long photoId) {

        try {
            // Get photo metadata (filename, original filename)
            Map<String, Object> photoData = eventPhotoService.getEventPhotos(eventId).stream()
                .filter(photo -> photo.get("id").equals(photoId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Photo not found"));

            String filename = (String) photoData.get("filename");
            String originalFilename = (String) photoData.get("originalFilename");

            // Download photo bytes from storage
            byte[] photoBytes = photoStorageService.downloadPhoto(eventId, filename);

            // Determine content type from filename
            String contentType = filename.toLowerCase().endsWith(".png")
                ? MediaType.IMAGE_PNG_VALUE
                : MediaType.IMAGE_JPEG_VALUE;

            // Return photo with download headers
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + originalFilename + "\"")
                .body(photoBytes);

        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
