package au.edu.rmit.sept.webapp.security;

import java.util.Collection;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import au.edu.rmit.sept.webapp.model.User;
import au.edu.rmit.sept.webapp.repository.UserRepository;

/**
 * custom user details service, loads user data from database for spring security authentication
 * converts user entity to userdetails object with authorities (user permissions/roles) for login process.
 * Spring Security uses authorities to control access to different parts of your application 
 * (like admin pages, specific endpoints, etc.). The getAuthorities() method tells Spring Security what 
 * permissions this user has. .
 * Spring Security expects the UserDetails interface, which defines specific
 * methods like getAuthorities(), isAccountNonExpired(), isAccountNonLocked(), etc.
 * User entity is a JPA domain model focused on data persistence, while UserDetails is a security contract.
 * allows: Separation of concerns - keeps domain model separate from security logic.
 * allows: Flexibility - any user model can work with Spring Security through this adapter pattern.
 * Could make User entity implement UserDetails directly, but that would mix 
 * persistence concerns with security concerns, violating clean architecture principles.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    // Constructor injection - modern Spring best practice for immutability, testability, and fail-fast behavior
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * loads user by username or email for authentication, converts to spring security userdetails
     * throws exception if user not found in database
     */
    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        // fetch user from database, try username first, then email if not found
        // better for user experience with both username and email login support
        // Uses Java's Optional.or() method for clean chaining.
        // Throws UsernameNotFoundException only if both lookups fail.
        User user = userRepository.findByUsername(usernameOrEmail)
            .or(() -> userRepository.findByEmail(usernameOrEmail))
            .orElseThrow(() -> new UsernameNotFoundException("user not found: " + usernameOrEmail));

        // convert user entity to spring security userdetails object
        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getUsername())
            .password(user.getPassword())
            .authorities(getAuthorities(user))
            .accountExpired(false) // account status flags, Spring Security checks during authentication
            .accountLocked(false) // account is not locked or expired by default
            .credentialsExpired(false) // credentials aren't expired by default
            .disabled(!user.isEnabled() || user.isDeactivated()) // prevent login for disabled or deactivated users
            .build();
            // Setting them all to false, makes all users "active" by default.
            // In a real system, typically read these values from your User 
            // entity's database fields (currenly commented out)
    }

    /**
     * gets user authorities/roles for spring security
     * currently assigns basic user role to all authenticated users
     */
    private Collection<? extends GrantedAuthority> getAuthorities(User user) {
        String role = user.getRole() != null ? user.getRole() : "ROLE_USER";
        return Collections.singletonList(new SimpleGrantedAuthority(role));
    }
}