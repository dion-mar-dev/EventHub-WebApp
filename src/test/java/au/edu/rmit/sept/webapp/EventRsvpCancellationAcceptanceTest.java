package au.edu.rmit.sept.webapp;

import au.edu.rmit.sept.webapp.model.Event;
import au.edu.rmit.sept.webapp.model.User;
import au.edu.rmit.sept.webapp.model.Category;
import au.edu.rmit.sept.webapp.model.RSVP;
import au.edu.rmit.sept.webapp.repository.EventRepository;
import au.edu.rmit.sept.webapp.repository.UserRepository;
import au.edu.rmit.sept.webapp.repository.CategoryRepository;
import au.edu.rmit.sept.webapp.repository.RSVPRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Acceptance tests for US-11: Cancel RSVP functionality.
 * Tests the complete flow for cancelling event attendance.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EventRsvpCancellationAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private RSVPRepository rsvpRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Test data
    private User testUser;
    private User otherUser;
    private Event testEvent;
    private Category testCategory;
    private long baselineRsvpCount;

    @BeforeEach
    void setUp() {
        baselineRsvpCount = rsvpRepository.count();

        // Create test category
        testCategory = new Category();
        testCategory.setName("Cancel Test Category");
        testCategory.setColourCode("#4287f5");
        testCategory = categoryRepository.save(testCategory);

        // Create test users
        testUser = new User();
        testUser.setUsername("canceltest_user");
        testUser.setEmail("canceluser@test.com");
        testUser.setPassword(passwordEncoder.encode("TestPass123!"));
        testUser = userRepository.save(testUser);

        otherUser = new User();
        otherUser.setUsername("canceltest_other");
        otherUser.setEmail("cancelother@test.com");
        otherUser.setPassword(passwordEncoder.encode("TestPass123!"));
        otherUser = userRepository.save(otherUser);

        // Create test event
        testEvent = new Event();
        testEvent.setTitle("Cancellation Test Event");
        testEvent.setDescription("Event for testing RSVP cancellation functionality");
        testEvent.setEventDate(LocalDate.now().plusDays(10));
        testEvent.setEventTime(LocalTime.of(15, 0));
        testEvent.setLocation("Cancel Test Venue");
        testEvent.setCapacity(10);
        testEvent.setCreatedBy(testUser);
        testEvent.setCategory(testCategory);
        testEvent = eventRepository.save(testEvent);
    }

    @Test
    void testSuccessfulRsvpCancellation() throws Exception {
        // Arrange: Create RSVP
        RSVP rsvp = new RSVP(testUser, testEvent);
        rsvpRepository.save(rsvp);
        assertEquals(baselineRsvpCount + 1, rsvpRepository.count());

        // Act: Cancel RSVP
        mockMvc.perform(post("/rsvp/cancel")
                .with(user(testUser.getUsername()))
                .param("eventId", testEvent.getId().toString())
                .with(csrf())
                .header("Referer", "/events/" + testEvent.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/events/" + testEvent.getId()))
                .andExpect(flash().attribute("successMessage", "RSVP cancelled successfully"));

        // Assert: RSVP removed
        assertEquals(baselineRsvpCount, rsvpRepository.count());
        assertFalse(rsvpRepository.existsByUser_UsernameAndEvent_Id(
                testUser.getUsername(), testEvent.getId()));
    }

    @Test
    void testCancelNonExistentRsvp() throws Exception {
        // Act: Attempt to cancel RSVP that doesn't exist
        mockMvc.perform(post("/rsvp/cancel")
                .with(user(testUser.getUsername()))
                .param("eventId", testEvent.getId().toString())
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/events/" + testEvent.getId())) // Fallback to event page
                .andExpect(flash().attribute("errorMessage", "RSVP not found"));

        // Assert: No change in RSVP count
        assertEquals(baselineRsvpCount, rsvpRepository.count());
    }

    @Test
    void testCannotCancelTwice() throws Exception {
        // Arrange: Create and cancel RSVP
        RSVP rsvp = new RSVP(testUser, testEvent);
        rsvpRepository.save(rsvp);

        // First cancellation
        mockMvc.perform(post("/rsvp/cancel")
                .with(user(testUser.getUsername()))
                .param("eventId", testEvent.getId().toString())
                .with(csrf()))
                .andExpect(flash().attribute("successMessage", "RSVP cancelled successfully"));

        // Act: Attempt second cancellation
        mockMvc.perform(post("/rsvp/cancel")
                .with(user(testUser.getUsername()))
                .param("eventId", testEvent.getId().toString())
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("errorMessage", "RSVP not found"));

        // Assert: Still no RSVPs
        assertEquals(baselineRsvpCount, rsvpRepository.count());
    }

    @Test
    void testCancelThenReRsvp() throws Exception {
        // Arrange: Create RSVP
        RSVP rsvp = new RSVP(testUser, testEvent);
        rsvpRepository.save(rsvp);

        // Act: Cancel RSVP
        mockMvc.perform(post("/rsvp/cancel")
                .with(user(testUser.getUsername()))
                .param("eventId", testEvent.getId().toString())
                .with(csrf()))
                .andExpect(flash().attribute("successMessage", "RSVP cancelled successfully"));

        // Act: Re-RSVP to same event
        mockMvc.perform(post("/rsvp/" + testEvent.getId())
                .with(user(testUser.getUsername()))
                .with(csrf()))
                .andExpect(flash().attribute("success", "Successfully RSVP'd to " + testEvent.getTitle()));

        // Assert: RSVP exists again
        assertEquals(baselineRsvpCount + 1, rsvpRepository.count());
        assertTrue(rsvpRepository.existsByUser_UsernameAndEvent_Id(
                testUser.getUsername(), testEvent.getId()));
    }

    @Test
    void testUserCannotCancelOthersRsvp() throws Exception {
        // Arrange: Other user has RSVP
        RSVP otherUserRsvp = new RSVP(otherUser, testEvent);
        rsvpRepository.save(otherUserRsvp);

        // Act: Test user tries to cancel (will fail because RSVP lookup is by username)
        mockMvc.perform(post("/rsvp/cancel")
                .with(user(testUser.getUsername()))
                .param("eventId", testEvent.getId().toString())
                .with(csrf()))
                .andExpect(flash().attribute("errorMessage", "RSVP not found"));

        // Assert: Other user's RSVP still exists
        assertTrue(rsvpRepository.existsByUser_UsernameAndEvent_Id(
                otherUser.getUsername(), testEvent.getId()));
        assertEquals(baselineRsvpCount + 1, rsvpRepository.count());
    }

    @Test
    void testCancelRsvpForNonExistentEvent() throws Exception {
        Long nonExistentId = 999999L;

        // Act: Attempt to cancel RSVP for non-existent event
        mockMvc.perform(post("/rsvp/cancel")
                .with(user(testUser.getUsername()))
                .param("eventId", nonExistentId.toString())
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/events/" + nonExistentId)) // Still redirects to event ID
                .andExpect(flash().attribute("errorMessage", "Event not found"));

        // Assert: No change
        assertEquals(baselineRsvpCount, rsvpRepository.count());
    }

    @Test
    void testCancelRedirectBehaviorWithCustomReferer() throws Exception {
        // Arrange: Create RSVP
        RSVP rsvp = new RSVP(testUser, testEvent);
        rsvpRepository.save(rsvp);
        String customReferer = "/home?filter=upcoming";

        // Act: Cancel with custom referer
        mockMvc.perform(post("/rsvp/cancel")
                .with(user(testUser.getUsername()))
                .param("eventId", testEvent.getId().toString())
                .with(csrf())
                .header("Referer", customReferer))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(customReferer));
    }

    @Test
    void testCancelReleasesCapacitySlot() throws Exception {
        // Create event with capacity of 1
        Event limitedEvent = new Event();
        limitedEvent.setTitle("Limited Capacity Event");
        limitedEvent.setDescription("Event with single slot");
        limitedEvent.setEventDate(LocalDate.now().plusDays(5));
        limitedEvent.setEventTime(LocalTime.of(18, 0));
        limitedEvent.setLocation("Small Venue");
        limitedEvent.setCapacity(1);
        limitedEvent.setCreatedBy(testUser);
        limitedEvent.setCategory(testCategory);
        limitedEvent = eventRepository.save(limitedEvent);

        // Arrange: First user takes the slot
        RSVP rsvp = new RSVP(testUser, limitedEvent);
        rsvpRepository.save(rsvp);

        // Verify other user cannot RSVP (full)
        mockMvc.perform(post("/rsvp/" + limitedEvent.getId())
                .with(user(otherUser.getUsername()))
                .with(csrf()))
                .andExpect(flash().attribute("error", "This event is full"));

        // Act: First user cancels
        mockMvc.perform(post("/rsvp/cancel")
                .with(user(testUser.getUsername()))
                .param("eventId", limitedEvent.getId().toString())
                .with(csrf()))
                .andExpect(flash().attribute("successMessage", "RSVP cancelled successfully"));

        // Assert: Other user can now RSVP
        mockMvc.perform(post("/rsvp/" + limitedEvent.getId())
                .with(user(otherUser.getUsername()))
                .with(csrf()))
                .andExpect(flash().attribute("success", "Successfully RSVP'd to " + limitedEvent.getTitle()));

        assertTrue(rsvpRepository.existsByUser_UsernameAndEvent_Id(
                otherUser.getUsername(), limitedEvent.getId()));
    }
}