package au.edu.rmit.sept.webapp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

import au.edu.rmit.sept.webapp.model.Category;
import au.edu.rmit.sept.webapp.model.Event;
import au.edu.rmit.sept.webapp.model.User;
import au.edu.rmit.sept.webapp.repository.CategoryRepository;
import au.edu.rmit.sept.webapp.repository.EventRepository;
import au.edu.rmit.sept.webapp.repository.UserRepository;
import au.edu.rmit.sept.webapp.service.EventService;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Acceptance tests for US-07: Event Card Display
 * Tests the display of events as cards showing:
 * - Title
 * - Date
 * - Location
 * - Brief description
 * - Category badges
 * Focuses on the card display functionality for visitors to quickly scan
 * events.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EventCardDisplayAcceptanceTest {

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

    private User testCreator;
    private Category techCategory;
    private Category sportsCategory;
    private Category musicCategory;
    private Category businessCategory;
    private Event shortDescEvent;
    private Event longDescEvent;
    private Event techEvent;
    private Event sportsEvent;
    private Event musicEvent;

    // Baseline count for relative assertions
    private int baselineEventCount;

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

        musicCategory = categoryRepository.findAll().stream()
                .filter(c -> c.getName().equals("Music"))
                .findFirst()
                .orElseThrow();

        businessCategory = categoryRepository.findAll().stream()
                .filter(c -> c.getName().equals("Business"))
                .findFirst()
                .orElseThrow();

        // Count baseline events before adding test data
        baselineEventCount = eventService.getUpcomingEvents(null).size();

        // Create test creator
        testCreator = new User();
        testCreator.setUsername("card.test.creator");
        testCreator.setEmail("cardcreator@example.com");
        testCreator.setPassword(passwordEncoder.encode("password123"));
        testCreator.setEnabled(true);
        userRepository.save(testCreator);

        // Create events to test different card display scenarios

        // Event with short description (won't be truncated)
        shortDescEvent = new Event();
        shortDescEvent.setTitle("Quick Python Workshop");
        shortDescEvent.setDescription("Learn Python basics in 2 hours");
        shortDescEvent.setEventDate(LocalDate.now().plusDays(3));
        shortDescEvent.setEventTime(LocalTime.of(10, 0));
        shortDescEvent.setLocation("Building 14, Room 12");
        shortDescEvent.setCapacity(30);
        shortDescEvent.setCategory(techCategory);
        shortDescEvent.setCreatedBy(testCreator);
        eventRepository.save(shortDescEvent);

        // Event with long description (will be truncated for card display)
        longDescEvent = new Event();
        longDescEvent.setTitle("Advanced Data Science Conference");
        longDescEvent.setDescription("Comprehensive conference covering machine learning, artificial intelligence, " +
                "data visualization techniques, statistical analysis, and hands-on workshops with industry experts " +
                "from leading tech companies. This full-day event includes networking sessions and practical labs.");
        longDescEvent.setEventDate(LocalDate.now().plusDays(5));
        longDescEvent.setEventTime(LocalTime.of(9, 30));
        longDescEvent.setLocation("RMIT Convention Centre, Level 3");
        longDescEvent.setCapacity(150);
        longDescEvent.setCategory(techCategory);
        longDescEvent.setCreatedBy(testCreator);
        eventRepository.save(longDescEvent);

        // Events with different categories to test badge colors
        techEvent = new Event();
        techEvent.setTitle("Cybersecurity Seminar");
        techEvent.setDescription("Latest trends in cybersecurity and threat prevention");
        techEvent.setEventDate(LocalDate.now().plusDays(7));
        techEvent.setEventTime(LocalTime.of(14, 0));
        techEvent.setLocation("Security Lab, Building 10");
        techEvent.setCapacity(40);
        techEvent.setCategory(techCategory);
        techEvent.setCreatedBy(testCreator);
        eventRepository.save(techEvent);

        sportsEvent = new Event();
        sportsEvent.setTitle("Basketball Tournament Finals");
        sportsEvent.setDescription("Championship finals with trophies for winning teams");
        sportsEvent.setEventDate(LocalDate.now().plusDays(10));
        sportsEvent.setEventTime(LocalTime.of(18, 0));
        sportsEvent.setLocation("RMIT Sports Centre");
        sportsEvent.setCapacity(200);
        sportsEvent.setCategory(sportsCategory);
        sportsEvent.setCreatedBy(testCreator);
        eventRepository.save(sportsEvent);

        musicEvent = new Event();
        musicEvent.setTitle("Student Jazz Performance");
        musicEvent.setDescription("Evening of jazz music performed by RMIT students");
        musicEvent.setEventDate(LocalDate.now().plusDays(12));
        musicEvent.setEventTime(LocalTime.of(19, 30));
        musicEvent.setLocation("Kaleide Theatre");
        musicEvent.setCapacity(100);
        musicEvent.setCategory(musicCategory);
        musicEvent.setCreatedBy(testCreator);
        eventRepository.save(musicEvent);
    }

    @Test
    void testVisitorSeesEventCardsWithAllRequiredInformation() throws Exception {
        // US-07: Visitor should see cards with title, date, location, brief
        // description, category badges
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attribute("events",
                        hasProperty("content", hasSize(greaterThanOrEqualTo(5)))))
                .andExpect(model().attribute("events", hasProperty("content", hasItem(
                        allOf(
                                // Title displayed
                                hasProperty("title", is("Quick Python Workshop")),
                                // Date displayed
                                hasProperty("eventDate", is(LocalDate.now().plusDays(3))),
                                // Time displayed
                                hasProperty("eventTime", is(LocalTime.of(10, 0))),
                                // Location displayed
                                hasProperty("location", is("Building 14, Room 12")),
                                // Brief description for scanning
                                hasProperty("briefDescription", is("Learn Python basics in 2 hours")),
                                // Category badge information
                                hasProperty("categoryName", is("Technology")),
                                hasProperty("categoryColor", is("#5dade2")))))));
    }

    @Test
    void testEventCardShowsTruncatedDescriptionForQuickScanning() throws Exception {
        // Brief descriptions should be truncated to 50 characters for quick scanning
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("events", hasProperty("content", hasItem(
                        allOf(
                                hasProperty("title", is("Advanced Data Science Conference")),
                                hasProperty("briefDescription", hasLength(50)),
                                hasProperty("briefDescription", endsWith("...")),
                                // Full description should be longer but capped at 100 for full mode
                                hasProperty("description", hasLength(100)))))));
    }

    @Test
    void testCategoryBadgesDisplayCorrectColorsAndNames() throws Exception {
        // Test different category badges show correct names and colors
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("events", hasProperty("content", hasItem(
                        allOf(
                                hasProperty("title", is("Cybersecurity Seminar")),
                                hasProperty("categoryName", is("Technology")),
                                hasProperty("categoryColor", is("#5dade2")))))))
                .andExpect(model().attribute("events", hasProperty("content", hasItem(
                        allOf(
                                hasProperty("title", is("Basketball Tournament Finals")),
                                hasProperty("categoryName", is("Sports")),
                                hasProperty("categoryColor", is("#ff8c69")))))))
                .andExpect(model().attribute("events", hasProperty("content", hasItem(
                        allOf(
                                hasProperty("title", is("Student Jazz Performance")),
                                hasProperty("categoryName", is("Music")),
                                hasProperty("categoryColor", is("#e91e63")))))));
    }

    @Test
    void testMultipleEventCardsDisplayConsistently() throws Exception {
        // All event cards should have consistent structure and required fields
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("events",
                        hasProperty("content", hasSize(greaterThanOrEqualTo(5)))))
                .andExpect(model().attribute("events", hasProperty("content", everyItem(
                        allOf(
                                // Every card has title
                                hasProperty("title", notNullValue()),
                                hasProperty("title", not(emptyString())),
                                // Every card has date
                                hasProperty("eventDate", notNullValue()),
                                // Every card has time
                                hasProperty("eventTime", notNullValue()),
                                // Every card has location
                                hasProperty("location", notNullValue()),
                                hasProperty("location", not(emptyString())),
                                // Every card has brief description
                                hasProperty("briefDescription", notNullValue()),
                                // Every card has category information
                                hasProperty("categoryName", notNullValue()),
                                hasProperty("categoryName", not(emptyString())),
                                hasProperty("categoryColor", notNullValue()),
                                hasProperty("categoryColor", not(emptyString())),
                                // Every card has creator info
                                hasProperty("creatorUsername", notNullValue()),
                                hasProperty("creatorUsername", not(emptyString())))))));
    }

    @Test
    void testAnonymousVisitorCanAccessEventCards() throws Exception {
        // US-07 specifies "As a visitor" - anonymous users should see cards
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attributeExists("events"))
                .andExpect(model().attributeDoesNotExist("username")) // Confirms anonymous access
                .andExpect(model().attribute("events", hasProperty("content", hasItem(
                        hasProperty("title", is("Quick Python Workshop"))))))
                .andExpect(model().attribute("events", hasProperty("content", hasItem(
                        hasProperty("title", is("Basketball Tournament Finals"))))));
    }

    @Test
    void testEventCardsShowCorrectDateTimeFormatting() throws Exception {
        // Cards should display properly formatted dates and times
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("events", hasProperty("content", hasItem(
                        allOf(
                                hasProperty("title", is("Advanced Data Science Conference")),
                                hasProperty("eventDate", is(LocalDate.now().plusDays(5))),
                                hasProperty("eventTime", is(LocalTime.of(9, 30))),
                                hasProperty("location", is("RMIT Convention Centre, Level 3")))))))
                .andExpect(model().attribute("events", hasProperty("content", hasItem(
                        allOf(
                                hasProperty("title", is("Student Jazz Performance")),
                                hasProperty("eventDate", is(LocalDate.now().plusDays(12))),
                                hasProperty("eventTime", is(LocalTime.of(19, 30))),
                                hasProperty("location", is("Kaleide Theatre")))))));
    }

    @Test
    void testEventCardsDisplayCreatorInformation() throws Exception {
        // Cards should show who created the event
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("events", hasProperty("content", hasItem(
                        allOf(
                                hasProperty("title", is("Quick Python Workshop")),
                                hasProperty("creatorUsername", is("card.test.creator")))))))
                .andExpect(model().attribute("events", hasProperty("content", hasItem(
                        allOf(
                                hasProperty("title", is("Advanced Data Science Conference")),
                                hasProperty("creatorUsername", is("card.test.creator")))))))
                .andExpect(model().attribute("events", hasProperty("content", hasItem(
                        allOf(
                                hasProperty("title", is("Cybersecurity Seminar")),
                                hasProperty("creatorUsername", is("card.test.creator")))))));
    }

    @Test
    void testEventCardsShowAttendeeInformation() throws Exception {
        // Cards should display attendee count information for quick scanning
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("events", hasProperty("content", hasItem(
                        allOf(
                                hasProperty("title", is("Quick Python Workshop")),
                                hasProperty("maxAttendees", is(30)),
                                hasProperty("attendeeCount", greaterThanOrEqualTo(0)))))))
                .andExpect(model().attribute("events", hasProperty("content", hasItem(
                        allOf(
                                hasProperty("title", is("Advanced Data Science Conference")),
                                hasProperty("maxAttendees", is(150)),
                                hasProperty("attendeeCount", greaterThanOrEqualTo(0)))))));
    }

    @Test
    void testEventCardsHandleEventsWithoutDescriptions() throws Exception {
        // Create an event with minimal description to test edge case
        Event minimalEvent = new Event();
        minimalEvent.setTitle("Brief Event");
        minimalEvent.setDescription("Short");
        minimalEvent.setEventDate(LocalDate.now().plusDays(2));
        minimalEvent.setEventTime(LocalTime.of(16, 0));
        minimalEvent.setLocation("Room 1");
        minimalEvent.setCapacity(20);
        minimalEvent.setCategory(businessCategory);
        minimalEvent.setCreatedBy(testCreator);
        eventRepository.save(minimalEvent);

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("events", hasProperty("content", hasItem(
                        allOf(
                                hasProperty("title", is("Brief Event")),
                                hasProperty("briefDescription", is("Short")), // No truncation needed
                                hasProperty("description", is("Short")),
                                hasProperty("categoryName", is("Business")))))));
    }

    @Test
    void testEventCardsDisplayInChronologicalOrder() throws Exception {
        // Cards should be displayed in chronological order for easy scanning
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("events"));

        // The chronological ordering is tested more thoroughly in
        // ViewUpcomingEventsAcceptanceTest
        // Here we just verify that events are present and ordered data is available
        mockMvc.perform(get("/"))
                .andExpect(model().attribute("events", hasProperty("content", hasItem(
                        hasProperty("eventDate", is(LocalDate.now().plusDays(3))))))) // Earliest test event
                .andExpect(model().attribute("events", hasProperty("content", hasItem(
                        hasProperty("eventDate", is(LocalDate.now().plusDays(12))))))); // Latest test event
    }
}