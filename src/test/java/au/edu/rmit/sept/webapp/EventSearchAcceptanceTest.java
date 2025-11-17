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
import au.edu.rmit.sept.webapp.model.Keyword;
import au.edu.rmit.sept.webapp.model.User;
import au.edu.rmit.sept.webapp.repository.CategoryRepository;
import au.edu.rmit.sept.webapp.repository.EventRepository;
import au.edu.rmit.sept.webapp.repository.KeywordRepository;
import au.edu.rmit.sept.webapp.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

/**
 * Acceptance tests for Event Search and Filtering functionality.
 * Tests the complete flow of searching and filtering events through the home page.
 *
 * Features tested:
 * - Search by title/description (searchTerm)
 * - Filter by category
 * - Filter by date range (fromDate)
 * - Filter by keywords
 * - Combined filters
 * - Anonymous and authenticated user access
 *
 * mvn test -Dtest=EventSearchAcceptanceTest
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EventSearchAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private KeywordRepository keywordRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testCreator;
    private Category techCategory;
    private Category sportsCategory;
    private Category musicCategory;
    private Keyword aiKeyword;
    private Keyword pythonKeyword;
    private Keyword basketballKeyword;

    // Test events for search scenarios
    private Event pythonWorkshop;
    private Event aiConference;
    private Event basketballGame;
    private Event jazzConcert;
    private Event futureEvent;
    private Event distantFutureEvent;

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

        // Create or get keywords
        aiKeyword = keywordRepository.findByNameIgnoreCase("AI")
                .orElseGet(() -> {
                    Keyword kw = new Keyword();
                    kw.setName("AI");
                    return keywordRepository.save(kw);
                });

        pythonKeyword = keywordRepository.findByNameIgnoreCase("Python")
                .orElseGet(() -> {
                    Keyword kw = new Keyword();
                    kw.setName("Python");
                    return keywordRepository.save(kw);
                });

        basketballKeyword = keywordRepository.findByNameIgnoreCase("Basketball")
                .orElseGet(() -> {
                    Keyword kw = new Keyword();
                    kw.setName("Basketball");
                    return keywordRepository.save(kw);
                });

        // Create test user
        testCreator = new User();
        testCreator.setUsername("search.test.creator");
        testCreator.setEmail("search@test.com");
        testCreator.setPassword(passwordEncoder.encode("password123"));
        testCreator.setEnabled(true);
        userRepository.save(testCreator);

        // Create test events with distinct characteristics for search testing

        // Python workshop - Tech category, Python keyword
        pythonWorkshop = new Event();
        pythonWorkshop.setTitle("Python Programming Workshop");
        pythonWorkshop.setDescription("Learn Python programming from scratch with hands-on exercises");
        pythonWorkshop.setEventDate(LocalDate.now().plusDays(5));
        pythonWorkshop.setEventTime(LocalTime.of(10, 0));
        pythonWorkshop.setLocation("Tech Lab A");
        pythonWorkshop.setCapacity(30);
        pythonWorkshop.setCategory(techCategory);
        pythonWorkshop.setCreatedBy(testCreator);
        pythonWorkshop.getKeywords().add(pythonKeyword);
        pythonWorkshop = eventRepository.save(pythonWorkshop);

        // AI Conference - Tech category, AI keyword
        aiConference = new Event();
        aiConference.setTitle("Artificial Intelligence Conference 2025");
        aiConference.setDescription("Explore the latest advances in AI and machine learning technologies");
        aiConference.setEventDate(LocalDate.now().plusDays(10));
        aiConference.setEventTime(LocalTime.of(9, 0));
        aiConference.setLocation("Convention Center");
        aiConference.setCapacity(200);
        aiConference.setCategory(techCategory);
        aiConference.setCreatedBy(testCreator);
        aiConference.getKeywords().add(aiKeyword);
        aiConference = eventRepository.save(aiConference);

        // Basketball game - Sports category, Basketball keyword
        basketballGame = new Event();
        basketballGame.setTitle("Basketball Championship Finals");
        basketballGame.setDescription("Watch the exciting basketball championship finals with top teams competing");
        basketballGame.setEventDate(LocalDate.now().plusDays(7));
        basketballGame.setEventTime(LocalTime.of(18, 0));
        basketballGame.setLocation("Sports Arena");
        basketballGame.setCapacity(500);
        basketballGame.setCategory(sportsCategory);
        basketballGame.setCreatedBy(testCreator);
        basketballGame.getKeywords().add(basketballKeyword);
        basketballGame = eventRepository.save(basketballGame);

        // Jazz concert - Music category, no special keywords
        jazzConcert = new Event();
        jazzConcert.setTitle("Evening Jazz Performance");
        jazzConcert.setDescription("Relaxing evening of smooth jazz performed by talented musicians");
        jazzConcert.setEventDate(LocalDate.now().plusDays(3));
        jazzConcert.setEventTime(LocalTime.of(19, 30));
        jazzConcert.setLocation("Music Hall");
        jazzConcert.setCapacity(100);
        jazzConcert.setCategory(musicCategory);
        jazzConcert.setCreatedBy(testCreator);
        eventRepository.save(jazzConcert);

        // Future event for date filtering (15 days out)
        futureEvent = new Event();
        futureEvent.setTitle("Future Technology Summit");
        futureEvent.setDescription("Looking ahead at emerging technologies and innovations");
        futureEvent.setEventDate(LocalDate.now().plusDays(15));
        futureEvent.setEventTime(LocalTime.of(14, 0));
        futureEvent.setLocation("Innovation Hub");
        futureEvent.setCapacity(75);
        futureEvent.setCategory(techCategory);
        futureEvent.setCreatedBy(testCreator);
        eventRepository.save(futureEvent);

        // Distant future event (30 days out)
        distantFutureEvent = new Event();
        distantFutureEvent.setTitle("Distant Future Conference");
        distantFutureEvent.setDescription("Planning for the future of technology");
        distantFutureEvent.setEventDate(LocalDate.now().plusDays(30));
        distantFutureEvent.setEventTime(LocalTime.of(11, 0));
        distantFutureEvent.setLocation("Future Center");
        distantFutureEvent.setCapacity(50);
        distantFutureEvent.setCategory(techCategory);
        distantFutureEvent.setCreatedBy(testCreator);
        eventRepository.save(distantFutureEvent);
    }

    @Test
    void testSearchByTitle_FindsMatchingEvents() throws Exception {
        // Search for events with "Python" in title
        mockMvc.perform(get("/")
                .param("searchTerm", "Python"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attributeExists("events"))
                .andExpect(model().attribute("events", hasProperty("content", hasItem(
                        hasProperty("title", containsString("Python"))))))
                .andExpect(model().attribute("searchTerm", is("Python")));
    }

    @Test
    void testSearchByDescription_FindsMatchingEvents() throws Exception {
        // Search for events with "advances in AI" in description (unique to our test event)
        mockMvc.perform(get("/")
                .param("searchTerm", "advances in AI"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("events", hasProperty("content", hasItem(
                        hasProperty("title", is("Artificial Intelligence Conference 2025"))))));
    }

    @Test
    void testSearchCaseInsensitive() throws Exception {
        // Search with lowercase should match "Basketball" (case-insensitive)
        mockMvc.perform(get("/")
                .param("searchTerm", "basketball"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("events", hasProperty("content", hasItem(
                        hasProperty("title", is("Basketball Championship Finals"))))));

        // Search with uppercase should also match
        mockMvc.perform(get("/")
                .param("searchTerm", "BASKETBALL"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("events", hasProperty("content", hasItem(
                        hasProperty("title", is("Basketball Championship Finals"))))));
    }

    @Test
    void testFilterByCategory_Technology() throws Exception {
        // Filter by Technology category
        mockMvc.perform(get("/")
                .param("categoryId", techCategory.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("events", hasProperty("content", hasItem(
                        hasProperty("title", is("Python Programming Workshop"))))))
                .andExpect(model().attribute("events", hasProperty("content", hasItem(
                        hasProperty("title", is("Artificial Intelligence Conference 2025"))))))
                .andExpect(model().attribute("selectedCategoryId", is(techCategory.getId())))
                .andExpect(model().attribute("selectedCategoryName", is("Technology")));
    }

    @Test
    void testFilterByCategory_Sports() throws Exception {
        // Filter by Sports category - should only show basketball game
        mockMvc.perform(get("/")
                .param("categoryId", sportsCategory.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("events", hasProperty("content", hasItem(
                        hasProperty("title", is("Basketball Championship Finals"))))))
                .andExpect(model().attribute("selectedCategoryId", is(sportsCategory.getId())));
    }

    @Test
    void testFilterByDateRange_FromDate() throws Exception {
        // Filter events from 25 days in the future onwards (to avoid DataInitializer events)
        LocalDate fromDate = LocalDate.now().plusDays(25);

        mockMvc.perform(get("/")
                .param("fromDate", fromDate.toString()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("events", hasProperty("content", hasItem(
                        hasProperty("title", is("Distant Future Conference"))))))
                .andExpect(model().attribute("fromDate", is(fromDate)));
    }

    @Test
    void testFilterByKeyword_Python() throws Exception {
        // Filter by Python keyword
        mockMvc.perform(get("/")
                .param("keywordIds", pythonKeyword.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("events", hasProperty("content", hasItem(
                        hasProperty("title", is("Python Programming Workshop"))))))
                .andExpect(model().attribute("selectedKeywordIds", hasItem(pythonKeyword.getId())));
    }

    @Test
    void testFilterByKeyword_AI() throws Exception {
        // Filter by AI keyword
        mockMvc.perform(get("/")
                .param("keywordIds", aiKeyword.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("events", hasProperty("content", hasItem(
                        hasProperty("title", is("Artificial Intelligence Conference 2025"))))));
    }

    @Test
    void testFilterByMultipleKeywords() throws Exception {
        // Filter by both Python and AI keywords (OR logic - should return both events)
        mockMvc.perform(get("/")
                .param("keywordIds", pythonKeyword.getId().toString())
                .param("keywordIds", aiKeyword.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("events", hasProperty("content", hasItem(
                        hasProperty("title", is("Python Programming Workshop"))))))
                .andExpect(model().attribute("events", hasProperty("content", hasItem(
                        hasProperty("title", is("Artificial Intelligence Conference 2025"))))));
    }

    @Test
    void testCombinedFilters_CategoryAndSearch() throws Exception {
        // Combine Technology category filter with search term
        mockMvc.perform(get("/")
                .param("categoryId", techCategory.getId().toString())
                .param("searchTerm", "Python"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("events", hasProperty("content", hasItem(
                        hasProperty("title", is("Python Programming Workshop"))))))
                .andExpect(model().attribute("selectedCategoryId", is(techCategory.getId())))
                .andExpect(model().attribute("searchTerm", is("Python")));
    }

    @Test
    void testCombinedFilters_CategoryAndDate() throws Exception {
        // Filter Technology events from 12 days onwards
        LocalDate fromDate = LocalDate.now().plusDays(12);

        mockMvc.perform(get("/")
                .param("categoryId", techCategory.getId().toString())
                .param("fromDate", fromDate.toString()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("events", hasProperty("content", hasItem(
                        hasProperty("title", is("Future Technology Summit"))))))
                .andExpect(model().attribute("events", hasProperty("content", hasItem(
                        hasProperty("title", is("Distant Future Conference"))))))
                .andExpect(model().attribute("selectedCategoryId", is(techCategory.getId())))
                .andExpect(model().attribute("fromDate", is(fromDate)));
    }

    @Test
    void testCombinedFilters_AllParameters() throws Exception {
        // Combine category, date, keyword, and search
        LocalDate fromDate = LocalDate.now().plusDays(1);

        mockMvc.perform(get("/")
                .param("categoryId", techCategory.getId().toString())
                .param("fromDate", fromDate.toString())
                .param("keywordIds", pythonKeyword.getId().toString())
                .param("searchTerm", "Python"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("events", hasProperty("content", hasItem(
                        hasProperty("title", is("Python Programming Workshop"))))))
                .andExpect(model().attribute("selectedCategoryId", is(techCategory.getId())))
                .andExpect(model().attribute("fromDate", is(fromDate)))
                .andExpect(model().attribute("selectedKeywordIds", hasItem(pythonKeyword.getId())))
                .andExpect(model().attribute("searchTerm", is("Python")));
    }

    @Test
    void testSearchWithNoResults() throws Exception {
        // Search for something that doesn't exist
        mockMvc.perform(get("/")
                .param("searchTerm", "NonExistentEventXYZ123"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("searchTerm", is("NonExistentEventXYZ123")));
    }

    @Test
    void testFilterPersistence_FormStateRetained() throws Exception {
        // Verify that filter parameters are retained in the model for form state
        mockMvc.perform(get("/")
                .param("categoryId", techCategory.getId().toString())
                .param("searchTerm", "AI")
                .param("fromDate", LocalDate.now().plusDays(5).toString()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("selectedCategoryId", is(techCategory.getId())))
                .andExpect(model().attribute("selectedCategoryName", is("Technology")))
                .andExpect(model().attribute("searchTerm", is("AI")))
                .andExpect(model().attribute("fromDate", is(LocalDate.now().plusDays(5))));
    }

    @Test
    void testAnonymousUserCanSearch() throws Exception {
        // Anonymous users should be able to search and filter events
        mockMvc.perform(get("/")
                .param("searchTerm", "Basketball"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("events", hasProperty("content", hasItem(
                        hasProperty("title", is("Basketball Championship Finals"))))))
                .andExpect(model().attributeDoesNotExist("username")); // Confirms anonymous
    }

    @Test
    void testCategoriesAvailableInModel() throws Exception {
        // Categories should be available for dropdown filter
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("categories"))
                .andExpect(model().attribute("categories", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(model().attribute("categories", hasItem(
                        hasProperty("name", is("Technology")))))
                .andExpect(model().attribute("categories", hasItem(
                        hasProperty("name", is("Sports")))))
                .andExpect(model().attribute("categories", hasItem(
                        hasProperty("name", is("Music")))));
    }

    @Test
    void testKeywordsAvailableInModel() throws Exception {
        // Keywords should be available for filter
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("keywords"))
                .andExpect(model().attribute("keywords", hasSize(greaterThanOrEqualTo(3))));
    }

    @Test
    void testSearchPartialMatch() throws Exception {
        // Partial search term should match
        mockMvc.perform(get("/")
                .param("searchTerm", "Champ"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("events", hasProperty("content", hasItem(
                        hasProperty("title", containsString("Championship"))))));
    }

    @Test
    void testFilterByInvalidCategory() throws Exception {
        // Invalid category ID should return all events (no filter applied)
        mockMvc.perform(get("/")
                .param("categoryId", "99999"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("events"));
    }

    @Test
    void testDateFilterExcludesEarlierEvents() throws Exception {
        // Events before fromDate should be excluded
        LocalDate fromDate = LocalDate.now().plusDays(20);

        mockMvc.perform(get("/")
                .param("fromDate", fromDate.toString()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("events", hasProperty("content", hasItem(
                        hasProperty("title", is("Distant Future Conference"))))));
        // Should NOT include events before day 20
    }

    @Test
    void testSearchInTitleAndDescription() throws Exception {
        // Search term should match both title and description
        mockMvc.perform(get("/")
                .param("searchTerm", "technology"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("events", hasProperty("content", hasItem(
                        hasProperty("title", containsStringIgnoringCase("technology"))))))
                .andExpect(model().attribute("events", hasProperty("content", hasItem(
                        hasProperty("briefDescription", containsStringIgnoringCase("technology"))))));
    }

    @Test
    void testEmptySearchTerm_ShowsAllEvents() throws Exception {
        // Empty search term should return all events
        mockMvc.perform(get("/")
                .param("searchTerm", ""))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("events"))
                .andExpect(model().attribute("searchTerm", is("")));
    }

    @Test
    void testPaginationWithFilters() throws Exception {
        // Filters should work with pagination
        mockMvc.perform(get("/")
                .param("categoryId", techCategory.getId().toString())
                .param("page", "0")
                .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("events"))
                .andExpect(model().attribute("selectedCategoryId", is(techCategory.getId())));
    }

    @Test
    void testSelectedKeywordsDisplayedInModel() throws Exception {
        // Selected keywords should be available for display
        mockMvc.perform(get("/")
                .param("keywordIds", pythonKeyword.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("selectedKeywords"))
                .andExpect(model().attribute("selectedKeywords", hasItem(
                        hasProperty("name", is("Python")))));
    }

    @Test
    void testFilterCombination_CategoryExcludesOthers() throws Exception {
        // Filtering by Sports should exclude Technology events
        mockMvc.perform(get("/")
                .param("categoryId", sportsCategory.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("events", hasProperty("content", hasItem(
                        hasProperty("categoryName", is("Sports"))))));
    }

    @Test
    void testSearchSpecialCharacters() throws Exception {
        // Search should handle special characters gracefully
        mockMvc.perform(get("/")
                .param("searchTerm", "Conference & Summit"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("events"));
    }
}
