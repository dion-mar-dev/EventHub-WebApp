package au.edu.rmit.sept.webapp.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import au.edu.rmit.sept.webapp.dto.AdminEventDTO;
import au.edu.rmit.sept.webapp.dto.AdminUserDTO;
import au.edu.rmit.sept.webapp.service.AdminService;
import au.edu.rmit.sept.webapp.service.UserService;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final UserService userService;

    public AdminController(AdminService adminService, UserService userService) {
        this.adminService = adminService;
        this.userService = userService;
    }

    @GetMapping
    public String adminDashboard(
            @RequestParam(defaultValue = "0") int futureEventsPage,
            @RequestParam(defaultValue = "active") String futureEventsFilter,
            @RequestParam(defaultValue = "0") int pastEventsPage,
            @RequestParam(defaultValue = "active") String pastEventsFilter,
            @RequestParam(defaultValue = "0") int usersPage,
            @RequestParam(defaultValue = "") String usersSearch,
            @RequestParam(defaultValue = "all") String usersFilter,
            @RequestParam(defaultValue = "0") int deactivatedUsersPage,
            @RequestParam(defaultValue = "") String deactivatedUsersSearch,
            @RequestParam(defaultValue = "all") String deactivatedUsersFilter,
            @RequestParam(defaultValue = "future-events") String tab,
            Model model) {

        // Fetch future events based on filter
        Page<AdminEventDTO> futureEvents;
        if ("deactivated".equals(futureEventsFilter)) {
            futureEvents = adminService.getDeactivatedFutureEvents(PageRequest.of(futureEventsPage, 20));
        } else {
            futureEvents = adminService.getActiveFutureEvents(PageRequest.of(futureEventsPage, 20));
        }

        // Fetch past events based on filter
        Page<AdminEventDTO> pastEvents;
        if ("deactivated".equals(pastEventsFilter)) {
            pastEvents = adminService.getDeactivatedPastEvents(PageRequest.of(pastEventsPage, 20));
        } else {
            pastEvents = adminService.getActivePastEvents(PageRequest.of(pastEventsPage, 20));
        }

        // Fetch users based on tab and parameters
        Page<AdminUserDTO> activeUsers = null;
        Page<AdminUserDTO> deactivatedUsers = null;

        if ("users".equals(tab)) {
            if (usersSearch != null && !usersSearch.trim().isEmpty()) {
                activeUsers = adminService.searchUsersAsAdmin(usersSearch, usersFilter, false, PageRequest.of(usersPage, 20));
            } else {
                activeUsers = adminService.searchUsersAsAdmin("", usersFilter, false, PageRequest.of(usersPage, 20));
            }
        } else if ("deactivated-users".equals(tab)) {
            if (deactivatedUsersSearch != null && !deactivatedUsersSearch.trim().isEmpty()) {
                deactivatedUsers = adminService.searchUsersAsAdmin(deactivatedUsersSearch, deactivatedUsersFilter, true, PageRequest.of(deactivatedUsersPage, 20));
            } else {
                deactivatedUsers = adminService.searchUsersAsAdmin("", deactivatedUsersFilter, true, PageRequest.of(deactivatedUsersPage, 20));
            }
        }

        // Fetch counts for all tabs
        long activeFutureCount = adminService.countActiveFutureEvents();
        long deactivatedFutureCount = adminService.countDeactivatedFutureEvents();
        long activePastCount = adminService.countActivePastEvents();
        long deactivatedPastCount = adminService.countDeactivatedPastEvents();
        long activeUsersCount = adminService.countActiveUsers();
        long deactivatedUsersCount = adminService.countDeactivatedUsers();

        model.addAttribute("futureEvents", futureEvents);
        model.addAttribute("futureEventsFilter", futureEventsFilter);
        model.addAttribute("pastEvents", pastEvents);
        model.addAttribute("pastEventsFilter", pastEventsFilter);
        model.addAttribute("activeUsers", activeUsers);
        model.addAttribute("usersSearch", usersSearch);
        model.addAttribute("usersFilter", usersFilter);
        model.addAttribute("deactivatedUsers", deactivatedUsers);
        model.addAttribute("deactivatedUsersSearch", deactivatedUsersSearch);
        model.addAttribute("deactivatedUsersFilter", deactivatedUsersFilter);
        model.addAttribute("activeTab", tab);

        // Add counts to model
        model.addAttribute("activeFutureCount", activeFutureCount);
        model.addAttribute("deactivatedFutureCount", deactivatedFutureCount);
        model.addAttribute("activePastCount", activePastCount);
        model.addAttribute("deactivatedPastCount", deactivatedPastCount);
        model.addAttribute("activeUsersCount", activeUsersCount);
        model.addAttribute("deactivatedUsersCount", deactivatedUsersCount);

        return "admin";
    }

    @PostMapping("/events/{id}/deactivate")
    public String deactivateEvent(@PathVariable Long id,
            @RequestParam(defaultValue = "future-events") String tab,
            Authentication auth,
            RedirectAttributes redirectAttributes) {
        try {
            Long adminId = userService.getUserIdByUsername(auth.getName());
            adminService.deactivateEvent(id, adminId);
            redirectAttributes.addFlashAttribute("successMessage", "Event deactivated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to deactivate event: " + e.getMessage());
        }
        return "redirect:/admin?tab=" + tab;
    }

    @PostMapping("/events/{id}/reactivate")
    public String reactivateEvent(@PathVariable Long id,
            @RequestParam(defaultValue = "future-events") String tab,
            RedirectAttributes redirectAttributes) {
        try {
            adminService.reactivateEvent(id);
            redirectAttributes.addFlashAttribute("successMessage", "Event reactivated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to reactivate event: " + e.getMessage());
        }
        return "redirect:/admin?tab=" + tab;
    }

    @PostMapping("/users/{id}/deactivate")
    public String deactivateUser(@PathVariable Long id,
            @RequestParam(defaultValue = "users") String tab,
            Authentication auth,
            RedirectAttributes redirectAttributes) {
        try {
            Long adminId = userService.getUserIdByUsername(auth.getName());
            adminService.deactivateUserAsAdmin(id, adminId);
            redirectAttributes.addFlashAttribute("successMessage", "User deactivated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to deactivate user: " + e.getMessage());
        }
        return "redirect:/admin?tab=" + tab;
    }

    @PostMapping("/users/{id}/reactivate")
    public String reactivateUser(@PathVariable Long id,
            @RequestParam(defaultValue = "deactivated-users") String tab,
            RedirectAttributes redirectAttributes) {
        try {
            adminService.reactivateUserAsAdmin(id);
            redirectAttributes.addFlashAttribute("successMessage", "User reactivated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to reactivate user: " + e.getMessage());
        }
        return "redirect:/admin?tab=" + tab;
    }
}