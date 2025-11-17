package au.edu.rmit.sept.webapp.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;

import au.edu.rmit.sept.webapp.model.Category;
import au.edu.rmit.sept.webapp.model.User;
import java.util.Set;

import au.edu.rmit.sept.webapp.model.Event;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

/**
 * Repository for Event entities.
 * Extends JpaRepository to automatically provide findById() and standard CRUD operations.
 */
@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    
    /**
     * Finds upcoming events that haven't started yet.
     * 
     * Uses full date+time filtering to correctly exclude events that already started today.
     * Returns results with built-in pagination support for scalability.
     * Results are sorted chronologically (by date then time) for proper event display.
     * 
     * @param date Current date for filtering
     * @param time Current time for filtering  
     * @param pageable Pagination parameters
     * @return Page of upcoming events sorted chronologically
     */
    @EntityGraph(attributePaths = {"category", "createdBy", "keywords"})
    @Query("SELECT e FROM Event e WHERE " +
           "(e.eventDate > :date OR (e.eventDate = :date AND e.eventTime > :time)) " +
           "AND e.deactivated = false " +
           "ORDER BY e.eventDate ASC, e.eventTime ASC")
    Page<Event> findUpcomingEvents(@Param("date") LocalDate date, 
                                   @Param("time") LocalTime time, 
                                   Pageable pageable);

    // Alternative method using JOIN FETCH (more explicit but less flexible)
    @Query("SELECT e FROM Event e JOIN FETCH e.category LEFT JOIN FETCH e.createdBy WHERE " +
           "(e.eventDate > :date OR (e.eventDate = :date AND e.eventTime > :time)) " +
           "AND e.deactivated = false " +
           "ORDER BY e.eventDate ASC, e.eventTime ASC")
    Page<Event> findUpcomingEventsWithJoinFetch(@Param("date") LocalDate date, 
                                                @Param("time") LocalTime time, 
                                                Pageable pageable);
    
    // Default JpaRepository methods with EntityGraph
    // This ensures category and createdBy data is eagerly loaded in a single 
    // query, preventing N+1 lazy loading issues during view rendering.
    @EntityGraph(attributePaths = {"category", "createdBy", "keywords"})
    @Query("SELECT e FROM Event e WHERE e.deactivated = false")
    Page<Event> findAll(Pageable pageable);

    /** Upcoming events after the given date, ascending by eventDate.
     * NOTE: The existing findAllByEventDateAfterOrderByEventDateAsc() method
     * should not include "keywords" in its @EntityGraph since keywords aren't
     * displayed in list views. This avoids unnecessary joins and data transfer 
     * for list pages.
     */
    @EntityGraph(attributePaths = { "category", "createdBy", "keywords" })
    @Query("SELECT e FROM Event e WHERE e.eventDate > :afterDate AND e.deactivated = false ORDER BY e.eventDate ASC")
    Page<Event> findAllByEventDateAfterOrderByEventDateAsc(@Param("afterDate") LocalDate afterDate, Pageable pageable);
    
    /** Optional convenience lookup using stable public UID */
    Optional<Event> findByUid(String uid);
    
    /** Find event by ID with pessimistic write lock for concurrent RSVP handling */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Event e WHERE e.id = :id")
    Optional<Event> findByIdWithLock(@Param("id") Long id);
    
    /**
     * Finds upcoming events filtered by category.
     * 
     * Filters events by category and excludes those that have already started.
     * Uses same date/time logic as findUpcomingEvents but adds category filter.
     * 
     * @param categoryId The category ID to filter by
     * @param date       Current date for filtering
     * @param time       Current time for filtering
     * @param pageable   Pagination parameters
     * @return Page of upcoming events in the specified category
     */
    @EntityGraph(attributePaths = { "category", "createdBy", "keywords" })
    @Query("SELECT e FROM Event e WHERE e.category.id = :categoryId AND " +
            "(e.eventDate > :date OR (e.eventDate = :date AND e.eventTime > :time)) " +
            "AND e.deactivated = false " +
            "ORDER BY e.eventDate ASC, e.eventTime ASC")
    Page<Event> findUpcomingEventsByCategory(
            @Param("categoryId") Long categoryId,
            @Param("date") LocalDate date,
            @Param("time") LocalTime time,
            Pageable pageable);

       /**
     * Finds recommended upcoming events based on a user's interested categories.
     * * This query filters for events that:
     * 1. Belong to a set of categories the user is interested in.
     * 2. Have not yet started.
     * 3. Were not created by the user themselves.
     * * @param categories    A set of Category entities representing the user's interests.
     * @param currentDate   The current date for filtering.
     * @param currentTime   The current time for filtering.
     * @param user          The User entity to exclude from the event creator.
     * @param pageable      Pagination parameters to limit the number of recommendations.
     * @return A Page of recommended Event entities.
     */
    @EntityGraph(attributePaths = {"category", "createdBy", "keywords"})
    @Query("SELECT e FROM Event e WHERE e.category IN :categories " +
           "AND (e.eventDate > :currentDate OR (e.eventDate = :currentDate AND e.eventTime > :currentTime)) " +
           "AND e.createdBy != :user " +
           "AND e.deactivated = false")
    Page<Event> findRecommendedUpcomingEvents(@Param("categories") Set<Category> categories,
                                              @Param("currentDate") LocalDate currentDate,
                                              @Param("currentTime") LocalTime currentTime,
                                              @Param("user") User user,
                                              Pageable pageable);

    /**
     * Finds upcoming events created by a specific user.
     * 
     * Filters events by creator and excludes those that have already started.
     * Uses same date/time logic as other upcoming event queries for consistency.
     * 
     * @param userId The ID of the user who created the events
     * @param date   Current date for filtering
     * @param time   Current time for filtering
     * @param pageable Pagination parameters
     * @return Page of upcoming events created by the user
     */
    @EntityGraph(attributePaths = {"category", "createdBy", "keywords"})
    @Query("SELECT e FROM Event e WHERE e.createdBy.id = :userId AND " +
           "(e.eventDate > :date OR (e.eventDate = :date AND e.eventTime > :time)) " +
           "AND e.deactivated = false " +
           "ORDER BY e.eventDate ASC, e.eventTime ASC")
    Page<Event> findUpcomingEventsByCreatedBy(@Param("userId") Long userId,
                                              @Param("date") LocalDate date,
                                              @Param("time") LocalTime time,
                                              Pageable pageable);

    /**
     * ACTIVE - Fetch single event with keywords eagerly loaded
     * Used ONLY for event detail view where keywords are displayed
     * Prevents N+1 queries when showing keyword badges on detail page
     */
    @EntityGraph(attributePaths = { "category", "createdBy", "keywords" })
    Optional<Event> findWithKeywordsById(Long id);

    // ============= FUTURE METHODS - For search functionality =============
    // These methods are not currently active but will be needed when
    // implementing the search-by-keyword feature.

    /**
     * FUTURE - Search events by single keyword
     * Will be used when users click a keyword badge to filter
     */
    @Query("SELECT DISTINCT e FROM Event e JOIN e.keywords k WHERE k.id = :keywordId AND " +
            "(e.eventDate > :date OR (e.eventDate = :date AND e.eventTime > :time)) " +
            "AND e.deactivated = false " +
            "ORDER BY e.eventDate ASC, e.eventTime ASC")
    Page<Event> findUpcomingEventsByKeyword(
            @Param("keywordId") Long keywordId,
            @Param("date") LocalDate date,
            @Param("time") LocalTime time,
            Pageable pageable);

    /**
     * FUTURE - Search events by multiple keywords (OR condition)
     * Finds events that have ANY of the specified keywords
     * Useful for broad search functionality
     */
    @Query("SELECT DISTINCT e FROM Event e JOIN e.keywords k WHERE k.id IN :keywordIds AND " +
            "(e.eventDate > :date OR (e.eventDate = :date AND e.eventTime > :time)) " +
            "AND e.deactivated = false " +
            "ORDER BY e.eventDate ASC, e.eventTime ASC")
    Page<Event> findUpcomingEventsByKeywordsOr(
            @Param("keywordIds") Set<Long> keywordIds,
            @Param("date") LocalDate date,
            @Param("time") LocalTime time,
            Pageable pageable);

    /**
     * FUTURE - Search events by multiple keywords (AND condition)
     * Finds events that have ALL of the specified keywords
     * For precise filtering when multiple keywords must match
     */
    @Query("SELECT e FROM Event e WHERE " +
            "(e.eventDate > :date OR (e.eventDate = :date AND e.eventTime > :time)) AND " +
            "e.deactivated = false AND " +
            "(SELECT COUNT(k) FROM Event ev JOIN ev.keywords k WHERE ev = e AND k.id IN :keywordIds) = :keywordCount " +
            "ORDER BY e.eventDate ASC, e.eventTime ASC")
    Page<Event> findUpcomingEventsByKeywordsAnd(
            @Param("keywordIds") Set<Long> keywordIds,
            @Param("keywordCount") long keywordCount,
            @Param("date") LocalDate date,
            @Param("time") LocalTime time,
            Pageable pageable);

    /**
     * Search upcoming events by text in title and description
     * Case-insensitive search that looks for the search term in both
     * event title and full description fields
     */
    @EntityGraph(attributePaths = {"category", "createdBy", "keywords"})
    @Query("SELECT e FROM Event e WHERE " +
            "(e.eventDate > :date OR (e.eventDate = :date AND e.eventTime > :time)) " +
            "AND e.deactivated = false " +
            "AND (LOWER(e.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "     OR LOWER(e.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
            "ORDER BY e.eventDate ASC, e.eventTime ASC")
    Page<Event> searchUpcomingEvents(
            @Param("searchTerm") String searchTerm,
            @Param("date") LocalDate date,
            @Param("time") LocalTime time,
            Pageable pageable);

    /**
     * Search upcoming events by text with category filter
     * Combines text search with category filtering
     */
    @EntityGraph(attributePaths = {"category", "createdBy", "keywords"})
    @Query("SELECT e FROM Event e WHERE e.category.id = :categoryId " +
            "AND (e.eventDate > :date OR (e.eventDate = :date AND e.eventTime > :time)) " +
            "AND e.deactivated = false " +
            "AND (LOWER(e.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "     OR LOWER(e.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
            "ORDER BY e.eventDate ASC, e.eventTime ASC")
    Page<Event> searchUpcomingEventsByCategory(
            @Param("searchTerm") String searchTerm,
            @Param("categoryId") Long categoryId,
            @Param("date") LocalDate date,
            @Param("time") LocalTime time,
            Pageable pageable);

    /**
     * Search upcoming events by text with keyword filter (OR condition)
     * Combines text search with keyword filtering
     */
    @EntityGraph(attributePaths = {"category", "createdBy", "keywords"})
    @Query("SELECT DISTINCT e FROM Event e JOIN e.keywords k WHERE k.id IN :keywordIds AND " +
            "(e.eventDate > :date OR (e.eventDate = :date AND e.eventTime > :time)) " +
            "AND e.deactivated = false " +
            "AND (LOWER(e.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "     OR LOWER(e.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Event> searchUpcomingEventsByKeywords(
            @Param("searchTerm") String searchTerm,
            @Param("keywordIds") Set<Long> keywordIds,
            @Param("date") LocalDate date,
            @Param("time") LocalTime time,
            Pageable pageable);

    /**
     * Search upcoming events by text with both category and keyword filters
     * Combines text search with both category and keyword filtering
     */
    @EntityGraph(attributePaths = {"category", "createdBy", "keywords"})
    @Query("SELECT DISTINCT e FROM Event e JOIN e.keywords k WHERE e.category.id = :categoryId AND " +
            "k.id IN :keywordIds AND " +
            "(e.eventDate > :date OR (e.eventDate = :date AND e.eventTime > :time)) " +
            "AND e.deactivated = false " +
            "AND (LOWER(e.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
            "     OR LOWER(e.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Event> searchUpcomingEventsByCategoryAndKeywords(
            @Param("searchTerm") String searchTerm,
            @Param("categoryId") Long categoryId,
            @Param("keywordIds") Set<Long> keywordIds,
            @Param("date") LocalDate date,
            @Param("time") LocalTime time,
            Pageable pageable);

    // ADMIN PRIVILEGED METHOD - INCLUDES DEACTIVATED EVENTS
    @EntityGraph(attributePaths = { "category", "createdBy" })
    @Query("SELECT e FROM Event e WHERE e.eventDate >= CURRENT_DATE " +
            "AND e.deactivated = false ORDER BY e.eventDate ASC, e.eventTime ASC")
    Page<Event> findActiveFutureEvents(Pageable pageable);

    // ADMIN PRIVILEGED METHOD - INCLUDES DEACTIVATED EVENTS
    // @EntityGraph(attributePaths = { "category", "createdBy" })
    // @Query("SELECT e FROM Event e WHERE e.eventDate >= CURRENT_DATE " +
    //         "ORDER BY e.eventDate ASC, e.eventTime ASC")
    // Page<Event> findAllFutureEvents(Pageable pageable); 

    // ADMIN PRIVILEGED METHOD - INCLUDES DEACTIVATED EVENTS
    @EntityGraph(attributePaths = { "category", "createdBy" })
    @Query("SELECT e FROM Event e WHERE e.eventDate >= CURRENT_DATE " +
            "AND e.deactivated = true ORDER BY e.eventDate ASC, e.eventTime ASC")
    Page<Event> findDeactivatedFutureEvents(Pageable pageable);

    // ADMIN PRIVILEGED METHOD - INCLUDES DEACTIVATED EVENTS
    @EntityGraph(attributePaths = { "category", "createdBy" })
    @Query("SELECT e FROM Event e WHERE e.eventDate < CURRENT_DATE " +
            "AND e.deactivated = false ORDER BY e.eventDate DESC, e.eventTime DESC")
    Page<Event> findActivePastEvents(Pageable pageable);

    // ADMIN PRIVILEGED METHOD - INCLUDES DEACTIVATED EVENTS
    @EntityGraph(attributePaths = { "category", "createdBy" })
    @Query("SELECT e FROM Event e WHERE e.eventDate < CURRENT_DATE " +
            "AND e.deactivated = true ORDER BY e.eventDate DESC, e.eventTime DESC")
    Page<Event> findDeactivatedPastEvents(Pageable pageable);

    /**
     * Deletes an active event by ID. Prevents deletion of deactivated events.
     * For regular users - only allows deletion of active events.
     */
    @Modifying
    @Query("DELETE FROM Event e WHERE e.id = :id AND e.deactivated = false")
    void deleteActiveEventById(@Param("id") Long id);

    /**
     * ADMIN PRIVILEGED METHOD - Force delete any event regardless of deactivation status.
     * For admin use only - can delete both active and deactivated events.
     */
    void deleteById(Long id); // Standard JPA method - can delete any event

    /**
     * Finds past events for display (events that have already occurred).
     * Returns events in reverse chronological order (most recent first).
     * Excludes deactivated events.
     * 
     * @param date     Current date for filtering
     * @param time     Current time for filtering
     * @param pageable Pagination parameters
     * @return Page of past events sorted by most recent first
     */
    @EntityGraph(attributePaths = { "category", "createdBy", "keywords" })
    @Query("SELECT e FROM Event e WHERE " +
            "(e.eventDate < :date OR (e.eventDate = :date AND e.eventTime < :time)) " +
            "AND e.deactivated = false " +
            "ORDER BY e.eventDate DESC, e.eventTime DESC")
    Page<Event> findPastEventsForDisplay(@Param("date") LocalDate date,
            @Param("time") LocalTime time,
            Pageable pageable);

    /**
     * Counts events within a date range.
     * Used for calendar statistics.
     * 
     * @param startDate Start date (inclusive)
     * @param endDate   End date (inclusive)
     * @return Number of events in the date range
     */
    @Query("SELECT COUNT(e) FROM Event e WHERE e.eventDate BETWEEN :startDate AND :endDate AND e.deactivated = false")
    long countByEventDateBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Counts upcoming (not yet started) active events in a specific category.
     */
    @Query("SELECT COUNT(e) FROM Event e WHERE e.category.id = :categoryId AND " +
           "(e.eventDate > :date OR (e.eventDate = :date AND e.eventTime > :time)) AND " +
           "e.deactivated = false")
    long countUpcomingEventsByCategory(@Param("categoryId") Long categoryId,
                                       @Param("date") LocalDate date,
                                       @Param("time") LocalTime time);

    /**
     * Counts events within a month range.
     * Used for monthly calendar statistics.
     *
     * @param monthStart Start of the month
     * @param nextMonthStart Start of the next month (exclusive)
     * @return Number of events in the month
     */
    @Query("SELECT COUNT(e) FROM Event e WHERE e.eventDate >= :monthStart AND e.eventDate < :nextMonthStart AND e.deactivated = false")
    long countByMonth(@Param("monthStart") LocalDate monthStart, @Param("nextMonthStart") LocalDate nextMonthStart);

    // ADMIN count methods for dashboard
    @Query("SELECT COUNT(e) FROM Event e WHERE e.eventDate >= CURRENT_DATE AND e.deactivated = false")
    long countActiveFutureEvents();

    @Query("SELECT COUNT(e) FROM Event e WHERE e.eventDate >= CURRENT_DATE AND e.deactivated = true")
    long countDeactivatedFutureEvents();

    @Query("SELECT COUNT(e) FROM Event e WHERE e.eventDate < CURRENT_DATE AND e.deactivated = false")
    long countActivePastEvents();

    @Query("SELECT COUNT(e) FROM Event e WHERE e.eventDate < CURRENT_DATE AND e.deactivated = true")
    long countDeactivatedPastEvents();
}

// ### Key Choices Made:
// 1. **Extends JpaRepository** - Automatically provides `findById()` and standard CRUD operations
// 2. **Page return type with Pageable** - Built-in pagination support for scalability, even if UI doesn't use it initially
// 3. **Full date+time filtering** - Correctly excludes events that already started today (no technical debt)
// 4. **JPQL @Query approach** - Cleaner than complex method naming for the date/time logic
// 5. **Sorted by date then time** - Chronological ordering for event display