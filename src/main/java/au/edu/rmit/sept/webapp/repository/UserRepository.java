package au.edu.rmit.sept.webapp.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import au.edu.rmit.sept.webapp.model.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    // Find active users (treating NULL as false)
    @Query("SELECT u FROM User u " +
           "WHERE (u.deactivated = false OR u.deactivated IS NULL) " +
           "ORDER BY u.createdAt DESC")
    Page<User> findByDeactivatedFalseOrderByCreatedAtDesc(Pageable pageable);

    // Admin only method - find deactivated users
    Page<User> findByDeactivatedTrueOrderByCreatedAtDesc(Pageable pageable);

    // Search active users by username or email (treating NULL as false)
    @Query("SELECT u FROM User u " +
           "WHERE (u.deactivated = false OR u.deactivated IS NULL) " +
           "AND (LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "     OR LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY u.createdAt DESC")
    Page<User> searchActiveUsers(@Param("searchTerm") String searchTerm, Pageable pageable);

    // ADMIN ONLY method - search deactivated users by username or email
    @Query("SELECT u FROM User u " +
           "WHERE u.deactivated = true " +
           "AND (LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "     OR LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY u.createdAt DESC")
    Page<User> searchDeactivatedUsers(@Param("searchTerm") String searchTerm, Pageable pageable);

    // ADMIN ONLY method, used by AdminService - count events created by user
    @Query("SELECT COUNT(e) FROM Event e WHERE e.createdBy.id = :userId")
    Long countEventsByUserId(@Param("userId") Long userId);

    // ADMIN ONLY method, used by AdminService - count RSVPs made by user
    @Query("SELECT COUNT(r) FROM RSVP r WHERE r.user.id = :userId")
    Long countRsvpsByUserId(@Param("userId") Long userId);

    // ADMIN ONLY method, used by AdminService - find event creators
    @Query("SELECT DISTINCT u FROM User u " +
           "WHERE u.deactivated = :deactivated " +
           "AND EXISTS (SELECT e FROM Event e WHERE e.createdBy = u) " +
           "ORDER BY u.createdAt DESC")
    Page<User> findEventCreators(@Param("deactivated") boolean deactivated, Pageable pageable);

    // ADMIN ONLY method, used by AdminService - search event creators
    @Query("SELECT DISTINCT u FROM User u " +
           "WHERE u.deactivated = :deactivated " +
           "AND EXISTS (SELECT e FROM Event e WHERE e.createdBy = u) " +
           "AND (LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "     OR LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY u.createdAt DESC")
    Page<User> searchEventCreators(@Param("searchTerm") String searchTerm, @Param("deactivated") boolean deactivated,
            Pageable pageable);

    // ADMIN ONLY method, used by AdminService - find regular users
    @Query("SELECT u FROM User u " +
           "WHERE u.deactivated = :deactivated " +
           "AND NOT EXISTS (SELECT e FROM Event e WHERE e.createdBy = u) " +
           "ORDER BY u.createdAt DESC")
    Page<User> findRegularUsers(@Param("deactivated") boolean deactivated, Pageable pageable);

    // ADMIN ONLY method, used by AdminService - search regular users
    @Query("SELECT u FROM User u " +
           "WHERE u.deactivated = :deactivated " +
           "AND NOT EXISTS (SELECT e FROM Event e WHERE e.createdBy = u) " +
           "AND (LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "     OR LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY u.createdAt DESC")
    Page<User> searchRegularUsers(@Param("searchTerm") String searchTerm, @Param("deactivated") boolean deactivated,
            Pageable pageable);

    // ADMIN count methods for dashboard
    @Query("SELECT COUNT(u) FROM User u WHERE (u.deactivated = false OR u.deactivated IS NULL)")
    long countActiveUsers();

    @Query("SELECT COUNT(u) FROM User u WHERE u.deactivated = true")
    long countDeactivatedUsers();
}