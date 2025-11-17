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

import au.edu.rmit.sept.webapp.model.Category;
import au.edu.rmit.sept.webapp.model.Event;
import au.edu.rmit.sept.webapp.model.User;
import au.edu.rmit.sept.webapp.model.RSVP;
import au.edu.rmit.sept.webapp.repository.CategoryRepository;
import au.edu.rmit.sept.webapp.repository.EventRepository;
import au.edu.rmit.sept.webapp.repository.UserRepository;
import au.edu.rmit.sept.webapp.repository.RSVPRepository;

import java.time.LocalDate;
import java.time.LocalTime;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ViewEventDetailsAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private RSVPRepository rsvpRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private User testCreator;
    private Category techCategory;
    private Category sportsCategory;
    private Event futureEvent;
    private Event startedEvent;
    private Event fullEvent;
    private Event unlimitedEvent;
    private Event existingEvent; // From DataInitializer

    @BeforeEach
    void setUp() {
        // Get existing categories from DataInitializer
        techCategory = categoryRepository.findAll().stream()
                .filter(c -> c.getName().equals("Technology"))
                .findFirst()
                .orElseThrow();

        sportsCategory = categoryRepository.findAll().stream()
                .filter(c -> c.getName().equals("Sports"))
                .findFirst()
                .orElseThrow();

        // Get existing user from DataInitializer (sarah)
        testCreator = userRepository.findByUsername("sarah.chen")
                .orElseThrow(() -> new RuntimeException("Expected user from DataInitializer not found"));

        // Create additional test user for authentication tests
        testUser = new User();
        testUser.setUsername("detail.viewer");
        testUser.setEmail("viewer@example.com");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setEnabled(true);
        userRepository.save(testUser);

        // Get one existing event from DataInitializer for testing
        existingEvent = eventRepository.findAll().stream()
                .filter(e -> e.getEventDate().isAfter(LocalDate.now()) || 
                           (e.getEventDate().isEqual(LocalDate.now()) && e.getEventTime().isAfter(LocalTime.now())))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No future events from DataInitializer found"));

        // Create comprehensive test events for different scenarios

        // Future event with normal capacity (baseline case)
        futureEvent = new Event();
        futureEvent.setTitle("Future Tech Conference");
        futureEvent.setDescription("A comprehensive technology conference covering the latest trends in software development, AI, and cloud computing. This event will feature keynote speakers from major tech companies and hands-on workshops.");
        futureEvent.setEventDate(LocalDate.now().plusDays(14));
        futureEvent.setEventTime(LocalTime.of(9, 30));
        futureEvent.setLocation("Melbourne Convention Centre, Hall A");
        futureEvent.setCapacity(150);
        futureEvent.setCategory(techCategory);
        futureEvent.setCreatedBy(testCreator);
        eventRepository.save(futureEvent);

        // Started event (past start time)
        startedEvent = new Event();
        startedEvent.setTitle("Started Workshop");
        startedEvent.setDescription("This workshop has already started but content is still available");
        startedEvent.setEventDate(LocalDate.now().minusDays(1));
        startedEvent.setEventTime(LocalTime.of(10, 0));
        startedEvent.setLocation("RMIT Building 14, Room 15");
        startedEvent.setCapacity(30);
        startedEvent.setCategory(sportsCategory);
        startedEvent.setCreatedBy(testCreator);
        eventRepository.save(startedEvent);

        // Full capacity event
        fullEvent = new Event();
        fullEvent.setTitle("Popular Sports Event");
        fullEvent.setDescription("Highly popular event that reached capacity quickly");
        fullEvent.setEventDate(LocalDate.now().plusDays(7));
        fullEvent.setEventTime(LocalTime.of(18, 0));
        fullEvent.setLocation("Sports Complex Arena");
        fullEvent.setCapacity(20);
        fullEvent.setCategory(sportsCategory);
        fullEvent.setCreatedBy(testCreator);
        eventRepository.save(fullEvent);

        // Create RSVPs to fill the event to capacity
        for (int i = 0; i < 20; i++) {
            User attendee = new User();
            attendee.setUsername("attendee" + i);
            attendee.setEmail("attendee" + i + "@example.com");
            attendee.setPassword(passwordEncoder.encode("password"));
            attendee.setEnabled(true);
            userRepository.save(attendee);

            RSVP rsvp = new RSVP(attendee, fullEvent);
            rsvpRepository.save(rsvp);
        }

        // Unlimited capacity event
        unlimitedEvent = new Event();
        unlimitedEvent.setTitle("Open Community Meetup");
        unlimitedEvent.setDescription("Community meetup with no capacity restrictions - everyone welcome!");
        unlimitedEvent.setEventDate(LocalDate.now().plusDays(21));
        unlimitedEvent.setEventTime(LocalTime.of(19, 0));
        unlimitedEvent.setLocation("Community Centre Main Hall");
        unlimitedEvent.setCapacity(null); // Unlimited
        unlimitedEvent.setCategory(techCategory);
        unlimitedEvent.setCreatedBy(testCreator);
        eventRepository.save(unlimitedEvent);

        // Add some RSVPs to test events
        RSVP userRsvp = new RSVP(testUser, futureEvent);
        rsvpRepository.save(userRsvp);
    }

    @Test
    void testAnonymousUserViewsEventDetails_CompleteFlow() throws Exception {
        // Step 1: Access event details page as anonymous user
        mockMvc.perform(get("/events/{id}", futureEvent.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("events/event-details"))
                .andExpect(model().attributeExists("event"));

        // Step 2: Verify all required event information is displayed
        mockMvc.perform(get("/events/{id}", futureEvent.getId()))
                .andExpect(model().attribute("event", 
                        allOf(
                                hasProperty("eventId", is(futureEvent.getId())),
                                hasProperty("title", is("Future Tech Conference")),
                                hasProperty("fullDescription", containsString("comprehensive technology conference")),
                                hasProperty("eventDate", is(LocalDate.now().plusDays(14))),
                                hasProperty("eventTime", is(LocalTime.of(9, 30))),
                                hasProperty("location", is("Melbourne Convention Centre, Hall A")),
                                hasProperty("categoryName", is("Technology")),
                                hasProperty("categoryColor", notNullValue()),
                                hasProperty("maxAttendees", is(150)),
                                hasProperty("attendeeCount", greaterThanOrEqualTo(0)),
                                hasProperty("createdByUsername", is("sarah.chen"))
                        )));

        // Step 3: Verify anonymous user sees no RSVP status
        mockMvc.perform(get("/events/{id}", futureEvent.getId()))
                .andExpect(model().attribute("event", 
                        hasProperty("userRsvpStatus", is(false))));

        // Step 4: Verify anonymous user gets empty attendee list (privacy)
        mockMvc.perform(get("/events/{id}", futureEvent.getId()))
                .andExpect(model().attribute("event", 
                        hasProperty("attendees", hasSize(0))));
    }

    @Test
    void testAuthenticatedUserViewsEventDetails_WithRSVPStatus() throws Exception {
        // Authenticated user should see their RSVP status and attendee list
        mockMvc.perform(get("/events/{id}", futureEvent.getId())
                .with(user(testUser.getUsername())))
                .andExpect(status().isOk())
                .andExpect(view().name("events/event-details"))
                .andExpect(model().attribute("event", 
                        allOf(
                                hasProperty("userRsvpStatus", is(true)), // testUser RSVP'd in setUp
                                hasProperty("attendees", hasSize(greaterThan(0))) // Should see attendee list
                        )));
    }

    @Test
    void testViewStartedEventDetails() throws Exception {
        // Users should be able to view details of started events
        mockMvc.perform(get("/events/{id}", startedEvent.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("events/event-details"))
                .andExpect(model().attribute("event", 
                        allOf(
                                hasProperty("title", is("Started Workshop")),
                                hasProperty("eventStarted", is(true)),
                                hasProperty("maxAttendees", is(30))
                        )));
    }

    @Test
    void testViewFullCapacityEventDetails() throws Exception {
        // Users should see when an event is at full capacity
        mockMvc.perform(get("/events/{id}", fullEvent.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("events/event-details"))
                .andExpect(model().attribute("event", 
                        allOf(
                                hasProperty("title", is("Popular Sports Event")),
                                hasProperty("eventFull", is(true)),
                                hasProperty("attendeeCount", is(20)),
                                hasProperty("maxAttendees", is(20))
                        )));
    }

    @Test
    void testViewUnlimitedCapacityEventDetails() throws Exception {
        // Events with unlimited capacity should show null maxAttendees
        mockMvc.perform(get("/events/{id}", unlimitedEvent.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("events/event-details"))
                .andExpect(model().attribute("event", 
                        allOf(
                                hasProperty("title", is("Open Community Meetup")),
                                hasProperty("maxAttendees", nullValue()),
                                hasProperty("eventFull", is(false)) // Never full with unlimited capacity
                        )));
    }

    @Test
    void testViewExistingEventFromDataInitializer() throws Exception {
        // Test viewing an event that was created by DataInitializer
        mockMvc.perform(get("/events/{id}", existingEvent.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("events/event-details"))
                .andExpect(model().attributeExists("event"))
                .andExpect(model().attribute("event", 
                        allOf(
                                hasProperty("eventId", is(existingEvent.getId())),
                                hasProperty("title", is(existingEvent.getTitle())),
                                hasProperty("fullDescription", is(existingEvent.getDescription())),
                                hasProperty("eventDate", is(existingEvent.getEventDate())),
                                hasProperty("eventTime", is(existingEvent.getEventTime())),
                                hasProperty("location", is(existingEvent.getLocation())),
                                hasProperty("categoryName", notNullValue()),
                                hasProperty("createdByUsername", notNullValue())
                        )));
    }

    @Test
    void testEventNotFound_ReturnsToHome() throws Exception {
        // Non-existent event ID should redirect to home
        mockMvc.perform(get("/events/{id}", 999999L))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));
    }

    @Test
    void testInvalidEventId_BadRequest() throws Exception {
        // Non-numeric event ID should return 400 Bad Request
        mockMvc.perform(get("/events/invalid-id"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testEventDetailsShowsAllRequiredInformation() throws Exception {
        // Comprehensive test ensuring all US-05 requirements are met
        mockMvc.perform(get("/events/{id}", futureEvent.getId()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("event", 
                        allOf(
                                // Title
                                hasProperty("title", notNullValue()),
                                hasProperty("title", not(emptyString())),
                                
                                // Description  
                                hasProperty("fullDescription", notNullValue()),
                                hasProperty("fullDescription", not(emptyString())),
                                
                                // Date and Time
                                hasProperty("eventDate", notNullValue()),
                                hasProperty("eventTime", notNullValue()),
                                
                                // Location
                                hasProperty("location", notNullValue()),
                                hasProperty("location", not(emptyString())),
                                
                                // Category
                                hasProperty("categoryName", notNullValue()),
                                hasProperty("categoryName", not(emptyString())),
                                hasProperty("categoryColor", notNullValue()),
                                
                                // Capacity information
                                hasProperty("attendeeCount", notNullValue()),
                                hasProperty("attendeeCount", greaterThanOrEqualTo(0)),
                                
                                // Creator information
                                hasProperty("createdByUsername", notNullValue()),
                                hasProperty("createdByUsername", not(emptyString()))
                        )));
    }

    @Test
    void testEventDetailsPageTitle() throws Exception {
        // Verify page title is set correctly for layout
        mockMvc.perform(get("/events/{id}", futureEvent.getId()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("pageTitle", "Future Tech Conference"));
    }

    @Test
    void testAttendeeListForAuthenticatedUsers() throws Exception {
        // Create a few more RSVPs for the future event
        User attendee1 = new User("attendee.one", "att1@example.com", passwordEncoder.encode("pass"));
        User attendee2 = new User("attendee.two", "att2@example.com", passwordEncoder.encode("pass"));
        userRepository.save(attendee1);
        userRepository.save(attendee2);
        
        rsvpRepository.save(new RSVP(attendee1, futureEvent));
        rsvpRepository.save(new RSVP(attendee2, futureEvent));

        // Authenticated user should see attendee details
        mockMvc.perform(get("/events/{id}", futureEvent.getId())
                .with(user(testUser.getUsername())))
                .andExpect(status().isOk())
                .andExpect(model().attribute("event", 
                        hasProperty("attendees", hasSize(greaterThan(2))))) // At least 3 attendees
                .andExpect(model().attribute("event", 
                        hasProperty("attendees", hasItem(
                                hasProperty("username", is("detail.viewer")))))) // testUser should be in list
                .andExpect(model().attribute("event", 
                        hasProperty("attendees", hasItem(
                                hasProperty("username", is("attendee.one"))))));
    }

    @Test
    void testEventStateIndicators_HelpDecisionMaking() throws Exception {
        // Test that events provide clear state information to help users decide
        
        // Future event - should show as available
        mockMvc.perform(get("/events/{id}", futureEvent.getId()))
                .andExpect(model().attribute("event", 
                        allOf(
                                hasProperty("eventStarted", is(false)),
                                hasProperty("eventFull", is(false))
                        )));

        // Started event - should show as started
        mockMvc.perform(get("/events/{id}", startedEvent.getId()))
                .andExpect(model().attribute("event", 
                        hasProperty("eventStarted", is(true))));

        // Full event - should show as full
        mockMvc.perform(get("/events/{id}", fullEvent.getId()))
                .andExpect(model().attribute("event", 
                        hasProperty("eventFull", is(true))));
    }

    @Test
    void testCategoryInformationDisplay() throws Exception {
        // Verify category information is properly displayed
        mockMvc.perform(get("/events/{id}", futureEvent.getId()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("event", 
                        allOf(
                                hasProperty("categoryName", is("Technology")),
                                hasProperty("categoryColor", is(techCategory.getColourCode()))
                        )));

        mockMvc.perform(get("/events/{id}", fullEvent.getId()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("event", 
                        allOf(
                                hasProperty("categoryName", is("Sports")),
                                hasProperty("categoryColor", is(sportsCategory.getColourCode()))
                        )));
    }

    @Test
    void testCapacityInformationForDecisionMaking() throws Exception {
        // Limited capacity event shows occupancy ratio
        mockMvc.perform(get("/events/{id}", futureEvent.getId()))
                .andExpect(model().attribute("event", 
                        allOf(
                                hasProperty("maxAttendees", is(150)),
                                hasProperty("attendeeCount", lessThan(150)), // Not full
                                hasProperty("eventFull", is(false))
                        )));

        // Unlimited capacity event shows open status  
        mockMvc.perform(get("/events/{id}", unlimitedEvent.getId()))
                .andExpect(model().attribute("event", 
                        allOf(
                                hasProperty("maxAttendees", nullValue()),
                                hasProperty("eventFull", is(false))
                        )));

        // Full capacity event shows unavailability
        mockMvc.perform(get("/events/{id}", fullEvent.getId()))
                .andExpect(model().attribute("event", 
                        allOf(
                                hasProperty("maxAttendees", is(20)),
                                hasProperty("attendeeCount", is(20)),
                                hasProperty("eventFull", is(true))
                        )));
    }

    @Test
    void testEventTimingInformation() throws Exception {
        // Verify detailed timing information is available for planning
        mockMvc.perform(get("/events/{id}", futureEvent.getId()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("event", 
                        allOf(
                                hasProperty("eventDate", is(LocalDate.now().plusDays(14))),
                                hasProperty("eventTime", is(LocalTime.of(9, 30))),
                                hasProperty("eventStarted", is(false))
                        )));
    }

    @Test
    void testErrorHandling_GracefulDegradation() throws Exception {
        // Even if some data is missing, page should still load
        // This tests robustness of the view layer
        
        // Test accessing event details still works when event exists
        mockMvc.perform(get("/events/{id}", futureEvent.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("events/event-details"));
    }
}