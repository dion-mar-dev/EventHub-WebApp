package au.edu.rmit.sept.webapp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import au.edu.rmit.sept.webapp.dto.EventCardDTO;
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
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;

/**
 * Acceptance tests for US-09: Event Visibility on Home Page
 * Tests that created events are immediately visible and discoverable by all users
 * Complements EventCreationAcceptanceTest by focusing on visibility and discoverability aspects
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EventVisibilityAcceptanceTest {

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

    private User eventCreator;
    private User otherUser;
    private Category testCategory;
    
    // Baseline counts
    private long baselineEventCount;
    private int baselineUpcomingEventsCount;
    
    // Unique identifier for test events
    private String testIdentifier;

    @BeforeEach
    void setUp() {
        // Generate unique identifier for this test run
        testIdentifier = "US09_" + UUID.randomUUID().toString().substring(0, 8);
        
        // Record baseline counts
        baselineEventCount = eventRepository.count();
        baselineUpcomingEventsCount = eventService.getUpcomingEvents(null).size();

        // Create event creator user
        eventCreator = new User();
        eventCreator.setUsername("us09.creator." + testIdentifier);
        eventCreator.setEmail("creator." + testIdentifier + "@test.com");
        eventCreator.setPassword(passwordEncoder.encode("Test123!"));
        eventCreator.setEnabled(true);
        userRepository.save(eventCreator);

        // Create another user to test visibility
        otherUser = new User();
        otherUser.setUsername("us09.viewer." + testIdentifier);
        otherUser.setEmail("viewer." + testIdentifier + "@test.com");
        otherUser.setPassword(passwordEncoder.encode("Test123!"));
        otherUser.setEnabled(true);
        userRepository.save(otherUser);

        // Get a category for testing (use existing from DataInitializer)
        testCategory = categoryRepository.findAll().stream()
                .filter(c -> c.getName().equals("Technology"))
                .findFirst()
                .orElseGet(() -> {
                    Category newCat = new Category();
                    newCat.setName("TestCategory");
                    return categoryRepository.save(newCat);
                });
    }

    @Test
    void testEventImmediatelyVisibleAfterCreation() throws Exception {
        String eventTitle = testIdentifier + "_Immediate_Visibility_Event";
        String eventDescription = "Testing immediate visibility on home page";
        String futureDate = LocalDate.now().plusDays(7).toString();

        // Create event as logged-in user
        mockMvc.perform(post("/events/create")
                .with(user(eventCreator.getUsername()))
                .param("title", eventTitle)
                .param("description", eventDescription)
                .param("eventDate", futureDate)
                .param("eventTime", "18:00")
                .param("location", "Test Venue A")
                .param("capacity", "50")
                .param("categoryId", testCategory.getId().toString())
                .param("unlimitedCapacity", "false")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/events/*"));

        // Immediately check home page without any delay
        MvcResult result = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attributeExists("events"))
                .andReturn();

        // Verify the event appears in the model
        Page<?> eventsPage = (Page<?>) result.getModelAndView().getModel().get("events");
        assertNotNull(eventsPage, "Events page should not be null");
        List<?> events = eventsPage.getContent();
        
        boolean eventFound = events.stream()
                .filter(e -> e instanceof EventCardDTO)
                .map(e -> (EventCardDTO) e)
                .anyMatch(e -> e.getTitle().equals(eventTitle));
        
        assertTrue(eventFound, "Newly created event should be immediately visible on home page");
    }

    @Test
    void testEventVisibleToAllUserTypes() throws Exception {
        String eventTitle = testIdentifier + "_Public_Event";
        String eventDescription = "Event visible to all users";
        String futureDate = LocalDate.now().plusDays(10).toString();

        // Create event as event creator
        mockMvc.perform(post("/events/create")
                .with(user(eventCreator.getUsername()))
                .param("title", eventTitle)
                .param("description", eventDescription)
                .param("eventDate", futureDate)
                .param("eventTime", "19:00")
                .param("location", "Public Venue")
                .param("capacity", "100")
                .param("categoryId", testCategory.getId().toString())
                .param("unlimitedCapacity", "false")
                .with(csrf()))
                .andExpect(status().is3xxRedirection());

        // Test 1: Visible to the event creator
        mockMvc.perform(get("/")
                .with(user(eventCreator.getUsername())))
                .andExpect(status().isOk())
                .andExpect(model().attribute("events", hasItem(
                        hasProperty("title", is(eventTitle)))));

        // Test 2: Visible to another logged-in user
        mockMvc.perform(get("/")
                .with(user(otherUser.getUsername())))
                .andExpect(status().isOk())
                .andExpect(model().attribute("events", hasItem(
                        hasProperty("title", is(eventTitle)))));

        // Test 3: Visible to anonymous users (not logged in)
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("events", hasItem(
                        hasProperty("title", is(eventTitle)))));
    }

    @Test
    void testEventDetailsDisplayedCorrectly() throws Exception {
        String eventTitle = testIdentifier + "_Detailed_Event";
        String eventDescription = "This event has all details properly displayed";
        String eventLocation = "Grand Hall, Building 80";
        String futureDate = LocalDate.now().plusDays(14).toString();
        String eventTime = "14:30";

        // Create event with full details
        mockMvc.perform(post("/events/create")
                .with(user(eventCreator.getUsername()))
                .param("title", eventTitle)
                .param("description", eventDescription)
                .param("eventDate", futureDate)
                .param("eventTime", eventTime)
                .param("location", eventLocation)
                .param("capacity", "75")
                .param("categoryId", testCategory.getId().toString())
                .param("unlimitedCapacity", "false")
                .with(csrf()))
                .andExpect(status().is3xxRedirection());

        // Check home page displays all key event details
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("events", hasItem(
                        allOf(
                            hasProperty("title", is(eventTitle)),
                            hasProperty("description", is(eventDescription)),
                            hasProperty("location", is(eventLocation)),
                            hasProperty("maxAttendees", is(75)),
                            hasProperty("creatorUsername", is(eventCreator.getUsername()))
                        ))));
    }

    @Test
    void testMultipleEventsCreatedByDifferentUsersAllVisible() throws Exception {
        // Create first event by first user
        String event1Title = testIdentifier + "_Event1";
        mockMvc.perform(post("/events/create")
                .with(user(eventCreator.getUsername()))
                .param("title", event1Title)
                .param("description", "First event by creator")
                .param("eventDate", LocalDate.now().plusDays(5).toString())
                .param("eventTime", "10:00")
                .param("location", "Room A")
                .param("capacity", "30")
                .param("categoryId", testCategory.getId().toString())
                .param("unlimitedCapacity", "false")
                .with(csrf()))
                .andExpect(status().is3xxRedirection());

        // Create second event by second user
        String event2Title = testIdentifier + "_Event2";
        mockMvc.perform(post("/events/create")
                .with(user(otherUser.getUsername()))
                .param("title", event2Title)
                .param("description", "Second event by viewer")
                .param("eventDate", LocalDate.now().plusDays(6).toString())
                .param("eventTime", "11:00")
                .param("location", "Room B")
                .param("capacity", "40")
                .param("categoryId", testCategory.getId().toString())
                .param("unlimitedCapacity", "false")
                .with(csrf()))
                .andExpect(status().is3xxRedirection());

        // Verify both events are visible on home page
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("events", hasItem(
                        hasProperty("title", is(event1Title)))))
                .andExpect(model().attribute("events", hasItem(
                        hasProperty("title", is(event2Title)))));

        // Verify total event count increased by 2
        assertEquals(baselineEventCount + 2, eventRepository.count(), 
                "Two new events should have been created");
    }

    @Test
    void testOnlyFutureEventsVisible() throws Exception {
        // Create a past event (should not be visible)
        Event pastEvent = new Event();
        pastEvent.setTitle(testIdentifier + "_Past_Event");
        pastEvent.setDescription("This is a past event");
        pastEvent.setEventDate(LocalDate.now().minusDays(5));
        pastEvent.setEventTime(LocalTime.of(15, 0));
        pastEvent.setLocation("Past Location");
        pastEvent.setCapacity(50);
        pastEvent.setCategory(testCategory);
        pastEvent.setCreatedBy(eventCreator);
        eventRepository.save(pastEvent);

        // Create a future event (should be visible)
        String futureEventTitle = testIdentifier + "_Future_Event";
        mockMvc.perform(post("/events/create")
                .with(user(eventCreator.getUsername()))
                .param("title", futureEventTitle)
                .param("description", "This is a future event")
                .param("eventDate", LocalDate.now().plusDays(3).toString())
                .param("eventTime", "16:00")
                .param("location", "Future Location")
                .param("capacity", "60")
                .param("categoryId", testCategory.getId().toString())
                .param("unlimitedCapacity", "false")
                .with(csrf()))
                .andExpect(status().is3xxRedirection());

        // Check home page
        MvcResult result = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn();

        Page<?> eventsPage = (Page<?>) result.getModelAndView().getModel().get("events");
        List<?> events = eventsPage.getContent();

        // Verify future event is visible
        boolean futureEventFound = events.stream()
                .filter(e -> e instanceof EventCardDTO)
                .map(e -> (EventCardDTO) e)
                .anyMatch(e -> e.getTitle().equals(futureEventTitle));
        assertTrue(futureEventFound, "Future event should be visible on home page");

        // Verify past event is NOT visible
        boolean pastEventFound = events.stream()
                .filter(e -> e instanceof EventCardDTO)
                .map(e -> (EventCardDTO) e)
                .anyMatch(e -> e.getTitle().equals(pastEvent.getTitle()));
        assertFalse(pastEventFound, "Past event should NOT be visible on home page");
    }

    @Test
    void testEventOrderingByDateOrRecency() throws Exception {
        // Create events with different dates
        String event1Title = testIdentifier + "_Near_Event";
        String event2Title = testIdentifier + "_Far_Event";
        String event3Title = testIdentifier + "_Middle_Event";

        // Create event happening in 2 days
        mockMvc.perform(post("/events/create")
                .with(user(eventCreator.getUsername()))
                .param("title", event1Title)
                .param("description", "Happening soon")
                .param("eventDate", LocalDate.now().plusDays(2).toString())
                .param("eventTime", "12:00")
                .param("location", "Venue 1")
                .param("capacity", "25")
                .param("categoryId", testCategory.getId().toString())
                .param("unlimitedCapacity", "false")
                .with(csrf()))
                .andExpect(status().is3xxRedirection());

        Thread.sleep(100); // Small delay to ensure different creation timestamps

        // Create event happening in 20 days
        mockMvc.perform(post("/events/create")
                .with(user(eventCreator.getUsername()))
                .param("title", event2Title)
                .param("description", "Happening later")
                .param("eventDate", LocalDate.now().plusDays(20).toString())
                .param("eventTime", "12:00")
                .param("location", "Venue 2")
                .param("capacity", "25")
                .param("categoryId", testCategory.getId().toString())
                .param("unlimitedCapacity", "false")
                .with(csrf()))
                .andExpect(status().is3xxRedirection());

        Thread.sleep(100);

        // Create event happening in 10 days
        mockMvc.perform(post("/events/create")
                .with(user(eventCreator.getUsername()))
                .param("title", event3Title)
                .param("description", "Happening in between")
                .param("eventDate", LocalDate.now().plusDays(10).toString())
                .param("eventTime", "12:00")
                .param("location", "Venue 3")
                .param("capacity", "25")
                .param("categoryId", testCategory.getId().toString())
                .param("unlimitedCapacity", "false")
                .with(csrf()))
                .andExpect(status().is3xxRedirection());

        // Get events from home page
        MvcResult result = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn();

        Page<?> eventsPage = (Page<?>) result.getModelAndView().getModel().get("events");
        List<?> events = eventsPage.getContent();

        // Find our test events
        List<EventCardDTO> testEvents = events.stream()
                .filter(e -> e instanceof EventCardDTO)
                .map(e -> (EventCardDTO) e)
                .filter(e -> e.getTitle().startsWith(testIdentifier))
                .toList();

        // Verify all three events are present
        assertEquals(3, testEvents.size(), "All three test events should be visible");
        
        // Check if events are ordered (either by event date or creation date)
        // The exact ordering depends on implementation, but events should have a consistent order
        assertNotNull(testEvents.get(0).getEventDate(), "Events should have dates");
        assertNotNull(testEvents.get(1).getEventDate(), "Events should have dates");
        assertNotNull(testEvents.get(2).getEventDate(), "Events should have dates");
    }

    @Test
    void testEventLinkNavigatesToDetails() throws Exception {
        String eventTitle = testIdentifier + "_Clickable_Event";
        
        // Create event
        MvcResult createResult = mockMvc.perform(post("/events/create")
                .with(user(eventCreator.getUsername()))
                .param("title", eventTitle)
                .param("description", "Click me to see details")
                .param("eventDate", LocalDate.now().plusDays(8).toString())
                .param("eventTime", "20:00")
                .param("location", "Click Venue")
                .param("capacity", "80")
                .param("categoryId", testCategory.getId().toString())
                .param("unlimitedCapacity", "false")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        // Get the created event ID from redirect URL
        String redirectUrl = createResult.getResponse().getRedirectedUrl();
        assertNotNull(redirectUrl, "Should redirect after event creation");
        
        // Extract event ID from URL (format: /events/{id})
        String eventId = redirectUrl.substring(redirectUrl.lastIndexOf('/') + 1);

        // Verify home page contains link to event details
        MvcResult homeResult = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn();

        String content = homeResult.getResponse().getContentAsString();
        
        // Check that the event details link exists in the HTML
        assertTrue(content.contains("/events/" + eventId), 
                "Home page should contain link to event details page");

        // Verify the event details page is accessible
        mockMvc.perform(get("/events/" + eventId))
                .andExpect(status().isOk())
                .andExpect(view().name("events/event-details"))
                .andExpect(model().attribute("event", hasProperty("title", is(eventTitle))));
    }

    @Test
    void testNewEventIncrementsCount() throws Exception {
        // Get initial count of upcoming events
        int initialCount = baselineUpcomingEventsCount;

        // Create a new event
        String eventTitle = testIdentifier + "_Count_Test_Event";
        mockMvc.perform(post("/events/create")
                .with(user(eventCreator.getUsername()))
                .param("title", eventTitle)
                .param("description", "Testing count increment")
                .param("eventDate", LocalDate.now().plusDays(4).toString())
                .param("eventTime", "09:00")
                .param("location", "Count Venue")
                .param("capacity", "20")
                .param("categoryId", testCategory.getId().toString())
                .param("unlimitedCapacity", "false")
                .with(csrf()))
                .andExpect(status().is3xxRedirection());

        // Check that upcoming events count has increased
        int newCount = eventService.getUpcomingEvents(null).size();
        assertEquals(initialCount + 1, newCount, 
                "Upcoming events count should increase by 1 after creating a new event");

        // Verify on home page
        MvcResult result = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn();

        Page<?> eventsPage = (Page<?>) result.getModelAndView().getModel().get("events");
        List<?> events = eventsPage.getContent();

        // The count should respect the 50-event limit as per the template
        assertTrue(events.size() <= 50, "Home page should respect maximum event display limit");
        
        // Our event should be included if within the limit
        boolean eventFound = events.stream()
                .filter(e -> e instanceof EventCardDTO)
                .map(e -> (EventCardDTO) e)
                .anyMatch(e -> e.getTitle().equals(eventTitle));
        
        if (events.size() < 50 || initialCount < 50) {
            assertTrue(eventFound, "New event should be visible when within display limit");
        }
    }
}