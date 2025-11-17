package au.edu.rmit.sept.webapp.repository;

import au.edu.rmit.sept.webapp.model.Keyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface KeywordRepository extends JpaRepository<Keyword, Long> {

    /**
     * ACTIVE - Find keyword by name (case-insensitive) to check for duplicates
     * Used when creating new keywords during event creation
     * Returns Optional for cleaner null handling
     */
    Optional<Keyword> findByNameIgnoreCase(String name);

    /**
     * ACTIVE - Get all keywords sorted alphabetically for dropdown population
     * Used in event creation/edit forms
     */
    List<Keyword> findAllByOrderByNameAsc();

    /**
     * ACTIVE - Check if a keyword with the given name already exists
     * (case-insensitive)
     * Alternative to findByNameIgnoreCase for simple existence checks
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * ACTIVE - Find multiple keywords by their IDs in a single query
     * Used when saving events with selected keywords
     */
    @Query("SELECT k FROM Keyword k WHERE k.id IN :ids")
    Set<Keyword> findAllByIds(@Param("ids") Set<Long> ids);

    /**
     * FUTURE - Find keywords by partial name match (case-insensitive)
     * For autocomplete/search functionality when using Select2 or similar
     */
    @Query("SELECT k FROM Keyword k WHERE LOWER(k.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) ORDER BY k.name")
    List<Keyword> findByNameContainingIgnoreCase(@Param("searchTerm") String searchTerm);

    /**
     * FUTURE - Find popular keywords based on usage count
     * Could be used to suggest popular keywords during event creation
     */
    @Query("SELECT k FROM Keyword k LEFT JOIN k.events e GROUP BY k ORDER BY COUNT(e) DESC")
    List<Keyword> findMostUsedKeywords();
}