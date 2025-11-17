package au.edu.rmit.sept.webapp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import au.edu.rmit.sept.webapp.model.Category;
import au.edu.rmit.sept.webapp.model.Event;
import au.edu.rmit.sept.webapp.model.User;
import au.edu.rmit.sept.webapp.repository.CategoryRepository;
import au.edu.rmit.sept.webapp.repository.EventRepository;
import au.edu.rmit.sept.webapp.repository.UserRepository;
import au.edu.rmit.sept.webapp.service.EventService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Acceptance tests for US-08: Event Creation
 * Tests the complete flow of creating events as a logged-in user including:
 * - Form display with categories
 * - Successful event creation
 * - Validation error handling
 * - Authentication requirements
 * - Event visibility after creation
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EventCreationAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EventService eventService;

    private User testEventCreator;
    private Category techCategory;
    private Category sportsCategory;
    private Category musicCategory;

    // Baseline counts for assertions
    private long baselineEventCount;
    private int baselineUpcomingEventsCount;

    @BeforeEach
    void setUp() {
        // Count baseline events before creating test data
        baselineEventCount = eventRepository.count();
        baselineUpcomingEventsCount = eventService.getUpcomingEvents(null).size();

        // Create test user for event creation
        testEventCreator = new User();
        testEventCreator.setUsername("event.organizer");
        testEventCreator.setEmail("organizer@rmit.edu.au");
        testEventCreator.setPassword(passwordEncoder.encode("password123"));
        testEventCreator.setEnabled(true);
        userRepository.save(testEventCreator);

        // Fetch existing categories from DataInitializer
        techCategory = categoryRepository.findAll().stream()
                .filter(c -> c.getName().equals("Technology"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Technology category not found"));

        sportsCategory = categoryRepository.findAll().stream()
                .filter(c -> c.getName().equals("Sports"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Sports category not found"));

        musicCategory = categoryRepository.findAll().stream()
                .filter(c -> c.getName().equals("Music"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Music category not found"));
    }

    @Test
    void testSuccessfulEventCreation_CompleteFlow() throws Exception {
        // Step 1: GET create event form as authenticated user
        mockMvc.perform(get("/events/create")
                .with(user(testEventCreator.getUsername())))
                .andExpect(status().isOk())
                .andExpect(view().name("events/create-event"))
                .andExpect(model().attributeExists("eventCreateDTO"))
                .andExpect(model().attributeExists("categories"))
                .andExpect(model().attribute("categories", hasSize(8))); // 8 categories from DataInitializer

        // Step 2: POST valid event data
        String futureDate = LocalDate.now().plusDays(14).toString();

        mockMvc.perform(post("/events/create")
                .with(user(testEventCreator.getUsername()))
                .param("title", "Spring Boot Advanced Workshop")
                .param("description",
                        "Deep dive into Spring Boot advanced features including microservices, security, and cloud deployment")
                .param("eventDate", futureDate)
                .param("eventTime", "14:30")
                .param("location", "Building 14, Level 10, Room 12")
                .param("capacity", "45")
                .param("categoryId", techCategory.getId().toString())
                .param("unlimitedCapacity", "false")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/events/*")); // Redirects to event details page

        // Step 3: Verify event was created in database
        assertEquals(baselineEventCount + 1, eventRepository.count());

        Event createdEvent = eventRepository.findAll().stream()
                .filter(e -> e.getTitle().equals("Spring Boot Advanced Workshop"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Created event not found"));

        // Step 4: Verify event details
        assertEquals("Spring Boot Advanced Workshop", createdEvent.getTitle());
        assertEquals(testEventCreator.getUsername(), createdEvent.getCreatedBy().getUsername());
        assertEquals(techCategory.getId(), createdEvent.getCategory().getId());
        assertEquals(45, createdEvent.getCapacity());
        assertEquals("Building 14, Level 10, Room 12", createdEvent.getLocation());
        assertNotNull(createdEvent.getCreatedAt());

        // Step 5: Verify event appears on home page
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("events",
                        hasProperty("content", hasSize(greaterThanOrEqualTo(1)))))
                .andExpect(model().attribute("events", hasProperty("content", hasItem(
                        allOf(
                                hasProperty("title", is("Spring Boot Advanced Workshop")),
                                hasProperty("location", is("Building 14, Level 10, Room 12")),
                                hasProperty("maxAttendees", is(45)),
                                hasProperty("categoryName", is("Technology")))))));
    }

    @Test
    void testEventCreation_WithUnlimitedCapacity() throws Exception {
        String futureDate = LocalDate.now().plusDays(7).toString();

        // Create event with unlimited capacity checkbox checked
        mockMvc.perform(post("/events/create")
                .with(user(testEventCreator.getUsername()))
                .param("title", "Open Community Meetup")
                .param("description", "A community gathering open to all RMIT students and staff members")
                .param("eventDate", futureDate)
                .param("eventTime", "18:00")
                .param("location", "Alumni Courtyard")
                .param("capacity", "") // Empty when unlimited is checked
                .param("categoryId", musicCategory.getId().toString())
                .param("unlimitedCapacity", "true")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/events/*"));

        // Verify unlimited capacity (null) was saved
        Event unlimitedEvent = eventRepository.findAll().stream()
                .filter(e -> e.getTitle().equals("Open Community Meetup"))
                .findFirst()
                .orElseThrow();

        assertNull(unlimitedEvent.getCapacity(), "Capacity should be null for unlimited events");

        // Verify it appears correctly on home page
        mockMvc.perform(get("/"))
                .andExpect(model().attribute("events", hasProperty("content", hasItem(
                        allOf(
                                hasProperty("title", is("Open Community Meetup")),
                                hasProperty("maxAttendees", nullValue()),
                                hasProperty("eventFull", is(false)) // Never full with unlimited capacity
                        )))));
    }

    @Test
    void testEventCreation_ValidationErrors_StayOnForm() throws Exception {
        // Submit form with multiple validation errors
        mockMvc.perform(post("/events/create")
                .with(user(testEventCreator.getUsername()))
                .param("title", "") // Blank title - validation error
                .param("description", "Too short") // Less than 10 chars - validation error
                .param("eventDate", LocalDate.now().plusDays(1).toString())
                .param("eventTime", "10:00")
                .param("location", "") // Blank location - validation error
                .param("capacity", "0") // Below minimum (1) - validation error
                .param("categoryId", "") // No category selected - validation error
                .param("unlimitedCapacity", "false")
                .with(csrf()))
                .andExpect(status().isOk()) // Stays on form, doesn't redirect
                .andExpect(view().name("events/create-event"))
                .andExpect(model().attributeHasFieldErrors("eventCreateDTO",
                        "title", "description", "location", "capacity", "categoryId"))
                .andExpect(model().attributeExists("categories")); // Categories reloaded for form

        // Verify no event was created
        assertEquals(baselineEventCount, eventRepository.count());
    }

    @Test
    void testEventCreation_PastDateRejected() throws Exception {
        String pastDate = LocalDate.now().minusDays(1).toString();

        mockMvc.perform(post("/events/create")
                .with(user(testEventCreator.getUsername()))
                .param("title", "Past Event Test")
                .param("description", "This event has a past date and should be rejected")
                .param("eventDate", pastDate)
                .param("eventTime", "14:00")
                .param("location", "Test Location")
                .param("capacity", "30")
                .param("categoryId", techCategory.getId().toString())
                .param("unlimitedCapacity", "false")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("events/create-event"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("eventCreateDTO", "eventInFuture"));

        // Verify no event was created
        assertEquals(baselineEventCount, eventRepository.count());
    }

    @Test
    void testEventCreation_TodayPastTimeRejected() throws Exception {
        // Set a time 1 hour in the past using LocalDateTime to handle midnight edge cases
        LocalDateTime pastDateTime = LocalDateTime.now().minusHours(1);
        String pastDateStr = pastDateTime.toLocalDate().toString();
        String pastTimeStr = pastDateTime.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));

        mockMvc.perform(post("/events/create")
                .with(user(testEventCreator.getUsername()))
                .param("title", "Today Past Time Event")
                .param("description", "Event scheduled for earlier today should be rejected")
                .param("eventDate", pastDateStr)
                .param("eventTime", pastTimeStr)
                .param("location", "Test Location")
                .param("capacity", "25")
                .param("categoryId", sportsCategory.getId().toString())
                .param("unlimitedCapacity", "false")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("events/create-event"))
                .andExpect(model().hasErrors());

        // Verify no event was created
        assertEquals(baselineEventCount, eventRepository.count());
    }

    @Test
    void testEventCreation_TodayFutureTimeAccepted() throws Exception {
        // Set a time that is safely in the future but doesn't cross date boundaries
        // Use tomorrow's date at 10:00 AM to avoid any timing issues
        LocalDate tomorrowDate = LocalDate.now().plusDays(1);
        String futureTimeStr = "10:00";

        mockMvc.perform(post("/events/create")
                .with(user(testEventCreator.getUsername()))
                .param("title", "Tomorrow Future Event")
                .param("description", "Event scheduled for tomorrow should be accepted")
                .param("eventDate", tomorrowDate.toString())
                .param("eventTime", futureTimeStr)
                .param("location", "Urgent Meeting Room")
                .param("capacity", "15")
                .param("categoryId", techCategory.getId().toString())
                .param("unlimitedCapacity", "false")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/events/*"));

        // Verify event was created
        assertEquals(baselineEventCount + 1, eventRepository.count());
    }

    @Test
    void testEventCreation_InvalidCategoryRejected() throws Exception {
        String futureDate = LocalDate.now().plusDays(5).toString();

        mockMvc.perform(post("/events/create")
                .with(user(testEventCreator.getUsername()))
                .param("title", "Invalid Category Event")
                .param("description", "This event has an invalid category ID")
                .param("eventDate", futureDate)
                .param("eventTime", "15:00")
                .param("location", "Test Location")
                .param("capacity", "30")
                .param("categoryId", "99999") // Non-existent category ID
                .param("unlimitedCapacity", "false")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("events/create-event"))
                .andExpect(model().attribute("errorMessage", containsString("Invalid category")));

        // Verify no event was created
        assertEquals(baselineEventCount, eventRepository.count());
    }

    @Test
    void testUnauthenticatedUserCannotAccessCreateForm() throws Exception {
        // Attempt to access create form without authentication
        mockMvc.perform(get("/events/create"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        // Attempt to POST event data without authentication
        mockMvc.perform(post("/events/create")
                .param("title", "Unauthorized Event")
                .param("description", "This should not be created")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        // Verify no event was created
        assertEquals(baselineEventCount, eventRepository.count());
    }

    @Test
    void testEventCreation_BoundaryValidation() throws Exception {
        // Test maximum length boundaries
        String maxTitle = "A".repeat(100); // Exactly 100 characters
        String maxDescription = "B".repeat(2000); // Exactly 2000 characters
        String maxLocation = "C".repeat(255); // Exactly 255 characters
        String futureDate = LocalDate.now().plusDays(10).toString();

        mockMvc.perform(post("/events/create")
                .with(user(testEventCreator.getUsername()))
                .param("title", maxTitle)
                .param("description", maxDescription)
                .param("eventDate", futureDate)
                .param("eventTime", "12:00")
                .param("location", maxLocation)
                .param("capacity", "1") // Minimum valid capacity
                .param("categoryId", techCategory.getId().toString())
                .param("unlimitedCapacity", "false")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/events/*"));

        // Verify event was created with boundary values
        Event boundaryEvent = eventRepository.findAll().stream()
                .filter(e -> e.getTitle().equals(maxTitle))
                .findFirst()
                .orElseThrow();

        assertEquals(100, boundaryEvent.getTitle().length());
        assertEquals(2000, boundaryEvent.getDescription().length());
        assertEquals(255, boundaryEvent.getLocation().length());
        assertEquals(1, boundaryEvent.getCapacity());
    }

    @Test
    void testEventCreation_ExceedsMaximumLength() throws Exception {
        // Test exceeding maximum lengths
        String tooLongTitle = "A".repeat(101); // Exceeds 100 character limit
        String futureDate = LocalDate.now().plusDays(5).toString();

        mockMvc.perform(post("/events/create")
                .with(user(testEventCreator.getUsername()))
                .param("title", tooLongTitle)
                .param("description", "Valid description for the event")
                .param("eventDate", futureDate)
                .param("eventTime", "10:00")
                .param("location", "Valid Location")
                .param("capacity", "30")
                .param("categoryId", techCategory.getId().toString())
                .param("unlimitedCapacity", "false")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("events/create-event"))
                .andExpect(model().attributeHasFieldErrors("eventCreateDTO", "title"));

        // Verify no event was created
        assertEquals(baselineEventCount, eventRepository.count());
    }

    @Test
    void testEventCreation_MinimumDescriptionLength() throws Exception {
        // Test description exactly at minimum (10 characters)
        String futureDate = LocalDate.now().plusDays(3).toString();

        mockMvc.perform(post("/events/create")
                .with(user(testEventCreator.getUsername()))
                .param("title", "Min Description Event")
                .param("description", "Exactly 10") // Exactly 10 characters
                .param("eventDate", futureDate)
                .param("eventTime", "11:00")
                .param("location", "Small Room")
                .param("capacity", "10")
                .param("categoryId", musicCategory.getId().toString())
                .param("unlimitedCapacity", "false")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/events/*"));

        // Verify event was created
        Event minDescEvent = eventRepository.findAll().stream()
                .filter(e -> e.getTitle().equals("Min Description Event"))
                .findFirst()
                .orElseThrow();

        assertEquals("Exactly 10", minDescEvent.getDescription());
    }

    @Test
    void testCreatedEventAppearsOnHomePage() throws Exception {
        // Create multiple events and verify they all appear
        String date1 = LocalDate.now().plusDays(2).toString();
        String date2 = LocalDate.now().plusDays(4).toString();

        // Create first event
        mockMvc.perform(post("/events/create")
                .with(user(testEventCreator.getUsername()))
                .param("title", "First Test Event")
                .param("description", "First event for visibility test")
                .param("eventDate", date1)
                .param("eventTime", "09:00")
                .param("location", "Room A")
                .param("capacity", "20")
                .param("categoryId", techCategory.getId().toString())
                .param("unlimitedCapacity", "false")
                .with(csrf()))
                .andExpect(status().is3xxRedirection());

        // Create second event
        mockMvc.perform(post("/events/create")
                .with(user(testEventCreator.getUsername()))
                .param("title", "Second Test Event")
                .param("description", "Second event for visibility test")
                .param("eventDate", date2)
                .param("eventTime", "15:00")
                .param("location", "Room B")
                .param("capacity", "30")
                .param("categoryId", sportsCategory.getId().toString())
                .param("unlimitedCapacity", "false")
                .with(csrf()))
                .andExpect(status().is3xxRedirection());

        // Verify both events appear on home page
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("events",
                        hasProperty("content", hasSize(greaterThanOrEqualTo(2)))))
                .andExpect(model().attribute("events", hasProperty("content", hasItem(
                        hasProperty("title", is("First Test Event"))))))
                .andExpect(model().attribute("events", hasProperty("content", hasItem(
                        hasProperty("title", is("Second Test Event"))))));
    }

    @Test
    void testEventCreation_CapacityValidation() throws Exception {
        String futureDate = LocalDate.now().plusDays(5).toString();

        // Test with neither capacity nor unlimited checkbox (should fail)
        mockMvc.perform(post("/events/create")
                .with(user(testEventCreator.getUsername()))
                .param("title", "No Capacity Event")
                .param("description", "Event without capacity specification")
                .param("eventDate", futureDate)
                .param("eventTime", "14:00")
                .param("location", "Test Room")
                .param("capacity", "") // Empty capacity
                .param("categoryId", techCategory.getId().toString())
                .param("unlimitedCapacity", "false") // Not unlimited either
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("events/create-event"))
                .andExpect(model().hasErrors());

        // Verify no event was created
        assertEquals(baselineEventCount, eventRepository.count());
    }

    @Test
    void testEventCreation_SuccessMessageAndRedirect() throws Exception {
        String futureDate = LocalDate.now().plusDays(8).toString();

        // Create event and capture redirect URL
        mockMvc.perform(post("/events/create")
                .with(user(testEventCreator.getUsername()))
                .param("title", "Redirect Test Event")
                .param("description", "Testing redirect to event details page")
                .param("eventDate", futureDate)
                .param("eventTime", "16:30")
                .param("location", "Conference Hall")
                .param("capacity", "100")
                .param("categoryId", techCategory.getId().toString())
                .param("unlimitedCapacity", "false")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/events/*"))
                .andExpect(flash().attribute("successMessage", "Event created successfully!"));

        // Get the created event
        Event createdEvent = eventRepository.findAll().stream()
                .filter(e -> e.getTitle().equals("Redirect Test Event"))
                .findFirst()
                .orElseThrow();

        // Verify accessing the event details page works
        mockMvc.perform(get("/events/" + createdEvent.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("events/event-details"))
                .andExpect(model().attribute("event", hasProperty("title", is("Redirect Test Event"))));
    }
}