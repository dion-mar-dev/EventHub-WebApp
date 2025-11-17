package au.edu.rmit.sept.webapp;

import au.edu.rmit.sept.webapp.model.Event;
import au.edu.rmit.sept.webapp.model.User;
import au.edu.rmit.sept.webapp.model.Category;
import au.edu.rmit.sept.webapp.model.RSVP;
import au.edu.rmit.sept.webapp.repository.EventRepository;
import au.edu.rmit.sept.webapp.repository.UserRepository;
import au.edu.rmit.sept.webapp.repository.CategoryRepository;
import au.edu.rmit.sept.webapp.repository.RSVPRepository;
import au.edu.rmit.sept.webapp.service.EventService;
import au.edu.rmit.sept.webapp.dto.EventCardDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Acceptance tests for the My Events feature.
 * Tests the complete flow from HTTP request through controller, service, and repository to database.
 * Tests focus on instructional coverage of the RSVP'd Events functionality.
 * 
 * mvn test -Dtest=MyEventsAcceptanceTest
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MyEventsAcceptanceTest {

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

    @Autowired
    private EventService eventService;

    // Test users
    private User authenticatedUser;
    private User eventCreator;
    private User otherUser;
    private User otherOrganiser;
    
    // Test events
    private Event userCreatedEvent;
    private Event userRsvpEvent1;
    private Event userRsvpEvent2;
    private Event nonRsvpEvent;
    private Event pastEvent;
    
    // Additional events for organiser tab testing
    private Event userCreatedEvent1;
    private Event userCreatedEvent2;
    private Event otherUserCreatedEvent;
    private Event pastUserCreatedEvent;
    
    // Test category
    private Category testCategory;
    
    // RSVPs
    private RSVP rsvp1;
    private RSVP rsvp2;
    private RSVP otherUserRsvp;

    @BeforeEach
    void setUp() {
        // Create test category
        testCategory = categoryRepository.findAll().stream()
                .filter(c -> c.getName().equals("Technology"))
                .findFirst()
                .orElseThrow();

        // Create test users
        authenticatedUser = new User();
        authenticatedUser.setUsername("myevents.testuser");
        authenticatedUser.setEmail("myevents@test.com");
        authenticatedUser.setPassword(passwordEncoder.encode("TestPass123!"));
        authenticatedUser.setEnabled(true);
        authenticatedUser = userRepository.save(authenticatedUser);

        eventCreator = new User();
        eventCreator.setUsername("event.creator");
        eventCreator.setEmail("creator@test.com");
        eventCreator.setPassword(passwordEncoder.encode("TestPass123!"));
        eventCreator.setEnabled(true);
        eventCreator = userRepository.save(eventCreator);

        otherUser = new User();
        otherUser.setUsername("other.user");
        otherUser.setEmail("other@test.com");
        otherUser.setPassword(passwordEncoder.encode("TestPass123!"));
        otherUser.setEnabled(true);
        otherUser = userRepository.save(otherUser);

        // Create event that authenticatedUser created (should NOT appear in RSVP'd events)
        userCreatedEvent = new Event();
        userCreatedEvent.setTitle("User's Own Event");
        userCreatedEvent.setDescription("Event created by the authenticated user");
        userCreatedEvent.setEventDate(LocalDate.now().plusDays(10));
        userCreatedEvent.setEventTime(LocalTime.of(14, 0));
        userCreatedEvent.setLocation("User's Venue");
        userCreatedEvent.setCategory(testCategory);
        userCreatedEvent.setCreatedBy(authenticatedUser);
        userCreatedEvent.setCapacity(50);
        userCreatedEvent = eventRepository.save(userCreatedEvent);

        // Create events that authenticatedUser has RSVP'd to
        userRsvpEvent1 = new Event();
        userRsvpEvent1.setTitle("RSVP Event 1");
        userRsvpEvent1.setDescription("First event user has RSVP'd to");
        userRsvpEvent1.setEventDate(LocalDate.now().plusDays(5));
        userRsvpEvent1.setEventTime(LocalTime.of(10, 0));
        userRsvpEvent1.setLocation("Conference Center");
        userRsvpEvent1.setCategory(testCategory);
        userRsvpEvent1.setCreatedBy(eventCreator);
        userRsvpEvent1.setCapacity(100);
        userRsvpEvent1 = eventRepository.save(userRsvpEvent1);

        userRsvpEvent2 = new Event();
        userRsvpEvent2.setTitle("RSVP Event 2");
        userRsvpEvent2.setDescription("Second event user has RSVP'd to");
        userRsvpEvent2.setEventDate(LocalDate.now().plusDays(15));
        userRsvpEvent2.setEventTime(LocalTime.of(18, 30));
        userRsvpEvent2.setLocation("Tech Hub");
        userRsvpEvent2.setCategory(testCategory);
        userRsvpEvent2.setCreatedBy(eventCreator);
        userRsvpEvent2.setCapacity(75);
        userRsvpEvent2 = eventRepository.save(userRsvpEvent2);

        // Create event that user has NOT RSVP'd to
        nonRsvpEvent = new Event();
        nonRsvpEvent.setTitle("Non-RSVP Event");
        nonRsvpEvent.setDescription("Event user has not RSVP'd to");
        nonRsvpEvent.setEventDate(LocalDate.now().plusDays(7));
        nonRsvpEvent.setEventTime(LocalTime.of(15, 0));
        nonRsvpEvent.setLocation("Other Venue");
        nonRsvpEvent.setCategory(testCategory);
        nonRsvpEvent.setCreatedBy(eventCreator);
        nonRsvpEvent.setCapacity(60);
        nonRsvpEvent = eventRepository.save(nonRsvpEvent);

        // Create past event (should not appear)
        pastEvent = new Event();
        pastEvent.setTitle("Past Event");
        pastEvent.setDescription("Event that already happened");
        pastEvent.setEventDate(LocalDate.now().minusDays(5));
        pastEvent.setEventTime(LocalTime.of(12, 0));
        pastEvent.setLocation("Old Venue");
        pastEvent.setCategory(testCategory);
        pastEvent.setCreatedBy(eventCreator);
        pastEvent.setCapacity(40);
        pastEvent = eventRepository.save(pastEvent);

        // Create RSVPs
        rsvp1 = new RSVP(authenticatedUser, userRsvpEvent1);
        rsvp1.setRsvpDate(LocalDateTime.now().minusDays(2));
        rsvpRepository.save(rsvp1);

        rsvp2 = new RSVP(authenticatedUser, userRsvpEvent2);
        rsvp2.setRsvpDate(LocalDateTime.now().minusDays(1));
        rsvpRepository.save(rsvp2);

        // Other user RSVP to nonRsvpEvent (to verify filtering)
        otherUserRsvp = new RSVP(otherUser, nonRsvpEvent);
        otherUserRsvp.setRsvpDate(LocalDateTime.now());
        rsvpRepository.save(otherUserRsvp);

        // Create past RSVP (should not appear)
        RSVP pastRsvp = new RSVP(authenticatedUser, pastEvent);
        pastRsvp.setRsvpDate(LocalDateTime.now().minusDays(6));
        rsvpRepository.save(pastRsvp);

        // Setup organiser tab test data
        setupOrganiserTabTestData();
    }

    private void setupOrganiserTabTestData() {
        // Create another user who will create events
        otherOrganiser = new User();
        otherOrganiser.setUsername("other.organiser");
        otherOrganiser.setEmail("other.organiser@test.com");
        otherOrganiser.setPassword(passwordEncoder.encode("TestPass123!"));
        otherOrganiser.setEnabled(true);
        otherOrganiser = userRepository.save(otherOrganiser);

        // Create events where authenticatedUser is the creator
        userCreatedEvent1 = new Event();
        userCreatedEvent1.setTitle("User Created Event 1");
        userCreatedEvent1.setDescription("First event created by authenticated user");
        userCreatedEvent1.setEventDate(LocalDate.now().plusDays(5));
        userCreatedEvent1.setEventTime(LocalTime.of(10, 0));
        userCreatedEvent1.setLocation("Creator Venue 1");
        userCreatedEvent1.setCategory(testCategory);
        userCreatedEvent1.setCreatedBy(authenticatedUser); // User is the creator
        userCreatedEvent1.setCapacity(50);
        userCreatedEvent1 = eventRepository.save(userCreatedEvent1);

        userCreatedEvent2 = new Event();
        userCreatedEvent2.setTitle("User Created Event 2");
        userCreatedEvent2.setDescription("Second event created by authenticated user");
        userCreatedEvent2.setEventDate(LocalDate.now().plusDays(12));
        userCreatedEvent2.setEventTime(LocalTime.of(14, 0));
        userCreatedEvent2.setLocation("Creator Venue 2");
        userCreatedEvent2.setCategory(testCategory);
        userCreatedEvent2.setCreatedBy(authenticatedUser); // User is the creator
        userCreatedEvent2.setCapacity(75);
        userCreatedEvent2 = eventRepository.save(userCreatedEvent2);

        // Create event by different organiser (should not appear in user's organiser tab)
        otherUserCreatedEvent = new Event();
        otherUserCreatedEvent.setTitle("Other Organiser Event");
        otherUserCreatedEvent.setDescription("Event created by different user");
        otherUserCreatedEvent.setEventDate(LocalDate.now().plusDays(8));
        otherUserCreatedEvent.setEventTime(LocalTime.of(16, 0));
        otherUserCreatedEvent.setLocation("Other Organiser Venue");
        otherUserCreatedEvent.setCategory(testCategory);
        otherUserCreatedEvent.setCreatedBy(otherOrganiser);
        otherUserCreatedEvent.setCapacity(100);
        otherUserCreatedEvent = eventRepository.save(otherUserCreatedEvent);

        // Create past event by authenticated user (should not appear)
        pastUserCreatedEvent = new Event();
        pastUserCreatedEvent.setTitle("Past Created Event");
        pastUserCreatedEvent.setDescription("Past event created by user");
        pastUserCreatedEvent.setEventDate(LocalDate.now().minusDays(3));
        pastUserCreatedEvent.setEventTime(LocalTime.of(11, 0));
        pastUserCreatedEvent.setLocation("Past Venue");
        pastUserCreatedEvent.setCategory(testCategory);
        pastUserCreatedEvent.setCreatedBy(authenticatedUser);
        pastUserCreatedEvent.setCapacity(30);
        pastUserCreatedEvent = eventRepository.save(pastUserCreatedEvent);
    }

    // Test 1: Authenticated user can access My Events page
    @Test
    void testAuthenticatedUserCanAccessMyEventsPage() throws Exception {
        mockMvc.perform(get("/my-events")
                .with(user(authenticatedUser.getUsername())))
                .andExpect(status().isOk())
                .andExpect(view().name("my-events"))
                .andExpect(model().attributeExists("username"))
                .andExpect(model().attributeExists("rsvpEvents"))
                .andExpect(model().attribute("username", authenticatedUser.getUsername()));
    }

    // Test 2: Unauthenticated user redirected to login
    @Test
    void testUnauthenticatedUserRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/my-events"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    // Test 3: RSVP'd Events tab is active by default
    @Test
    void testRsvpTabIsActiveByDefault() throws Exception {
        mockMvc.perform(get("/my-events")
                .with(user(authenticatedUser.getUsername())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("nav-link active\"")))
                .andExpect(content().string(containsString("RSVP'd Events")))
                .andExpect(content().string(containsString("tab-pane fade show active\" id=\"rsvp-events\"")));
    }


    // Test 5: User with no RSVPs sees empty state message
    @Test
    void testUserWithNoRsvpsSeesEmptyState() throws Exception {
        // Create new user with no RSVPs
        User noRsvpUser = new User();
        noRsvpUser.setUsername("no.rsvp.user");
        noRsvpUser.setEmail("norsvp@test.com");
        noRsvpUser.setPassword(passwordEncoder.encode("TestPass123!"));
        noRsvpUser.setEnabled(true);
        userRepository.save(noRsvpUser);

        mockMvc.perform(get("/my-events")
                .with(user(noRsvpUser.getUsername())))
                .andExpect(status().isOk())
                .andExpect(model().attribute("rsvpEvents", hasSize(0)))
                .andExpect(content().string(containsString("You haven't RSVP'd to any events yet")))
                .andExpect(content().string(containsString("Browse Events")))
                .andExpect(content().string(containsString("href=\"/\"")));
    }

    // Test 6: User sees events they've RSVP'd to
    @Test
    void testUserSeesRsvpEvents() throws Exception {
        mockMvc.perform(get("/my-events")
                .with(user(authenticatedUser.getUsername())))
                .andExpect(status().isOk())
                .andExpect(model().attribute("rsvpEvents", hasSize(2)))
                .andExpect(content().string(containsString("RSVP Event 1")))
                .andExpect(content().string(containsString("RSVP Event 2")))
                .andExpect(content().string(containsString("Conference Center")))
                .andExpect(content().string(containsString("Tech Hub")));
    }

    // Test 7: User doesn't see events they created
    @Test
    void testUserDoesNotSeeOwnCreatedEvents() throws Exception {
        // Add RSVP to own event (edge case)
        RSVP ownEventRsvp = new RSVP(authenticatedUser, userCreatedEvent);
        rsvpRepository.save(ownEventRsvp);

        mockMvc.perform(get("/my-events")
                .with(user(authenticatedUser.getUsername())))
                .andExpect(status().isOk())
                .andExpect(model().attribute("rsvpEvents", hasSize(2))) // Still only 2
                .andExpect(content().string(not(containsString("User's Own Event"))));
    }

    // Test 8: User doesn't see events they haven't RSVP'd to
    @Test
    void testUserDoesNotSeeNonRsvpEvents() throws Exception {
        mockMvc.perform(get("/my-events")
                .with(user(authenticatedUser.getUsername())))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Non-RSVP Event"))))
                .andExpect(content().string(not(containsString("Other Venue"))));
    }

    // Test 9: RSVP'd events show "Going" badge
    @Test
    void testRsvpEventsShowGoingBadge() throws Exception {
        mockMvc.perform(get("/my-events")
                .with(user(authenticatedUser.getUsername())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("badge bg-success\">")))
                .andExpect(content().string(containsString("Going")));
    }

    // Test 10: Event cards maintain functionality
    @Test
    void testEventCardsFunctionality() throws Exception {
        mockMvc.perform(get("/my-events")
                .with(user(authenticatedUser.getUsername())))
                .andExpect(status().isOk())
                // View Details buttons
                .andExpect(content().string(containsString("href=\"/events/" + userRsvpEvent1.getId() + "\"")))
                .andExpect(content().string(containsString("href=\"/events/" + userRsvpEvent2.getId() + "\"")))
                .andExpect(content().string(containsString("View Details")))
                // Cancel RSVP forms
                .andExpect(content().string(containsString("action=\"/rsvp/cancel\"")))
                .andExpect(content().string(containsString("Cancel RSVP")));
    }

    // Additional test: Verify events are ordered by date
    @Test
    void testRsvpEventsOrderedByDate() throws Exception {
        // Get the actual RSVP events for the user
        List<EventCardDTO> rsvpEvents = eventService.getUserRSVPEvents(authenticatedUser.getId());
        
        // Verify ordering
        assertEquals(2, rsvpEvents.size());
        assertEquals("RSVP Event 1", rsvpEvents.get(0).getTitle()); // Sooner event first
        assertEquals("RSVP Event 2", rsvpEvents.get(1).getTitle()); // Later event second
        
        // Verify through web request as well
        mockMvc.perform(get("/my-events")
                .with(user(authenticatedUser.getUsername())))
                .andExpect(status().isOk())
                .andExpect(model().attribute("rsvpEvents", contains(
                    hasProperty("title", is("RSVP Event 1")),
                    hasProperty("title", is("RSVP Event 2"))
                )));
    }

    // Additional test: Verify past events are excluded
    @Test
    void testPastEventsExcluded() throws Exception {
        mockMvc.perform(get("/my-events")
                .with(user(authenticatedUser.getUsername())))
                .andExpect(status().isOk())
                .andExpect(model().attribute("rsvpEvents", hasSize(2)))
                .andExpect(content().string(not(containsString("Past Event"))));
    }

    // Additional test: Verify badge display for full mode
    @Test
    void testEventCardBadgesInFullMode() throws Exception {
        mockMvc.perform(get("/my-events")
                .with(user(authenticatedUser.getUsername())))
                .andExpect(status().isOk())
                // Check for full mode specific elements
                .andExpect(content().string(containsString("badge bg-success\">")))
                .andExpect(content().string(containsString("Going")))
                // Category badge
                .andExpect(content().string(containsString("badge\"")))
                .andExpect(content().string(containsString("Technology")));
    }

    // Test 1: Organiser tab displays user's created events
    @Test
    void testOrganiserTabShowsUserCreatedEvents() throws Exception {
        mockMvc.perform(get("/my-events")
                .with(user(authenticatedUser.getUsername())))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("createdEvents"))
                .andExpect(content().string(containsString("User Created Event 1")))
                .andExpect(content().string(containsString("User Created Event 2")))
                .andExpect(content().string(containsString("Creator Venue 1")))
                .andExpect(content().string(containsString("Creator Venue 2")));
    }

    // Test 2: Organiser tab excludes events user didn't create
    @Test
    void testOrganiserTabExcludesOtherUsersEvents() throws Exception {
        mockMvc.perform(get("/my-events")
                .with(user(authenticatedUser.getUsername())))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Other Organiser Event"))))
                .andExpect(content().string(not(containsString("Other Organiser Venue"))));
    }

    // Test 3: Organiser tab shows upcoming events only
    @Test
    void testOrganiserTabExcludesPastEvents() throws Exception {
        mockMvc.perform(get("/my-events")
                .with(user(authenticatedUser.getUsername())))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Past Created Event"))))
                .andExpect(content().string(not(containsString("Past Venue"))));
    }

    // Test 4: Events in organiser tab are ordered by date ascending
    @Test
    void testOrganiserEventsOrderedByDateAscending() throws Exception {
        List<EventCardDTO> createdEvents = eventService.getUserCreatedEvents(authenticatedUser.getId());

        // Verify ordering in service layer
        assertTrue(createdEvents.size() >= 2, "Should have at least 2 created events");
        for (int i = 0; i < createdEvents.size() - 1; i++) {
            LocalDateTime current = LocalDateTime.of(
                    createdEvents.get(i).getEventDate(),
                    createdEvents.get(i).getEventTime());
            LocalDateTime next = LocalDateTime.of(
                    createdEvents.get(i + 1).getEventDate(),
                    createdEvents.get(i + 1).getEventTime());
            assertTrue(current.isBefore(next) || current.isEqual(next),
                    "Events should be ordered by date/time ascending");
        }

        // Verify in view
        String response = mockMvc.perform(get("/my-events")
                .with(user(authenticatedUser.getUsername())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        int index1 = response.indexOf("User Created Event 1");
        int index2 = response.indexOf("User Created Event 2");
        assertTrue(index1 < index2, "Earlier event should appear before later event in HTML");
    }

    // Test 5: Organiser tab is not active by default
    @Test
    void testOrganiserTabNotActiveByDefault() throws Exception {
        mockMvc.perform(get("/my-events")
                .with(user(authenticatedUser.getUsername())))
                .andExpect(status().isOk())
                // Organiser tab should not have active class
                .andExpect(content().string(containsString("id=\"organiser-tab\"")))
                .andExpect(content().string(not(containsString("nav-link active\" id=\"organiser-tab\""))))
                // Tab pane should not be active
                .andExpect(content().string(containsString("tab-pane fade\" id=\"organiser-events\"")))
                .andExpect(
                        content().string(not(containsString("tab-pane fade show active\" id=\"organiser-events\""))));
    }

    // Test 6: Tab switching - clicking organiser tab activates it
    @Test
    void testOrganiserTabContentStructure() throws Exception {
        mockMvc.perform(get("/my-events")
                .with(user(authenticatedUser.getUsername())))
                .andExpect(status().isOk())
                // Verify tab button exists with correct attributes
                .andExpect(content().string(containsString("data-bs-target=\"#organiser-events\"")))
                .andExpect(content().string(containsString("aria-controls=\"organiser-events\"")))
                .andExpect(content().string(containsString("My Events (Organiser is You)")))
                // Verify tab pane exists
                .andExpect(content().string(containsString("id=\"organiser-events\"")))
                .andExpect(content().string(containsString("aria-labelledby=\"organiser-tab\"")));
    }

    // Test 7: Event count badge shows correct number
    @Test
    void testOrganiserTabEventCountBadge() throws Exception {
        // Count expected events (accounting for DataInitializer)
        long userCreatedCount = eventRepository.findAll().stream()
                .filter(e -> e.getCreatedBy() != null &&
                        e.getCreatedBy().getId().equals(authenticatedUser.getId()) &&
                        e.getEventDate().isAfter(LocalDate.now().minusDays(1)))
                .count();

        mockMvc.perform(get("/my-events")
                .with(user(authenticatedUser.getUsername())))
                .andExpect(status().isOk())
                .andExpect(model().attribute("createdEvents", hasSize((int) userCreatedCount)))
                // Badge should show the count
                .andExpect(content().string(
                        containsString("<span class=\"badge bg-secondary ms-2\">" + userCreatedCount + "</span>")));
    }

    // Test 8: Empty state shows for users with no created events
    @Test
    void testOrganiserTabEmptyState() throws Exception {
        // Create new user with no created events
        User noEventsUser = new User();
        noEventsUser.setUsername("no.events.organiser");
        noEventsUser.setEmail("noevents@test.com");
        noEventsUser.setPassword(passwordEncoder.encode("TestPass123!"));
        noEventsUser.setEnabled(true);
        noEventsUser = userRepository.save(noEventsUser);

        mockMvc.perform(get("/my-events")
                .with(user(noEventsUser.getUsername())))
                .andExpect(status().isOk())
                .andExpect(model().attribute("createdEvents", hasSize(0)))
                // Empty state message
                .andExpect(content().string(containsString("No Created Events")))
                .andExpect(content().string(containsString("You haven't created any events yet")))
                .andExpect(content().string(containsString("Start organizing your first event!")))
                // Create Event button
                .andExpect(content().string(containsString("href=\"/events/create\"")))
                .andExpect(content().string(containsString("Create Event")));
    }

    // Test 9: Created events use same event card component
    @Test
    void testOrganiserTabUsesEventCardComponent() throws Exception {
        mockMvc.perform(get("/my-events")
                .with(user(authenticatedUser.getUsername())))
                .andExpect(status().isOk())
                // Verify event cards are rendered with expected classes
                .andExpect(content().string(containsString("class=\"card h-100 event-card\"")))
                // Verify card structure elements exist
                .andExpect(content().string(containsString("card-body")))
                .andExpect(content().string(containsString("card-title")));
    }

    // Test 10: Event cards maintain all functionality in organiser tab
    @Test
    void testOrganiserTabEventCardsFunctionality() throws Exception {
        mockMvc.perform(get("/my-events")
                .with(user(authenticatedUser.getUsername())))
                .andExpect(status().isOk())
                // View Details buttons
                .andExpect(content().string(containsString("href=\"/events/" + userCreatedEvent1.getId() + "\"")))
                .andExpect(content().string(containsString("href=\"/events/" + userCreatedEvent2.getId() + "\"")))
                .andExpect(content().string(containsString("View Details")))
                // Event cards use standard functionality (no edit buttons added)
                .andExpect(content().string(containsString("action=\"/rsvp/cancel\"")));
    }

    // Test 11: User who creates and RSVPs to own event sees it only in organiser tab
    @Test
    void testUserCreatorAndRsvpShowsOnlyInOrganiserTab() throws Exception {
        // Create RSVP for user's own created event
        RSVP selfRsvp = new RSVP();
        selfRsvp.setUser(authenticatedUser);
        selfRsvp.setEvent(userCreatedEvent1);
        selfRsvp.setRsvpDate(LocalDateTime.now());
        rsvpRepository.save(selfRsvp);

        String response = mockMvc.perform(get("/my-events")
                .with(user(authenticatedUser.getUsername())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Count occurrences of the event title
        int occurrences = response.split("User Created Event 1", -1).length - 1;
        assertEquals(1, occurrences, "Event should appear only in organiser tab, not RSVP'd tab");
        
        // Verify it appears in organiser tab content
        assertTrue(response.contains("User Created Event 1"), "Event should appear in page");
    }

    // Test 12: Verify 30-event limit is enforced for organiser tab
    @Test
    void testOrganiserTabEventLimit() throws Exception {
        // Create 35 events for the user
        for (int i = 0; i < 35; i++) {
            Event event = new Event();
            event.setTitle("Bulk Event " + i);
            event.setDescription("Bulk event for limit testing");
            event.setEventDate(LocalDate.now().plusDays(i + 1));
            event.setEventTime(LocalTime.of(12, 0));
            event.setLocation("Bulk Venue " + i);
            event.setCategory(testCategory);
            event.setCreatedBy(authenticatedUser);
            event.setCapacity(50);
            eventRepository.save(event);
        }

        List<EventCardDTO> createdEvents = eventService.getUserCreatedEvents(authenticatedUser.getId());

        mockMvc.perform(get("/my-events")
                .with(user(authenticatedUser.getUsername())))
                .andExpect(status().isOk())
                .andExpect(model().attribute("createdEvents", hasSize(Math.min(createdEvents.size(), 30))));
    }

    // Test 13: Page refresh functionality with JavaScript
    @Test
    void testPageRefreshJavaScriptPresent() throws Exception {
        mockMvc.perform(get("/my-events")
                .with(user(authenticatedUser.getUsername())))
                .andExpect(status().isOk())
                // Verify the page refresh JavaScript is present
                .andExpect(content().string(containsString("window.addEventListener('pageshow'")))
                .andExpect(content().string(containsString("event.persisted")))
                .andExpect(content().string(containsString("location.reload()")));
    }

    // Test 14: Organiser tab heading and icon
    @Test
    void testOrganiserTabHeadingAndIcon() throws Exception {
        mockMvc.perform(get("/my-events")
                .with(user(authenticatedUser.getUsername())))
                .andExpect(status().isOk())
                // Tab icon and text
                .andExpect(content().string(containsString("fa-user-crown")))
                .andExpect(content().string(containsString("My Events (Organiser is You)")))
                // Content heading
                .andExpect(content().string(containsString("Events You've Created")));
    }
}
