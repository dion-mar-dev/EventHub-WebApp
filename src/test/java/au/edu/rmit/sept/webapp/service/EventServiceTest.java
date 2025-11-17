package au.edu.rmit.sept.webapp.service;

import au.edu.rmit.sept.webapp.dto.EventCardDTO;
import au.edu.rmit.sept.webapp.dto.EventCreateDTO;
import au.edu.rmit.sept.webapp.dto.EventDetailsDTO;
import au.edu.rmit.sept.webapp.dto.CancelledRSVPDTO;
import au.edu.rmit.sept.webapp.model.Category;
import au.edu.rmit.sept.webapp.model.Event;
import au.edu.rmit.sept.webapp.model.Keyword;
import au.edu.rmit.sept.webapp.model.RSVP;
import au.edu.rmit.sept.webapp.model.User;
import au.edu.rmit.sept.webapp.model.CancelledRSVP;
import au.edu.rmit.sept.webapp.repository.CategoryRepository;
import au.edu.rmit.sept.webapp.repository.EventRepository;
import au.edu.rmit.sept.webapp.repository.RSVPRepository;
import au.edu.rmit.sept.webapp.repository.UserRepository;
import au.edu.rmit.sept.webapp.repository.BlockedRSVPRepository;
import au.edu.rmit.sept.webapp.repository.CancelledRSVPRepository;
import au.edu.rmit.sept.webapp.repository.PaymentRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import com.stripe.exception.StripeException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private RSVPRepository rsvpRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private KeywordService keywordService;

    @Mock
    private UserService userService;

    @Mock
    private BlockedRSVPRepository blockedRSVPRepository;

    @Mock
    private CancelledRSVPRepository cancelledRSVPRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RSVPService rsvpService;

    @Mock
    private StripeService stripeService;

    @InjectMocks
    private EventService eventService;

    private User testUser;
    private User testCreator;
    private Category techCategory;
    private Event futureEvent;
    private Event pastEvent;
    private EventCreateDTO validCreateDTO;

    @BeforeEach
    void setUp() {
        // Create test users
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("john.doe");
        testUser.setEmail("john@example.com");

        testCreator = new User();
        testCreator.setId(2L);
        testCreator.setUsername("jane.smith");
        testCreator.setEmail("jane@example.com");

        // Create test category
        techCategory = new Category("Technology", "Tech events", "#5dade2");
        techCategory.setId(1L);

        // Create future event (7 days from now)
        futureEvent = new Event();
        futureEvent.setId(1L);
        futureEvent.setTitle("Spring Boot Workshop");
        futureEvent.setDescription("Learn Spring Boot fundamentals and best practices for building modern web applications");
        futureEvent.setEventDate(LocalDate.now().plusDays(7));
        futureEvent.setEventTime(LocalTime.of(14, 0));
        futureEvent.setLocation("Building 14, Room 12");
        futureEvent.setCapacity(30);
        futureEvent.setCategory(techCategory);
        futureEvent.setCreatedBy(testCreator);
        futureEvent.setCreatedAt(LocalDateTime.now().minusDays(1));

        // Create past event (7 days ago)
        pastEvent = new Event();
        pastEvent.setId(2L);
        pastEvent.setTitle("Past Event");
        pastEvent.setDescription("This event has already happened");
        pastEvent.setEventDate(LocalDate.now().minusDays(7));
        pastEvent.setEventTime(LocalTime.of(10, 0));
        pastEvent.setLocation("Old Building");
        pastEvent.setCapacity(50);
        pastEvent.setCategory(techCategory);
        pastEvent.setCreatedBy(testCreator);

        // Create valid EventCreateDTO
        validCreateDTO = new EventCreateDTO();
        validCreateDTO.setTitle("New Tech Event");
        validCreateDTO.setDescription("A comprehensive workshop on new technologies");
        validCreateDTO.setEventDate(LocalDate.now().plusDays(14));
        validCreateDTO.setEventTime(LocalTime.of(15, 30));
        validCreateDTO.setLocation("Innovation Hub");
        validCreateDTO.setCapacity(40);
        validCreateDTO.setCategoryId(1L);
        validCreateDTO.setUnlimitedCapacity(false);
    }

    // ============== getUpcomingEvents Tests ==============

    @Test
    void getUpcomingEvents_ReturnsEmptyList_WhenNoEventsExist() {
        // Arrange
        Page<Event> emptyPage = new PageImpl<>(Collections.emptyList());
        when(eventRepository.findUpcomingEvents(any(LocalDate.class), any(LocalTime.class), any(Pageable.class)))
                .thenReturn(emptyPage);

        // Act
        List<EventCardDTO> result = eventService.getUpcomingEvents(null, null, null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(eventRepository).findUpcomingEvents(any(LocalDate.class), any(LocalTime.class), any(Pageable.class));
    }

    @Test
    void getUpcomingEvents_ReturnsEvents_ForAnonymousUser() {
        // Arrange
        List<Event> events = Arrays.asList(futureEvent);
        Page<Event> eventPage = new PageImpl<>(events);
        when(eventRepository.findUpcomingEvents(any(LocalDate.class), any(LocalTime.class), any(Pageable.class)))
                .thenReturn(eventPage);
        when(rsvpRepository.countByEvent(futureEvent)).thenReturn(15L);

        // Act
        List<EventCardDTO> result = eventService.getUpcomingEvents(null, null, null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        
        EventCardDTO dto = result.get(0);
        assertEquals("Spring Boot Workshop", dto.getTitle());
        assertEquals(15, dto.getAttendeeCount());
        assertEquals(30, dto.getMaxAttendees());
        assertFalse(dto.isUserRsvpStatus()); // Anonymous user not RSVP'd
        assertFalse(dto.isEventStarted());
        assertFalse(dto.isEventFull());
    }

    @Test
    void getUpcomingEvents_ReturnsEventsWithRSVPStatus_ForAuthenticatedUser() {
        // Arrange
        List<Event> events = Arrays.asList(futureEvent);
        Page<Event> eventPage = new PageImpl<>(events);
        when(eventRepository.findUpcomingEvents(any(LocalDate.class), any(LocalTime.class), any(Pageable.class)))
                .thenReturn(eventPage);
        when(rsvpRepository.countByEvent(futureEvent)).thenReturn(15L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(rsvpRepository.existsByUser_UsernameAndEvent_Id("john.doe", 1L)).thenReturn(true);

        // Act
        List<EventCardDTO> result = eventService.getUpcomingEvents(1L, null, null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        
        EventCardDTO dto = result.get(0);
        assertTrue(dto.isUserRsvpStatus()); // User has RSVP'd
        assertEquals("jane.smith", dto.getCreatorUsername());
    }

    @Test
    void getUpcomingEvents_FiltersByCategory_WhenCategoryIdProvided() {
        // Arrange
        List<Event> events = Arrays.asList(futureEvent);
        Page<Event> eventPage = new PageImpl<>(events);
        when(eventRepository.findUpcomingEventsByCategory(eq(1L), any(LocalDate.class), any(LocalTime.class), any(Pageable.class)))
                .thenReturn(eventPage);
        when(rsvpRepository.countByEvent(futureEvent)).thenReturn(10L);

        // Act
        List<EventCardDTO> result = eventService.getUpcomingEvents(null, 1L, null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(eventRepository).findUpcomingEventsByCategory(eq(1L), any(LocalDate.class), any(LocalTime.class), any(Pageable.class));
        verify(eventRepository, never()).findUpcomingEvents(any(LocalDate.class), any(LocalTime.class), any(Pageable.class));
    }

    @Test
    void getUpcomingEvents_HandlesEventWithUnlimitedCapacity() {
        // Arrange
        futureEvent.setCapacity(null); // Unlimited capacity
        List<Event> events = Arrays.asList(futureEvent);
        Page<Event> eventPage = new PageImpl<>(events);
        when(eventRepository.findUpcomingEvents(any(LocalDate.class), any(LocalTime.class), any(Pageable.class)))
                .thenReturn(eventPage);
        when(rsvpRepository.countByEvent(futureEvent)).thenReturn(100L);

        // Act
        List<EventCardDTO> result = eventService.getUpcomingEvents(null, null, null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        
        EventCardDTO dto = result.get(0);
        assertNull(dto.getMaxAttendees());
        assertFalse(dto.isEventFull()); // Never full with unlimited capacity
        assertEquals(100, dto.getAttendeeCount());
    }

    @Test
    void getUpcomingEvents_ShowsEventAsFull_WhenAtCapacity() {
        // Arrange
        futureEvent.setCapacity(30);
        List<Event> events = Arrays.asList(futureEvent);
        Page<Event> eventPage = new PageImpl<>(events);
        when(eventRepository.findUpcomingEvents(any(LocalDate.class), any(LocalTime.class), any(Pageable.class)))
                .thenReturn(eventPage);
        when(rsvpRepository.countByEvent(futureEvent)).thenReturn(30L); // At capacity

        // Act
        List<EventCardDTO> result = eventService.getUpcomingEvents(null, null, null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).isEventFull());
    }

    @Test
    void getUpcomingEvents_TruncatesDescriptions() {
        // Arrange
        String longDescription = "This is a very long description that should be truncated for display purposes. " +
                "It contains detailed information about the event that exceeds the character limit.";
        futureEvent.setDescription(longDescription);
        
        List<Event> events = Arrays.asList(futureEvent);
        Page<Event> eventPage = new PageImpl<>(events);
        when(eventRepository.findUpcomingEvents(any(LocalDate.class), any(LocalTime.class), any(Pageable.class)))
                .thenReturn(eventPage);
        when(rsvpRepository.countByEvent(futureEvent)).thenReturn(5L);

        // Act
        List<EventCardDTO> result = eventService.getUpcomingEvents(null, null, null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        
        EventCardDTO dto = result.get(0);
        assertTrue(dto.getBriefDescription().endsWith("..."));
        assertTrue(dto.getBriefDescription().length() <= 50);
        assertTrue(dto.getDescription().length() <= 100);
    }

    @Test
    void getUpcomingEvents_HandlesNullCategory() {
        // Arrange
        futureEvent.setCategory(null); // No category
        List<Event> events = Arrays.asList(futureEvent);
        Page<Event> eventPage = new PageImpl<>(events);
        when(eventRepository.findUpcomingEvents(any(LocalDate.class), any(LocalTime.class), any(Pageable.class)))
                .thenReturn(eventPage);
        when(rsvpRepository.countByEvent(futureEvent)).thenReturn(5L);

        // Act
        List<EventCardDTO> result = eventService.getUpcomingEvents(null, null, null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Unknown", result.get(0).getCategoryName());
        assertEquals("#666666", result.get(0).getCategoryColor());
    }

    @Test
    void getUpcomingEvents_HandlesException_ReturnsEmptyList() {
        // Arrange
        when(eventRepository.findUpcomingEvents(any(LocalDate.class), any(LocalTime.class), any(Pageable.class)))
                .thenThrow(new RuntimeException("Database error"));

        // Act
        List<EventCardDTO> result = eventService.getUpcomingEvents(null, null, null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ============== getEventById Tests ==============

    @Test
    void getEventById_ReturnsEventDetails_ForExistingEvent() {
        // Arrange
        when(eventRepository.findWithKeywordsById(1L)).thenReturn(Optional.of(futureEvent));
        when(rsvpRepository.countByEvent(futureEvent)).thenReturn(20L);

        // Act
        EventDetailsDTO result = eventService.getEventById(1L, null);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getEventId());
        assertEquals("Spring Boot Workshop", result.getTitle());
        assertEquals(futureEvent.getDescription(), result.getFullDescription()); // Full description, not truncated
        assertEquals(20, result.getAttendeeCount());
        assertEquals(30, result.getMaxAttendees());
        assertEquals("jane.smith", result.getCreatedByUsername());
        assertFalse(result.isEventFull());
        assertFalse(result.isEventStarted());
    }

    @Test
    void getEventById_ThrowsException_WhenEventNotFound() {
        // Arrange
        when(eventRepository.findWithKeywordsById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> eventService.getEventById(999L, null));

        assertTrue(exception.getMessage().contains("Event not found"));
    }

    @Test
    void getEventById_PopulatesAttendeeList_ForAuthenticatedUser() {
        // Arrange
        when(eventRepository.findWithKeywordsById(1L)).thenReturn(Optional.of(futureEvent));
        when(rsvpRepository.countByEvent(futureEvent)).thenReturn(2L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Create mock RSVPs with attendees
        User attendee1 = new User();
        attendee1.setId(3L);
        attendee1.setUsername("attendee1");

        User attendee2 = new User();
        attendee2.setId(4L);
        attendee2.setUsername("attendee2");

        RSVP rsvp1 = new RSVP(attendee1, futureEvent);
        rsvp1.setRsvpDate(LocalDateTime.now().minusDays(2));

        RSVP rsvp2 = new RSVP(attendee2, futureEvent);
        rsvp2.setRsvpDate(LocalDateTime.now().minusDays(1));

        List<RSVP> rsvps = Arrays.asList(rsvp1, rsvp2);
        when(rsvpRepository.findByEventIdWithUsers(1L)).thenReturn(rsvps);

        // Act
        EventDetailsDTO result = eventService.getEventById(1L, 1L);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getAttendees());
        assertEquals(2, result.getAttendees().size());
        assertEquals("attendee1", result.getAttendees().get(0).getUsername());
        assertEquals("attendee2", result.getAttendees().get(1).getUsername());
    }

    @Test
    void getEventById_EmptyAttendeeList_ForAnonymousUser() {
        // Arrange
        when(eventRepository.findWithKeywordsById(1L)).thenReturn(Optional.of(futureEvent));
        when(rsvpRepository.countByEvent(futureEvent)).thenReturn(5L);

        // Act
        EventDetailsDTO result = eventService.getEventById(1L, null);

        // Assert
        assertNotNull(result);
        assertNotNull(result.getAttendees());
        assertTrue(result.getAttendees().isEmpty());
        verify(rsvpRepository, never()).findByEventIdWithUsers(anyLong());
    }

    @Test
    void getEventById_ShowsUserRSVPStatus_WhenAuthenticated() {
        // Arrange
        when(eventRepository.findWithKeywordsById(1L)).thenReturn(Optional.of(futureEvent));
        when(rsvpRepository.countByEvent(futureEvent)).thenReturn(10L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(rsvpRepository.existsByUser_UsernameAndEvent_Id("john.doe", 1L)).thenReturn(true);
        when(rsvpRepository.findByEventIdWithUsers(1L)).thenReturn(new ArrayList<>());

        // Act
        EventDetailsDTO result = eventService.getEventById(1L, 1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.isUserRsvpStatus());
    }

    // ============== createEvent Tests ==============

    @Test
    void createEvent_Success_WithValidData() {
        // Arrange
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(techCategory));
        when(keywordService.processKeywordSelection(any(), any())).thenReturn(new HashSet<>());

        Event savedEvent = new Event();
        savedEvent.setId(100L);
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

        // Act
        Long eventId = eventService.createEvent(validCreateDTO, testCreator);

        // Assert
        assertEquals(100L, eventId);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(eventCaptor.capture());

        Event capturedEvent = eventCaptor.getValue();
        assertEquals("New Tech Event", capturedEvent.getTitle());
        assertEquals("A comprehensive workshop on new technologies", capturedEvent.getDescription());
        assertEquals(LocalDate.now().plusDays(14), capturedEvent.getEventDate());
        assertEquals(LocalTime.of(15, 30), capturedEvent.getEventTime());
        assertEquals("Innovation Hub", capturedEvent.getLocation());
        assertEquals(40, capturedEvent.getCapacity());
        assertEquals(testCreator, capturedEvent.getCreatedBy());
        assertEquals(techCategory, capturedEvent.getCategory());
    }

    @Test
    void createEvent_Success_WithUnlimitedCapacity() {
        // Arrange
        validCreateDTO.setUnlimitedCapacity(true);
        validCreateDTO.setCapacity(null);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(techCategory));
        when(keywordService.processKeywordSelection(any(), any())).thenReturn(new HashSet<>());

        Event savedEvent = new Event();
        savedEvent.setId(101L);
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

        // Act
        Long eventId = eventService.createEvent(validCreateDTO, testCreator);

        // Assert
        assertEquals(101L, eventId);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(eventCaptor.capture());

        Event capturedEvent = eventCaptor.getValue();
        assertNull(capturedEvent.getCapacity()); // Unlimited capacity
    }

    @Test
    void createEvent_ThrowsException_WhenEventDateInPast() {
        // Arrange
        validCreateDTO.setEventDate(LocalDate.now().minusDays(1));
        validCreateDTO.setEventTime(LocalTime.of(10, 0));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> eventService.createEvent(validCreateDTO, testCreator));
        
        assertEquals("Event date and time must be in the future", exception.getMessage());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void createEvent_ThrowsException_WhenEventTimeInPastToday() {
        // Arrange
        LocalDateTime pastDateTime = LocalDateTime.now().minusHours(1);
        validCreateDTO.setEventDate(pastDateTime.toLocalDate());
        validCreateDTO.setEventTime(pastDateTime.toLocalTime());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> eventService.createEvent(validCreateDTO, testCreator));

        assertEquals("Event date and time must be in the future", exception.getMessage());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void createEvent_ThrowsException_WhenCategoryNotFound() {
        // Arrange
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());
        validCreateDTO.setCategoryId(999L);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> eventService.createEvent(validCreateDTO, testCreator));
        
        assertEquals("Invalid category selected", exception.getMessage());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void createEvent_ThrowsException_WhenDateTimeNull() {
        // Arrange
        validCreateDTO.setEventDate(null);
        validCreateDTO.setEventTime(null);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> eventService.createEvent(validCreateDTO, testCreator));

        assertEquals("Event date and time must be in the future", exception.getMessage());
        verify(eventRepository, never()).save(any());
    }

    // ============== Keyword Processing Tests ==============

    @Test
    void createEvent_WithExistingKeywords_AssignsKeywords() {
        // Arrange
        Set<Long> keywordIds = Set.of(1L, 2L);
        validCreateDTO.setKeywordIds(keywordIds);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(techCategory));

        Set<Keyword> mockKeywords = new HashSet<>();
        Keyword k1 = new Keyword();
        k1.setId(1L);
        k1.setName("Java");
        Keyword k2 = new Keyword();
        k2.setId(2L);
        k2.setName("Spring");
        mockKeywords.add(k1);
        mockKeywords.add(k2);

        when(keywordService.processKeywordSelection(eq(keywordIds), any())).thenReturn(mockKeywords);

        Event savedEvent = new Event();
        savedEvent.setId(200L);
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

        // Act
        Long eventId = eventService.createEvent(validCreateDTO, testCreator);

        // Assert
        assertEquals(200L, eventId);
        verify(keywordService).processKeywordSelection(eq(keywordIds), any());

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(eventCaptor.capture());

        Event capturedEvent = eventCaptor.getValue();
        assertNotNull(capturedEvent.getKeywords());
        assertEquals(2, capturedEvent.getKeywords().size());
    }

    @Test
    void createEvent_WithCustomKeywords_ProcessesAndAssigns() {
        // Arrange
        List<String> customKeywords = Arrays.asList("AI", "Machine Learning");
        validCreateDTO.setCustomKeywords(customKeywords);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(techCategory));

        Set<Keyword> mockKeywords = new HashSet<>();
        Keyword k1 = new Keyword();
        k1.setId(10L);
        k1.setName("ai");
        mockKeywords.add(k1);

        when(keywordService.processKeywordSelection(any(), eq(customKeywords))).thenReturn(mockKeywords);

        Event savedEvent = new Event();
        savedEvent.setId(201L);
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

        // Act
        Long eventId = eventService.createEvent(validCreateDTO, testCreator);

        // Assert
        assertEquals(201L, eventId);
        verify(keywordService).processKeywordSelection(any(), eq(customKeywords));

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(eventCaptor.capture());

        Event capturedEvent = eventCaptor.getValue();
        assertNotNull(capturedEvent.getKeywords());
        assertEquals(1, capturedEvent.getKeywords().size());
    }

    @Test
    void createEvent_WithMixedKeywords_ProcessesBoth() {
        // Arrange
        Set<Long> keywordIds = Set.of(1L, 2L);
        List<String> customKeywords = Arrays.asList("Deep Learning");
        validCreateDTO.setKeywordIds(keywordIds);
        validCreateDTO.setCustomKeywords(customKeywords);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(techCategory));

        Set<Keyword> mockKeywords = new HashSet<>();
        Keyword k1 = new Keyword();
        k1.setId(1L);
        k1.setName("Java");
        Keyword k2 = new Keyword();
        k2.setId(2L);
        k2.setName("Spring");
        Keyword k3 = new Keyword();
        k3.setId(11L);
        k3.setName("deep learning");
        mockKeywords.add(k1);
        mockKeywords.add(k2);
        mockKeywords.add(k3);

        when(keywordService.processKeywordSelection(eq(keywordIds), eq(customKeywords))).thenReturn(mockKeywords);

        Event savedEvent = new Event();
        savedEvent.setId(202L);
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

        // Act
        Long eventId = eventService.createEvent(validCreateDTO, testCreator);

        // Assert
        assertEquals(202L, eventId);
        verify(keywordService).processKeywordSelection(eq(keywordIds), eq(customKeywords));

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(eventCaptor.capture());

        Event capturedEvent = eventCaptor.getValue();
        assertEquals(3, capturedEvent.getKeywords().size());
    }

    @Test
    void createEvent_KeywordServiceThrowsException_PropagatesException() {
        // Arrange
        Set<Long> keywordIds = Set.of(1L, 2L, 3L, 4L, 5L, 6L);
        validCreateDTO.setKeywordIds(keywordIds);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(techCategory));
        when(keywordService.processKeywordSelection(eq(keywordIds), any()))
                .thenThrow(new IllegalArgumentException("An event can have maximum 5 keywords"));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> eventService.createEvent(validCreateDTO, testCreator));

        assertEquals("An event can have maximum 5 keywords", exception.getMessage());
        verify(eventRepository, never()).save(any());
    }

    // ============== DateTime Boundary Tests ==============

    @Test
    void createEvent_EventExactly5MinutesInFuture_Succeeds() {
        // Arrange
        LocalDateTime futureTime = LocalDateTime.now().plusMinutes(5);
        validCreateDTO.setEventDate(futureTime.toLocalDate());
        validCreateDTO.setEventTime(futureTime.toLocalTime());

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(techCategory));
        when(keywordService.processKeywordSelection(any(), any())).thenReturn(new HashSet<>());

        Event savedEvent = new Event();
        savedEvent.setId(203L);
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

        // Act
        Long eventId = eventService.createEvent(validCreateDTO, testCreator);

        // Assert
        assertNotNull(eventId);
        assertEquals(203L, eventId);
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void createEvent_EventAt6MinutesInPast_Fails() {
        // Arrange
        LocalDateTime pastTime = LocalDateTime.now().minusMinutes(6);
        validCreateDTO.setEventDate(pastTime.toLocalDate());
        validCreateDTO.setEventTime(pastTime.toLocalTime());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> eventService.createEvent(validCreateDTO, testCreator));

        assertEquals("Event date and time must be in the future", exception.getMessage());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void createEvent_EventAtExactlyNowMinus5Minutes_Fails() {
        // Arrange
        LocalDateTime boundaryTime = LocalDateTime.now().minusMinutes(5);
        validCreateDTO.setEventDate(boundaryTime.toLocalDate());
        validCreateDTO.setEventTime(boundaryTime.toLocalTime());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> eventService.createEvent(validCreateDTO, testCreator));

        assertEquals("Event date and time must be in the future", exception.getMessage());
        verify(eventRepository, never()).save(any());
    }

    // ============== Repository Exception Handling Tests ==============

    @Test
    void createEvent_RepositorySaveThrowsException_PropagatesException() {
        // Arrange
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(techCategory));
        when(keywordService.processKeywordSelection(any(), any())).thenReturn(new HashSet<>());
        when(eventRepository.save(any(Event.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> eventService.createEvent(validCreateDTO, testCreator));

        assertEquals("Database connection failed", exception.getMessage());
    }

    @Test
    void createEvent_CategoryRepositoryThrowsException_PropagatesException() {
        // Arrange
        when(categoryRepository.findById(1L))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> eventService.createEvent(validCreateDTO, testCreator));

        assertEquals("Database error", exception.getMessage());
        verify(eventRepository, never()).save(any());
    }

    // ============== Field Assignment Verification Tests ==============

    @Test
    void createEvent_AllFieldsProperlyMapped_IncludingNullables() {
        // Arrange
        validCreateDTO.setKeywordIds(null);
        validCreateDTO.setCustomKeywords(null);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(techCategory));
        when(keywordService.processKeywordSelection(null, null)).thenReturn(new HashSet<>());

        Event savedEvent = new Event();
        savedEvent.setId(204L);
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

        // Act
        Long eventId = eventService.createEvent(validCreateDTO, testCreator);

        // Assert
        assertEquals(204L, eventId);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(eventCaptor.capture());

        Event capturedEvent = eventCaptor.getValue();
        assertEquals("New Tech Event", capturedEvent.getTitle());
        assertEquals("A comprehensive workshop on new technologies", capturedEvent.getDescription());
        assertEquals(LocalDate.now().plusDays(14), capturedEvent.getEventDate());
        assertEquals(LocalTime.of(15, 30), capturedEvent.getEventTime());
        assertEquals("Innovation Hub", capturedEvent.getLocation());
        assertEquals(40, capturedEvent.getCapacity());
        assertNotNull(capturedEvent.getKeywords());
    }

    @Test
    void createEvent_VerifiesCreatorAndCategoryAssignment() {
        // Arrange
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(techCategory));
        when(keywordService.processKeywordSelection(any(), any())).thenReturn(new HashSet<>());

        Event savedEvent = new Event();
        savedEvent.setId(205L);
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

        // Act
        Long eventId = eventService.createEvent(validCreateDTO, testCreator);

        // Assert
        assertEquals(205L, eventId);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(eventCaptor.capture());

        Event capturedEvent = eventCaptor.getValue();
        assertEquals(testCreator, capturedEvent.getCreatedBy());
        assertEquals(testCreator.getId(), capturedEvent.getCreatedBy().getId());
        assertEquals("jane.smith", capturedEvent.getCreatedBy().getUsername());
        assertEquals(techCategory, capturedEvent.getCategory());
        assertEquals(1L, capturedEvent.getCategory().getId());
        assertEquals("Technology", capturedEvent.getCategory().getName());
    }

    // ============== Edge Cases Tests ==============

    @Test
    void createEvent_WithCapacityZero_TreatedAsUnlimited() {
        // Arrange
        validCreateDTO.setCapacity(0);
        validCreateDTO.setUnlimitedCapacity(true);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(techCategory));
        when(keywordService.processKeywordSelection(any(), any())).thenReturn(new HashSet<>());

        Event savedEvent = new Event();
        savedEvent.setId(206L);
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

        // Act
        Long eventId = eventService.createEvent(validCreateDTO, testCreator);

        // Assert
        assertEquals(206L, eventId);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(eventCaptor.capture());

        Event capturedEvent = eventCaptor.getValue();
        assertNull(capturedEvent.getCapacity()); // Unlimited capacity = null
    }

    @Test
    void createEvent_WithEmptyKeywordCollections_HandlesGracefully() {
        // Arrange
        validCreateDTO.setKeywordIds(new HashSet<>());
        validCreateDTO.setCustomKeywords(new ArrayList<>());

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(techCategory));
        when(keywordService.processKeywordSelection(any(), any())).thenReturn(new HashSet<>());

        Event savedEvent = new Event();
        savedEvent.setId(207L);
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

        // Act
        Long eventId = eventService.createEvent(validCreateDTO, testCreator);

        // Assert
        assertEquals(207L, eventId);
        verify(keywordService).processKeywordSelection(any(), any());

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(eventCaptor.capture());

        Event capturedEvent = eventCaptor.getValue();
        assertNotNull(capturedEvent.getKeywords());
        assertEquals(0, capturedEvent.getKeywords().size());
    }

    // ============== deleteEvent Tests ==============

    @Test
    void deleteEvent_Success_CreatorDeletesEvent() {
        // Arrange
        when(eventRepository.findById(1L)).thenReturn(Optional.of(futureEvent));

        // Act
        eventService.deleteEvent(1L, 2L); // testCreator (ID=2) deletes their event

        // Assert
        verify(eventRepository).findById(1L);
        verify(rsvpRepository).deleteByActiveEvent(1L);
        verify(eventRepository).deleteActiveEventById(1L);
    }

    @Test
    void deleteEvent_Success_AdminDeletesAnyEvent() {
        // Arrange
        when(eventRepository.findById(1L)).thenReturn(Optional.of(futureEvent));
        when(userService.hasRole(99L, "ROLE_ADMIN")).thenReturn(true); // Admin user

        // Act
        eventService.deleteEvent(1L, 99L); // Admin (not creator) deletes event

        // Assert
        verify(eventRepository).findById(1L);
        verify(userService).hasRole(99L, "ROLE_ADMIN");
        verify(rsvpRepository).deleteByActiveEvent(1L);
        verify(eventRepository).deleteActiveEventById(1L);
    }

    @Test
    void deleteEvent_Fails_NonCreatorNonAdmin() {
        // Arrange
        when(eventRepository.findById(1L)).thenReturn(Optional.of(futureEvent));
        when(userService.hasRole(99L, "ROLE_ADMIN")).thenReturn(false); // Not admin

        // Act & Assert
        assertThrows(AccessDeniedException.class,
                () -> eventService.deleteEvent(1L, 99L)); // Non-creator, non-admin tries to delete

        verify(eventRepository).findById(1L);
        verify(rsvpRepository, never()).deleteByActiveEvent(anyLong());
        verify(eventRepository, never()).deleteActiveEventById(anyLong());
    }

    @Test
    void deleteEvent_Fails_EventAlreadyStarted() {
        // Arrange
        when(eventRepository.findById(2L)).thenReturn(Optional.of(pastEvent));

        // Act & Assert
        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> eventService.deleteEvent(2L, 2L)); // Creator tries to delete started event

        assertTrue(exception.getMessage().contains("Cannot delete an event that has already started"));
        verify(eventRepository).findById(2L);
        verify(rsvpRepository, never()).deleteByActiveEvent(anyLong());
        verify(eventRepository, never()).deleteActiveEventById(anyLong());
    }

    @Test
    void deleteEvent_Fails_EventNotFound() {
        // Arrange
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> eventService.deleteEvent(999L, 1L));

        assertTrue(exception.getMessage().contains("Event not found"));
        verify(eventRepository).findById(999L);
        verify(rsvpRepository, never()).deleteByActiveEvent(anyLong());
        verify(eventRepository, never()).deleteActiveEventById(anyLong());
    }

    @Test
    void deleteEvent_VerifiesCascadeDeletion() {
        // Arrange
        when(eventRepository.findById(1L)).thenReturn(Optional.of(futureEvent));

        org.mockito.InOrder inOrder = inOrder(rsvpRepository, eventRepository);

        // Act
        eventService.deleteEvent(1L, 2L);

        // Assert - RSVPs must be deleted BEFORE event
        inOrder.verify(rsvpRepository).deleteByActiveEvent(1L);
        inOrder.verify(eventRepository).deleteActiveEventById(1L);
    }

    @Test
    void deleteEvent_HandlesNullCreator() {
        // Arrange
        futureEvent.setCreatedBy(null);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(futureEvent));

        // Act & Assert
        assertThrows(NullPointerException.class,
                () -> eventService.deleteEvent(1L, 1L));

        verify(eventRepository).findById(1L);
        verify(rsvpRepository, never()).deleteByActiveEvent(anyLong());
    }

    // ============== getUserRSVPEvents Tests ==============

    @Test
    void getUserRSVPEvents_Success_ReturnsUserRSVPEvents() {
        // Arrange
        List<Event> rsvpEvents = Arrays.asList(futureEvent);
        when(rsvpRepository.findUpcomingEventsByUserId(1L)).thenReturn(rsvpEvents);
        when(rsvpRepository.countByEvent(futureEvent)).thenReturn(15L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(rsvpRepository.existsByUser_UsernameAndEvent_Id("john.doe", 1L)).thenReturn(true);

        // Act
        List<EventCardDTO> result = eventService.getUserRSVPEvents(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Spring Boot Workshop", result.get(0).getTitle());
        verify(rsvpRepository).findUpcomingEventsByUserId(1L);
    }

    @Test
    void getUserRSVPEvents_NullUserId_ReturnsEmptyList() {
        // Arrange & Act
        List<EventCardDTO> result = eventService.getUserRSVPEvents(null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(rsvpRepository, never()).findUpcomingEventsByUserId(any());
    }

    @Test
    void getUserRSVPEvents_EmptyList_ReturnsEmptyList() {
        // Arrange
        when(rsvpRepository.findUpcomingEventsByUserId(1L)).thenReturn(Collections.emptyList());

        // Act
        List<EventCardDTO> result = eventService.getUserRSVPEvents(1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(rsvpRepository).findUpcomingEventsByUserId(1L);
    }

    @Test
    void getUserRSVPEvents_NullList_ReturnsEmptyList() {
        // Arrange
        when(rsvpRepository.findUpcomingEventsByUserId(1L)).thenReturn(null);

        // Act
        List<EventCardDTO> result = eventService.getUserRSVPEvents(1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(rsvpRepository).findUpcomingEventsByUserId(1L);
    }

    @Test
    void getUserRSVPEvents_ExceptionThrown_ReturnsEmptyList() {
        // Arrange
        when(rsvpRepository.findUpcomingEventsByUserId(1L))
                .thenThrow(new RuntimeException("Database error"));

        // Act
        List<EventCardDTO> result = eventService.getUserRSVPEvents(1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(rsvpRepository).findUpcomingEventsByUserId(1L);
    }

    // ============== getUserCreatedEvents Tests ==============

    @Test
    void getUserCreatedEvents_Success_ReturnsCreatedEvents() {
        // Arrange
        Page<Event> createdEvents = new PageImpl<>(Arrays.asList(futureEvent));
        when(eventRepository.findUpcomingEventsByCreatedBy(eq(2L), any(LocalDate.class), any(LocalTime.class), any(Pageable.class)))
                .thenReturn(createdEvents);
        when(rsvpRepository.countByEvent(futureEvent)).thenReturn(20L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(testCreator));
        when(rsvpRepository.existsByUser_UsernameAndEvent_Id("jane.smith", 1L)).thenReturn(false);

        // Act
        List<EventCardDTO> result = eventService.getUserCreatedEvents(2L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Spring Boot Workshop", result.get(0).getTitle());
        verify(eventRepository).findUpcomingEventsByCreatedBy(eq(2L), any(LocalDate.class), any(LocalTime.class), any(Pageable.class));
    }

    @Test
    void getUserCreatedEvents_NullUserId_ReturnsEmptyList() {
        // Arrange & Act
        List<EventCardDTO> result = eventService.getUserCreatedEvents(null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(eventRepository, never()).findUpcomingEventsByCreatedBy(any(), any(), any(), any());
    }

    @Test
    void getUserCreatedEvents_EmptyPage_ReturnsEmptyList() {
        // Arrange
        Page<Event> emptyPage = new PageImpl<>(Collections.emptyList());
        when(eventRepository.findUpcomingEventsByCreatedBy(eq(1L), any(LocalDate.class), any(LocalTime.class), any(Pageable.class)))
                .thenReturn(emptyPage);

        // Act
        List<EventCardDTO> result = eventService.getUserCreatedEvents(1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getUserCreatedEvents_NullPage_ReturnsEmptyList() {
        // Arrange
        when(eventRepository.findUpcomingEventsByCreatedBy(eq(1L), any(LocalDate.class), any(LocalTime.class), any(Pageable.class)))
                .thenReturn(null);

        // Act
        List<EventCardDTO> result = eventService.getUserCreatedEvents(1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getUserCreatedEvents_ExceptionThrown_ReturnsEmptyList() {
        // Arrange
        when(eventRepository.findUpcomingEventsByCreatedBy(eq(1L), any(LocalDate.class), any(LocalTime.class), any(Pageable.class)))
                .thenThrow(new RuntimeException("Database error"));

        // Act
        List<EventCardDTO> result = eventService.getUserCreatedEvents(1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ============== cancelAttendeeRsvpAsOrganiser Tests ==============

    @Test
    void cancelAttendeeRsvpAsOrganiser_Success_OrganizerCancelsRSVP() {
        // Arrange
        RSVP rsvp = new RSVP(testUser, futureEvent);
        rsvp.setId(10L);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(futureEvent));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testCreator));
        when(rsvpRepository.findByUserAndEvent(testUser, futureEvent)).thenReturn(Optional.of(rsvp));

        // Act
        eventService.cancelAttendeeRsvpAsOrganiser(1L, 1L, 2L);

        // Assert
        verify(paymentRepository).deleteByRsvp(rsvp);
        verify(rsvpRepository).delete(rsvp);
    }

    @Test
    void cancelAttendeeRsvpAsOrganiser_Success_AdminCancelsRSVP() {
        // Arrange
        RSVP rsvp = new RSVP(testUser, futureEvent);
        rsvp.setId(10L);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(futureEvent));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(99L)).thenReturn(Optional.of(testUser));
        when(userService.hasRole(99L, "ROLE_ADMIN")).thenReturn(true);
        when(rsvpRepository.findByUserAndEvent(testUser, futureEvent)).thenReturn(Optional.of(rsvp));

        // Act
        eventService.cancelAttendeeRsvpAsOrganiser(1L, 1L, 99L);

        // Assert
        verify(userService).hasRole(99L, "ROLE_ADMIN");
        verify(paymentRepository).deleteByRsvp(rsvp);
        verify(rsvpRepository).delete(rsvp);
    }

    @Test
    void cancelAttendeeRsvpAsOrganiser_Fails_NonOrganizerNonAdmin() {
        // Arrange
        when(eventRepository.findById(1L)).thenReturn(Optional.of(futureEvent));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(99L)).thenReturn(Optional.of(testUser));
        when(userService.hasRole(99L, "ROLE_ADMIN")).thenReturn(false);

        // Act & Assert
        assertThrows(AccessDeniedException.class,
                () -> eventService.cancelAttendeeRsvpAsOrganiser(1L, 1L, 99L));

        verify(rsvpRepository, never()).delete(any());
    }

    @Test
    void cancelAttendeeRsvpAsOrganiser_Fails_EventNotFound() {
        // Arrange
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class,
                () -> eventService.cancelAttendeeRsvpAsOrganiser(999L, 1L, 2L));

        verify(rsvpRepository, never()).delete(any());
    }

    @Test
    void cancelAttendeeRsvpAsOrganiser_Fails_AttendeeNotFound() {
        // Arrange
        when(eventRepository.findById(1L)).thenReturn(Optional.of(futureEvent));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class,
                () -> eventService.cancelAttendeeRsvpAsOrganiser(1L, 999L, 2L));

        verify(rsvpRepository, never()).delete(any());
    }

    @Test
    void cancelAttendeeRsvpAsOrganiser_Fails_RSVPNotFound() {
        // Arrange
        when(eventRepository.findById(1L)).thenReturn(Optional.of(futureEvent));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testCreator));
        when(rsvpRepository.findByUserAndEvent(testUser, futureEvent)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class,
                () -> eventService.cancelAttendeeRsvpAsOrganiser(1L, 1L, 2L));

        verify(rsvpRepository, never()).delete(any());
    }

    // ============== Edge Cases and Integration Tests ==============

    @Test
    void getUpcomingEvents_HandlesMultipleEvents_WithDifferentStates() {
        // Arrange
        Event todayEvent = new Event();
        todayEvent.setId(3L);
        todayEvent.setTitle("Today Event");
        todayEvent.setDescription("Happening today");
        todayEvent.setEventDate(LocalDate.now());
        todayEvent.setEventTime(LocalTime.now().plusHours(2)); // Future time today
        todayEvent.setCapacity(20);
        todayEvent.setCategory(techCategory);
        todayEvent.setCreatedBy(testCreator);

        Event fullEvent = new Event();
        fullEvent.setId(4L);
        fullEvent.setTitle("Full Event");
        fullEvent.setDescription("This event is full");
        fullEvent.setEventDate(LocalDate.now().plusDays(3));
        fullEvent.setEventTime(LocalTime.of(18, 0));
        fullEvent.setCapacity(10);
        fullEvent.setCategory(techCategory);
        fullEvent.setCreatedBy(testCreator);

        List<Event> events = Arrays.asList(futureEvent, todayEvent, fullEvent);
        Page<Event> eventPage = new PageImpl<>(events);
        
        when(eventRepository.findUpcomingEvents(any(LocalDate.class), any(LocalTime.class), any(Pageable.class)))
                .thenReturn(eventPage);
        when(rsvpRepository.countByEvent(futureEvent)).thenReturn(15L);
        when(rsvpRepository.countByEvent(todayEvent)).thenReturn(5L);
        when(rsvpRepository.countByEvent(fullEvent)).thenReturn(10L); // At capacity

        // Act
        List<EventCardDTO> result = eventService.getUpcomingEvents(null, null, null);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        
        // Verify different states
        assertFalse(result.get(0).isEventFull()); // futureEvent not full
        assertFalse(result.get(1).isEventFull()); // todayEvent not full
        assertTrue(result.get(2).isEventFull());  // fullEvent is full
    }

    @Test
    void getUpcomingEvents_UsesCorrectPagination() {
        // Arrange
        Page<Event> emptyPage = new PageImpl<>(Collections.emptyList());
        when(eventRepository.findUpcomingEvents(any(LocalDate.class), any(LocalTime.class), any(Pageable.class)))
                .thenReturn(emptyPage);

        // Act
        eventService.getUpcomingEvents(null, null, null);

        // Assert
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(eventRepository).findUpcomingEvents(any(LocalDate.class), any(LocalTime.class), pageableCaptor.capture());
        
        Pageable capturedPageable = pageableCaptor.getValue();
        assertEquals(0, capturedPageable.getPageNumber());
        assertEquals(100, capturedPageable.getPageSize());
    }

    @Test
    void getEventById_HandlesNullCreator() {
        // Arrange
        futureEvent.setCreatedBy(null);
        when(eventRepository.findWithKeywordsById(1L)).thenReturn(Optional.of(futureEvent));
        when(rsvpRepository.countByEvent(futureEvent)).thenReturn(0L);

        // Act
        EventDetailsDTO result = eventService.getEventById(1L, null);

        // Assert
        assertNotNull(result);
        assertNull(result.getCreatedByUsername());
        assertNull(result.getCreatedById());
    }

    @Test
    void mapToEventCardDTO_HandlesNullDescription() {
        // Arrange
        futureEvent.setDescription(null);
        List<Event> events = Arrays.asList(futureEvent);
        Page<Event> eventPage = new PageImpl<>(events);
        when(eventRepository.findUpcomingEvents(any(LocalDate.class), any(LocalTime.class), any(Pageable.class)))
                .thenReturn(eventPage);
        when(rsvpRepository.countByEvent(futureEvent)).thenReturn(0L);

        // Act
        List<EventCardDTO> result = eventService.getUpcomingEvents(null, null, null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("", result.get(0).getBriefDescription());
        assertEquals("", result.get(0).getDescription());
    }

    // ============== getUpcomingEvents() - Filter Combinations ==============

    @Test
    void getUpcomingEvents_CategoryFilterOnly_Success() {
        Page<Event> eventPage = new PageImpl<>(Arrays.asList(futureEvent));
        when(eventRepository.findUpcomingEventsByCategory(eq(1L), any(LocalDate.class), any(LocalTime.class), any(Pageable.class)))
                .thenReturn(eventPage);
        when(rsvpRepository.countByEvent(futureEvent)).thenReturn(10L);

        Page<EventCardDTO> result = eventService.getUpcomingEvents(null, 1L, null, null, null, PageRequest.of(0, 100));

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(eventRepository).findUpcomingEventsByCategory(eq(1L), any(LocalDate.class), any(LocalTime.class), any(Pageable.class));
    }

    @Test
    void getUpcomingEvents_KeywordFilterOnly_Success() {
        Set<Long> keywordIds = Set.of(1L, 2L);
        Page<Event> eventPage = new PageImpl<>(Arrays.asList(futureEvent));
        when(eventRepository.findUpcomingEventsByKeywordsOr(eq(keywordIds), any(LocalDate.class), any(LocalTime.class), any(Pageable.class)))
                .thenReturn(eventPage);
        when(rsvpRepository.countByEvent(futureEvent)).thenReturn(10L);

        Page<EventCardDTO> result = eventService.getUpcomingEvents(null, null, null, keywordIds, null, PageRequest.of(0, 100));

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(eventRepository).findUpcomingEventsByKeywordsOr(eq(keywordIds), any(LocalDate.class), any(LocalTime.class), any(Pageable.class));
    }

    @Test
    void getUpcomingEvents_CategoryAndKeywords_Success() {
        Set<Long> keywordIds = Set.of(1L, 2L);
        Page<Event> eventPage = new PageImpl<>(Arrays.asList(futureEvent));
        when(eventRepository.findUpcomingEventsByKeywordsOr(eq(keywordIds), any(LocalDate.class), any(LocalTime.class), any(Pageable.class)))
                .thenReturn(eventPage);
        when(rsvpRepository.countByEvent(futureEvent)).thenReturn(10L);

        Page<EventCardDTO> result = eventService.getUpcomingEvents(null, 1L, null, keywordIds, null, PageRequest.of(0, 100));

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(eventRepository).findUpcomingEventsByKeywordsOr(eq(keywordIds), any(LocalDate.class), any(LocalTime.class), any(Pageable.class));
    }

    @Test
    void getUpcomingEvents_SearchTermOnly_Success() {
        Page<Event> eventPage = new PageImpl<>(Arrays.asList(futureEvent));
        when(eventRepository.searchUpcomingEvents(eq("tech"), any(LocalDate.class), any(LocalTime.class), any(Pageable.class)))
                .thenReturn(eventPage);
        when(rsvpRepository.countByEvent(futureEvent)).thenReturn(10L);

        Page<EventCardDTO> result = eventService.getUpcomingEvents(null, null, null, null, "tech", PageRequest.of(0, 100));

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(eventRepository).searchUpcomingEvents(eq("tech"), any(LocalDate.class), any(LocalTime.class), any(Pageable.class));
    }

    @Test
    void getUpcomingEvents_SearchAndCategory_Success() {
        Page<Event> eventPage = new PageImpl<>(Arrays.asList(futureEvent));
        when(eventRepository.searchUpcomingEventsByCategory(eq("tech"), eq(1L), any(LocalDate.class), any(LocalTime.class), any(Pageable.class)))
                .thenReturn(eventPage);
        when(rsvpRepository.countByEvent(futureEvent)).thenReturn(10L);

        Page<EventCardDTO> result = eventService.getUpcomingEvents(null, 1L, null, null, "tech", PageRequest.of(0, 100));

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(eventRepository).searchUpcomingEventsByCategory(eq("tech"), eq(1L), any(LocalDate.class), any(LocalTime.class), any(Pageable.class));
    }

    @Test
    void getUpcomingEvents_SearchAndKeywords_Success() {
        Set<Long> keywordIds = Set.of(1L, 2L);
        Page<Event> eventPage = new PageImpl<>(Arrays.asList(futureEvent));
        when(eventRepository.searchUpcomingEventsByKeywords(eq("tech"), eq(keywordIds), any(LocalDate.class), any(LocalTime.class), any(Pageable.class)))
                .thenReturn(eventPage);
        when(rsvpRepository.countByEvent(futureEvent)).thenReturn(10L);

        Page<EventCardDTO> result = eventService.getUpcomingEvents(null, null, null, keywordIds, "tech", PageRequest.of(0, 100));

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(eventRepository).searchUpcomingEventsByKeywords(eq("tech"), eq(keywordIds), any(LocalDate.class), any(LocalTime.class), any(Pageable.class));
    }

    @Test
    void getUpcomingEvents_SearchAndCategoryAndKeywords_Success() {
        Set<Long> keywordIds = Set.of(1L, 2L);
        Page<Event> eventPage = new PageImpl<>(Arrays.asList(futureEvent));
        when(eventRepository.searchUpcomingEventsByCategoryAndKeywords(eq("tech"), eq(1L), eq(keywordIds), any(LocalDate.class), any(LocalTime.class), any(Pageable.class)))
                .thenReturn(eventPage);
        when(rsvpRepository.countByEvent(futureEvent)).thenReturn(10L);

        Page<EventCardDTO> result = eventService.getUpcomingEvents(null, 1L, null, keywordIds, "tech", PageRequest.of(0, 100));

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(eventRepository).searchUpcomingEventsByCategoryAndKeywords(eq("tech"), eq(1L), eq(keywordIds), any(LocalDate.class), any(LocalTime.class), any(Pageable.class));
    }

    @Test
    void getUpcomingEvents_EmptyResults_ReturnsEmptyPage() {
        Page<Event> emptyPage = new PageImpl<>(Collections.emptyList());
        when(eventRepository.findUpcomingEvents(any(LocalDate.class), any(LocalTime.class), any(Pageable.class)))
                .thenReturn(emptyPage);

        Page<EventCardDTO> result = eventService.getUpcomingEvents(null, null, null, null, null, PageRequest.of(0, 100));

        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void getUpcomingEvents_NullEventsFromRepository_ReturnsEmptyPage() {
        when(eventRepository.findUpcomingEvents(any(LocalDate.class), any(LocalTime.class), any(Pageable.class)))
                .thenReturn(null);

        Page<EventCardDTO> result = eventService.getUpcomingEvents(null, null, null, null, null, PageRequest.of(0, 100));

        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void getUpcomingEvents_ExceptionThrown_ReturnsEmptyPage() {
        when(eventRepository.findUpcomingEvents(any(LocalDate.class), any(LocalTime.class), any(Pageable.class)))
                .thenThrow(new RuntimeException("Database error"));

        Page<EventCardDTO> result = eventService.getUpcomingEvents(null, null, null, null, null, PageRequest.of(0, 100));

        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    // ============== getEventById() - Additional Coverage ==============

    @Test
    void getEventById_NullCategory_SetsDefaultValues() {
        futureEvent.setCategory(null);
        when(eventRepository.findWithKeywordsById(1L)).thenReturn(Optional.of(futureEvent));
        when(rsvpRepository.countByEvent(futureEvent)).thenReturn(0L);

        EventDetailsDTO result = eventService.getEventById(1L, null);

        assertNotNull(result);
        assertEquals("Unknown", result.getCategoryName());
        assertEquals("#666666", result.getCategoryColor());
    }

    @Test
    void getEventById_UserBlocked_SetsBlockedStatus() {
        when(eventRepository.findWithKeywordsById(1L)).thenReturn(Optional.of(futureEvent));
        when(rsvpRepository.countByEvent(futureEvent)).thenReturn(0L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(rsvpRepository.existsByUser_UsernameAndEvent_Id("john.doe", 1L)).thenReturn(false);
        when(blockedRSVPRepository.existsByEvent_IdAndUser_Id(1L, 1L)).thenReturn(true);

        EventDetailsDTO result = eventService.getEventById(1L, 1L);

        assertNotNull(result);
        assertTrue(result.isUserBlockedStatus());
        assertFalse(result.isUserRsvpStatus());
    }

    @Test
    void getEventById_EmptyKeywords_ReturnsEmptyList() {
        futureEvent.setKeywords(new HashSet<>());
        when(eventRepository.findWithKeywordsById(1L)).thenReturn(Optional.of(futureEvent));
        when(rsvpRepository.countByEvent(futureEvent)).thenReturn(0L);

        EventDetailsDTO result = eventService.getEventById(1L, null);

        assertNotNull(result);
        assertNotNull(result.getKeywords());
        assertTrue(result.getKeywords().isEmpty());
    }

    @Test
    void getEventById_NullKeywords_ReturnsEmptyList() {
        futureEvent.setKeywords(null);
        when(eventRepository.findWithKeywordsById(1L)).thenReturn(Optional.of(futureEvent));
        when(rsvpRepository.countByEvent(futureEvent)).thenReturn(0L);

        EventDetailsDTO result = eventService.getEventById(1L, null);

        assertNotNull(result);
        assertNotNull(result.getKeywords());
        assertTrue(result.getKeywords().isEmpty());
    }

    @Test
    void getEventById_UserWithRSVP_IncludesPaymentStatus() {
        RSVP rsvp = new RSVP(testUser, futureEvent);
        rsvp.setId(100L);
        rsvp.setPaymentStatus("PAID");

        when(eventRepository.findWithKeywordsById(1L)).thenReturn(Optional.of(futureEvent));
        when(rsvpRepository.countByEvent(futureEvent)).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(rsvpRepository.existsByUser_UsernameAndEvent_Id("john.doe", 1L)).thenReturn(true);
        when(rsvpRepository.findByUser_UsernameAndEvent_Id("john.doe", 1L)).thenReturn(Optional.of(rsvp));
        when(blockedRSVPRepository.existsByEvent_IdAndUser_Id(1L, 1L)).thenReturn(false);

        EventDetailsDTO result = eventService.getEventById(1L, 1L);

        assertNotNull(result);
        assertTrue(result.isUserRsvpStatus());
        assertEquals("PAID", result.getUserPaymentStatus());
        assertEquals(100L, result.getUserRsvpId());
    }

    @Test
    void getEventById_EventFull_SetsFullStatus() {
        futureEvent.setCapacity(10);
        when(eventRepository.findWithKeywordsById(1L)).thenReturn(Optional.of(futureEvent));
        when(rsvpRepository.countByEvent(futureEvent)).thenReturn(10L);

        EventDetailsDTO result = eventService.getEventById(1L, null);

        assertNotNull(result);
        assertTrue(result.isEventFull());
        assertEquals(10, result.getAttendeeCount());
        assertEquals(10, result.getMaxAttendees());
    }

    @Test
    void getEventById_UnlimitedCapacity_NotFull() {
        futureEvent.setCapacity(null);
        when(eventRepository.findWithKeywordsById(1L)).thenReturn(Optional.of(futureEvent));
        when(rsvpRepository.countByEvent(futureEvent)).thenReturn(100L);

        EventDetailsDTO result = eventService.getEventById(1L, null);

        assertNotNull(result);
        assertFalse(result.isEventFull());
        assertEquals(100, result.getAttendeeCount());
        assertNull(result.getMaxAttendees());
    }

    @Test
    void getEventById_PaidEvent_SetsPaymentRequired() {
        futureEvent.setPrice(new BigDecimal("25.00"));
        when(eventRepository.findWithKeywordsById(1L)).thenReturn(Optional.of(futureEvent));
        when(rsvpRepository.countByEvent(futureEvent)).thenReturn(0L);

        EventDetailsDTO result = eventService.getEventById(1L, null);

        assertNotNull(result);
        assertTrue(result.isRequiresPayment());
        assertEquals(new BigDecimal("25.00"), result.getPrice());
    }

    @Test
    void getEventById_FreeEvent_NoPaymentRequired() {
        futureEvent.setPrice(BigDecimal.ZERO);
        when(eventRepository.findWithKeywordsById(1L)).thenReturn(Optional.of(futureEvent));
        when(rsvpRepository.countByEvent(futureEvent)).thenReturn(0L);

        EventDetailsDTO result = eventService.getEventById(1L, null);

        assertNotNull(result);
        assertFalse(result.isRequiresPayment());
    }

    @Test
    void getEventById_UserNotFoundById_DefaultsRSVPStatus() {
        when(eventRepository.findWithKeywordsById(1L)).thenReturn(Optional.of(futureEvent));
        when(rsvpRepository.countByEvent(futureEvent)).thenReturn(0L);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        EventDetailsDTO result = eventService.getEventById(1L, 99L);

        assertNotNull(result);
        assertFalse(result.isUserRsvpStatus());
        assertFalse(result.isUserBlockedStatus());
    }

    // ============== refundCancelledRSVP() ==============

    @Test
    void refundCancelledRSVP_NotFound_ThrowsException() throws StripeException {
        when(cancelledRSVPRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> eventService.refundCancelledRSVP(999L, 2L));

        verify(stripeService, never()).refundPayment(any(), any());
    }

    @Test
    void refundCancelledRSVP_Success_OrganizerProcessesRefund() throws StripeException {
        CancelledRSVP cancelledRsvp = new CancelledRSVP();
        cancelledRsvp.setId(1L);
        cancelledRsvp.setEvent(futureEvent);
        cancelledRsvp.setUser(testUser);
        cancelledRsvp.setPaymentStatus("paid");
        cancelledRsvp.setAmountPaid(new BigDecimal("25.00"));
        cancelledRsvp.setStripePaymentIntentId("pi_123");

        when(cancelledRSVPRepository.findById(1L)).thenReturn(Optional.of(cancelledRsvp));
        when(stripeService.refundPayment("pi_123", new BigDecimal("25.00"))).thenReturn("re_123");
        when(userRepository.findById(2L)).thenReturn(Optional.of(testCreator));

        eventService.refundCancelledRSVP(1L, 2L);

        assertEquals("refunded", cancelledRsvp.getRefundStatus());
        assertNotNull(cancelledRsvp.getRefundedAt());
        assertEquals("re_123", cancelledRsvp.getStripeRefundId());
        assertEquals(testCreator, cancelledRsvp.getRefundedBy());
        verify(cancelledRSVPRepository).save(cancelledRsvp);
    }

    @Test
    void refundCancelledRSVP_Success_AdminProcessesRefund() throws StripeException {
        User admin = new User();
        admin.setId(3L);
        admin.setUsername("admin");

        CancelledRSVP cancelledRsvp = new CancelledRSVP();
        cancelledRsvp.setId(1L);
        cancelledRsvp.setEvent(futureEvent);
        cancelledRsvp.setUser(testUser);
        cancelledRsvp.setPaymentStatus("paid");
        cancelledRsvp.setAmountPaid(new BigDecimal("25.00"));
        cancelledRsvp.setStripePaymentIntentId("pi_123");

        when(cancelledRSVPRepository.findById(1L)).thenReturn(Optional.of(cancelledRsvp));
        when(userService.hasRole(3L, "ROLE_ADMIN")).thenReturn(true);
        when(stripeService.refundPayment("pi_123", new BigDecimal("25.00"))).thenReturn("re_123");
        when(userRepository.findById(3L)).thenReturn(Optional.of(admin));

        eventService.refundCancelledRSVP(1L, 3L);

        assertEquals("refunded", cancelledRsvp.getRefundStatus());
        verify(cancelledRSVPRepository).save(cancelledRsvp);
    }

    @Test
    void refundCancelledRSVP_Failure_NonOrganizerNonAdmin() throws StripeException {
        User otherUser = new User();
        otherUser.setId(99L);

        CancelledRSVP cancelledRsvp = new CancelledRSVP();
        cancelledRsvp.setId(1L);
        cancelledRsvp.setEvent(futureEvent);
        cancelledRsvp.setUser(testUser);
        cancelledRsvp.setPaymentStatus("paid");

        when(cancelledRSVPRepository.findById(1L)).thenReturn(Optional.of(cancelledRsvp));
        when(userService.hasRole(99L, "ROLE_ADMIN")).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> eventService.refundCancelledRSVP(1L, 99L));

        verify(stripeService, never()).refundPayment(any(), any());
    }

    @Test
    void refundCancelledRSVP_Failure_PaymentStatusNull() throws StripeException {
        CancelledRSVP cancelledRsvp = new CancelledRSVP();
        cancelledRsvp.setId(1L);
        cancelledRsvp.setEvent(futureEvent);
        cancelledRsvp.setUser(testUser);
        cancelledRsvp.setPaymentStatus(null);

        when(cancelledRSVPRepository.findById(1L)).thenReturn(Optional.of(cancelledRsvp));

        assertThrows(IllegalStateException.class,
                () -> eventService.refundCancelledRSVP(1L, 2L));

        verify(stripeService, never()).refundPayment(any(), any());
    }

    @Test
    void refundCancelledRSVP_Failure_PaymentStatusPending() throws StripeException {
        CancelledRSVP cancelledRsvp = new CancelledRSVP();
        cancelledRsvp.setId(1L);
        cancelledRsvp.setEvent(futureEvent);
        cancelledRsvp.setUser(testUser);
        cancelledRsvp.setPaymentStatus("pending");

        when(cancelledRSVPRepository.findById(1L)).thenReturn(Optional.of(cancelledRsvp));

        assertThrows(IllegalStateException.class,
                () -> eventService.refundCancelledRSVP(1L, 2L));

        verify(stripeService, never()).refundPayment(any(), any());
    }

    @Test
    void refundCancelledRSVP_Failure_AlreadyRefunded() throws StripeException {
        CancelledRSVP cancelledRsvp = new CancelledRSVP();
        cancelledRsvp.setId(1L);
        cancelledRsvp.setEvent(futureEvent);
        cancelledRsvp.setUser(testUser);
        cancelledRsvp.setPaymentStatus("paid");
        cancelledRsvp.setRefundStatus("refunded");

        when(cancelledRSVPRepository.findById(1L)).thenReturn(Optional.of(cancelledRsvp));

        assertThrows(IllegalStateException.class,
                () -> eventService.refundCancelledRSVP(1L, 2L));

        verify(stripeService, never()).refundPayment(any(), any());
    }

    @Test
    void refundCancelledRSVP_StripeSuccess_UpdatesRecord() throws StripeException {
        CancelledRSVP cancelledRsvp = new CancelledRSVP();
        cancelledRsvp.setId(1L);
        cancelledRsvp.setEvent(futureEvent);
        cancelledRsvp.setUser(testUser);
        cancelledRsvp.setPaymentStatus("paid");
        cancelledRsvp.setAmountPaid(new BigDecimal("25.00"));
        cancelledRsvp.setStripePaymentIntentId("pi_123");

        when(cancelledRSVPRepository.findById(1L)).thenReturn(Optional.of(cancelledRsvp));
        when(stripeService.refundPayment("pi_123", new BigDecimal("25.00"))).thenReturn("re_123");
        when(userRepository.findById(2L)).thenReturn(Optional.of(testCreator));

        eventService.refundCancelledRSVP(1L, 2L);

        ArgumentCaptor<CancelledRSVP> captor = ArgumentCaptor.forClass(CancelledRSVP.class);
        verify(cancelledRSVPRepository).save(captor.capture());

        CancelledRSVP saved = captor.getValue();
        assertEquals("refunded", saved.getRefundStatus());
        assertEquals("re_123", saved.getStripeRefundId());
        assertNotNull(saved.getRefundedAt());
        assertEquals(testCreator, saved.getRefundedBy());
    }

    @Test
    void refundCancelledRSVP_StripeFailure_SetsFailedStatus() throws StripeException {
        CancelledRSVP cancelledRsvp = new CancelledRSVP();
        cancelledRsvp.setId(1L);
        cancelledRsvp.setEvent(futureEvent);
        cancelledRsvp.setUser(testUser);
        cancelledRsvp.setPaymentStatus("paid");
        cancelledRsvp.setAmountPaid(new BigDecimal("25.00"));
        cancelledRsvp.setStripePaymentIntentId("pi_123");

        when(cancelledRSVPRepository.findById(1L)).thenReturn(Optional.of(cancelledRsvp));
        when(stripeService.refundPayment("pi_123", new BigDecimal("25.00")))
                .thenThrow(new RuntimeException("Stripe API error"));

        assertThrows(RuntimeException.class,
                () -> eventService.refundCancelledRSVP(1L, 2L));

        ArgumentCaptor<CancelledRSVP> captor = ArgumentCaptor.forClass(CancelledRSVP.class);
        verify(cancelledRSVPRepository).save(captor.capture());

        CancelledRSVP saved = captor.getValue();
        assertEquals("failed", saved.getRefundStatus());
    }

    // ============== getCancelledRSVPs() ==============

    @Test
    void getCancelledRSVPs_EventNotFound_ThrowsException() {
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> eventService.getCancelledRSVPs(999L, 2L, PageRequest.of(0, 10)));
    }

    @Test
    void getCancelledRSVPs_Success_OrganizerViews() {
        CancelledRSVP cancelledRsvp = new CancelledRSVP();
        cancelledRsvp.setId(1L);
        cancelledRsvp.setUser(testUser);
        cancelledRsvp.setEvent(futureEvent);
        cancelledRsvp.setInitiatedBy("organiser");
        cancelledRsvp.setPaymentStatus("paid");
        cancelledRsvp.setAmountPaid(new BigDecimal("25.00"));
        cancelledRsvp.setRefundStatus("refunded");
        cancelledRsvp.setCancelledAt(LocalDateTime.now());
        cancelledRsvp.setRefundedAt(LocalDateTime.now());

        Page<CancelledRSVP> page = new PageImpl<>(Arrays.asList(cancelledRsvp));

        when(eventRepository.findById(1L)).thenReturn(Optional.of(futureEvent));
        when(cancelledRSVPRepository.findByEventIdWithUsersPaginated(eq(1L), any(Pageable.class)))
                .thenReturn(page);

        Page<CancelledRSVPDTO> result = eventService.getCancelledRSVPs(1L, 2L, PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(testUser.getId(), result.getContent().get(0).getUserId());
        assertEquals("organiser", result.getContent().get(0).getInitiatedBy());
    }

    @Test
    void getCancelledRSVPs_Success_AdminViews() {
        User admin = new User();
        admin.setId(3L);

        CancelledRSVP cancelledRsvp = new CancelledRSVP();
        cancelledRsvp.setId(1L);
        cancelledRsvp.setUser(testUser);
        cancelledRsvp.setEvent(futureEvent);
        cancelledRsvp.setInitiatedBy("organiser");

        Page<CancelledRSVP> page = new PageImpl<>(Arrays.asList(cancelledRsvp));

        when(eventRepository.findById(1L)).thenReturn(Optional.of(futureEvent));
        when(userService.hasRole(3L, "ROLE_ADMIN")).thenReturn(true);
        when(cancelledRSVPRepository.findByEventIdWithUsersPaginated(eq(1L), any(Pageable.class)))
                .thenReturn(page);

        Page<CancelledRSVPDTO> result = eventService.getCancelledRSVPs(1L, 3L, PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getCancelledRSVPs_Failure_NonOrganizerNonAdmin() {
        User otherUser = new User();
        otherUser.setId(99L);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(futureEvent));
        when(userService.hasRole(99L, "ROLE_ADMIN")).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> eventService.getCancelledRSVPs(1L, 99L, PageRequest.of(0, 10)));

        verify(cancelledRSVPRepository, never()).findByEventIdWithUsersPaginated(any(), any());
    }

    @Test
    void getCancelledRSVPs_EmptyResults_ReturnsEmptyPage() {
        Page<CancelledRSVP> emptyPage = new PageImpl<>(Collections.emptyList());

        when(eventRepository.findById(1L)).thenReturn(Optional.of(futureEvent));
        when(cancelledRSVPRepository.findByEventIdWithUsersPaginated(eq(1L), any(Pageable.class)))
                .thenReturn(emptyPage);

        Page<CancelledRSVPDTO> result = eventService.getCancelledRSVPs(1L, 2L, PageRequest.of(0, 10));

        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void getCancelledRSVPs_VerifiesPaginationParameters() {
        Page<CancelledRSVP> emptyPage = new PageImpl<>(Collections.emptyList());
        Pageable pageable = PageRequest.of(2, 20);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(futureEvent));
        when(cancelledRSVPRepository.findByEventIdWithUsersPaginated(eq(1L), any(Pageable.class)))
                .thenReturn(emptyPage);

        eventService.getCancelledRSVPs(1L, 2L, pageable);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(cancelledRSVPRepository).findByEventIdWithUsersPaginated(eq(1L), pageableCaptor.capture());

        Pageable captured = pageableCaptor.getValue();
        assertEquals(2, captured.getPageNumber());
        assertEquals(20, captured.getPageSize());
    }
}