package au.edu.rmit.sept.webapp.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import au.edu.rmit.sept.webapp.model.Category;
import au.edu.rmit.sept.webapp.model.Event;
import au.edu.rmit.sept.webapp.model.RSVP;
import au.edu.rmit.sept.webapp.model.User;
import au.edu.rmit.sept.webapp.model.BlockedRSVP;
import au.edu.rmit.sept.webapp.model.CancelledRSVP;
import au.edu.rmit.sept.webapp.repository.CategoryRepository;
import au.edu.rmit.sept.webapp.repository.EventRepository;
import au.edu.rmit.sept.webapp.repository.RSVPRepository;
import au.edu.rmit.sept.webapp.repository.UserRepository;
import au.edu.rmit.sept.webapp.repository.BlockedRSVPRepository;
import au.edu.rmit.sept.webapp.repository.CancelledRSVPRepository;
import au.edu.rmit.sept.webapp.repository.PaymentRepository;
import jakarta.persistence.EntityNotFoundException;
import au.edu.rmit.sept.webapp.dto.EventCardDTO;
import au.edu.rmit.sept.webapp.dto.EventDetailsDTO;
import au.edu.rmit.sept.webapp.dto.EventCreateDTO;
import au.edu.rmit.sept.webapp.dto.AttendeeDTO;
import au.edu.rmit.sept.webapp.dto.BlockedAttendeeDTO;
import au.edu.rmit.sept.webapp.dto.CancelledRSVPDTO;

import com.opencsv.CSVWriter;
import java.io.StringWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Set;

import au.edu.rmit.sept.webapp.service.KeywordService;
import au.edu.rmit.sept.webapp.dto.KeywordDTO;
import au.edu.rmit.sept.webapp.model.Keyword;

import com.opencsv.CSVWriter;
import java.io.StringWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

/**
 * EventService handles all business logic for event-related operations.
 * 
 * This service is the primary component responsible for:
 * - Converting Event entities to EventCardDTO display objects
 * - Calculating RSVP-related data (attendee counts, user status)
 * - Truncating descriptions to appropriate lengths for different display modes
 * - Determining event states (started, full, available)
 * - Handling both authenticated and anonymous user contexts
 * 
 * The service follows the typical Spring MVC data flow pattern:
 * 1. Controller receives request
 * 2. Controller calls Service method
 * 3. Service gets Model objects from Repository
 * 4. Service converts Model objects → EventCardDTO
 * 5. Controller adds EventCardDTO to model and returns view
 * 
 * @Transactional(readOnly = true) is applied at the class level for performance
 *                         optimization on read-only queries and to prevent
 *                         accidental writes in getter methods.
 */
@Service
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;
    private final RSVPRepository rsvpRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final KeywordService keywordService;
    private final UserService userService;
    private final BlockedRSVPRepository blockedRSVPRepository;
    private final CancelledRSVPRepository cancelledRSVPRepository;
    private final PaymentRepository paymentRepository;
    private final RSVPService rsvpService;
    private final StripeService stripeService;

    /**
     * Checks if a user has the ADMIN role.
     * @param userId The ID of the user to check
     * @return true if the user has the ADMIN role, false otherwise
     */
    private boolean isUserAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        return userService.hasRole(userId, "ROLE_ADMIN");
    }

    // Constructor injection - modern Spring best practice for immutability,
    // testability, and fail-fast behavior
    public EventService(EventRepository eventRepository, RSVPRepository rsvpRepository, UserRepository userRepository,
            CategoryRepository categoryRepository, KeywordService keywordService, UserService userService,
            BlockedRSVPRepository blockedRSVPRepository, CancelledRSVPRepository cancelledRSVPRepository,
            PaymentRepository paymentRepository, RSVPService rsvpService, StripeService stripeService) {
        this.eventRepository = eventRepository;
        this.rsvpRepository = rsvpRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.keywordService = keywordService;
        this.userService = userService;
        this.blockedRSVPRepository = blockedRSVPRepository;
        this.cancelledRSVPRepository = cancelledRSVPRepository;
        this.paymentRepository = paymentRepository;
        this.rsvpService = rsvpService;
        this.stripeService = stripeService;
    }

    // Update the existing overloaded method (currently has 3 parameters)
    public List<EventCardDTO> getUpcomingEvents(Long userId, Long categoryId, LocalDate fromDate) {
        Page<EventCardDTO> page = getUpcomingEvents(userId, categoryId, fromDate, null, null, PageRequest.of(0, 100));
        return page.getContent();
    }

    public List<EventCardDTO> getUpcomingEvents(Long userId) {
        Page<EventCardDTO> page = getUpcomingEvents(userId, null, null, null, null, PageRequest.of(0, 100));
        return page.getContent();
    }

    /**
     * Retrieves all upcoming events and converts them to EventCardDTO objects for
     * display.
     * 
     * This method handles the core logic for the home page event listing by:
     * - Fetching events that haven't started yet (using current date/time
     * filtering)
     * - Converting raw Event entities to display-ready EventCardDTO objects
     * - Supporting both authenticated and anonymous users through nullable userId
     * 
     * Method signature rationale:
     * - Returns List<EventCardDTO> instead of Page<EventCardDTO> because pagination
     * is
     * a repository concern, while service returns business objects
     * - Accepts Long userId parameter (nullable) to support both logged-in and
     * anonymous users
     * - For anonymous users (userId = null), userRsvpStatus will always be false
     * - For authenticated users, checks actual RSVP status per event
     * 
     * Pagination approach for MVP:
     * - Uses hardcoded PageRequest.of(0, 100) to prevent unbounded queries
     * - 100 events is reasonable for MVP home page display
     * - Can easily add overloaded method with Pageable parameter later if needed
     * - Keeps the interface simple while laying groundwork for future pagination
     * features
     * 
     * @param userId The ID of the currently authenticated user, or null for
     *               anonymous users
     * @return List of EventCardDTO objects containing all display data for the home
     *         page
     */
    public Page<EventCardDTO> getUpcomingEvents(Long userId, Long categoryId, LocalDate fromDate,
            Set<Long> keywordIds, String searchTerm, Pageable pageable) {
        try {
            LocalDate currentDate = LocalDate.now();
            LocalTime currentTime = LocalTime.now();

            // Use fromDate if provided, otherwise use current date
            LocalDate filterDate = fromDate != null ? fromDate : currentDate;

            Page<Event> events;

            // Determine which filtering method to use based on provided parameters
            boolean hasSearch = searchTerm != null && !searchTerm.trim().isEmpty();
            boolean hasKeywords = keywordIds != null && !keywordIds.isEmpty();
            boolean hasCategory = categoryId != null;

            if (hasSearch) {
                // Search functionality - use search repository methods with explicit sorting
                Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), 
                                                        Sort.by("eventDate").ascending().and(Sort.by("eventTime").ascending()));
                
                if (hasKeywords && hasCategory) {
                    // Search with both category and keyword filtering
                    events = eventRepository.searchUpcomingEventsByCategoryAndKeywords(
                            searchTerm.trim(), categoryId, keywordIds, filterDate, currentTime, sortedPageable);
                } else if (hasKeywords) {
                    // Search with keyword filtering only
                    events = eventRepository.searchUpcomingEventsByKeywords(
                            searchTerm.trim(), keywordIds, filterDate, currentTime, sortedPageable);
                } else if (hasCategory) {
                    // Search with category filtering only
                    events = eventRepository.searchUpcomingEventsByCategory(
                            searchTerm.trim(), categoryId, filterDate, currentTime, pageable);
                } else {
                    // Search only (no other filters)
                    events = eventRepository.searchUpcomingEvents(
                            searchTerm.trim(), filterDate, currentTime, pageable);
                }
            } else {
                // No search - use existing filter logic
                if (hasKeywords && hasCategory) {
                    // Both keyword and category filtering
                    // Apply keyword filter first, then filter by category in memory
                    
                    // OR binding (current implementation) - events with ANY of the selected keywords
                    events = eventRepository.findUpcomingEventsByKeywordsOr(
                            keywordIds, filterDate, currentTime, pageable);

                    // Filter by category in memory (simple approach for MVP)
                    List<Event> eventList = events.getContent().stream()
                            .filter(event -> event.getCategory() != null && event.getCategory().getId().equals(categoryId))
                            .collect(Collectors.toList());

                    events = new PageImpl<>(eventList);

                } else if (hasKeywords) {
                    // Only keyword filtering
                    
                    // OR binding (current implementation) - events with ANY of the selected keywords
                    events = eventRepository.findUpcomingEventsByKeywordsOr(
                            keywordIds, filterDate, currentTime, pageable);

                } else if (hasCategory) {
                    // Only category filtering (existing logic)
                    events = eventRepository.findUpcomingEventsByCategory(
                            categoryId, filterDate, currentTime, pageable);

                } else {
                    // No specific filtering, just date (existing logic)
                    events = eventRepository.findUpcomingEvents(
                            filterDate, currentTime, pageable);
                }
            }

            if (events == null || events.isEmpty()) {
                return new PageImpl<>(new ArrayList<>(), pageable, 0);
            }

            // Convert to DTOs (existing logic)
            List<EventCardDTO> result = events.stream()
                    .filter(event -> event != null)
                    .map(event -> mapToEventCardDTO(event, userId))
                    .filter(dto -> dto != null)
                    .collect(Collectors.toList());

            return new PageImpl<>(result, pageable, events.getTotalElements());

        } catch (Exception e) {
            System.out.println("DEBUG: Exception in getUpcomingEvents: " + e.getMessage());
            e.printStackTrace();
            return new PageImpl<>(new ArrayList<>(), pageable, 0);
        }
    }

    /**
     * Retrieves recommended upcoming events based on a logged-in user's interested
     * categories.
     *
     * This method:
     * - Returns an empty list for anonymous users or users with no specified
     * interests.
     * - Excludes events the user has created themselves.
     * - Limits the results to a reasonable number for a carousel display.
     *
     * @param userId The ID of the currently authenticated user, or null.
     * @return A List of EventCardDTOs tailored to the user's interests.
     */
    public List<EventCardDTO> getRecommendedEvents(Long userId) {
        // Handle anonymous users - they don't get recommendations
        if (userId == null) {
            return new ArrayList<>();
        }

        // Find the user and their interested categories
        User user = userRepository.findById(userId).orElse(null);

        // If user not found or they have no interests, return an empty list
        if (user == null || user.getCategories() == null || user.getCategories().isEmpty()) {
            return new ArrayList<>();
        }
        Set<Category> interestedCategories = user.getCategories();

        // Define a limit for the carousel (e.g., 5 events)
        Pageable limit = PageRequest.of(0, 5);

        // Call the repository method to find the events
        Page<Event> recommendedEventsPage = eventRepository.findRecommendedUpcomingEvents(
                interestedCategories,
                LocalDate.now(),
                LocalTime.now(),
                user,
                limit);

        // Convert the found Event entities to DTOs and return the list
        return recommendedEventsPage.stream()
                .map(event -> mapToEventCardDTO(event, userId))
                .collect(Collectors.toList());
    }

    // // Overloaded method for pagination
    // public List<EventCardDTO> getUpcomingEvents(Long userId, int page, int size)
    // {
    // Page<Event> events = eventRepository.findUpcomingEvents(
    // LocalDate.now(), LocalTime.now(),
    // PageRequest.of(page, size));
    // // Still return List, not Page
    // return events.stream()
    // .map(event -> mapToEventCardDTO(event, userId))
    // .collect(Collectors.toList());
    // }

    /**
     * Maps an Event entity to an EventCardDTO with all necessary display data and
     * calculations.
     * 
     * This private helper method handles the complex transformation from database
     * entities
     * to display-ready DTOs by performing several critical operations:
     * 
     * 1. Basic field mapping (title, date, time, location)
     * 2. Description truncation for different display contexts
     * 3. Category relationship data extraction
     * 4. RSVP count calculations via repository queries
     * 5. Event state determinations (full, started, available)
     * 6. User-specific RSVP status checking
     * 
     * The method abstracts away all the business logic complexity so that
     * controllers
     * and templates only deal with clean, ready-to-display data objects.
     * 
     * @param event  The Event entity from the database
     * @param userId The current user's ID (null for anonymous users)
     * @return EventCardDTO containing all data needed for card display
     */
    private EventCardDTO mapToEventCardDTO(Event event, Long userId) {
        if (event == null) {
            System.out.println("DEBUG: mapToEventCardDTO received null event");
            return null;
        }

        try {
            EventCardDTO dto = new EventCardDTO();

            // Map basic event fields directly from entity
            dto.setEventId(event.getId());
            dto.setTitle(event.getTitle());
            dto.setEventDate(event.getEventDate());
            dto.setEventTime(event.getEventTime());
            dto.setLocation(event.getLocation());

            // Truncate descriptions for different display contexts
            // EventCardDTO supports two description fields:
            // - briefDescription (50 chars): Used in compact mode (carousel cards)
            // - description (100 chars): Used in full mode (main event grid)
            // This allows the same DTO to serve both display contexts efficiently
            dto.setBriefDescription(truncate(event.getDescription(), 50));
            dto.setDescription(truncate(event.getDescription(), 100));

            // Extract category information from @ManyToOne relationship
            // This avoids N+1 queries by leveraging JPA lazy loading efficiently
            Category category = event.getCategory();
            if (category == null) {
                dto.setCategoryName("Unknown");
                dto.setCategoryColor("#666666");
            } else {
                dto.setCategoryName(category.getName());
                dto.setCategoryColor(category.getColourCode());
            }
            // Extract creator username
            User creator = event.getCreatedBy();
            if (creator != null) {
                dto.setCreatorUsername(creator.getUsername());
            } else {
                dto.setCreatorUsername("Unknown");
            }

            // Calculate RSVP counts using repository methods
            // These methods will be implemented in RSVPRepository to handle:
            // - countByEvent(event): Returns total number of RSVPs for this event
            // - Cleaner approach than fetching all RSVP entities and counting in memory
            // - Avoids N+1 problem by using proper COUNT queries
            // TODO: Temporarily hardcoded for testing until RSVP functionality is
            // implemented
            Long attendeeCount = rsvpRepository.countByEvent(event);
            dto.setAttendeeCount(attendeeCount.intValue());
            dto.setMaxAttendees(event.getCapacity());

            // Calculate event states for UI display logic
            // isEventFull: Compares current attendee count with capacity limit
            // - Handles null capacity (unlimited events) by treating them as never full
            // - Used by template to show "Full" badge or disable RSVP button
            dto.setEventFull(event.getCapacity() != null && attendeeCount >= event.getCapacity().longValue());
            dto.setEventStarted(isEventStarted(event));

            // Determine user-specific RSVP status for personalized UI
            // - If userId is null (anonymous user), always returns false
            // - If userId is provided, checks if user has an active RSVP for this event
            // - Used by template to show "RSVP" vs "Cancel RSVP" button states
            if (userId != null) {
                String currentUsername = userRepository.findById(userId)
                        .map(User::getUsername)
                        .orElse(null);
                if (currentUsername != null) {
                    boolean isGoing = rsvpRepository.existsByUser_UsernameAndEvent_Id(currentUsername, event.getId());
                    dto.setUserRsvpStatus(isGoing);
                } else {
                    dto.setUserRsvpStatus(false);
                }

                // Determine if user is the organiser of this event
                dto.setOrganiser(event.getCreatedBy() != null && event.getCreatedBy().getId().equals(userId));
            } else {
                dto.setUserRsvpStatus(false);
                dto.setOrganiser(false);
            }

            // Map keywords to DTOs
            if (event.getKeywords() != null && !event.getKeywords().isEmpty()) {
                List<KeywordDTO> keywordDTOs = event.getKeywords().stream()
                        .map(k -> new KeywordDTO(k.getId(), k.getName(), k.getColor()))
                        .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                        .collect(Collectors.toList());
                dto.setKeywords(keywordDTOs);
            } else {
                dto.setKeywords(new ArrayList<>());
            }

            return dto;
        } catch (Exception e) {
            System.out.println(
                    "DEBUG: Exception in mapToEventCardDTO for event " + event.getId() + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Truncates text to a specified maximum length with ellipsis if needed.
     * 
     * This utility method handles description truncation for different display
     * contexts:
     * - Compact mode cards need brief descriptions (50 characters)
     * - Full mode cards use longer descriptions (100 characters)
     * 
     * The truncation logic:
     * - Returns empty string for null input (defensive programming)
     * - If text fits within limit, returns original text unchanged
     * - If text exceeds limit, truncates to (maxLength - 3) and appends "..."
     * - Subtracts 3 from maxLength to account for ellipsis characters
     * 
     * @param text      The original text to potentially truncate
     * @param maxLength Maximum allowed length including ellipsis
     * @return Truncated text with ellipsis, or original text if short enough
     */
    private String truncate(String text, int maxLength) {
        if (text == null)
            return "";
        return text.length() <= maxLength ? text : text.substring(0, maxLength - 3) + "...";
    }

    /**
     * Determines if an event has already started based on current date and time.
     * 
     * This method performs precise date/time comparison to determine event state:
     * - Combines separate eventDate and eventTime fields into single LocalDateTime
     * - Compares against current system date/time using LocalDateTime.now()
     * - Returns true if the event's start time has passed
     * 
     * Used for UI logic to:
     * - Disable RSVP functionality for started events
     * - Show "Event Started" badge instead of RSVP buttons
     * - Prevent new RSVPs after event has begun
     * 
     * @param event The event to check for start status
     * @return true if event has started, false if it's still upcoming
     */
    private boolean isEventStarted(Event event) {
        LocalDateTime eventDateTime = LocalDateTime.of(event.getEventDate(), event.getEventTime());
        return eventDateTime.isBefore(LocalDateTime.now());
    }

    /**
     * Retrieves a single event by ID with complete details for the view page.
     * Returns EventDetailsDTO with full description and creator information.
     * 
     * @param eventId The event ID to fetch
     * @param userId  The current user's ID (null for anonymous)
     * @return EventDetailsDTO with full event details
     * @throws EntityNotFoundException if event doesn't exist
     */
    public EventDetailsDTO getEventById(Long eventId, Long userId) {
        Event event = eventRepository.findWithKeywordsById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found with id: " + eventId));

        return mapToEventDetailsDTO(event, userId);
    }

    /**
     * Maps an Event entity to EventDetailsDTO with complete information.
     * Unlike mapToEventCardDTO, this includes full descriptions and creator
     * details.
     * 
     * @param event  The Event entity
     * @param userId The current user's ID (null for anonymous)
     * @return EventDetailsDTO with all event details
     */
    private EventDetailsDTO mapToEventDetailsDTO(Event event, Long userId) {
        EventDetailsDTO dto = new EventDetailsDTO();

        // Basic event fields
        dto.setEventId(event.getId());
        dto.setTitle(event.getTitle());
        dto.setFullDescription(event.getDescription()); // Full description, no truncation
        dto.setEventDate(event.getEventDate());
        dto.setEventTime(event.getEventTime());
        dto.setLocation(event.getLocation());

        // Category information
        Category category = event.getCategory();
        if (category != null) {
            dto.setCategoryName(category.getName());
            dto.setCategoryColor(category.getColourCode());
        } else {
            dto.setCategoryName("Unknown");
            dto.setCategoryColor("#666666");
        }

        // Creator information
        User creator = event.getCreatedBy();
        if (creator != null) {
            dto.setCreatedByUsername(creator.getUsername());
            dto.setCreatedById(creator.getId());
        }
        dto.setCreatedAt(event.getCreatedAt());

        // RSVP and capacity calculations
        Long attendeeCount = rsvpRepository.countByEvent(event);
        dto.setAttendeeCount(attendeeCount.intValue());
        dto.setMaxAttendees(event.getCapacity());

        // Event states
        dto.setEventFull(event.getCapacity() != null && attendeeCount >= event.getCapacity().longValue());
        dto.setEventStarted(isEventStarted(event));

        // Payment information
        dto.setPrice(event.getPrice());
        dto.setRequiresPayment(event.getPrice() != null && event.getPrice().compareTo(BigDecimal.ZERO) > 0);

        // User RSVP status and blocked status
        if (userId != null) {
            String currentUsername = userRepository.findById(userId)
                    .map(User::getUsername)
                    .orElse(null);
            if (currentUsername != null) {
                boolean isGoing = rsvpRepository.existsByUser_UsernameAndEvent_Id(currentUsername, event.getId());
                dto.setUserRsvpStatus(isGoing);
                // Check if user is blocked from this event
                dto.setUserBlockedStatus(blockedRSVPRepository.existsByEvent_IdAndUser_Id(
                        event.getId(), userId));

                // Get user's RSVP payment status and ID if they have RSVP'd
                if (isGoing) {
                    rsvpRepository.findByUser_UsernameAndEvent_Id(currentUsername, event.getId())
                            .ifPresent(rsvp -> {
                                dto.setUserPaymentStatus(rsvp.getPaymentStatus());
                                dto.setUserRsvpId(rsvp.getId());
                            });
                }
            } else {
                dto.setUserRsvpStatus(false);
                dto.setUserBlockedStatus(false);
            }
        } else {
            dto.setUserRsvpStatus(false);
            dto.setUserBlockedStatus(false);
        }

        // Populate attendee list for authenticated users only
        // MVP: Fetch all attendees (no pagination for simplicity)
        if (userId != null) { // Only fetch if user is authenticated
            List<RSVP> rsvps = rsvpRepository.findByEventIdWithUsers(event.getId());

            // Convert to AttendeeDTO objects
            List<EventDetailsDTO.AttendeeDTO> attendeeList = rsvps.stream()
                    .map(rsvp -> new EventDetailsDTO.AttendeeDTO(
                            rsvp.getUser().getId(),
                            rsvp.getUser().getUsername(),
                            rsvp.getRsvpDate()))
                    .collect(Collectors.toList());

            dto.setAttendees(attendeeList);
        } else {
            // Anonymous users get empty list (template will hide anyway)
            dto.setAttendees(new ArrayList<>());
        }

        if (event.getKeywords() != null && !event.getKeywords().isEmpty()) {
            List<KeywordDTO> keywordDTOs = event.getKeywords().stream()
                    .map(k -> new KeywordDTO(k.getId(), k.getName(), k.getColor()))
                    .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                    .collect(Collectors.toList());
            dto.setKeywords(keywordDTOs);
        } else {
            dto.setKeywords(new ArrayList<>());
        }

        return dto;
    }

    /**
     * Creates a new event from the provided DTO.
     * 
     * This method handles the complete event creation workflow:
     * - Validates that the event date/time is in the future
     * - Converts DTO to Event entity
     * - Sets the creator to the current user
     * - Handles unlimited capacity (null value)
     * - Persists the event to database
     * 
     * @param dto       The event creation data from the form
     * @param createdBy The user creating the event
     * @return The ID of the newly created event
     * @throws IllegalArgumentException if event date/time is in the past
     * @throws EntityNotFoundException  if category doesn't exist
     */
    @Transactional
    public Long createEvent(EventCreateDTO dto, User createdBy) {
        // Validate event is in the future
        LocalDateTime eventDateTime = dto.getEventDateTime();
        if (eventDateTime == null || eventDateTime.isBefore(LocalDateTime.now().minusMinutes(5))) {
            throw new IllegalArgumentException("Event date and time must be in the future");
        }

        // Fetch the category
        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid category selected"));

        // Create new Event entity
        Event event = new Event();
        event.setTitle(dto.getTitle());
        event.setDescription(dto.getDescription());
        event.setEventDate(dto.getEventDate());
        event.setEventTime(dto.getEventTime());
        event.setLocation(dto.getLocation());

        // Handle capacity - null for unlimited
        if (dto.isUnlimitedCapacity()) {
            event.setCapacity(null);
        } else {
            event.setCapacity(dto.getCapacity());
        }

        event.setCreatedBy(createdBy);
        event.setCategory(category);
        // createdAt is handled by @CreationTimestamp

        Set<Keyword> keywords = keywordService.processKeywordSelection(
                dto.getKeywordIds(),
                dto.getCustomKeywords());
        event.setKeywords(keywords);

        // Save and return event ID
        Event savedEvent = eventRepository.save(event);
        return savedEvent.getId();
    }

    /**
     * Deletes an event if the user is the creator and the event hasn't started.
     * 
     * This method handles:
     * - Validation that the user is the event creator
     * - Validation that the event hasn't started yet
     * - Cascade deletion of RSVPs first, then the event
     * - Transaction management for data consistency
     * 
     * @param eventId The ID of the event to delete
     * @param userId  The ID of the user attempting to delete
     * @throws EntityNotFoundException if event doesn't exist
     * @throws AccessDeniedException   if user is not creator or event has started
     */
    @Transactional
    public void deleteEvent(Long eventId, Long userId) {
        // Fetch the event
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found with id: " + eventId));

        // Check if user is the creator or an admin
        if (!event.getCreatedBy().getId().equals(userId) && !isUserAdmin(userId)) {
            throw new AccessDeniedException("You can only delete events you created or if you are an admin");
        }

        // Check if event has already started
        if (isEventStarted(event)) {
            throw new AccessDeniedException("Cannot delete an event that has already started");
        }

        // Delete associated RSVPs first (only if event is active - preserves RSVPs for deactivated events)
        rsvpRepository.deleteByActiveEvent(event.getId());

        // Delete the event (only if active - deactivated events cannot be deleted by organizers)
        eventRepository.deleteActiveEventById(event.getId());
    }

    /**
     * Retrieves events that the authenticated user has RSVP'd to.
     * 
     * This method fetches all upcoming events where the user has an active RSVP
     * but is not the event organizer. It follows the same DTO mapping pattern
     * as other event retrieval methods.
     * 
     * @param userId The ID of the authenticated user
     * @return List of EventCardDTO objects for events the user has RSVP'd to
     */
    public List<EventCardDTO> getUserRSVPEvents(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }

        try {
            // Fetch events from RSVP repository where user has RSVP'd
            List<Event> rsvpEvents = rsvpRepository.findUpcomingEventsByUserId(userId);

            if (rsvpEvents == null || rsvpEvents.isEmpty()) {
                return new ArrayList<>();
            }

            // Map Event entities to EventCardDTO using existing mapping logic
            List<EventCardDTO> result = rsvpEvents.stream()
                    .filter(event -> event != null)
                    .map(event -> mapToEventCardDTO(event, userId))
                    .filter(dto -> dto != null)
                    .collect(Collectors.toList());

            return result;

        } catch (Exception e) {
            System.out.println("DEBUG: Exception in getUserRSVPEvents: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Retrieves events that the authenticated user has created.
     * 
     * This method fetches all upcoming events where the user is the organizer.
     * It follows the same DTO mapping pattern as other event retrieval methods.
     * 
     * @param userId The ID of the authenticated user
     * @return List of EventCardDTO objects for events the user has created
     */
    public List<EventCardDTO> getUserCreatedEvents(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }

        try {
            LocalDate currentDate = LocalDate.now();
            LocalTime currentTime = LocalTime.now();

            // Fetch events from EventRepository where user is the creator
            Page<Event> createdEvents = eventRepository.findUpcomingEventsByCreatedBy(
                    userId, currentDate, currentTime, PageRequest.of(0, 30));

            if (createdEvents == null || createdEvents.isEmpty()) {
                return new ArrayList<>();
            }

            // Map Event entities to EventCardDTO using existing mapping logic
            List<EventCardDTO> result = createdEvents.stream()
                    .filter(event -> event != null)
                    .map(event -> mapToEventCardDTO(event, userId))
                    .filter(dto -> dto != null)
                    .collect(Collectors.toList());

            return result;

        } catch (Exception e) {
            System.out.println("DEBUG: Exception in getUserCreatedEvents: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    /**
     * Fetches paginated list of attendees for an event - ORGANISER ONLY.
     * Verifies the requesting user is the event organiser before returning data.
     * 
     * @param eventId     The event ID
     * @param organizerId The user ID of the requester (must be event organiser)
     * @param pageable    Pagination parameters
     * @return Page of AttendeeDTO objects
     * @throws AccessDeniedException if user is not the event organiser
     */
    @Transactional(readOnly = true)
    public Page<AttendeeDTO> getEventAttendeesAsOrganiser(Long eventId, Long organizerId, Pageable pageable) {
        return getEventAttendeesAsOrganiser(eventId, organizerId, "", pageable);
    }

    @Transactional(readOnly = true)
    public Page<AttendeeDTO> getEventAttendeesAsOrganiser(Long eventId, Long organizerId, String searchTerm, Pageable pageable) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found"));

        // Verify the user is the event organiser or an admin
        if (!event.getCreatedBy().getId().equals(organizerId) && !isUserAdmin(organizerId)) {
            throw new AccessDeniedException("Only the event organiser or admin can view detailed attendee list");
        }

        // Get paginated RSVPs (with optional search)
        Page<RSVP> rsvps;
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            rsvps = rsvpRepository.searchEventAttendees(eventId, searchTerm.trim(), pageable);
        } else {
            rsvps = rsvpRepository.findByEventIdWithUsersPaginated(eventId, pageable);
        }

        // Convert to DTOs
        List<AttendeeDTO> attendees = rsvps.getContent().stream()
                .map(rsvp -> new AttendeeDTO(
                        rsvp.getUser().getId(),
                        rsvp.getUser().getUsername(),
                        rsvp.getUser().getEmail(),
                        rsvp.getRsvpDate(),
                        rsvp.getEvent().getPrice(),
                        rsvp.getPaymentStatus()))
                .collect(Collectors.toList());

        return new PageImpl<>(attendees, pageable, rsvps.getTotalElements());
    }

    /**
     * Cancels/blocks an attendee's RSVP - ORGANISER ONLY.
     * Allows event organiser to remove an attendee from their event.
     * 
     * @param eventId        The event ID
     * @param attendeeUserId The user ID of the attendee to remove
     * @param organizerId    The user ID of the requester (must be event organiser)
     * @throws AccessDeniedException if user is not the event organiser
     */
    @Transactional
    public void cancelAttendeeRsvpAsOrganiser(Long eventId, Long attendeeUserId, Long organizerId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found"));

        User attendee = userRepository.findById(attendeeUserId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        User organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new EntityNotFoundException("Organizer not found"));

        // Verify the user is the event organiser or an admin
        if (!event.getCreatedBy().getId().equals(organizerId) && !isUserAdmin(organizerId)) {
            throw new AccessDeniedException("Only the event organiser or admin can cancel attendee RSVPs");
        }

        // Find RSVP
        RSVP rsvp = rsvpRepository.findByUserAndEvent(attendee, event)
                .orElseThrow(() -> new EntityNotFoundException("RSVP not found"));

        // If paid event, create cancelled RSVP record
        if (event.getRequiresPayment() && rsvp.getPaymentStatus() != null) {
            CancelledRSVP cancelledRsvp = new CancelledRSVP();
            cancelledRsvp.setRsvpId(rsvp.getId());
            cancelledRsvp.setUser(attendee);
            cancelledRsvp.setEvent(event);
            cancelledRsvp.setInitiatedBy("organiser");
            cancelledRsvp.setCancelledBy(organizer);
            cancelledRsvp.setPaymentStatus(rsvp.getPaymentStatus());
            cancelledRsvp.setAmountPaid(rsvp.getAmountPaid());
            cancelledRsvp.setStripePaymentIntentId(rsvp.getStripePaymentIntentId());
            cancelledRSVPRepository.save(cancelledRsvp);
        }

        // Delete associated payment records first to avoid FK constraint violation
        paymentRepository.deleteByRsvp(rsvp);

        // Delete RSVP
        rsvpRepository.delete(rsvp);
    }

    /**
     * Exports event attendees to CSV format - ORGANISER ONLY.
     * Generates a CSV file with attendee information for event check-in and record
     * keeping.
     * 
     * @param eventId     The event ID
     * @param organizerId The user ID of the requester (must be event organiser)
     * @return CSV file content as byte array
     * @throws AccessDeniedException if user is not the event organiser
     * @throws IOException           if CSV generation fails
     */
    @Transactional(readOnly = true)
    public byte[] exportAttendeesToCSVAsOrganiser(Long eventId, Long organizerId) throws IOException {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found"));

        // Verify the user is the event organiser or an admin
        if (!event.getCreatedBy().getId().equals(organizerId) && !isUserAdmin(organizerId)) {
            throw new AccessDeniedException("Only the event organiser or admin can export attendee list");
        }

        // Fetch all attendees (unpaged)
        Page<RSVP> rsvps = rsvpRepository.findByEventIdWithUsersPaginated(eventId, Pageable.unpaged());

        // Create CSV
        StringWriter stringWriter = new StringWriter();
        try (CSVWriter csvWriter = new CSVWriter(stringWriter)) {
            // Add event title as metadata
            csvWriter.writeNext(new String[] { "Event:", event.getTitle(), "" });
            csvWriter.writeNext(new String[] { "Export Date:",
                    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), "" });
            csvWriter.writeNext(new String[] { "Total Attendees:", String.valueOf(rsvps.getTotalElements()), "" });

            // Empty row for separation
            csvWriter.writeNext(new String[] {});

            // Add headers
            csvWriter.writeNext(new String[] { "Username", "Email", "RSVP Date" });

            // Add attendee data
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            for (RSVP rsvp : rsvps.getContent()) {
                csvWriter.writeNext(new String[] {
                        rsvp.getUser().getUsername(),
                        rsvp.getUser().getEmail(),
                        rsvp.getRsvpDate().format(formatter)
                });
            }
        }

        return stringWriter.toString().getBytes("UTF-8");
    }

    /**
     * Gets the title of an event by ID.
     * 
     * @param eventId The event ID
     * @return The event title, or "event" if not found
     */
    public String getEventTitle(Long eventId) {
        return eventRepository.findById(eventId)
                .map(Event::getTitle)
                .orElse("event");
    }

    /**
     * Sanitizes event title for use in filename.
     * Removes special characters and limits length.
     * 
     * @param title The original event title
     * @return Sanitized title safe for filenames
     */
    public String sanitizeFilename(String title) {
        if (title == null || title.isEmpty()) {
            return "event";
        }
        // Replace non-alphanumeric characters with hyphens
        String sanitized = title.replaceAll("[^a-zA-Z0-9\\s]", "")
                .trim()
                .replaceAll("\\s+", "-")
                .toLowerCase();
        // Limit to 50 characters
        if (sanitized.length() > 50) {
            sanitized = sanitized.substring(0, 50);
        }
        return sanitized.isEmpty() ? "event" : sanitized;
    }

    /**
     * Retrieves past events for display on the home page.
     * Returns events in reverse chronological order (most recent first).
     * No search or filter functionality for MVP.
     * 
     * @param userId   The current user's ID (null for anonymous)
     * @param pageable Pagination parameters
     * @return Page of EventCardDTO objects for past events
     */
    public Page<EventCardDTO> getPastEvents(Long userId, Pageable pageable) {
        try {
            LocalDate currentDate = LocalDate.now();
            LocalTime currentTime = LocalTime.now();

            Page<Event> events = eventRepository.findPastEventsForDisplay(
                    currentDate, currentTime, pageable);

            if (events == null || events.isEmpty()) {
                return new PageImpl<>(new ArrayList<>(), pageable, 0);
            }

            // Convert to DTOs using existing mapToEventCardDTO method
            List<EventCardDTO> result = events.stream()
                    .filter(event -> event != null)
                    .map(event -> mapToEventCardDTO(event, userId))
                    .filter(dto -> dto != null)
                    .collect(Collectors.toList());

            return new PageImpl<>(result, pageable, events.getTotalElements());

        } catch (Exception e) {
            System.out.println("DEBUG: Exception in getPastEvents: " + e.getMessage());
            e.printStackTrace();
            return new PageImpl<>(new ArrayList<>(), pageable, 0);
        }
    }

    /**
     * Gets paginated list of blocked users for an event - ORGANIZER/ADMIN ONLY.
     * Verifies the requesting user is the event organizer or admin before returning data.
     *
     * @param eventId     The event ID
     * @param organizerId The user ID of the requester (must be event organizer or admin)
     * @param pageable    Pagination parameters
     * @return Page of BlockedAttendeeDTO objects
     * @throws AccessDeniedException if user is not the event organizer or admin
     */
    @Transactional(readOnly = true)
    public Page<BlockedAttendeeDTO> getBlockedUsersAsOrganiser(Long eventId, Long organizerId, Pageable pageable) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found"));

        // Verify the user is the event organizer or an admin
        if (!event.getCreatedBy().getId().equals(organizerId) && !isUserAdmin(organizerId)) {
            throw new AccessDeniedException("Only the event organizer or admin can view blocked users");
        }

        // Get paginated blocked users
        Page<BlockedRSVP> blockedRSVPs = blockedRSVPRepository.findByEventIdWithUsersPaginated(eventId, pageable);

        // Convert to DTOs
        List<BlockedAttendeeDTO> blockedUsers = blockedRSVPs.getContent().stream()
                .map(blocked -> new BlockedAttendeeDTO(
                        blocked.getUser().getId(),
                        blocked.getUser().getUsername(),
                        blocked.getUser().getEmail(),
                        blocked.getBlockedDate(),
                        blocked.getBlockedBy().getUsername()))
                .collect(Collectors.toList());

        return new PageImpl<>(blockedUsers, pageable, blockedRSVPs.getTotalElements());
    }

    /**
     * Blocks an attendee's RSVP - ORGANIZER/ADMIN ONLY.
     * Allows event organizer or admin to block an attendee from the event.
     * This deletes their RSVP and prevents future RSVPs.
     *
     * @param eventId        The event ID
     * @param attendeeUserId The user ID of the attendee to block
     * @param organizerId    The user ID of the requester (must be event organizer or admin)
     * @throws AccessDeniedException if user is not the event organizer or admin
     */
    @Transactional
    public void blockAttendeeAsOrganiser(Long eventId, Long attendeeUserId, Long organizerId) {
        rsvpService.blockUserFromEventAsOrganiser(eventId, attendeeUserId, organizerId);
    }

    /**
     * Unblocks a user - ORGANIZER/ADMIN ONLY.
     * Allows event organizer or admin to unblock a user from the event.
     * This removes the block but does not automatically recreate their RSVP.
     *
     * @param eventId     The event ID
     * @param userId      The user ID to unblock
     * @param organizerId The user ID of the requester (must be event organizer or admin)
     * @throws AccessDeniedException if user is not the event organizer or admin
     */
    @Transactional
    public void unblockUserAsOrganiser(Long eventId, Long userId, Long organizerId) {
        rsvpService.unblockUserFromEventAsOrganiser(eventId, userId, organizerId);
    }

    /**
     * Get cancelled RSVPs for an event - ORGANISER ONLY.
     *
     * @param eventId The event ID
     * @param userId The user ID of the requester (must be event organizer or admin)
     * @param pageable Pagination parameters
     * @return Paginated list of cancelled RSVPs
     * @throws AccessDeniedException if user is not the event organizer or admin
     */
    @Transactional(readOnly = true)
    public Page<CancelledRSVPDTO> getCancelledRSVPs(Long eventId, Long userId, Pageable pageable) {
        // Verify user is organizer or admin
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found"));

        if (!event.getCreatedBy().getId().equals(userId) && !isUserAdmin(userId)) {
            throw new AccessDeniedException("Only the event organizer or admin can view cancelled RSVPs");
        }

        // Fetch cancelled RSVPs
        Page<CancelledRSVP> cancelledRsvps = cancelledRSVPRepository
                .findByEventIdWithUsersPaginated(eventId, pageable);

        // Convert to DTOs
        List<CancelledRSVPDTO> dtos = cancelledRsvps.getContent().stream()
                .map(cr -> new CancelledRSVPDTO(
                        cr.getId(),
                        cr.getUser().getId(),
                        cr.getUser().getUsername(),
                        cr.getUser().getEmail(),
                        cr.getCancelledAt(),
                        cr.getInitiatedBy(),
                        cr.getPaymentStatus(),
                        cr.getAmountPaid(),
                        cr.getRefundStatus(),
                        cr.getRefundedAt()))
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, cancelledRsvps.getTotalElements());
    }

    /**
     * Process refund for a cancelled RSVP - ORGANISER ONLY.
     *
     * @param cancelledRsvpId The cancelled RSVP ID
     * @param organizerId The user ID of the requester (must be event organizer or admin)
     * @throws AccessDeniedException if user is not the event organizer or admin
     * @throws IllegalStateException if payment was not made or already refunded
     */
    @Transactional
    public void refundCancelledRSVP(Long cancelledRsvpId, Long organizerId) {
        // Find cancelled RSVP
        CancelledRSVP cancelledRsvp = cancelledRSVPRepository.findById(cancelledRsvpId)
                .orElseThrow(() -> new EntityNotFoundException("Cancelled RSVP not found"));

        Event event = cancelledRsvp.getEvent();

        // Verify user is organizer or admin
        if (!event.getCreatedBy().getId().equals(organizerId) && !isUserAdmin(organizerId)) {
            throw new AccessDeniedException("Only the event organizer or admin can process refunds");
        }

        // Verify payment was made
        if (cancelledRsvp.getPaymentStatus() == null || !cancelledRsvp.getPaymentStatus().equals("paid")) {
            throw new IllegalStateException("Cannot refund - no payment was made");
        }

        // Verify not already refunded
        if (cancelledRsvp.getRefundStatus() != null && cancelledRsvp.getRefundStatus().equals("refunded")) {
            throw new IllegalStateException("Payment has already been refunded");
        }

        // Process refund via Stripe
        try {
            String refundId = stripeService.refundPayment(
                    cancelledRsvp.getStripePaymentIntentId(),
                    cancelledRsvp.getAmountPaid());

            // Update cancelled RSVP record
            cancelledRsvp.setRefundStatus("refunded");
            cancelledRsvp.setRefundedAt(LocalDateTime.now());
            cancelledRsvp.setStripeRefundId(refundId);
            cancelledRsvp.setRefundedBy(userRepository.findById(organizerId).orElse(null));
            cancelledRSVPRepository.save(cancelledRsvp);

        } catch (Exception e) {
            // Mark as failed
            cancelledRsvp.setRefundStatus("failed");
            cancelledRSVPRepository.save(cancelledRsvp);
            throw new RuntimeException("Refund failed: " + e.getMessage());
        }
    }
}

// > so EventCardDTO is the carrier of the data and is how the html file gets
// the
// data it needs, but who populates eventcarddto with the data? who insert the
// data into the dto object?

// Based on the typical Spring MVC pattern,
// here's who would populate the EventCardDTO:

// The data flow would be:

// 1. Controller receives request
// 2. Controller calls Service method
// 3. Service gets Model objects from Repository
// 4. Service converts Model objects → EventCardDTO
// 5. Controller adds EventCardDTO to model and returns view

// Example of how it would work:

// // In EventService
// public List<EventCardDTO> getEventCards(String userId) {
// List<Event> events = eventRepository.findAll();
// return events.stream()
// .map(event -> new EventCardDTO(
// event.getId(),
// event.getTitle(),
// event.getDate(),
// // ... map all fields from Event model to DTO
// ))
// .collect(toList());
// }

// // In Controller
// @GetMapping("/events")
// public String showEvents(Model model) {
// List<EventCardDTO> eventCards = eventService.getEventCards(userId);
// model.addAttribute("events", eventCards);
// return "events";
// }

// So to answer your question: The Service layer typically populates the DTO by
// extracting data from Model objects and transforming it into the format the
// view
// needs.

// Service Layer Responsibilities
// The EventService creates these DTOs and handles ALL business logic:
// Truncates descriptions to appropriate lengths (50/100 chars)
// Fetches category details from relationships
// Calculates attendee counts from RSVP table
// Determines user-specific status (checking authentication)
// Computes event states (started/full)