package au.edu.rmit.sept.webapp.controller;

import au.edu.rmit.sept.webapp.model.Event;
import au.edu.rmit.sept.webapp.repository.EventRepository;
import au.edu.rmit.sept.webapp.repository.RSVPRepository;
import au.edu.rmit.sept.webapp.dto.CalendarEventDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/events")
public class CalendarController {

    private final EventRepository eventRepository;
    private final RSVPRepository rsvpRepository;

    public CalendarController(EventRepository eventRepository, RSVPRepository rsvpRepository) {
        this.eventRepository = eventRepository;
        this.rsvpRepository = rsvpRepository;
    }

    @GetMapping("/calendar")
    public String calendarPage(Model model, Authentication authentication) {
        // Use findAll with EntityGraph to avoid lazy loading issues
        List<Event> events = eventRepository.findAll(Pageable.unpaged()).getContent().stream()
                .filter(event -> !event.isDeactivated())
                .collect(Collectors.toList());
        model.addAttribute("pageTitle", "Calendar");
        model.addAttribute("events", events);

        // Build DTOs for FullCalendar
        List<CalendarEventDTO> calendarEvents = events.stream().map(e -> {
            LocalDateTime start = LocalDateTime.of(e.getEventDate(), e.getEventTime());
            LocalDateTime end = start.plusHours(2);
            String color = e.getCategory() != null && e.getCategory().getColourCode() != null
                    ? e.getCategory().getColourCode() : "#667eea";
            boolean attending = false;
            if (authentication != null && authentication.isAuthenticated()) {
                attending = rsvpRepository.existsByUser_UsernameAndEvent_Id(authentication.getName(), e.getId());
            }
            String categoryName = e.getCategory() != null ? e.getCategory().getName() : "General";
            return new CalendarEventDTO(e.getId(), e.getTitle(), start, end, color, categoryName, e.getLocation(), e.getDescription(), attending);
        }).collect(Collectors.toList());
        model.addAttribute("calendarEvents", calendarEvents);

        // Stats
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);
        long eventsThisWeek = eventRepository.countByEventDateBetween(weekStart, weekEnd);
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate nextMonthStart = monthStart.plusMonths(1);
        long totalThisMonth = eventRepository.countByMonth(monthStart, nextMonthStart);
        long attendingCount = 0;

        // Build attending list for current user
        List<Event> attendingEvents = List.of();
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            attendingEvents = events.stream()
                    .filter(e -> rsvpRepository.existsByUser_UsernameAndEvent_Id(username, e.getId()))
                    .sorted(Comparator.comparing(Event::getEventDate).thenComparing(Event::getEventTime))
                    .collect(Collectors.toList());
            attendingCount = attendingEvents.size();
        }
        model.addAttribute("attendingEvents", attendingEvents);

        model.addAttribute("eventsThisWeek", eventsThisWeek);
        model.addAttribute("attendingCount", attendingCount);
        model.addAttribute("recommendedCount", 0);
        model.addAttribute("totalThisMonth", totalThisMonth);

        return "calendar";
    }
}