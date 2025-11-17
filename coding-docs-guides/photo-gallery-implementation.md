⏺ Photo Gallery Implementation Plan - Layer by Layer

  ---
  1. Database Layer

  Create event_photos table:
  - id - primary key
  - event_id - foreign key to events table
  - filename - UUID-based filename stored on disk
  - original_filename - what user uploaded (for display)
  - file_size - in bytes (for validation tracking)
  - uploaded_at - timestamp
  - uploaded_by_user_id - foreign key to users (organizer who uploaded)
  - Index on event_id for fast lookups

  Add to data-prod-schema.sql with CASCADE delete when event is deleted.

  ---
  2. Backend - Model Layer

  Create EventPhoto.java:
  - JPA entity mapping to event_photos table
  - ManyToOne relationship to Event
  - ManyToOne relationship to User (uploader)
  - Standard getters/setters

  ---
  3. Backend - Repository Layer

  Create EventPhotoRepository.java:
  - Extends JpaRepository
  - Method: findByEventIdOrderByUploadedAtDesc(Long eventId) - get all photos for an event
  - Method: countByEventId(Long eventId) - check how many photos exist (for 20 limit)
  - Method: deleteByEventId(Long eventId) - cleanup when event deleted (optional, CASCADE handles this)

  ---
  4. Backend - File Storage Service

  Create FileStorageService.java:
  - uploadPhoto() method:
    - Validate file type (jpg/png only via content type check)
    - Validate file size (<5MB)
    - Generate UUID filename (e.g., uuid.jpg)
    - Create directory uploads/events/{eventId}/ if doesn't exist
    - Save file to disk using MultipartFile.transferTo()
    - Return saved filename
  - deletePhoto() method:
    - Takes filename and eventId
    - Delete file from uploads/events/{eventId}/{filename}
    - Handle file not found gracefully
  - getPhotoPath() method:
    - Return full path for serving files
    - Used for download/display

  Configuration:
  - Set upload directory as src/main/resources/static/uploads/ (Spring Boot auto-serves static files)

  ---
  5. Backend - Event Service

  Add to EventService.java:

  - uploadEventPhoto() method:
    - Verify user is event organizer
    - Verify event has ended (past event only)
    - Check photo count < 20 (query repository)
    - Call FileStorageService to save file
    - Create EventPhoto entity and save to DB
    - Return success/error
  - deleteEventPhoto() method:
    - Verify user is event organizer
    - Find photo by ID
    - Call FileStorageService to delete file
    - Delete EventPhoto record from DB
  - getEventPhotos() method:
    - Return list of photos for an event
    - Used by frontend to display gallery

  ---
  6. Backend - Controller Layer

  Add to EventController.java or create EventPhotoController.java:

  - POST /events/{eventId}/photos/upload:
    - @RequestParam MultipartFile[] files (multi-file upload)
    - Loop through files, call EventService.uploadEventPhoto()
    - Return success/error JSON
    - Handle max 20 limit across all uploads
  - DELETE /events/{eventId}/photos/{photoId}:
    - Call EventService.deleteEventPhoto()
    - Return success JSON
  - GET /events/{eventId}/photos:
    - Call EventService.getEventPhotos()
    - Return list of photo DTOs (id, filename, originalFilename, uploadedAt)

  Static file serving: Spring Boot auto-serves files from /uploads/ path since they're in static/ folder.

  ---
  7. Frontend - HTML (event-details.html)

  Add Photo Gallery Section (after reviews, before sidebar):

  For Organizers (past events only):
  - Show "Manage Gallery" button
  - Clicking opens upload form modal/section
  - Upload form:
    - Multi-file input (<input type="file" multiple accept="image/jpeg,image/png">)
    - "Upload Photos" submit button
    - Show current count: "X/20 photos uploaded"
    - Validation: max 20 files, <5MB each

  For All Users Who RSVP'd:
  - Show photo grid (3-4 columns, responsive)
  - Each photo:
    - Display image (<img src="/uploads/events/{eventId}/{filename}">)
    - Show original filename below
    - If organizer: show "Delete" button (red X icon)
    - Right-click image = browser download (no custom download button needed)

  For Non-RSVP'd Users:
  - Show stub: "RSVP to this event to view photo gallery" with lock icon
  - Or hide gallery section entirely

  Conditional rendering:
  th:if="${event.eventHasPassed and didUserRsvp}"  // Show gallery
  th:if="${event.eventHasPassed and isOrganizer}"  // Show upload UI

  ---
  8. Frontend - JavaScript

  Add to event-details.html scripts:

  - uploadPhotos() function:
    - Get file input element
    - Validate client-side: count ≤ 20, each file <5MB, jpg/png only
    - Create FormData with files
    - Fetch POST to /events/{eventId}/photos/upload
    - On success: reload page or dynamically add photos to grid
    - On error: show alert with message
  - deletePhoto(photoId) function:
    - Show confirm dialog: "Delete this photo?"
    - Fetch DELETE to /events/{eventId}/photos/{photoId}
    - On success: remove photo from grid (or reload page)
    - On error: show alert
  - loadPhotos() (optional):
    - Fetch GET /events/{eventId}/photos
    - Render photo grid dynamically
    - OR rely on Thymeleaf server-side rendering (simpler for MVP)

  ---
  9. DTO Layer (Optional but Clean)

  Create EventPhotoDTO.java:
  - id
  - filename (for URL construction)
  - originalFilename (for display)
  - uploadedAt
  - uploadedBy (username)

  Used for API responses instead of exposing full entity.

  ---
  10. Security & Validation

  Server-side validation in EventService:
  - Check file type via contentType.startsWith("image/jpeg") or "image/png"
  - Check file size: file.getSize() <= 5 * 1024 * 1024 (5MB)
  - Check total photo count before upload
  - Verify event.eventHasPassed() == true
  - Verify user is organizer for upload/delete

  Prevent path traversal:
  - Use UUID filenames (no user input in filename)
  - Store in controlled directory only

  ---
  11. Edge Cases to Handle

  - Event deleted: Cascade delete photos from DB, manually delete files in directory
  - Photo upload fails mid-way: Rollback DB transaction, delete orphaned files
  - File doesn't exist but DB record does: Show placeholder or "Image not found"
  - User uploads 21 photos: Reject with error "Maximum 20 photos allowed"
  - Organizer changes: Original uploader can still delete their photos? Or only current organizer? (Keep it simple: any
  organizer can delete any photo)

  ---
  12. Testing Checklist

  - Organizer can upload photos to past event
  - Cannot upload >20 photos
  - Cannot upload files >5MB
  - Cannot upload .pdf or other non-image files
  - RSVP'd users can view gallery
  - Non-RSVP'd users see stub or nothing
  - Organizer can delete photos
  - Non-organizer cannot delete photos
  - Photos persist after page reload
  - Photos display correctly in grid
  - Right-click save image works (download)

  ---
  Implementation Order

  1. Database schema
  2. Model + Repository
  3. FileStorageService
  4. EventService methods
  5. Controller endpoints
  6. Frontend HTML gallery section
  7. Frontend upload form + JavaScript
  8. Test upload/delete/view flows

  Estimated time: 3-4 hours

  ---
  That's the complete layer-by-layer plan. Ready to proceed when you approve!