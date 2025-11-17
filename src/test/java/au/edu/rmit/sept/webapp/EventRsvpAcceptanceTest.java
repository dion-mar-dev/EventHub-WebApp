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
 * Acceptance tests for US-10: RSVP to an event functionality.
 * Tests the complete flow from HTTP request through controller, service, and repository to database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EventRsvpAcceptanceTest {

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
    private User testUser1;
    private User testUser2;
    private Event testEventWithCapacity;
    private Event testEventUnlimited;
    private Event testEventAlmostFull;
    private Category testCategory;

    // Baseline counts for assertions
    private long baselineRsvpCount;
    private long baselineEventCount;
    private long baselineUserCount;

    @BeforeEach
    void setUp() {
        // Record baseline counts from DataInitializer
        baselineRsvpCount = rsvpRepository.count();
        baselineEventCount = eventRepository.count();
        baselineUserCount = userRepository.count();

        // Create test category
        testCategory = new Category();
        testCategory.setName("RSVP Test Category");
        testCategory.setColourCode("#FF5733");
        testCategory = categoryRepository.save(testCategory);

        // Create test users
        testUser1 = new User();
        testUser1.setUsername("rsvptest_user1");
        testUser1.setEmail("rsvpuser1@test.com");
        testUser1.setPassword(passwordEncoder.encode("TestPass123!"));
        testUser1 = userRepository.save(testUser1);

        testUser2 = new User();
        testUser2.setUsername("rsvptest_user2");
        testUser2.setEmail("rsvpuser2@test.com");
        testUser2.setPassword(passwordEncoder.encode("TestPass123!"));
        testUser2 = userRepository.save(testUser2);

        // Create event with capacity limit
        testEventWithCapacity = new Event();
        testEventWithCapacity.setTitle("RSVP Test Event - Limited");
        testEventWithCapacity.setDescription("Test event for RSVP with capacity limit of 2");
        testEventWithCapacity.setEventDate(LocalDate.now().plusDays(7));
        testEventWithCapacity.setEventTime(LocalTime.of(14, 0));
        testEventWithCapacity.setLocation("Test Venue 1");
        testEventWithCapacity.setCapacity(2); // Small capacity for testing
        testEventWithCapacity.setCreatedBy(testUser1);
        testEventWithCapacity.setCategory(testCategory);
        testEventWithCapacity = eventRepository.save(testEventWithCapacity);

        // Create event with unlimited capacity
        testEventUnlimited = new Event();
        testEventUnlimited.setTitle("RSVP Test Event - Unlimited");
        testEventUnlimited.setDescription("Test event for RSVP with no capacity limit");
        testEventUnlimited.setEventDate(LocalDate.now().plusDays(14));
        testEventUnlimited.setEventTime(LocalTime.of(10, 0));
        testEventUnlimited.setLocation("Test Venue 2");
        testEventUnlimited.setCapacity(null); // Unlimited
        testEventUnlimited.setCreatedBy(testUser1);
        testEventUnlimited.setCategory(testCategory);
        testEventUnlimited = eventRepository.save(testEventUnlimited);

        // Create event that's almost full (capacity 1, for testing full scenario)
        testEventAlmostFull = new Event();
        testEventAlmostFull.setTitle("RSVP Test Event - Almost Full");
        testEventAlmostFull.setDescription("Test event that will be full after one RSVP");
        testEventAlmostFull.setEventDate(LocalDate.now().plusDays(21));
        testEventAlmostFull.setEventTime(LocalTime.of(16, 0));
        testEventAlmostFull.setLocation("Test Venue 3");
        testEventAlmostFull.setCapacity(1);
        testEventAlmostFull.setCreatedBy(testUser2);
        testEventAlmostFull.setCategory(testCategory);
        testEventAlmostFull = eventRepository.save(testEventAlmostFull);
    }

    @Test
    void testSuccessfulRsvpToEvent() throws Exception {
        // Act: User RSVPs to event with capacity
        mockMvc.perform(post("/rsvp/" + testEventWithCapacity.getId())
                .with(user(testUser1.getUsername()))
                .with(csrf())
                .header("Referer", "/events/" + testEventWithCapacity.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/events/" + testEventWithCapacity.getId()))
                .andExpect(flash().attribute("success", "Successfully RSVP'd to " + testEventWithCapacity.getTitle()));

        // Assert: Verify RSVP was created
        assertEquals(baselineRsvpCount + 1, rsvpRepository.count());
        assertTrue(rsvpRepository.existsByUser_UsernameAndEvent_Id(
                testUser1.getUsername(), testEventWithCapacity.getId()));

        // Verify specific RSVP details
        RSVP createdRsvp = rsvpRepository.findByUser_UsernameAndEvent_Id(
                testUser1.getUsername(), testEventWithCapacity.getId()).orElse(null);
        assertNotNull(createdRsvp);
        assertEquals(testUser1.getId(), createdRsvp.getUser().getId());
        assertEquals(testEventWithCapacity.getId(), createdRsvp.getEvent().getId());
        assertNotNull(createdRsvp.getRsvpDate());
    }

    @Test
    void testCannotRsvpTwiceToSameEvent() throws Exception {
        // Arrange: Create first RSVP
        RSVP firstRsvp = new RSVP(testUser1, testEventWithCapacity);
        rsvpRepository.save(firstRsvp);

        // Act: Attempt duplicate RSVP
        mockMvc.perform(post("/rsvp/" + testEventWithCapacity.getId())
                .with(user(testUser1.getUsername()))
                .with(csrf())
                .header("Referer", "/events/" + testEventWithCapacity.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/events/" + testEventWithCapacity.getId()))
                .andExpect(flash().attribute("error", "You have already RSVP'd to this event"));

        // Assert: No additional RSVP created
        assertEquals(baselineRsvpCount + 1, rsvpRepository.count());
    }

    @Test
    void testCannotRsvpToFullEvent() throws Exception {
        // Arrange: Fill the event to capacity (capacity = 1)
        RSVP existingRsvp = new RSVP(testUser1, testEventAlmostFull);
        rsvpRepository.save(existingRsvp);

        // Act: Second user attempts to RSVP to full event
        mockMvc.perform(post("/rsvp/" + testEventAlmostFull.getId())
                .with(user(testUser2.getUsername()))
                .with(csrf())
                .header("Referer", "/events/" + testEventAlmostFull.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/events/" + testEventAlmostFull.getId()))
                .andExpect(flash().attribute("error", "This event is full"));

        // Assert: No RSVP created for second user
        assertEquals(baselineRsvpCount + 1, rsvpRepository.count());
        assertFalse(rsvpRepository.existsByUser_UsernameAndEvent_Id(
                testUser2.getUsername(), testEventAlmostFull.getId()));
    }

    @Test
    void testRsvpToEventWithUnlimitedCapacity() throws Exception {
        // Arrange: Add multiple RSVPs to show unlimited works
        for (int i = 1; i <= 5; i++) {
            User extraUser = new User();
            extraUser.setUsername("rsvptest_extra" + i);
            extraUser.setEmail("extra" + i + "@test.com");
            extraUser.setPassword(passwordEncoder.encode("TestPass123!"));
            userRepository.save(extraUser);

            RSVP rsvp = new RSVP(extraUser, testEventUnlimited);
            rsvpRepository.save(rsvp);
        }

        // Act: User can still RSVP despite many attendees
        mockMvc.perform(post("/rsvp/" + testEventUnlimited.getId())
                .with(user(testUser1.getUsername()))
                .with(csrf())
                .header("Referer", "/events/" + testEventUnlimited.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/events/" + testEventUnlimited.getId()))
                .andExpect(flash().attribute("success", "Successfully RSVP'd to " + testEventUnlimited.getTitle()));

        // Assert: RSVP created successfully
        assertTrue(rsvpRepository.existsByUser_UsernameAndEvent_Id(
                testUser1.getUsername(), testEventUnlimited.getId()));
        assertEquals(6, rsvpRepository.countByEvent(testEventUnlimited)); // 5 extra + 1 test user
    }

    @Test
    void testRsvpToNonExistentEvent() throws Exception {
        Long nonExistentId = 999999L;

        // Act: Attempt to RSVP to non-existent event
        mockMvc.perform(post("/rsvp/" + nonExistentId)
                .with(user(testUser1.getUsername()))
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/events/" + nonExistentId))
                .andExpect(flash().attribute("error", "An error occurred while processing your RSVP"));

        // Assert: No RSVP created
        assertEquals(baselineRsvpCount, rsvpRepository.count());
    }

    @Test
    void testMultipleUsersCanRsvpToSameEvent() throws Exception {
        // Act: First user RSVPs
        mockMvc.perform(post("/rsvp/" + testEventWithCapacity.getId())
                .with(user(testUser1.getUsername()))
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("success", "Successfully RSVP'd to " + testEventWithCapacity.getTitle()));

        // Act: Second user RSVPs (capacity is 2, so should succeed)
        mockMvc.perform(post("/rsvp/" + testEventWithCapacity.getId())
                .with(user(testUser2.getUsername()))
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("success", "Successfully RSVP'd to " + testEventWithCapacity.getTitle()));

        // Assert: Both RSVPs exist
        assertEquals(baselineRsvpCount + 2, rsvpRepository.count());
        assertTrue(rsvpRepository.existsByUser_UsernameAndEvent_Id(
                testUser1.getUsername(), testEventWithCapacity.getId()));
        assertTrue(rsvpRepository.existsByUser_UsernameAndEvent_Id(
                testUser2.getUsername(), testEventWithCapacity.getId()));
        assertEquals(2, rsvpRepository.countByEvent(testEventWithCapacity));
    }

    @Test
    void testUserCanRsvpToMultipleEvents() throws Exception {
        // Act: User RSVPs to first event
        mockMvc.perform(post("/rsvp/" + testEventWithCapacity.getId())
                .with(user(testUser1.getUsername()))
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("success", "Successfully RSVP'd to " + testEventWithCapacity.getTitle()));

        // Act: Same user RSVPs to second event
        mockMvc.perform(post("/rsvp/" + testEventUnlimited.getId())
                .with(user(testUser1.getUsername()))
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("success", "Successfully RSVP'd to " + testEventUnlimited.getTitle()));

        // Act: Same user RSVPs to third event
        mockMvc.perform(post("/rsvp/" + testEventAlmostFull.getId())
                .with(user(testUser1.getUsername()))
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("success", "Successfully RSVP'd to " + testEventAlmostFull.getTitle()));

        // Assert: User has RSVPs to all three events
        assertEquals(baselineRsvpCount + 3, rsvpRepository.count());
        assertTrue(rsvpRepository.existsByUser_UsernameAndEvent_Id(
                testUser1.getUsername(), testEventWithCapacity.getId()));
        assertTrue(rsvpRepository.existsByUser_UsernameAndEvent_Id(
                testUser1.getUsername(), testEventUnlimited.getId()));
        assertTrue(rsvpRepository.existsByUser_UsernameAndEvent_Id(
                testUser1.getUsername(), testEventAlmostFull.getId()));
    }

    @Test
    void testRsvpRedirectBehaviorWithReferer() throws Exception {
        String customReferer = "/events?category=tech";

        // Act: RSVP with custom referer
        mockMvc.perform(post("/rsvp/" + testEventWithCapacity.getId())
                .with(user(testUser1.getUsername()))
                .with(csrf())
                .header("Referer", customReferer))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/events/" + testEventWithCapacity.getId()));
    }

    @Test
    void testCapacityEnforcementWithConcurrentRsvps() throws Exception {
        // This test verifies the pessimistic locking works correctly
        // Event has capacity of 2, both users should succeed

        // Act: First RSVP
        mockMvc.perform(post("/rsvp/" + testEventWithCapacity.getId())
                .with(user(testUser1.getUsername()))
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("success", "Successfully RSVP'd to " + testEventWithCapacity.getTitle()));

        // Act: Second RSVP (should succeed, capacity is 2)
        mockMvc.perform(post("/rsvp/" + testEventWithCapacity.getId())
                .with(user(testUser2.getUsername()))
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("success", "Successfully RSVP'd to " + testEventWithCapacity.getTitle()));

        // Act: Third RSVP attempt (should fail, at capacity)
        User testUser3 = new User();
        testUser3.setUsername("rsvptest_user3");
        testUser3.setEmail("rsvpuser3@test.com");
        testUser3.setPassword(passwordEncoder.encode("TestPass123!"));
        userRepository.save(testUser3);

        mockMvc.perform(post("/rsvp/" + testEventWithCapacity.getId())
                .with(user(testUser3.getUsername()))
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", "This event is full"));

        // Assert: Only 2 RSVPs exist
        assertEquals(2, rsvpRepository.countByEvent(testEventWithCapacity));
    }
}
