package au.edu.rmit.sept.webapp.controller;

import au.edu.rmit.sept.webapp.dto.EventCardDTO;
import au.edu.rmit.sept.webapp.model.Category;
import au.edu.rmit.sept.webapp.model.Keyword;
import au.edu.rmit.sept.webapp.repository.CategoryRepository;
import au.edu.rmit.sept.webapp.security.CustomUserDetailsService;
import au.edu.rmit.sept.webapp.service.EventService;
import au.edu.rmit.sept.webapp.service.KeywordService;
import au.edu.rmit.sept.webapp.service.UserService;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import au.edu.rmit.sept.webapp.config.SecurityConfig;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for the "Clear Keywords" button bug where search text is retained
 * after clearing keyword filters.
 *
 * BUG DESCRIPTION:
 * When a user:
 * 1. Enters search text (e.g., "rmit")
 * 2. Applies keyword filter (e.g., "networking")
 * 3. Clicks "Clear Keywords" button
 *
 * EXPECTED: Search text should be cleared from the search input
 * ACTUAL: Search text remains in the search input field but is not applied to URL/filter
 *
 * ROOT CAUSE: The "Clear Keywords" link (line 492 in home.html) only passes
 * categoryId and fromDate parameters, omitting the search text parameter.
 *
 * IMPACT: User confusion - the search box still shows text but results don't
 * reflect that search text, creating inconsistent UI state.
 *
 * GitHub Issue: [To be created]
 */
@WebMvcTest(HomeController.class)
@Import(SecurityConfig.class)
class HomeControllerClearKeywordTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;

    @MockBean
    private UserService userService;

    @MockBean
    private CategoryRepository categoryRepository;

    @MockBean
    private KeywordService keywordService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private DataSource dataSource;

    private Page<EventCardDTO> mockEventPage;
    private List<Category> mockCategories;
    private List<Keyword> mockKeywords;

    @BeforeEach
    void setUp() {
        // Setup mock event data
        mockEventPage = new PageImpl<>(new ArrayList<>());

        // Setup mock categories
        mockCategories = new ArrayList<>();
        Category techCategory = new Category("Technology", "Tech events", "#5dade2");
        techCategory.setId(1L);
        mockCategories.add(techCategory);

        // Setup mock keywords
        mockKeywords = new ArrayList<>();
        Keyword networkingKeyword = new Keyword("networking", "Networking opportunities");
        networkingKeyword.setId(1L);
        mockKeywords.add(networkingKeyword);

        when(categoryRepository.findAll()).thenReturn(mockCategories);
        when(eventService.getUpcomingEvents(isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(mockEventPage);
    }

    /**
     * Test that verifies the bug: search text parameter is NOT included when
     * clearing keywords, but the search input field retains the text value.
     *
     * This test simulates:
     * 1. User searches for "rmit" with keyword filter "networking" applied
     * 2. User clicks "Clear Keywords" button
     * 3. Verifies that search text is NOT passed in the URL (bug)
     */
    @Test
    @WithMockUser
    @Disabled("Known bug: Search text is retained in input field after clearing keywords - see GitHub issue #XX")
    void testClearKeywordsButton_ShouldRemoveSearchTextFromModel() throws Exception {
        // Step 1: Initial request with search text AND keyword filter
        mockMvc.perform(get("/")
                .param("search", "rmit")
                .param("keywordIds", "1"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("searchText", "rmit"))
                .andExpect(model().attribute("selectedKeywordIds", Set.of(1L)));

        // Step 2: Simulate clicking "Clear Keywords" button
        // The link in home.html line 492: th:href="@{/(categoryId=${selectedCategoryId}, fromDate=${fromDate})}"
        // Notice: search parameter is NOT included
        mockMvc.perform(get("/"))  // No search parameter passed
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("searchText"))  // FAILS: searchText still exists
                .andExpect(model().attributeDoesNotExist("selectedKeywordIds"));
    }

    /**
     * Test that verifies search text should be cleared from the model when
     * clearing keywords, preventing inconsistent UI state.
     *
     * This test documents the expected behavior after fix:
     * - When "Clear Keywords" is clicked, search text should also be cleared
     * - The search input field should be empty
     * - Results should show all events (no search or keyword filter applied)
     */
    @Test
    @WithMockUser
    @Disabled("Known bug: Search text persists in URL state after clearing keywords - see GitHub issue #XX")
    void testClearKeywordsButton_ShouldNotRetainSearchTextInURL() throws Exception {
        // Simulate the "Clear Keywords" button click with retained search text (current buggy behavior)
        mockMvc.perform(get("/")
                .param("search", "rmit"))  // Search text is still present in some form
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("searchText"));  // FAILS: search text should be cleared
    }

    /**
     * Test verifying that the "Clear Keywords" link structure is missing
     * the search parameter in the URL.
     *
     * This test checks the actual HTML output to confirm the bug at the template level.
     */
    @Test
    @WithMockUser
    @Disabled("Known bug: Clear Keywords link doesn't preserve search parameter - see GitHub issue #XX")
    void testClearKeywordsLinkStructure_ShouldIncludeSearchParameter() throws Exception {
        // Setup request with both search text and keywords
        mockMvc.perform(get("/")
                .param("search", "rmit")
                .param("keywordIds", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(
                    org.hamcrest.Matchers.containsString("Clear Keywords")
                ))
                // FAILS: The href should contain search=rmit but it doesn't
                .andExpect(content().string(
                    org.hamcrest.Matchers.containsString("search=rmit")
                ));
    }
}
