package au.edu.rmit.sept.webapp.service;

import au.edu.rmit.sept.webapp.dto.KeywordDTO;
import au.edu.rmit.sept.webapp.model.Keyword;
import au.edu.rmit.sept.webapp.repository.KeywordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
public class KeywordService {

    private final KeywordRepository keywordRepository;
    
    // Custom keyword ordering - grouped by category
    private static final List<String> KEYWORD_ORDER = Arrays.asList(
        "Beginner", "Intermediate", "Advanced",           // Skill levels
        "Food Provided", "BYO",                           // Food options  
        "On-Campus", "Virtual",                           // Location types
        "Workshop", "Networking",                         // Event types
        "Free Parking"                                    // Other amenities
    );

    public KeywordService(KeywordRepository keywordRepository) {
        this.keywordRepository = keywordRepository;
    }

    /**
     * Finds existing keyword or creates new one if not exists
     * Ensures case-insensitive uniqueness
     */
    @Transactional
    public Keyword findOrCreateKeyword(String name, String color) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Keyword name cannot be empty");
        }

        final String trimmedName = name.trim();
        if (trimmedName.length() > 16) {
            throw new IllegalArgumentException("Keyword name cannot exceed 16 characters");
        }

        return keywordRepository.findByNameIgnoreCase(trimmedName)
                .orElseGet(() -> {
                    Keyword newKeyword = new Keyword(trimmedName, color);
                    return keywordRepository.save(newKeyword);
                });
    }

    /**
     * Get all keywords sorted by custom priority order for dropdown
     */
    @Transactional(readOnly = true)
    public List<KeywordDTO> getAllKeywords() {
        List<Keyword> allKeywords = keywordRepository.findAllByOrderByNameAsc();
        
        // Create priority map for custom ordering
        Map<String, Integer> priorityMap = new HashMap<>();
        for (int i = 0; i < KEYWORD_ORDER.size(); i++) {
            priorityMap.put(KEYWORD_ORDER.get(i), i);
        }
        
        // Sort keywords by custom priority, then alphabetically for unlisted ones
        List<KeywordDTO> sortedKeywords = allKeywords.stream()
                .map(this::mapToDTO)
                .sorted((a, b) -> {
                    Integer priorityA = priorityMap.get(a.getName());
                    Integer priorityB = priorityMap.get(b.getName());
                    
                    // If both have priority, sort by priority
                    if (priorityA != null && priorityB != null) {
                        return priorityA.compareTo(priorityB);
                    }
                    // If only A has priority, A comes first
                    if (priorityA != null) return -1;
                    // If only B has priority, B comes first
                    if (priorityB != null) return 1;
                    // If neither has priority, sort alphabetically
                    return a.getName().compareToIgnoreCase(b.getName());
                })
                .collect(Collectors.toList());
        
        return sortedKeywords;
    }

    /**
     * Find keywords by IDs for event assignment
     */
    @Transactional(readOnly = true)
    public Set<Keyword> findKeywordsByIds(Set<Long> keywordIds) {
        if (keywordIds == null || keywordIds.isEmpty()) {
            return Set.of();
        }
        return keywordRepository.findAllByIds(keywordIds);
    }

    /**
     * Process keyword selection including custom keywords
     * Handles both existing keyword IDs and new keyword names
     */
    @Transactional
    public Set<Keyword> processKeywordSelection(Set<Long> existingKeywordIds, List<String> customKeywords) {
        Set<Keyword> keywords = findKeywordsByIds(existingKeywordIds);

        if (customKeywords != null) {
            // Use LinkedHashSet to maintain order and prevent duplicates
            Set<String> uniqueCustomKeywords = new LinkedHashSet<>();
            for (String customKeyword : customKeywords) {
                if (customKeyword != null && !customKeyword.trim().isEmpty()) {
                    uniqueCustomKeywords.add(customKeyword.trim().toLowerCase());
                }
            }
            
            // Process deduplicated custom keywords
            for (String uniqueKeyword : uniqueCustomKeywords) {
                Keyword custom = findOrCreateKeyword(uniqueKeyword, "#6B7280");
                keywords.add(custom);
            }
        }

        if (keywords.size() > 5) {
            throw new IllegalArgumentException("An event can have maximum 5 keywords");
        }

        return keywords;
    }

    private KeywordDTO mapToDTO(Keyword keyword) {
        return new KeywordDTO(keyword.getId(), keyword.getName(), keyword.getColor());
    }
}