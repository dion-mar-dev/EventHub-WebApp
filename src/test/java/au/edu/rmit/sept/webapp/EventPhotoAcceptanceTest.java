package au.edu.rmit.sept.webapp;

import au.edu.rmit.sept.webapp.model.Event;
import au.edu.rmit.sept.webapp.model.User;
import au.edu.rmit.sept.webapp.model.Category;
import au.edu.rmit.sept.webapp.model.EventPhoto;
import au.edu.rmit.sept.webapp.repository.EventRepository;
import au.edu.rmit.sept.webapp.repository.UserRepository;
import au.edu.rmit.sept.webapp.repository.CategoryRepository;
import au.edu.rmit.sept.webapp.repository.EventPhotoRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/**
 * Acceptance tests for Event Photo Gallery functionality.
 * Tests the complete flow from HTTP request through controller, service, storage, and repository to database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EventPhotoAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private EventPhotoRepository eventPhotoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Test data
    private User organizer;
    private User nonOrganizer;
    private User admin;
    private Event testEvent;
    private Category testCategory;

    // Baseline counts for assertions
    private long baselinePhotoCount;
    private long baselineEventCount;
    private long baselineUserCount;

    @BeforeEach
    void setUp() {
        // Record baseline counts from DataInitializer
        baselinePhotoCount = eventPhotoRepository.count();
        baselineEventCount = eventRepository.count();
        baselineUserCount = userRepository.count();

        // Create test category
        testCategory = new Category();
        testCategory.setName("Photo Test Category");
        testCategory.setColourCode("#FF5733");
        testCategory = categoryRepository.save(testCategory);

        // Create organizer user
        organizer = new User();
        organizer.setUsername("photo_organizer");
        organizer.setEmail("organizer@test.com");
        organizer.setPassword(passwordEncoder.encode("TestPass123!"));
        organizer = userRepository.save(organizer);

        // Create non-organizer user
        nonOrganizer = new User();
        nonOrganizer.setUsername("photo_nonorganizer");
        nonOrganizer.setEmail("nonorganizer@test.com");
        nonOrganizer.setPassword(passwordEncoder.encode("TestPass123!"));
        nonOrganizer = userRepository.save(nonOrganizer);

        // Create admin user
        admin = new User();
        admin.setUsername("photo_admin");
        admin.setEmail("admin@test.com");
        admin.setPassword(passwordEncoder.encode("TestPass123!"));
        admin.setRole("ROLE_ADMIN");
        admin = userRepository.save(admin);

        // Create test event owned by organizer (past event - photos can only be uploaded after event ends)
        testEvent = new Event();
        testEvent.setTitle("Photo Test Event");
        testEvent.setDescription("Test event for photo gallery");
        testEvent.setEventDate(LocalDate.now().minusDays(7)); // Past event
        testEvent.setEventTime(LocalTime.of(14, 0));
        testEvent.setLocation("Test Venue");
        testEvent.setCapacity(50);
        testEvent.setCreatedBy(organizer);
        testEvent.setCategory(testCategory);
        testEvent = eventRepository.save(testEvent);
    }

    /**
     * Helper method to create a valid test image as byte array
     */
    private byte[] createTestImageBytes() throws IOException {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        return baos.toByteArray();
    }

    /**
     * Test 1: Upload Photos - Happy Path
     * Organizer uploads a photo to their event successfully.
     */
    @Test
    void testUploadPhoto_AsOrganizer_Success() throws Exception {
        // Create a valid test image file
        MockMultipartFile mockFile = new MockMultipartFile(
                "files",
                "test-photo.jpg",
                "image/jpeg",
                createTestImageBytes()
        );

        // Perform upload request as organizer
        mockMvc.perform(multipart("/api/events/" + testEvent.getId() + "/photos/upload")
                        .file(mockFile)
                        .with(csrf())
                        .with(user(organizer.getUsername())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("All photos uploaded successfully"))
                .andExpect(jsonPath("$.count").value(1));

        // Verify photo was saved to database
        long photoCountAfter = eventPhotoRepository.count();
        assertEquals(baselinePhotoCount + 1, photoCountAfter, "Photo count should increase by 1");

        // Verify photo metadata
        EventPhoto savedPhoto = eventPhotoRepository.findAll().stream()
                .filter(p -> p.getEvent().getId().equals(testEvent.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Photo should be saved to database"));

        assertEquals("test-photo.jpg", savedPhoto.getOriginalFilename());
        assertEquals(organizer.getId(), savedPhoto.getUploadedBy().getId());
        assertEquals(testEvent.getId(), savedPhoto.getEvent().getId());
        assertNotNull(savedPhoto.getUploadedAt());
    }

    /**
     * Test 2: Upload Photos - Authorization Check
     * Non-organizer cannot upload photos to someone else's event.
     */
    @Test
    void testUploadPhoto_AsNonOrganizer_Forbidden() throws Exception {
        // Create a valid test image file
        MockMultipartFile mockFile = new MockMultipartFile(
                "files",
                "unauthorized-photo.jpg",
                "image/jpeg",
                createTestImageBytes()
        );

        // Attempt upload as non-organizer user
        mockMvc.perform(multipart("/api/events/" + testEvent.getId() + "/photos/upload")
                        .file(mockFile)
                        .with(csrf())
                        .with(user(nonOrganizer.getUsername())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Only event organizers and admins can upload photos"));

        // Verify no photo was saved to database
        long photoCountAfter = eventPhotoRepository.count();
        assertEquals(baselinePhotoCount, photoCountAfter, "Photo count should remain unchanged");
    }

    /**
     * Test 3: Upload Photos - Invalid Event
     * Attempting to upload to non-existent event returns 404.
     */
    @Test
    void testUploadPhoto_InvalidEvent_NotFound() throws Exception {
        // Create a valid test image file
        MockMultipartFile mockFile = new MockMultipartFile(
                "files",
                "test-photo.jpg",
                "image/jpeg",
                createTestImageBytes()
        );

        // Use non-existent event ID
        Long nonExistentEventId = 999999L;

        // Attempt upload to non-existent event
        mockMvc.perform(multipart("/api/events/" + nonExistentEventId + "/photos/upload")
                        .file(mockFile)
                        .with(csrf())
                        .with(user(organizer.getUsername())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());

        // Verify no photo was saved to database
        long photoCountAfter = eventPhotoRepository.count();
        assertEquals(baselinePhotoCount, photoCountAfter, "Photo count should remain unchanged");
    }

    /**
     * Test 4: Get Photos - Happy Path
     * Retrieve photos for an event that has photos.
     */
    @Test
    void testGetPhotos_WithPhotos_Success() throws Exception {
        // First, upload a photo
        MockMultipartFile mockFile = new MockMultipartFile(
                "files",
                "test-photo.jpg",
                "image/jpeg",
                createTestImageBytes()
        );

        mockMvc.perform(multipart("/api/events/" + testEvent.getId() + "/photos/upload")
                        .file(mockFile)
                        .with(csrf())
                        .with(user(organizer.getUsername())))
                .andExpect(status().isOk());

        // Now retrieve the photos (as authenticated user)
        mockMvc.perform(get("/api/events/" + testEvent.getId() + "/photos")
                        .with(user(organizer.getUsername())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.maxPhotos").value(20))
                .andExpect(jsonPath("$.photos").isArray())
                .andExpect(jsonPath("$.photos[0].originalFilename").value("test-photo.jpg"))
                .andExpect(jsonPath("$.photos[0].id").exists())
                .andExpect(jsonPath("$.photos[0].photoUrl").exists())
                .andExpect(jsonPath("$.photos[0].thumbnailUrl").exists())
                .andExpect(jsonPath("$.photos[0].uploadedAt").exists());
    }

    /**
     * Test 5: Get Photos - Empty State
     * Retrieve photos for an event with no photos returns empty array.
     */
    @Test
    void testGetPhotos_EmptyState_Success() throws Exception {
        // Retrieve photos from event with no photos
        mockMvc.perform(get("/api/events/" + testEvent.getId() + "/photos")
                        .with(user(organizer.getUsername())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0))
                .andExpect(jsonPath("$.maxPhotos").value(20))
                .andExpect(jsonPath("$.photos").isArray())
                .andExpect(jsonPath("$.photos").isEmpty());
    }

    /**
     * Test 6: Delete Photo - Happy Path
     * Organizer deletes their event photo successfully.
     */
    @Test
    void testDeletePhoto_AsOrganizer_Success() throws Exception {
        // First, upload a photo
        MockMultipartFile mockFile = new MockMultipartFile(
                "files",
                "test-photo-delete.jpg",
                "image/jpeg",
                createTestImageBytes()
        );

        mockMvc.perform(multipart("/api/events/" + testEvent.getId() + "/photos/upload")
                        .file(mockFile)
                        .with(csrf())
                        .with(user(organizer.getUsername())))
                .andExpect(status().isOk());

        // Get the photo ID
        EventPhoto uploadedPhoto = eventPhotoRepository.findAll().stream()
                .filter(p -> p.getEvent().getId().equals(testEvent.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Photo should exist"));

        Long photoId = uploadedPhoto.getId();
        long photoCountBefore = eventPhotoRepository.count();

        // Delete the photo as organizer
        mockMvc.perform(delete("/api/events/" + testEvent.getId() + "/photos/" + photoId)
                        .with(csrf())
                        .with(user(organizer.getUsername())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Photo deleted successfully"));

        // Verify photo was deleted from database
        long photoCountAfter = eventPhotoRepository.count();
        assertEquals(photoCountBefore - 1, photoCountAfter, "Photo count should decrease by 1");

        // Verify photo no longer exists
        assertFalse(eventPhotoRepository.existsById(photoId), "Photo should be deleted from database");
    }

    /**
     * Test 7: Delete Photo - Authorization Check
     * Non-organizer cannot delete photos from someone else's event.
     */
    @Test
    void testDeletePhoto_AsNonOrganizer_Forbidden() throws Exception {
        // First, upload a photo as organizer
        MockMultipartFile mockFile = new MockMultipartFile(
                "files",
                "test-photo-delete-auth.jpg",
                "image/jpeg",
                createTestImageBytes()
        );

        mockMvc.perform(multipart("/api/events/" + testEvent.getId() + "/photos/upload")
                        .file(mockFile)
                        .with(csrf())
                        .with(user(organizer.getUsername())))
                .andExpect(status().isOk());

        // Get the photo ID
        EventPhoto uploadedPhoto = eventPhotoRepository.findAll().stream()
                .filter(p -> p.getEvent().getId().equals(testEvent.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Photo should exist"));

        Long photoId = uploadedPhoto.getId();
        long photoCountBefore = eventPhotoRepository.count();

        // Attempt to delete photo as non-organizer
        mockMvc.perform(delete("/api/events/" + testEvent.getId() + "/photos/" + photoId)
                        .with(csrf())
                        .with(user(nonOrganizer.getUsername())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Only event organizers and admins can delete photos"));

        // Verify photo still exists in database
        long photoCountAfter = eventPhotoRepository.count();
        assertEquals(photoCountBefore, photoCountAfter, "Photo count should remain unchanged");
        assertTrue(eventPhotoRepository.existsById(photoId), "Photo should still exist in database");
    }

    /**
     * Test 8: Download Photo - Happy Path
     * Download a photo file successfully with correct headers.
     */
    @Test
    void testDownloadPhoto_Success() throws Exception {
        // First, upload a photo
        MockMultipartFile mockFile = new MockMultipartFile(
                "files",
                "test-photo-download.jpg",
                "image/jpeg",
                createTestImageBytes()
        );

        mockMvc.perform(multipart("/api/events/" + testEvent.getId() + "/photos/upload")
                        .file(mockFile)
                        .with(csrf())
                        .with(user(organizer.getUsername())))
                .andExpect(status().isOk());

        // Get the photo ID
        EventPhoto uploadedPhoto = eventPhotoRepository.findAll().stream()
                .filter(p -> p.getEvent().getId().equals(testEvent.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Photo should exist"));

        Long photoId = uploadedPhoto.getId();

        // Download the photo
        mockMvc.perform(get("/api/events/" + testEvent.getId() + "/photos/" + photoId + "/download")
                        .with(user(organizer.getUsername())))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Type"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"test-photo-download.jpg\""))
                .andExpect(content().contentTypeCompatibleWith("image/jpeg"))
                .andExpect(content().bytes(createTestImageBytes()));
    }

    /**
     * Test 9: Upload Photos - No Files Provided
     * Attempting to upload with no files should return error.
     */
    @Test
    void testUploadPhoto_NoFiles_BadRequest() throws Exception {
        // Create empty files array
        MockMultipartFile[] emptyFiles = new MockMultipartFile[0];

        // Attempt upload with empty files array - Spring requires the parameter to exist
        // We use param("files", "") to simulate empty upload
        mockMvc.perform(multipart("/api/events/" + testEvent.getId() + "/photos/upload")
                        .param("files", "")
                        .with(csrf())
                        .with(user(organizer.getUsername())))
                .andExpect(status().isBadRequest());

        // Verify no photos were saved to database
        long photoCountAfter = eventPhotoRepository.count();
        assertEquals(baselinePhotoCount, photoCountAfter, "Photo count should remain unchanged");
    }

    /**
     * Test 10: Upload Photos - Multiple Photos at Once
     * Organizer can upload multiple valid photos in a single request.
     */
    @Test
    void testUploadPhoto_MultiplePhotos_Success() throws Exception {
        // Create 3 valid test image files
        MockMultipartFile mockFile1 = new MockMultipartFile(
                "files",
                "test-photo-1.jpg",
                "image/jpeg",
                createTestImageBytes()
        );
        MockMultipartFile mockFile2 = new MockMultipartFile(
                "files",
                "test-photo-2.jpg",
                "image/jpeg",
                createTestImageBytes()
        );
        MockMultipartFile mockFile3 = new MockMultipartFile(
                "files",
                "test-photo-3.jpg",
                "image/jpeg",
                createTestImageBytes()
        );

        // Upload all 3 photos at once
        mockMvc.perform(multipart("/api/events/" + testEvent.getId() + "/photos/upload")
                        .file(mockFile1)
                        .file(mockFile2)
                        .file(mockFile3)
                        .with(csrf())
                        .with(user(organizer.getUsername())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("All photos uploaded successfully"))
                .andExpect(jsonPath("$.count").value(3));

        // Verify all 3 photos were saved to database
        long photoCountAfter = eventPhotoRepository.count();
        assertEquals(baselinePhotoCount + 3, photoCountAfter, "Photo count should increase by 3");

        // Verify all 3 photos have correct metadata
        long eventPhotoCount = eventPhotoRepository.findAll().stream()
                .filter(p -> p.getEvent().getId().equals(testEvent.getId()))
                .count();
        assertEquals(3, eventPhotoCount, "Should have 3 photos for this event");
    }

    /**
     * Test 11: Upload Photos - Exceeds Max Photo Limit
     * Cannot upload photos beyond the 20-photo limit per event.
     */
    @Test
    void testUploadPhoto_ExceedsMaxLimit_BadRequest() throws Exception {
        // Upload 19 photos to get close to the limit
        for (int i = 1; i <= 19; i++) {
            MockMultipartFile mockFile = new MockMultipartFile(
                    "files",
                    "test-photo-" + i + ".jpg",
                    "image/jpeg",
                    createTestImageBytes()
            );

            mockMvc.perform(multipart("/api/events/" + testEvent.getId() + "/photos/upload")
                            .file(mockFile)
                            .with(csrf())
                            .with(user(organizer.getUsername())))
                    .andExpect(status().isOk());
        }

        // Verify we have 19 photos
        long photoCount = eventPhotoRepository.findAll().stream()
                .filter(p -> p.getEvent().getId().equals(testEvent.getId()))
                .count();
        assertEquals(19, photoCount, "Should have 19 photos before attempting to exceed limit");

        // Attempt to upload 2 more photos (would exceed 20 limit)
        MockMultipartFile mockFile1 = new MockMultipartFile(
                "files",
                "test-photo-20.jpg",
                "image/jpeg",
                createTestImageBytes()
        );
        MockMultipartFile mockFile2 = new MockMultipartFile(
                "files",
                "test-photo-21.jpg",
                "image/jpeg",
                createTestImageBytes()
        );

        mockMvc.perform(multipart("/api/events/" + testEvent.getId() + "/photos/upload")
                        .file(mockFile1)
                        .file(mockFile2)
                        .with(csrf())
                        .with(user(organizer.getUsername())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Cannot upload 2 photos. Current: 19, Maximum: 20, Remaining: 1"));

        // Verify only 19 photos remain (not 21)
        long photoCountAfter = eventPhotoRepository.findAll().stream()
                .filter(p -> p.getEvent().getId().equals(testEvent.getId()))
                .count();
        assertEquals(19, photoCountAfter, "Photo count should remain at 19");
    }

    /**
     * Test 12: Admin Can Upload Photos to Any Event
     * Admin user can upload photos to events they don't organize.
     */
    @Test
    void testUploadPhoto_AsAdmin_Success() throws Exception {
        // Create a valid test image file
        MockMultipartFile mockFile = new MockMultipartFile(
                "files",
                "admin-upload.jpg",
                "image/jpeg",
                createTestImageBytes()
        );

        // Upload photo as admin user (event is owned by organizer)
        mockMvc.perform(multipart("/api/events/" + testEvent.getId() + "/photos/upload")
                        .file(mockFile)
                        .with(csrf())
                        .with(user(admin.getUsername()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("All photos uploaded successfully"))
                .andExpect(jsonPath("$.count").value(1));

        // Verify photo was saved with admin as uploader
        EventPhoto savedPhoto = eventPhotoRepository.findAll().stream()
                .filter(p -> p.getEvent().getId().equals(testEvent.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Photo should be saved to database"));

        assertEquals(admin.getId(), savedPhoto.getUploadedBy().getId(), "Photo should be uploaded by admin");
        assertEquals(testEvent.getId(), savedPhoto.getEvent().getId());
    }

    /**
     * Test 13: Admin Can Delete Photos from Any Event
     * Admin user can delete photos from events they don't organize.
     */
    @Test
    void testDeletePhoto_AsAdmin_Success() throws Exception {
        // First, upload a photo as organizer
        MockMultipartFile mockFile = new MockMultipartFile(
                "files",
                "test-photo-admin-delete.jpg",
                "image/jpeg",
                createTestImageBytes()
        );

        mockMvc.perform(multipart("/api/events/" + testEvent.getId() + "/photos/upload")
                        .file(mockFile)
                        .with(csrf())
                        .with(user(organizer.getUsername())))
                .andExpect(status().isOk());

        // Get the photo ID
        EventPhoto uploadedPhoto = eventPhotoRepository.findAll().stream()
                .filter(p -> p.getEvent().getId().equals(testEvent.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Photo should exist"));

        Long photoId = uploadedPhoto.getId();
        long photoCountBefore = eventPhotoRepository.count();

        // Delete the photo as admin user
        mockMvc.perform(delete("/api/events/" + testEvent.getId() + "/photos/" + photoId)
                        .with(csrf())
                        .with(user(admin.getUsername()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Photo deleted successfully"));

        // Verify photo was deleted from database
        long photoCountAfter = eventPhotoRepository.count();
        assertEquals(photoCountBefore - 1, photoCountAfter, "Photo count should decrease by 1");
        assertFalse(eventPhotoRepository.existsById(photoId), "Photo should be deleted from database");
    }
}
