package au.edu.rmit.sept.webapp.controller;

import au.edu.rmit.sept.webapp.dto.EventCreateDTO;
import au.edu.rmit.sept.webapp.dto.EventDetailsDTO;
import au.edu.rmit.sept.webapp.model.Category;
import au.edu.rmit.sept.webapp.model.Event;
import au.edu.rmit.sept.webapp.model.User;
import au.edu.rmit.sept.webapp.repository.CategoryRepository;
import au.edu.rmit.sept.webapp.repository.EventRepository;
import au.edu.rmit.sept.webapp.repository.RSVPRepository;
import au.edu.rmit.sept.webapp.repository.UserRepository;
import au.edu.rmit.sept.webapp.service.EventService;
import au.edu.rmit.sept.webapp.service.KeywordService;
import au.edu.rmit.sept.webapp.service.ReviewService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.persistence.EntityNotFoundException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventController.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private CategoryRepository categoryRepository;

    @MockBean
    private KeywordService keywordService;

    @MockBean
    private EventRepository eventRepository;

    @MockBean
    private ReviewService reviewService;

    @MockBean
    private RSVPRepository rsvpRepository;

    private User testUser;
    private Event testEvent;
    private Category testCategory;
    private EventDetailsDTO testEventDetailsDTO;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        testCategory = new Category("Technology", "Tech events", "#5dade2");
        testCategory.setId(1L);

        testEvent = new Event();
        testEvent.setId(1L);
        testEvent.setTitle("Test Event");
        testEvent.setDescription("Test Description");
        testEvent.setEventDate(LocalDate.now().plusDays(7));
        testEvent.setEventTime(LocalTime.of(14, 0));
        testEvent.setLocation("Test Location");
        testEvent.setCapacity(50);
        testEvent.setCategory(testCategory);
        testEvent.setCreatedBy(testUser);
        testEvent.setDeactivated(false);

        testEventDetailsDTO = new EventDetailsDTO();
        testEventDetailsDTO.setEventId(1L);
        testEventDetailsDTO.setTitle("Test Event");
        testEventDetailsDTO.setEventDate(LocalDate.now().plusDays(7));
        testEventDetailsDTO.setEventTime(LocalTime.of(14, 0));
    }

    // ============== GET /events/{id} ==============

    @Test
    @WithMockUser(username = "testuser")
    void showEventDetails_Success_EventFound() throws Exception {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(eventService.getEventById(eq(1L), any())).thenReturn(testEventDetailsDTO);
        when(reviewService.getRecentReviewsForEvent(1L)).thenReturn(Arrays.asList());
        when(reviewService.getAverageRatingForEvent(1L)).thenReturn(0.0);
        when(reviewService.countReviewsForEvent(1L)).thenReturn(0L);
        when(rsvpRepository.existsByUser_UsernameAndEvent_Id("testuser", 1L)).thenReturn(false);

        mockMvc.perform(get("/events/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("events/event-details"))
                .andExpect(model().attributeExists("event"));

        verify(eventService).getEventById(eq(1L), any());
    }

    @Test
    @WithMockUser(username = "testuser")
    void showEventDetails_EventNotFound_ThrowsException() throws Exception {
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/events/999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void showEventDetails_DeactivatedEvent_NonAdmin_ThrowsException() throws Exception {
        testEvent.setDeactivated(true);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));

        mockMvc.perform(get("/events/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));
    }

    @Test
    @WithMockUser(username = "adminuser", roles = "ADMIN")
    void showEventDetails_DeactivatedEvent_Admin_ShowsEvent() throws Exception {
        testEvent.setDeactivated(true);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(eventService.getEventById(eq(1L), any())).thenReturn(testEventDetailsDTO);
        when(reviewService.getRecentReviewsForEvent(1L)).thenReturn(Arrays.asList());
        when(reviewService.getAverageRatingForEvent(1L)).thenReturn(0.0);
        when(reviewService.countReviewsForEvent(1L)).thenReturn(0L);

        mockMvc.perform(get("/events/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("events/event-details"));
    }

    @Test
    @Disabled("Bug: QR codes shown to deactivated users")
    @WithMockUser(username = "deactivateduser")
    void showEventDetails_DeactivatedUser_QRCodeSectionsNotVisible() throws Exception {
        // Setup deactivated user
        User deactivatedUser = new User();
        deactivatedUser.setId(2L);
        deactivatedUser.setUsername("deactivateduser");
        deactivatedUser.setEmail("deactivated@example.com");
        deactivatedUser.setDeactivated(true);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(userRepository.findByUsername("deactivateduser")).thenReturn(Optional.of(deactivatedUser));
        when(eventService.getEventById(eq(1L), any())).thenReturn(testEventDetailsDTO);
        when(reviewService.getRecentReviewsForEvent(1L)).thenReturn(Arrays.asList());
        when(reviewService.getAverageRatingForEvent(1L)).thenReturn(0.0);
        when(reviewService.countReviewsForEvent(1L)).thenReturn(0L);
        when(rsvpRepository.existsByUser_UsernameAndEvent_Id("deactivateduser", 1L)).thenReturn(false);

        // This test will FAIL because QR code sections are currently shown to deactivated users
        // Expected behavior: QR code sections should be hidden for deactivated users
        mockMvc.perform(get("/events/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("events/event-details"))
                .andExpect(model().attributeExists("event"))
                .andExpect(model().attribute("ticketQrCodeUrl", (Object) null))  // QR should not be generated
                .andExpect(model().attribute("eventQrCodeUrl", (Object) null));  // QR should not be generated

        verify(eventService).getEventById(eq(1L), any());
    }

    // ============== GET /events/create ==============

    @Test
    @WithMockUser
    void showCreateForm_AuthenticatedUser_ShowsForm() throws Exception {
        when(categoryRepository.findAll()).thenReturn(Arrays.asList(testCategory));
        when(keywordService.getAllKeywords()).thenReturn(Arrays.asList());

        mockMvc.perform(get("/events/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("events/create-event"))
                .andExpect(model().attributeExists("eventCreateDTO"))
                .andExpect(model().attributeExists("categories"))
                .andExpect(model().attributeExists("keywords"));
    }

    @Test
    void showCreateForm_AnonymousUser_Returns401() throws Exception {
        mockMvc.perform(get("/events/create"))
                .andExpect(status().isUnauthorized());
    }

    // ============== POST /events/create ==============

    @Test
    @WithMockUser(username = "testuser")
    void createEvent_Success_ValidData() throws Exception {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(eventService.createEvent(any(EventCreateDTO.class), eq(testUser))).thenReturn(1L);

        mockMvc.perform(post("/events/create")
                .with(csrf())
                .param("title", "New Event")
                .param("description", "Event Description with at least 10 chars")
                .param("eventDate", LocalDate.now().plusDays(7).toString())
                .param("eventTime", "14:00")
                .param("location", "Event Location")
                .param("capacity", "50")
                .param("categoryId", "1")
                .param("unlimitedCapacity", "false"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/events/1"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(eventService).createEvent(any(EventCreateDTO.class), eq(testUser));
    }

    @Test
    @WithMockUser(username = "testuser")
    void createEvent_ValidationError_ReturnsFormWithErrors() throws Exception {
        when(categoryRepository.findAll()).thenReturn(Arrays.asList(testCategory));
        when(keywordService.getAllKeywords()).thenReturn(Arrays.asList());

        mockMvc.perform(post("/events/create")
                .with(csrf())
                .param("title", "")  // Empty title - validation error
                .param("description", "Short")  // Too short - validation error
                .param("eventDate", LocalDate.now().plusDays(7).toString())
                .param("eventTime", "14:00")
                .param("location", "Location")
                .param("categoryId", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("events/create-event"))
                .andExpect(model().attributeHasFieldErrors("eventCreateDTO", "title", "description"));

        verify(eventService, never()).createEvent(any(), any());
    }

    @Test
    void createEvent_AnonymousUser_Returns401() throws Exception {
        mockMvc.perform(post("/events/create")
                .with(csrf())
                .param("title", "New Event"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "testuser")
    void createEvent_ServiceError_ReturnsFormWithError() throws Exception {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(eventService.createEvent(any(EventCreateDTO.class), eq(testUser)))
                .thenThrow(new IllegalArgumentException("Event date must be in the future"));
        when(categoryRepository.findAll()).thenReturn(Arrays.asList(testCategory));
        when(keywordService.getAllKeywords()).thenReturn(Arrays.asList());

        mockMvc.perform(post("/events/create")
                .with(csrf())
                .param("title", "New Event")
                .param("description", "Event Description with at least 10 chars")
                .param("eventDate", LocalDate.now().plusDays(7).toString())
                .param("eventTime", "14:00")
                .param("location", "Event Location")
                .param("capacity", "50")
                .param("categoryId", "1")
                .param("unlimitedCapacity", "false"))
                .andExpect(status().isOk())
                .andExpect(view().name("events/create-event"))
                .andExpect(model().attributeExists("errorMessage"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void createEvent_UserNotFound_ReturnsFormWithError() throws Exception {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());
        when(categoryRepository.findAll()).thenReturn(Arrays.asList(testCategory));
        when(keywordService.getAllKeywords()).thenReturn(Arrays.asList());

        mockMvc.perform(post("/events/create")
                .with(csrf())
                .param("title", "New Event")
                .param("description", "Event Description with at least 10 chars")
                .param("eventDate", LocalDate.now().plusDays(7).toString())
                .param("eventTime", "14:00")
                .param("location", "Event Location")
                .param("capacity", "50")
                .param("categoryId", "1")
                .param("unlimitedCapacity", "false"))
                .andExpect(status().isOk())
                .andExpect(view().name("events/create-event"))
                .andExpect(model().attributeExists("errorMessage"));
    }

    // ============== POST /events/{id}/delete ==============

    @Test
    @WithMockUser(username = "testuser")
    void deleteEvent_Success_CreatorDeletes() throws Exception {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        doNothing().when(eventService).deleteEvent(1L, 1L);

        mockMvc.perform(post("/events/1/delete")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(eventService).deleteEvent(1L, 1L);
    }

    @Test
    void deleteEvent_AnonymousUser_Returns401() throws Exception {
        mockMvc.perform(post("/events/1/delete")
                .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "testuser")
    void deleteEvent_EventNotFound_RedirectsWithError() throws Exception {
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/events/999/delete")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void deleteEvent_AccessDenied_NonCreator() throws Exception {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        doThrow(new org.springframework.security.access.AccessDeniedException("Access denied"))
                .when(eventService).deleteEvent(1L, 1L);

        mockMvc.perform(post("/events/1/delete")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/events/1"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    @WithMockUser
    void deleteEvent_DeactivatedEvent_NonAdmin_ThrowsException() throws Exception {
        testEvent.setDeactivated(true);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));

        mockMvc.perform(post("/events/1/delete")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void deleteEvent_UnexpectedError_RedirectsWithError() throws Exception {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        doThrow(new RuntimeException("Unexpected error"))
                .when(eventService).deleteEvent(1L, 1L);

        mockMvc.perform(post("/events/1/delete")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/events/1"))
                .andExpect(flash().attributeExists("errorMessage"));
    }
}
