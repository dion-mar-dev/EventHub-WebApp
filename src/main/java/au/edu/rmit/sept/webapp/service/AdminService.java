package au.edu.rmit.sept.webapp.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import au.edu.rmit.sept.webapp.dto.AdminEventDTO;
import au.edu.rmit.sept.webapp.dto.AdminUserDTO;
import au.edu.rmit.sept.webapp.model.Event;
import au.edu.rmit.sept.webapp.model.User;
import au.edu.rmit.sept.webapp.repository.EventRepository;
import au.edu.rmit.sept.webapp.repository.RSVPRepository;
import au.edu.rmit.sept.webapp.repository.UserRepository;

@Service
@Transactional
public class AdminService {

    private final EventRepository eventRepository;
    private final RSVPRepository rsvpRepository;
    private final UserRepository userRepository;

    public AdminService(EventRepository eventRepository, RSVPRepository rsvpRepository, UserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.rsvpRepository = rsvpRepository;
        this.userRepository = userRepository;
    }

    public Page<AdminEventDTO> getActiveFutureEvents(Pageable pageable) {
        Page<Event> events = eventRepository.findActiveFutureEvents(pageable);
        return events.map(this::mapToAdminEventDTO);
    }

    public Page<AdminEventDTO> getDeactivatedFutureEvents(Pageable pageable) {
        Page<Event> events = eventRepository.findDeactivatedFutureEvents(pageable);
        return events.map(this::mapToAdminEventDTO);
    }

    public Page<AdminEventDTO> getActivePastEvents(Pageable pageable) {
        Page<Event> events = eventRepository.findActivePastEvents(pageable);
        return events.map(this::mapToAdminEventDTO);
    }

    public Page<AdminEventDTO> getDeactivatedPastEvents(Pageable pageable) {
        Page<Event> events = eventRepository.findDeactivatedPastEvents(pageable);
        return events.map(this::mapToAdminEventDTO);
    }

    @Transactional
    public void deactivateEvent(Long eventId, Long adminId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

        event.setDeactivated(true);
        event.setDeactivatedByAdminId(adminId);
        eventRepository.save(event);
    }

    @Transactional
    public void reactivateEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

        event.setDeactivated(false);
        event.setDeactivatedByAdminId(null);
        eventRepository.save(event);
    }

    private AdminEventDTO mapToAdminEventDTO(Event event) {
        AdminEventDTO dto = new AdminEventDTO();
        dto.setEventId(event.getId());
        dto.setTitle(event.getTitle());
        dto.setEventDate(event.getEventDate());
        dto.setEventTime(event.getEventTime());

        if (event.getCreatedBy() != null) {
            dto.setOrganizerUsername(event.getCreatedBy().getUsername());
        }

        Long rsvpCount = rsvpRepository.countByEvent(event);
        dto.setRsvpCount(rsvpCount);

        if (event.getCategory() != null) {
            dto.setCategoryName(event.getCategory().getName());
        }

        dto.setDeactivated(event.isDeactivated());

        if (event.getDeactivatedByAdminId() != null) {
            userRepository.findById(event.getDeactivatedByAdminId())
                    .ifPresent(admin -> dto.setDeactivatedByUsername(admin.getUsername()));
        }

        return dto;
    }

    // User management methods (role-segmented)

    public Page<AdminUserDTO> getActiveUsersAsAdmin(Pageable pageable) {
        Page<User> users = userRepository.findByDeactivatedFalseOrderByCreatedAtDesc(pageable);
        return users.map(this::mapToAdminUserDTO);
    }

    public Page<AdminUserDTO> getDeactivatedUsersAsAdmin(Pageable pageable) {
        Page<User> users = userRepository.findByDeactivatedTrueOrderByCreatedAtDesc(pageable);
        return users.map(this::mapToAdminUserDTO);
    }

    public Page<AdminUserDTO> searchUsersAsAdmin(String searchTerm, String filter, boolean includeDeactivated,
            Pageable pageable) {
        Page<User> users;

        // Apply filter at database level
        if ("creators".equals(filter)) {
            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                users = userRepository.searchEventCreators(searchTerm.trim(), includeDeactivated, pageable);
            } else {
                users = userRepository.findEventCreators(includeDeactivated, pageable);
            }
        } else if ("regular".equals(filter)) {
            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                users = userRepository.searchRegularUsers(searchTerm.trim(), includeDeactivated, pageable);
            } else {
                users = userRepository.findRegularUsers(includeDeactivated, pageable);
            }
        } else {
            // existing "all" filter logic
            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                if (includeDeactivated) {
                    users = userRepository.searchDeactivatedUsers(searchTerm.trim(), pageable);
                } else {
                    users = userRepository.searchActiveUsers(searchTerm.trim(), pageable);
                }
            } else {
                if (includeDeactivated) {
                    users = userRepository.findByDeactivatedTrueOrderByCreatedAtDesc(pageable);
                } else {
                    users = userRepository.findByDeactivatedFalseOrderByCreatedAtDesc(pageable);
                }
            }
        }

        return users.map(this::mapToAdminUserDTO);
    }

    @Transactional
    public void deactivateUserAsAdmin(Long userId, Long adminId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        user.setDeactivated(true);
        user.setDeactivatedByAdminId(adminId);
        userRepository.save(user);
    }

    @Transactional
    public void reactivateUserAsAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        user.setDeactivated(false);
        user.setDeactivatedByAdminId(null);
        userRepository.save(user);
    }

    private AdminUserDTO mapToAdminUserDTO(User user) {
        AdminUserDTO dto = new AdminUserDTO();
        dto.setUserId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setJoinDate(user.getCreatedAt());
        dto.setDeactivated(user.isDeactivated());

        Long eventsCount = userRepository.countEventsByUserId(user.getId());
        dto.setEventsCreatedCount(eventsCount);

        Long rsvpsCount = userRepository.countRsvpsByUserId(user.getId());
        dto.setRsvpsMadeCount(rsvpsCount);

        if (user.getDeactivatedByAdminId() != null) {
            userRepository.findById(user.getDeactivatedByAdminId())
                    .ifPresent(admin -> dto.setDeactivatedByUsername(admin.getUsername()));
        }

        return dto;
    }

    // Count methods for dashboard statistics
    public long countActiveFutureEvents() {
        return eventRepository.countActiveFutureEvents();
    }

    public long countDeactivatedFutureEvents() {
        return eventRepository.countDeactivatedFutureEvents();
    }

    public long countActivePastEvents() {
        return eventRepository.countActivePastEvents();
    }

    public long countDeactivatedPastEvents() {
        return eventRepository.countDeactivatedPastEvents();
    }

    public long countActiveUsers() {
        return userRepository.countActiveUsers();
    }

    public long countDeactivatedUsers() {
        return userRepository.countDeactivatedUsers();
    }
}