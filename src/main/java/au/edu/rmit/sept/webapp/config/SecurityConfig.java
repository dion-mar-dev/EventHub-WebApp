package au.edu.rmit.sept.webapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;




import au.edu.rmit.sept.webapp.security.CustomUserDetailsService;

import javax.sql.DataSource;

/**
 * Implements SecurityConfig class with **SecurityFilterChain bean**.
 * spring security configuration, handles authentication, authorisation, session management
 * configures form login, logout behaviour, csrf protection, h2 console access
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    private final CustomUserDetailsService userDetailsService;
    private final DataSource dataSource;

    // Constructor injection - modern Spring best practice for immutability, testability, and fail-fast behavior
    public SecurityConfig(CustomUserDetailsService userDetailsService, DataSource dataSource) {
        this.userDetailsService = userDetailsService;
        this.dataSource = dataSource;
    }

    // injects spring.h2.console.enabled property from application.properties
    // defaults to false if property not found, controls h2 console access
    @Value("${spring.h2.console.enabled:false}")
    private boolean h2ConsoleEnabled;
    
    // remember me key for token signing, production: uncomment env var line, comment property line
    // @Value("${REMEMBER_ME_KEY:${app.remember-me.key}}")  // production: environment variable with fallback
    @Value("${app.remember-me.key}")  // development: application.properties
    private String rememberMeKey;
    
    /**
     * main security filter chain, defines url access patterns, authentication methods
     * configures form login, logout, session management, csrf protection
     */



    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, PersistentTokenRepository persistentTokenRepository) throws Exception {
        http
            // url access control, public paths vs authenticated paths\
            // Static non-sensitive resources:
            //     - `/css/**`, `/js/**`, `/images/**` - Stylesheets, scripts, and images needed to render pages properly
            //     - `/webjars/**` - Client-side libraries (Bootstrap, jQuery, etc.) packaged as JARs
            // Static resources must be public or pages won't display correctly (no CSS/JS/images)
            // Alternative: serving static resources through separate web server (nginx, Apache)/CDN, but for most applications, this config is the norm, perfectly secure since non-sensitive resources.
            .authorizeHttpRequests(authz -> {
                authz.requestMatchers("/", "/home", "/register", "/login", "/events/**", "/css/**", "/js/**", "/images/**", "/webjars/**").permitAll(); // Changed patterns
                authz.requestMatchers("/admin/**").hasRole("ADMIN");
                authz.requestMatchers("/api/payments/webhook").permitAll();
                // h2 console access only if enabled in properties
                // Quick database inspection: check table, query data without logging in first.
                // Make sure variable is false for prod
                if (h2ConsoleEnabled) {
                    authz.requestMatchers("/h2-console/**").permitAll();
                }
                authz.anyRequest().authenticated();
            })
            // form login configuration, custom login page, success/failure redirects.  textbook Spring Security configuration.
            // Spring automatically uses any AuthenticationProvider beans it finds (below).
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/home", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            // remember me functionality for persistent login sessions
            // uses profile-based storage for remember me tokens (file in dev, database in prod)
            // rememberMeKey: unique private key, used to sign 'remember me' cookies, helps prevent attackers from forging their own tokens
            .rememberMe(remember -> remember
                .key(rememberMeKey)
                // uses the injected PersistentTokenRepository bean (profile-specific: file-based for default, JDBC for prod)
                .tokenRepository(persistentTokenRepository)
                // specifies the service that Spring should use to load a user's details when they present a valid remember-me token
                .userDetailsService(userDetailsService)
                .tokenValiditySeconds(1209600) // 14 days
            )
            // logout configuration, invalidate session, delete cookies, redirect to login
            .logout(logout -> logout
                // defines the specific URL endpoint that a user must visit to trigger the logout process
                .logoutUrl("/logout")
                // user is redirected to login page, and NOT home page (check with AC)
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                // JSESSIONID -standard name for the session cookie used by Java web applications.
                .deleteCookies("JSESSIONID", "remember-me")
                // allows logout for authenticated users
                .permitAll()
            )
            // session management, limit concurrent sessions (only 1 active session per user).
            // new login kicks out the old session instead of blocking the new login.
            // session expiry handling redirects to login with expired parameter
            .sessionManagement(session -> session
                .sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.IF_REQUIRED)
                .invalidSessionUrl("/login?expired=true")
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
            )
            // H2 console makes POST requests without CSRF tokens (Spring 
            // Security blocks requests without CSRF tokens) coz a separate 
            // application that doesn't know about Spring's CSRF tokens, so 
            // its forms fail without the exemption.
            // Normal Spring forms automatically include CSRF tokens
            // csrf protection, h2 console exemption needed for post requests without tokens
            // stops H2 console forms fail with 403 errors
            // Note: "/h2-console/**" is a URL path pattern, not an HTTP header.
            // Spring Security looks at the request URL and checks if it matches this pattern.
            // more info at bottom of file
            .csrf(csrf -> {
                if (h2ConsoleEnabled) {
                    csrf.ignoringRequestMatchers("/h2-console/**");
                }
                // Stripe webhooks can't send CSRF tokens - must be exempted
                csrf.ignoringRequestMatchers("/api/payments/webhook");
                // csrf stays enabled for everything else in both dev and prod
            })
            // H2 console runs inside an HTML iframe. By default, Spring 
            // Security blocks iframes for security (prevents clickjacking attacks)
            // frame options for h2 console, allows iframes from same origin
            // this code allows iframes from the same domain.
            // without this  blank/broken H2 console page
            .headers(headers -> {
                if (h2ConsoleEnabled) {
                    headers.frameOptions(frameOptions -> frameOptions.sameOrigin());
                }
            });
        
        return http.build();
    }
    
    /**
     * password encoder bean, bcrypt for hashing passwords
     * centralised encoder used by both authentication provider and user service
     * ensures consistent password encoding across the application
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    /**
     * authentication provider, connects user details service with password encoder
     * handles user lookup and password verification during login
     * Used automatically by Spring Security's authentication framework
     * during login attempts. When a user submits login credentials through
     * the form, Spring Security's authentication manager automatically
     * discovers and uses this bean to: Look up the user via
     * CustomUserDetailsService,
     * and Verify the submitted password against the stored hash using the
     * passwordEncoder() defined in this class.
     * HTML login form just submits username/password to `/login` (POST). Spring
     * Security intercepts this, extracts credentials, and automatically routes
     * to your DaoAuthenticationProvider for validation.
     * Purpose of a DAO (Data Access Object) is to encapsulate data access and
     * manipulation logic.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        // configures the DaoAuthenticationProvider with a UserDetailsService
        // setter injection. handles fetching user data
        authProvider.setUserDetailsService(userDetailsService);
        // configures the provider with a PasswordEncoder
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
    
    /**
     * authentication manager bean, required for manual programmatic 
     * authentication operations (e.g., custom login endpoints, API authentication)
     * delegates to configured authentication providers  (e.g. DaoAuthenticationProvider above).
     * Returns authenticated user or throws exception.
     * Current configuration only uses standard form login, AuthenticationManager 
     * bean is defined but not actively used (dormant infrastructure), 
     * only for later if manual auth needed e.g. REST APIs or custom login logic.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    
    /**
     * File-based persistent token repository for development/default profile.
     * Stores remember me tokens in JSON file (./persistent-tokens.json).
     * Does not require database setup - tokens persist between server restarts in a simple file.
     * Active when no specific profile is set (default) or when 'dev' profile is active.
     */
    @Bean
    @Profile({"default", "dev"})
    public PersistentTokenRepository fileBasedTokenRepository() {
        return new FileBasedTokenRepository();
    }

    /**
     * Database-based persistent token repository for production and devprod profiles.
     * Stores remember me tokens in database table (persistent_logins), survives server restarts.
     * Spring Security auto-creates persistent_logins table on first run.
     * Spring Boot automatically creates a DataSource bean based on your database
     * configuration in application.properties. It: Detects H2 on classpath (dev
     * only), Creates DataSource bean pointing to H2 in-memory database, @Autowired
     * injects that auto-configured DataSource, No manual setup - Spring handles the
     * H2 connection automatically. @Autowired gets whatever DataSource Spring Boot
     * created (H2 in this case), without knowing or caring about the specific
     * database type.
     * One-time setup: The @Bean method creates the repository once when Spring starts.
     * Automatic everything: JdbcTokenRepositoryImpl handles: Auto-creates
     * persistent_logins table on first run, All SQL operations (insert, select,
     * delete tokens), Token storage and retrieval logic, automatic token deletion.
     * No manual cleanup needed - Spring Security handles all token lifecycle
     * management automatically.
     * Spring Security provides the complete implementation out-of-the-box.
     * Active when 'prod' or 'devprod' profile is set.
     */
    @Bean
    @Profile({"prod", "devprod"})
    public PersistentTokenRepository jdbcTokenRepository() {
        JdbcTokenRepositoryImpl tokenRepository = new JdbcTokenRepositoryImpl();
        tokenRepository.setDataSource(dataSource);
        return tokenRepository;
    }
}

/*
 * more on.. csrf protection, h2 console
 * CSRF (Cross-Site Request Forgery) tokens are generated by the server and sent to the client's browser, typically embedded in a form. 
 * A breakdown:
1. Server generates token: When a user requests a form or page, the server creates a unique, secret token (usually tied to the user's session).
2. Token sent to client: The server includes the token in the form or page, often as a hidden input field.
3. Client submits token: When the user submits the form, the token is sent back to the server along with the request.
4. Server verifies token: The server checks the token sent in the request against the one stored in the user's session. If they match, the request is valid.
 * H2 Database: Repository layer only, No web interface, Doesn't affect CSRF flow. 
 * H2 Console (what creates the CSRF issue): Separate web admin tool for browsing/querying the database, Accessible 
 * at /h2-console/ , Has its own forms that bypass your Spring MVC flow entirely.
 * is an optional development tool that lets you run SQL queries through a web interface. 
 * Not part of your application's business logic. 
 * H2 console requests do go through Spring Security's filter chain (which includes CSRF validation), but they go to H2's handlers, not the app business controllers.
 * H2 console form submits to `/h2-console/query.do`, Spring Security intercepts the request, Checks for CSRF token (fails - H2 forms don't have them), Without exemption: 403 Forbidden error, With exemption: Request passes to H2's query handler.
 * `/h2-console/query.do` is an H2 console endpoint that processes SQL queries.
 * H2 console has its own handlers: `/h2-console/query.do` - executes SQL queries, `/h2-console/login.do` - handles login, `/h2-console/test.do` - tests connections.
 * H2 console is a mini web application with its own: HTML pages (query forms, result tables), URL endpoints (.do files), Request handlers (Java code that processes the requests).
 * when the H2 console form submits to /h2-console/query.do it's really just sending a request to itself (the H2 console)? and then this h2 console or the endpoint/handlers can access the actual h2 database and it's tables?
 * Yes, When H2 console submits to /h2-console/query.do, it's sending the request to H2's own handlers within the same application.
 * The H2 handlers can directly access the H2 database because they're part of the H2 database engine itself.
 * H2 console is both the web interface and has direct database access - it's a complete database admin tool bundled with the H2 database engine.
 * The Spring app talks to H2 via repositories/JPA, but H2 console talks to H2 directly via its built-in handlers.
 * how/why does spring security intercept these request if they don't go through the app at all and h2 console is just really sending itself the request?
 * why does spring security intercept these request if they don't go through the app at all and h2 console is just really sending itself the request?
 * Spring Security intercepts all HTTP requests because it operates at the servlet filter level, before any handlers (yours or H2's) get the request.
 * H2 console is embedded in your Spring Boot application, not separate. It shares the same: Servlet container (Tomcat), Port (8080), Security filter chain. 
 * When accessing `/h2-console/query.do`, the request goes through Spring Security first, then gets routed to H2's handler if allowed. 
 * Without the CSRF exemption, Spring Security blocks the request before it ever reaches H2's handlers.
 * H2 console is a development/admin tool only. The app's normal users never see or use it - it's for developers to inspect/query the database during development.
 * Normal app flow: User → the controllers → the views
 * H2 console: Developer → database admin interface
 * 
 * why should we have csrf not disabled during dev stage?
Production parity - Dev environment should mirror production security settings
Early bug detection - Catch CSRF token issues before deployment
Form validation - Ensures your forms properly include CSRF tokens
Developer awareness - Team learns to handle CSRF correctly from start
Testing authenticity - Verify your application works with proper security measures
Integration testing - Third-party tools/APIs learn to work with CSRF tokens
Prevent bad habits - Developers don't get used to insecure shortcuts
Bottom line: If it's disabled in dev but enabled in prod, you're essentially developing against a different application than what you'll deploy.
 * 
 * Benefits of public H2 console access being public: 
 * Quick database inspection - Check table contents, query data without logging in first, 
 * Faster debugging - Immediately verify if data was saved correctly, check relationships.
 * Trade-off: Convenience vs security best practices. Many teams keep it public in dev for speed, then secure it in production configs.
 */