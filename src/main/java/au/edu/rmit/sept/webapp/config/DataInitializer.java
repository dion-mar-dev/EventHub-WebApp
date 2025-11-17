package au.edu.rmit.sept.webapp.config;

import au.edu.rmit.sept.webapp.model.Category;
import au.edu.rmit.sept.webapp.model.User;
import au.edu.rmit.sept.webapp.model.Event;
import au.edu.rmit.sept.webapp.model.RSVP;
import au.edu.rmit.sept.webapp.model.Keyword;
import au.edu.rmit.sept.webapp.repository.CategoryRepository;
import au.edu.rmit.sept.webapp.repository.UserRepository;
import au.edu.rmit.sept.webapp.repository.EventRepository;
import au.edu.rmit.sept.webapp.repository.RSVPRepository;
import au.edu.rmit.sept.webapp.repository.KeywordRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * DataInitializer for Development and Initial Production Setup
 * DEV (standard) PROFILE: @Profile("dev")
 * PROD PROFILE: @Profile("prod") - Creates sample data when app starts
 * 
 * AFTER FIRST RUN: Change to @Profile("nonexistent-profile") to disable
 * - This prevents duplicate data creation on subsequent app restarts
 * - MySQL persistence means data survives restarts (unlike H2 in-memory)
 * - Only run this ONCE initially, then disable to avoid constraint violations
 * 
 * Creates: 35 users, 8 categories, 33 events, 11 keywords, 41 RSVPs
 */     
@Component
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final RSVPRepository rsvpRepository;
    private final KeywordRepository keywordRepository;
    private final PasswordEncoder passwordEncoder;

    // Constructor injection - modern Spring best practice for immutability, testability, and fail-fast behavior
    public DataInitializer(CategoryRepository categoryRepository, UserRepository userRepository,
                          EventRepository eventRepository, RSVPRepository rsvpRepository,
                          KeywordRepository keywordRepository, PasswordEncoder passwordEncoder) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.rsvpRepository = rsvpRepository;
        this.keywordRepository = keywordRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {

        // IDEMPOTENT CHECK: Only create data if database is empty
        if (userRepository.count() > 0) {
            System.out.println("✓ Database already contains data. Skipping initialization.");
            System.out.println("\n🌐 Application is ready!");
            System.out.println("Home: http://localhost:8080/");
            return;
        }
        System.out.println("→ Database is empty. Creating sample data...");

        // Initialize Users
        Map<String, User> users = new HashMap<>();
        
        if (userRepository.count() == 0) {
            User sarah = new User();
            sarah.setUsername("sarah.chen");
            sarah.setEmail("sarah.chen@student.rmit.edu.au");
            sarah.setPassword(passwordEncoder.encode("password123"));
            sarah.setEnabled(true);
            users.put("sarah", userRepository.save(sarah));

            User james = new User();
            james.setUsername("james.wilson");
            james.setEmail("james.wilson@rmit.edu.au");
            james.setPassword(passwordEncoder.encode("password123"));
            james.setEnabled(true);
            users.put("james", userRepository.save(james));

            User priya = new User();
            priya.setUsername("fad");
            priya.setEmail("priya.sharma@student.rmit.edu.au");
            priya.setPassword(passwordEncoder.encode("ffffffff"));
            priya.setEnabled(true);
            priya.setRole("ROLE_ADMIN");
            users.put("priya", userRepository.save(priya));

            User michael = new User();
            michael.setUsername("michael.nguyen");
            michael.setEmail("michael.nguyen@rmit.edu.au");
            michael.setPassword(passwordEncoder.encode("password123"));
            michael.setEnabled(true);
            users.put("michael", userRepository.save(michael));

            User emma = new User();
            emma.setUsername("emma.thompson");
            emma.setEmail("emma.thompson@student.rmit.edu.au");
            emma.setPassword(passwordEncoder.encode("password123"));
            emma.setEnabled(true);
            users.put("emma", userRepository.save(emma));

            User liam = new User();
            liam.setUsername("liam.zhang");
            liam.setEmail("liam.zhang@student.rmit.edu.au");
            liam.setPassword(passwordEncoder.encode("password123"));
            liam.setEnabled(true);
            users.put("liam", userRepository.save(liam));

            User sophia = new User();
            sophia.setUsername("sophia.martinez");
            sophia.setEmail("sophia.martinez@student.rmit.edu.au");
            sophia.setPassword(passwordEncoder.encode("password123"));
            sophia.setEnabled(true);
            users.put("sophia", userRepository.save(sophia));

            User noah = new User();
            noah.setUsername("noah.anderson");
            noah.setEmail("noah.anderson@student.rmit.edu.au");
            noah.setPassword(passwordEncoder.encode("password123"));
            noah.setEnabled(true);
            users.put("noah", userRepository.save(noah));

            User olivia = new User();
            olivia.setUsername("olivia.kim");
            olivia.setEmail("olivia.kim@student.rmit.edu.au");
            olivia.setPassword(passwordEncoder.encode("password123"));
            olivia.setEnabled(true);
            users.put("olivia", userRepository.save(olivia));

            User william = new User();
            william.setUsername("william.brown");
            william.setEmail("william.brown@student.rmit.edu.au");
            william.setPassword(passwordEncoder.encode("password123"));
            william.setEnabled(true);
            users.put("william", userRepository.save(william));

            User ava = new User();
            ava.setUsername("ava.patel");
            ava.setEmail("ava.patel@student.rmit.edu.au");
            ava.setPassword(passwordEncoder.encode("password123"));
            ava.setEnabled(true);
            users.put("ava", userRepository.save(ava));

            User ethan = new User();
            ethan.setUsername("ethan.garcia");
            ethan.setEmail("ethan.garcia@student.rmit.edu.au");
            ethan.setPassword(passwordEncoder.encode("password123"));
            ethan.setEnabled(true);
            users.put("ethan", userRepository.save(ethan));

            User isabella = new User();
            isabella.setUsername("isabella.lee");
            isabella.setEmail("isabella.lee@student.rmit.edu.au");
            isabella.setPassword(passwordEncoder.encode("password123"));
            isabella.setEnabled(true);
            users.put("isabella", userRepository.save(isabella));

            User mason = new User();
            mason.setUsername("mason.taylor");
            mason.setEmail("mason.taylor@student.rmit.edu.au");
            mason.setPassword(passwordEncoder.encode("password123"));
            mason.setEnabled(true);
            users.put("mason", userRepository.save(mason));

            User mia = new User();
            mia.setUsername("mia.robinson");
            mia.setEmail("mia.robinson@student.rmit.edu.au");
            mia.setPassword(passwordEncoder.encode("password123"));
            mia.setEnabled(true);
            users.put("mia", userRepository.save(mia));

            User lucas = new User();
            lucas.setUsername("lucas.white");
            lucas.setEmail("lucas.white@student.rmit.edu.au");
            lucas.setPassword(passwordEncoder.encode("password123"));
            lucas.setEnabled(true);
            users.put("lucas", userRepository.save(lucas));

            User charlotte = new User();
            charlotte.setUsername("charlotte.harris");
            charlotte.setEmail("charlotte.harris@student.rmit.edu.au");
            charlotte.setPassword(passwordEncoder.encode("password123"));
            charlotte.setEnabled(true);
            users.put("charlotte", userRepository.save(charlotte));

            User oliver = new User();
            oliver.setUsername("oliver.clark");
            oliver.setEmail("oliver.clark@student.rmit.edu.au");
            oliver.setPassword(passwordEncoder.encode("password123"));
            oliver.setEnabled(true);
            users.put("oliver", userRepository.save(oliver));

            User amelia = new User();
            amelia.setUsername("amelia.lewis");
            amelia.setEmail("amelia.lewis@student.rmit.edu.au");
            amelia.setPassword(passwordEncoder.encode("password123"));
            amelia.setEnabled(true);
            users.put("amelia", userRepository.save(amelia));

            User elijah = new User();
            elijah.setUsername("elijah.walker");
            elijah.setEmail("elijah.walker@student.rmit.edu.au");
            elijah.setPassword(passwordEncoder.encode("password123"));
            elijah.setEnabled(true);
            users.put("elijah", userRepository.save(elijah));

            User harper = new User();
            harper.setUsername("harper.hall");
            harper.setEmail("harper.hall@student.rmit.edu.au");
            harper.setPassword(passwordEncoder.encode("password123"));
            harper.setEnabled(true);
            users.put("harper", userRepository.save(harper));

            User benjamin = new User();
            benjamin.setUsername("benjamin.allen");
            benjamin.setEmail("benjamin.allen@student.rmit.edu.au");
            benjamin.setPassword(passwordEncoder.encode("password123"));
            benjamin.setEnabled(true);
            users.put("benjamin", userRepository.save(benjamin));

            User evelyn = new User();
            evelyn.setUsername("evelyn.young");
            evelyn.setEmail("evelyn.young@student.rmit.edu.au");
            evelyn.setPassword(passwordEncoder.encode("password123"));
            evelyn.setEnabled(true);
            users.put("evelyn", userRepository.save(evelyn));

            User henry = new User();
            henry.setUsername("henry.king");
            henry.setEmail("henry.king@student.rmit.edu.au");
            henry.setPassword(passwordEncoder.encode("password123"));
            henry.setEnabled(true);
            users.put("henry", userRepository.save(henry));

            User luna = new User();
            luna.setUsername("luna.wright");
            luna.setEmail("luna.wright@student.rmit.edu.au");
            luna.setPassword(passwordEncoder.encode("password123"));
            luna.setEnabled(true);
            users.put("luna", userRepository.save(luna));

            User alexander = new User();
            alexander.setUsername("alexander.scott");
            alexander.setEmail("alexander.scott@student.rmit.edu.au");
            alexander.setPassword(passwordEncoder.encode("password123"));
            alexander.setEnabled(true);
            users.put("alexander", userRepository.save(alexander));

            User daniel = new User();
            daniel.setUsername("daniel.mitchell");
            daniel.setEmail("daniel.mitchell@student.rmit.edu.au");
            daniel.setPassword(passwordEncoder.encode("password123"));
            daniel.setEnabled(true);
            users.put("daniel", userRepository.save(daniel));

            User grace = new User();
            grace.setUsername("grace.cooper");
            grace.setEmail("grace.cooper@rmit.edu.au");
            grace.setPassword(passwordEncoder.encode("password123"));
            grace.setEnabled(true);
            users.put("grace", userRepository.save(grace));

            User jackson = new User();
            jackson.setUsername("jackson.reed");
            jackson.setEmail("jackson.reed@student.rmit.edu.au");
            jackson.setPassword(passwordEncoder.encode("password123"));
            jackson.setEnabled(true);
            users.put("jackson", userRepository.save(jackson));

            User lily = new User();
            lily.setUsername("lily.bailey");
            lily.setEmail("lily.bailey@student.rmit.edu.au");
            lily.setPassword(passwordEncoder.encode("password123"));
            lily.setEnabled(true);
            users.put("lily", userRepository.save(lily));

            User owen = new User();
            owen.setUsername("owen.rivera");
            owen.setEmail("owen.rivera@rmit.edu.au");
            owen.setPassword(passwordEncoder.encode("password123"));
            owen.setEnabled(true);
            users.put("owen", userRepository.save(owen));

            User chloe = new User();
            chloe.setUsername("chloe.ward");
            chloe.setEmail("chloe.ward@student.rmit.edu.au");
            chloe.setPassword(passwordEncoder.encode("password123"));
            chloe.setEnabled(true);
            users.put("chloe", userRepository.save(chloe));

            User ryan = new User();
            ryan.setUsername("ryan.campbell");
            ryan.setEmail("ryan.campbell@student.rmit.edu.au");
            ryan.setPassword(passwordEncoder.encode("password123"));
            ryan.setEnabled(true);
            users.put("ryan", userRepository.save(ryan));

            User zoe = new User();
            zoe.setUsername("zoe.morgan");
            zoe.setEmail("zoe.morgan@rmit.edu.au");
            zoe.setPassword(passwordEncoder.encode("password123"));
            zoe.setEnabled(true);
            users.put("zoe", userRepository.save(zoe));

            User max = new User();
            max.setUsername("max.fisher");
            max.setEmail("max.fisher@student.rmit.edu.au");
            max.setPassword(passwordEncoder.encode("password123"));
            max.setEnabled(true);
            users.put("max", userRepository.save(max));

            User ruby = new User();
            ruby.setUsername("ruby.turner");
            ruby.setEmail("ruby.turner@student.rmit.edu.au");
            ruby.setPassword(passwordEncoder.encode("password123"));
            ruby.setEnabled(true);
            users.put("ruby", userRepository.save(ruby));

        } else {
            // If users already exist, load them all by username
            userRepository.findAll().forEach(user -> {
                String key = user.getUsername().split("\\.")[0]; // Get first part of username (e.g., "sarah" from "sarah.chen")
                users.put(key, user);
            });
        }

        // Initialize Categories
        Map<String, Category> categories = new HashMap<>();
        if (categoryRepository.count() == 0) {
            categories.put("tech", categoryRepository.save(
                    new Category("Technology", "Tech events and conferences", "#5dade2")));
            categories.put("sports", categoryRepository.save(
                    new Category("Sports", "Sports events and competitions", "#ff8c69")));
            categories.put("music", categoryRepository.save(
                    new Category("Music", "Concerts and music festivals", "#e91e63")));
            categories.put("business", categoryRepository.save(
                    new Category("Business", "Networking and business events", "#17a2b8")));
            categories.put("education", categoryRepository.save(
                    new Category("Education", "Workshops and learning events", "#ffc107")));
            categories.put("cultural", categoryRepository.save(
                    new Category("Cultural", "Cultural events and festivals", "#9c27b0")));
            categories.put("social", categoryRepository.save(
                    new Category("Social", "Social gatherings and meetups", "#ff5722")));
            categories.put("academic", categoryRepository.save(
                    new Category("Academic", "Academic conferences and seminars", "#795548")));
        } else {
            // Load existing categories
            categoryRepository.findAll().forEach(cat -> {
                String key = cat.getName().toLowerCase();
                categories.put(key, cat);
            });
        }

        // Initialize Keywords
        Map<String, Keyword> keywords = new HashMap<>();
        if (keywordRepository.count() == 0) {
            keywords.put("virtual", keywordRepository.save(
                    new Keyword("Virtual", "#007bff")));
            keywords.put("on-campus", keywordRepository.save(
                    new Keyword("On-Campus", "#28a745")));
            keywords.put("food-provided", keywordRepository.save(
                    new Keyword("Food Provided", "#fd7e14")));
            keywords.put("byo", keywordRepository.save(
                    new Keyword("BYO", "#6f42c1")));
            keywords.put("networking", keywordRepository.save(
                    new Keyword("Networking", "#20c997")));
            keywords.put("workshop", keywordRepository.save(
                    new Keyword("Workshop", "#ffc107")));
            keywords.put("beginner-friendly", keywordRepository.save(
                    new Keyword("Beginner", "#17a2b8")));
            keywords.put("intermediate", keywordRepository.save(
                    new Keyword("Intermediate", "#fd7e14")));
            keywords.put("advanced", keywordRepository.save(
                    new Keyword("Advanced", "#dc3545")));
            keywords.put("free-parking", keywordRepository.save(
                    new Keyword("Free Parking", "#6c757d")));
            keywords.put("paid-event", keywordRepository.save(
                    new Keyword("Paid Event", "#e83e8c")));
        } else {
            // Load existing keywords
            keywordRepository.findAll().forEach(keyword -> {
                String key = keyword.getName().toLowerCase().replace(" ", "-");
                keywords.put(key, keyword);
            });
        }

        // Assign categories to new users
        users.get("priya").getCategories().add(categories.get("tech"));
        users.get("priya").getCategories().add(categories.get("music"));
        users.get("priya").getCategories().add(categories.get("education"));
        userRepository.save(users.get("priya"));
        
        users.get("emma").getCategories().add(categories.get("tech"));
        users.get("emma").getCategories().add(categories.get("business"));
        userRepository.save(users.get("emma"));

        users.get("liam").getCategories().add(categories.get("sports"));
        users.get("liam").getCategories().add(categories.get("music"));
        userRepository.save(users.get("liam"));

        users.get("sophia").getCategories().add(categories.get("education"));
        users.get("sophia").getCategories().add(categories.get("business"));
        userRepository.save(users.get("sophia"));

        users.get("noah").getCategories().add(categories.get("tech"));
        userRepository.save(users.get("noah"));

        users.get("olivia").getCategories().add(categories.get("music"));
        users.get("olivia").getCategories().add(categories.get("education"));
        userRepository.save(users.get("olivia"));

        users.get("william").getCategories().add(categories.get("sports"));
        users.get("william").getCategories().add(categories.get("tech"));
        userRepository.save(users.get("william"));

        users.get("ava").getCategories().add(categories.get("business"));
        userRepository.save(users.get("ava"));

        users.get("ethan").getCategories().add(categories.get("tech"));
        users.get("ethan").getCategories().add(categories.get("sports"));
        users.get("ethan").getCategories().add(categories.get("music"));
        userRepository.save(users.get("ethan"));

        users.get("isabella").getCategories().add(categories.get("education"));
        userRepository.save(users.get("isabella"));

        users.get("mason").getCategories().add(categories.get("sports"));
        userRepository.save(users.get("mason"));

        users.get("mia").getCategories().add(categories.get("music"));
        users.get("mia").getCategories().add(categories.get("business"));
        userRepository.save(users.get("mia"));

        users.get("lucas").getCategories().add(categories.get("tech"));
        users.get("lucas").getCategories().add(categories.get("education"));
        userRepository.save(users.get("lucas"));

        users.get("charlotte").getCategories().add(categories.get("business"));
        users.get("charlotte").getCategories().add(categories.get("education"));
        userRepository.save(users.get("charlotte"));

        users.get("oliver").getCategories().add(categories.get("sports"));
        users.get("oliver").getCategories().add(categories.get("music"));
        userRepository.save(users.get("oliver"));

        users.get("amelia").getCategories().add(categories.get("tech"));
        userRepository.save(users.get("amelia"));

        users.get("elijah").getCategories().add(categories.get("business"));
        users.get("elijah").getCategories().add(categories.get("sports"));
        userRepository.save(users.get("elijah"));

        users.get("harper").getCategories().add(categories.get("music"));
        userRepository.save(users.get("harper"));

        users.get("benjamin").getCategories().add(categories.get("education"));
        users.get("benjamin").getCategories().add(categories.get("tech"));
        userRepository.save(users.get("benjamin"));

        users.get("evelyn").getCategories().add(categories.get("business"));
        users.get("evelyn").getCategories().add(categories.get("music"));
        users.get("evelyn").getCategories().add(categories.get("education"));
        userRepository.save(users.get("evelyn"));

        users.get("henry").getCategories().add(categories.get("sports"));
        userRepository.save(users.get("henry"));

        users.get("luna").getCategories().add(categories.get("tech"));
        users.get("luna").getCategories().add(categories.get("music"));
        userRepository.save(users.get("luna"));

        users.get("alexander").getCategories().add(categories.get("education"));
        users.get("alexander").getCategories().add(categories.get("sports"));
        userRepository.save(users.get("alexander"));

        users.get("daniel").getCategories().add(categories.get("tech"));
        users.get("daniel").getCategories().add(categories.get("education"));
        userRepository.save(users.get("daniel"));

        users.get("grace").getCategories().add(categories.get("business"));
        users.get("grace").getCategories().add(categories.get("cultural"));
        userRepository.save(users.get("grace"));

        users.get("jackson").getCategories().add(categories.get("sports"));
        users.get("jackson").getCategories().add(categories.get("social"));
        userRepository.save(users.get("jackson"));

        users.get("lily").getCategories().add(categories.get("music"));
        users.get("lily").getCategories().add(categories.get("academic"));
        userRepository.save(users.get("lily"));

        users.get("owen").getCategories().add(categories.get("tech"));
        users.get("owen").getCategories().add(categories.get("business"));
        userRepository.save(users.get("owen"));

        users.get("chloe").getCategories().add(categories.get("education"));
        users.get("chloe").getCategories().add(categories.get("cultural"));
        userRepository.save(users.get("chloe"));

        users.get("ryan").getCategories().add(categories.get("sports"));
        users.get("ryan").getCategories().add(categories.get("music"));
        userRepository.save(users.get("ryan"));

        users.get("zoe").getCategories().add(categories.get("academic"));
        users.get("zoe").getCategories().add(categories.get("social"));
        userRepository.save(users.get("zoe"));

        users.get("max").getCategories().add(categories.get("tech"));
        users.get("max").getCategories().add(categories.get("sports"));
        userRepository.save(users.get("max"));

        users.get("ruby").getCategories().add(categories.get("music"));
        users.get("ruby").getCategories().add(categories.get("business"));
        userRepository.save(users.get("ruby"));

        // Initialize Events
        if (eventRepository.count() == 0) {
            // Event 1: June 2025 - Career Fair (Business)
            Event careerFair = new Event(
                    "Mid-Year Career Fair 2025",
                    "Connect with top employers from various industries. Bring your resume! " +
                            "Over 50 companies will be present including tech giants, consulting firms, " +
                            "and startups. Professional attire recommended.",
                    LocalDate.of(2025, 6, 18),
                    LocalTime.of(10, 0),
                    "RMIT Storey Hall, Swanston Street",
                    500,
                    users.get("james"),
                    categories.get("business"));
            // Add keywords to Career Fair
            Set<Keyword> careerKeywords = new HashSet<>();
            careerKeywords.add(keywords.get("networking"));
            careerKeywords.add(keywords.get("on-campus"));
            careerKeywords.add(keywords.get("free-parking"));
            careerFair.setKeywords(careerKeywords);
            eventRepository.save(careerFair);

            // Event 2: September 20, 2025 - Tech Workshop (Technology)
            Event aiWorkshop = new Event(
                    "Introduction to Machine Learning with Python",
                    "Hands-on workshop covering ML fundamentals, supervised learning, and " +
                            "neural networks basics. Bring your laptop with Python installed. " +
                            "Perfect for beginners and intermediate programmers.",
                    LocalDate.of(2025, 10, 20),
                    LocalTime.of(14, 0),
                    "Building 14, Level 10, Room 12",
                    45,
                    users.get("michael"),
                    categories.get("tech"));
            // Add keywords to AI Workshop
            Set<Keyword> aiKeywords = new HashSet<>();
            aiKeywords.add(keywords.get("workshop"));
            aiKeywords.add(keywords.get("beginner-friendly"));
            aiKeywords.add(keywords.get("on-campus"));
            aiWorkshop.setKeywords(aiKeywords);
            eventRepository.save(aiWorkshop);

            // Event 3: October 2025 - Hackathon (Technology)
            Event hackathon = new Event(
                    "RMIT Spring Hackathon 2025",
                    "48-hour coding marathon! Build innovative solutions for real-world problems. " +
                            "Teams of 4-5 people. Prizes worth $10,000. Food and drinks provided throughout. " +
                            "Mentors from Google, Atlassian, and Canva will be present.",
                    LocalDate.of(2025, 10, 21),
                    LocalTime.of(17, 0),
                    "Building 80, Levels 7-9",
                    200,
                    users.get("sarah"),
                    categories.get("tech"));
            // Add keywords to Hackathon
            Set<Keyword> hackathonKeywords = new HashSet<>();
            hackathonKeywords.add(keywords.get("networking"));
            hackathonKeywords.add(keywords.get("food-provided"));
            hackathonKeywords.add(keywords.get("advanced"));
            hackathonKeywords.add(keywords.get("on-campus"));
            hackathon.setKeywords(hackathonKeywords);
            hackathon.setPrice(new BigDecimal("25.00"));
            hackathon.setRequiresPayment(true);
            eventRepository.save(hackathon);

            // Event 4: October 2025 - Music Event (Music)
            Event bandNight = new Event(
                    "RMIT Battle of the Bands",
                    "Annual band competition featuring RMIT's finest student musicians. " +
                            "10 bands competing for the grand prize of studio recording time. " +
                            "Free entry, food trucks available.",
                    LocalDate.of(2025, 10, 24),
                    LocalTime.of(18, 30),
                    "Alumni Courtyard",
                    350,
                    users.get("priya"),
                    categories.get("music"));
            eventRepository.save(bandNight);

            // Event 5: November 2025 - Sports Tournament (Sports)
            Event basketball = new Event(
                    "3v3 Basketball Championship",
                    "Inter-faculty basketball tournament. Register as a team of 3-4 players. " +
                            "Round-robin followed by knockout stages. Trophies and RMIT Sport memberships " +
                            "for top 3 teams. Entry fee: $30 per team.",
                    LocalDate.of(2025, 11, 7),
                    LocalTime.of(16, 0),
                    "RMIT Sports Centre, Bundoora Campus",
                    32,
                    users.get("james"),
                    categories.get("sports"));
            eventRepository.save(basketball);

            // Event 6: November 2025 - Academic Workshop (Education)
            Event thesisWorkshop = new Event(
                    "Thesis Writing Masterclass",
                    "Essential workshop for honours and postgraduate students. Learn about " +
                            "academic writing, research methodologies, citation management, and defending " +
                            "your thesis. Guest speaker: Prof. Eleanor Martinez.",
                    LocalDate.of(2025, 11, 15),
                    LocalTime.of(13, 0),
                    "Building 12, Level 5, Lecture Theatre 2",
                    80,
                    users.get("michael"),
                    categories.get("education"));
            eventRepository.save(thesisWorkshop);

            // Event 7: December 2025 - Networking Event (Business)
            Event startupPitch = new Event(
                    "Startup Pitch Night & Networking",
                    "Watch 10 student startups pitch their ideas to real investors. " +
                            "Network with entrepreneurs, VCs, and industry professionals. " +
                            "Light refreshments and drinks provided. Business casual attire.",
                    LocalDate.of(2025, 12, 3),
                    LocalTime.of(17, 30),
                    "The Capitol Theatre, RMIT Building 20",
                    150,
                    users.get("sarah"),
                    categories.get("business"));
            eventRepository.save(startupPitch);

            // Event 8: December 2025 - End of Year Concert (Music)
            Event orchestra = new Event(
                    "RMIT Orchestra: End of Year Gala",
                    "Celebrate the year with a spectacular performance by the RMIT Symphony Orchestra. " +
                            "Program includes classical masterpieces and contemporary film scores. " +
                            "Free for students, $10 for guests. Formal attire encouraged.",
                    LocalDate.of(2025, 12, 12),
                    LocalTime.of(19, 0),
                    "Kaleide Theatre, RMIT Building 22",
                    400,
                    users.get("priya"),
                    categories.get("music"));
            eventRepository.save(orchestra);

            // ==================== ADDITIONAL 20 EVENTS ====================

            // Event 9: September 2025 - Cyber Security Workshop (Technology)
            Event cyberSecurity = new Event(
                    "Cybersecurity Fundamentals Workshop",
                    "Learn essential cybersecurity practices including password management, " +
                            "phishing detection, and network security basics. Hands-on lab sessions " +
                            "with real-world scenarios. Suitable for all skill levels.",
                    LocalDate.of(2025, 10, 22),
                    LocalTime.of(10, 0),
                    "Building 14, Level 8, Computer Lab 3",
                    40,
                    users.get("michael"),
                    categories.get("tech"));
            eventRepository.save(cyberSecurity);

            // Event 10: September 2025 - Volleyball Tournament (Sports)
            Event volleyball = new Event(
                    "Beach Volleyball Championship",
                    "Outdoor volleyball tournament on sand courts. Teams of 2-4 players. " +
                            "BBQ and drinks included. Prizes for top 3 teams. " +
                            "Rain date: November 1st, 2025.",
                    LocalDate.of(2025, 10, 25),
                    LocalTime.of(15, 30),
                    "RMIT Outdoor Sports Complex",
                    1,
                    users.get("james"),
                    categories.get("sports"));
            eventRepository.save(volleyball);

            // Event 11: September 2025 - Jazz Night (Music)
            Event jazzNight = new Event(
                    "Smooth Jazz Evening",
                    "Intimate jazz performance featuring RMIT's finest student jazz musicians. " +
                            "Wine and cheese platters available for purchase. " +
                            "Perfect date night or evening with friends.",
                    LocalDate.of(2025, 10, 28),
                    LocalTime.of(19, 30),
                    "The Corner Hotel - Upstairs Lounge",
                    0,
                    users.get("priya"),
                    categories.get("music"));
            // Add keywords to Smooth Jazz Evening
            Set<Keyword> jazzKeywords = new HashSet<>();
            jazzKeywords.add(keywords.get("byo"));
            jazzKeywords.add(keywords.get("networking"));
            jazzKeywords.add(keywords.get("paid-event"));
            jazzNight.setKeywords(jazzKeywords);
            eventRepository.save(jazzNight);

            // Event 12: October 2025 - Industry Panel (Business)
            Event industryPanel = new Event(
                    "Future of Work: Industry Leaders Panel",
                    "CEOs and industry experts discuss remote work, AI impact, and career trends. " +
                            "Q&A session and networking lunch included. " +
                            "Panelists from Microsoft, Atlassian, and PwC.",
                    LocalDate.of(2025, 10, 3),
                    LocalTime.of(11, 0),
                    "RMIT Capitol Theatre",
                    Integer.MAX_VALUE,
                    users.get("sarah"),
                    categories.get("business"));
            // Add keywords to Industry Panel
            Set<Keyword> industryPanelKeywords = new HashSet<>();
            industryPanelKeywords.add(keywords.get("networking"));
            industryPanelKeywords.add(keywords.get("on-campus"));
            industryPanelKeywords.add(keywords.get("food-provided"));
            industryPanelKeywords.add(keywords.get("advanced"));
            industryPanel.setKeywords(industryPanelKeywords);
            eventRepository.save(industryPanel);

            // Event 13: October 2025 - Study Skills Workshop (Education)
            Event studySkills = new Event(
                    "Exam Preparation & Study Techniques",
                    "Proven study methods, time management, and exam strategies. " +
                            "Interactive sessions with practice exercises. " +
                            "Free study planner and materials provided.",
                    LocalDate.of(2025, 10, 7),
                    LocalTime.of(13, 0),
                    "Building 12, Level 3, Room 15",
                    60,
                    users.get("michael"),
                    categories.get("education"));
            // Add keywords to Study Skills Workshop
            Set<Keyword> studySkillsKeywords = new HashSet<>();
            studySkillsKeywords.add(keywords.get("workshop"));
            studySkillsKeywords.add(keywords.get("on-campus"));
            studySkillsKeywords.add(keywords.get("beginner-friendly"));
            studySkillsKeywords.add(keywords.get("free-parking"));
            studySkills.setKeywords(studySkillsKeywords);
            eventRepository.save(studySkills);

            // Event 14: October 2025 - Web Development Bootcamp (Technology)
            Event webBootcamp = new Event(
                    "React & Node.js Weekend Bootcamp",
                    "Intensive weekend workshop covering modern web development. " +
                            "Build a full-stack application from scratch. " +
                            "Includes lunch and coffee. Bring your laptop.",
                    LocalDate.of(2025, 10, 12),
                    LocalTime.of(9, 0),
                    "Building 14, Entire Level 12",
                    50,
                    users.get("sarah"),
                    categories.get("tech"));
            eventRepository.save(webBootcamp);

            // Event 15: October 2025 - Swimming Competition (Sports)
            Event swimming = new Event(
                    "RMIT Aquatic Championships",
                    "Inter-faculty swimming competition with individual and relay events. " +
                            "All skill levels welcome. Medals for top 3 in each category. " +
                            "Spectators welcome, poolside BBQ available.",
                    LocalDate.of(2025, 10, 18),
                    LocalTime.of(14, 0),
                    "Melbourne Sports & Aquatic Centre",
                    80,
                    users.get("james"),
                    categories.get("sports"));
            swimming.setPrice(new BigDecimal("30.00"));
            swimming.setRequiresPayment(true);
            eventRepository.save(swimming);

            // Event 16: October 2025 - Electronic Music Festival (Music)
            Event edmFestival = new Event(
                    "RMIT Electronic Music Festival",
                    "Outdoor electronic music festival featuring local and international DJs. " +
                            "4 stages, food trucks, and art installations. " +
                            "21+ event, ID required. Early bird tickets available.",
                    LocalDate.of(2025, 10, 25),
                    LocalTime.of(16, 0),
                    "Alexandra Gardens, Yarra River",
                    500,
                    users.get("priya"),
                    categories.get("music"));
            eventRepository.save(edmFestival);

            // Event 17: October 2025 - Entrepreneurship Seminar (Business)
            Event entrepreneurship = new Event(
                    "From Idea to IPO: Startup Journey",
                    "Successful entrepreneurs share their journey from concept to public company. " +
                            "Case studies, pitching tips, and investor insights. " +
                            "Networking mixer with light refreshments.",
                    LocalDate.of(2025, 10, 30),
                    LocalTime.of(17, 0),
                    "Building 20, Innovation Hub",
                    100,
                    users.get("michael"),
                    categories.get("business"));
            eventRepository.save(entrepreneurship);

            // Event 18: November 2025 - Research Methods Workshop (Education)
            Event researchMethods = new Event(
                    "Quantitative Research & Data Analysis",
                    "Statistical analysis, survey design, and data interpretation workshop. " +
                            "SPSS and R software training included. " +
                            "Perfect for final year students and researchers.",
                    LocalDate.of(2025, 11, 5),
                    LocalTime.of(10, 0),
                    "Building 12, Computer Lab 5",
                    35,
                    users.get("sarah"),
                    categories.get("education"));
            eventRepository.save(researchMethods);

            // Event 19: November 2025 - Gaming Tournament (Technology)
            Event gaming = new Event(
                    "RMIT Esports Championship",
                    "Multi-game tournament featuring League of Legends, CS2, and FIFA. " +
                            "Cash prizes totaling $5,000. Live streaming and commentary. " +
                            "Team and solo competitions available.",
                    LocalDate.of(2025, 11, 9),
                    LocalTime.of(12, 0),
                    "Building 80, Gaming Arena",
                    150,
                    users.get("james"),
                    categories.get("tech"));
            eventRepository.save(gaming);

            // Event 20: November 2025 - Rock Climbing Competition (Sports)
            Event rockClimbing = new Event(
                    "Indoor Climbing Championship",
                    "Bouldering and sport climbing competition for all skill levels. " +
                            "Beginner-friendly with coaching available. " +
                            "Gear rental included, chalk and snacks provided.",
                    LocalDate.of(2025, 11, 13),
                    LocalTime.of(18, 0),
                    "Urban Climb Melbourne",
                    60,
                    users.get("priya"),
                    categories.get("sports"));
            eventRepository.save(rockClimbing);

            // Event 21: November 2025 - Open Mic Night (Music)
            Event openMic = new Event(
                    "Acoustic Open Mic Night",
                    "Showcase your musical talent in an intimate setting. " +
                            "All instruments and vocal performances welcome. " +
                            "Sign-up starts 7pm, performances 8pm. Free entry.",
                    LocalDate.of(2025, 11, 20),
                    LocalTime.of(19, 0),
                    "The Tote Hotel, Front Bar",
                    80,
                    users.get("michael"),
                    categories.get("music"));
            eventRepository.save(openMic);

            // Event 22: November 2025 - Investment Workshop (Business)
            Event investment = new Event(
                    "Personal Finance & Investment Basics",
                    "Learn about budgeting, saving, and investment strategies for students. " +
                            "Stock market simulation and portfolio building exercise. " +
                            "Guest speakers from Commonwealth Bank and Vanguard.",
                    LocalDate.of(2025, 11, 22),
                    LocalTime.of(14, 30),
                    "Building 20, Lecture Theatre 5",
                    120,
                    users.get("sarah"),
                    categories.get("business"));
            eventRepository.save(investment);

            // Event 23: November 2025 - Academic Writing Workshop (Education)
            Event academicWriting = new Event(
                    "Advanced Academic Writing Skills",
                    "Improve your essay writing, referencing, and critical analysis skills. " +
                            "One-on-one feedback sessions available. " +
                            "Suitable for undergraduate and postgraduate students.",
                    LocalDate.of(2025, 11, 27),
                    LocalTime.of(16, 0),
                    "Library, Level 4, Study Room 12",
                    25,
                    users.get("james"),
                    categories.get("education"));
            eventRepository.save(academicWriting);

            // Event 24: December 2025 - AI Workshop (Technology)
            Event aiEthics = new Event(
                    "Ethics in Artificial Intelligence",
                    "Explore the moral implications of AI technology. " +
                            "Case studies, group discussions, and expert panels. " +
                            "Features ethicists from Google AI and Microsoft Research.",
                    LocalDate.of(2025, 12, 5),
                    LocalTime.of(13, 30),
                    "Building 14, Auditorium A",
                    200,
                    users.get("priya"),
                    categories.get("tech"));
            eventRepository.save(aiEthics);

            // Event 25: December 2025 - Boxing Tournament (Sports)
            Event boxing = new Event(
                    "RMIT Amateur Boxing Championship",
                    "Safe, supervised amateur boxing matches with proper protective gear. " +
                            "Weight classes for all participants. Medical supervision on-site. " +
                            "Training sessions available beforehand.",
                    LocalDate.of(2025, 12, 8),
                    LocalTime.of(17, 30),
                    "Melbourne Pavilion Sports Centre",
                    40,
                    users.get("michael"),
                    categories.get("sports"));
            eventRepository.save(boxing);

            // Event 26: December 2025 - Holiday Concert (Music)
            Event holidayConcert = new Event(
                    "RMIT Holiday Choir Performance",
                    "Festive choir performance featuring traditional and modern holiday music. " +
                            "Mulled wine and mince pies available. " +
                            "Free event, donations welcome for music program.",
                    LocalDate.of(2025, 12, 15),
                    LocalTime.of(18, 0),
                    "St. Paul's Cathedral",
                    300,
                    users.get("sarah"),
                    categories.get("music"));
            eventRepository.save(holidayConcert);

            // Event 27: December 2025 - Year End Networking (Business)
            Event yearEndNetworking = new Event(
                    "End of Year Professional Mixer",
                    "Network with alumni, industry professionals, and fellow students. " +
                            "Cocktails, canapés, and live jazz music. " +
                            "Business cards recommended, door prizes available.",
                    LocalDate.of(2025, 12, 18),
                    LocalTime.of(18, 30),
                    "Eureka Skydeck 88, Crown Casino",
                    180,
                    users.get("james"),
                    categories.get("business"));
            eventRepository.save(yearEndNetworking);

            // Event 28: December 2025 - Final Study Session (Education)
            Event finalStudy = new Event(
                    "Collaborative Study Marathon",
                    "24-hour study session with group activities, quiet zones, and peer support. " +
                            "Free coffee, snacks, and pizza provided throughout. " +
                            "Tutors available for major subjects.",
                    LocalDate.of(2025, 12, 20),
                    LocalTime.of(8, 0),
                    "Library, All Levels",
                    null,
                    users.get("priya"),
                    categories.get("education"));
            eventRepository.save(finalStudy);

            // ==================== AUGUST 2025 EVENTS ====================

            // Event 29: August 5, 2025 - Mobile App Development Workshop (Technology)
            Event mobileAppWorkshop = new Event(
                    "Flutter Mobile App Development Workshop",
                    "Learn to build cross-platform mobile apps using Flutter and Dart. " +
                            "Create your first app from scratch with user authentication and data persistence. " +
                            "No prior mobile development experience required. Laptop required.",
                    LocalDate.of(2025, 8, 5),
                    LocalTime.of(13, 30),
                    "Building 14, Level 9, Lab 7",
                    35,
                    users.get("michael"),
                    categories.get("tech"));
            // Add keywords to Mobile App Workshop
            Set<Keyword> mobileAppKeywords = new HashSet<>();
            mobileAppKeywords.add(keywords.get("workshop"));
            mobileAppKeywords.add(keywords.get("beginner-friendly"));
            mobileAppKeywords.add(keywords.get("on-campus"));
            mobileAppKeywords.add(keywords.get("free-parking"));
            mobileAppWorkshop.setKeywords(mobileAppKeywords);
            eventRepository.save(mobileAppWorkshop);

            // Event 30: August 12, 2025 - RMIT Winter Ball (Social/Cultural)
            Event winterBall = new Event(
                    "RMIT Annual Winter Ball 2025",
                    "Elegant formal evening with live orchestra, three-course dinner, and dancing. " +
                            "Professional photography included. Formal/black-tie attire required. " +
                            "Limited tickets available - early booking recommended.",
                    LocalDate.of(2025, 8, 12),
                    LocalTime.of(18, 0),
                    "Crown Palladium Ballroom, Southbank",
                    280,
                    users.get("sarah"),
                    categories.get("cultural"));
            // Add keywords to Winter Ball
            Set<Keyword> winterBallKeywords = new HashSet<>();
            winterBallKeywords.add(keywords.get("networking"));
            winterBallKeywords.add(keywords.get("food-provided"));
            winterBallKeywords.add(keywords.get("paid-event"));
            winterBall.setKeywords(winterBallKeywords);
            eventRepository.save(winterBall);

            // Event 31: August 18, 2025 - Cricket Tournament (Sports)
            Event cricketTournament = new Event(
                    "RMIT Indoor Cricket Championship",
                    "Fast-paced indoor cricket tournament with teams of 8 players. " +
                            "Round-robin format followed by finals. Trophies for winners and runners-up. " +
                            "Equipment provided, just bring sports shoes and team spirit!",
                    LocalDate.of(2025, 8, 18),
                    LocalTime.of(10, 0),
                    "Melbourne Cricket Ground - Indoor Centre",
                    64,
                    users.get("james"),
                    categories.get("sports"));
            // Add keywords to Cricket Tournament
            Set<Keyword> cricketKeywords = new HashSet<>();
            cricketKeywords.add(keywords.get("food-provided"));
            cricketKeywords.add(keywords.get("free-parking"));
            cricketTournament.setKeywords(cricketKeywords);
            eventRepository.save(cricketTournament);

            // Event 32: August 22, 2025 - Industry Mentorship Program (Business)
            Event mentorshipProgram = new Event(
                    "Speed Mentoring: Industry Professionals",
                    "15-minute one-on-one sessions with experienced professionals from various industries. " +
                            "Get career advice, industry insights, and networking opportunities. " +
                            "Bring your questions and business cards. Light refreshments provided.",
                    LocalDate.of(2025, 8, 22),
                    LocalTime.of(16, 0),
                    "Building 20, Innovation Hub & Breakout Rooms",
                    90,
                    users.get("grace"),
                    categories.get("business"));
            // Add keywords to Mentorship Program
            Set<Keyword> mentorshipKeywords = new HashSet<>();
            mentorshipKeywords.add(keywords.get("networking"));
            mentorshipKeywords.add(keywords.get("on-campus"));
            mentorshipKeywords.add(keywords.get("intermediate"));
            mentorshipKeywords.add(keywords.get("food-provided"));
            mentorshipProgram.setKeywords(mentorshipKeywords);
            eventRepository.save(mentorshipProgram);

            // Event 33: August 28, 2025 - Classical Music Recital (Music)
            Event classicalRecital = new Event(
                    "RMIT Chamber Music Society Recital",
                    "Intimate chamber music performance featuring piano, violin, and cello works by Mozart, " +
                            "Beethoven, and contemporary Australian composers. Wine and cheese reception follows. " +
                            "Free for students and staff, $15 for external guests.",
                    LocalDate.of(2025, 8, 28),
                    LocalTime.of(19, 30),
                    "Kaleide Theatre, RMIT Building 22",
                    120,
                    users.get("olivia"),
                    categories.get("music"));
            // Add keywords to Classical Recital
            Set<Keyword> classicalKeywords = new HashSet<>();
            classicalKeywords.add(keywords.get("on-campus"));
            classicalKeywords.add(keywords.get("food-provided"));
            classicalKeywords.add(keywords.get("networking"));
            classicalRecital.setKeywords(classicalKeywords);
            eventRepository.save(classicalRecital);

            // Initialize RSVPs
            if (rsvpRepository.count() == 0) {
                // Beach Volleyball Championship RSVP
                RSVP beachVolleyballRsvp = new RSVP();
                beachVolleyballRsvp.setUser(users.get("sarah"));
                beachVolleyballRsvp.setEvent(volleyball);
                beachVolleyballRsvp.setRsvpDate(LocalDateTime.of(2025, 10, 10, 14, 30));
                rsvpRepository.save(beachVolleyballRsvp);

                // careerFair RSVP
                RSVP careerFairRsvp = new RSVP();
                careerFairRsvp.setUser(users.get("sarah"));
                careerFairRsvp.setEvent(careerFair);
                careerFairRsvp.setRsvpDate(LocalDateTime.of(2025, 5, 1, 14, 30));
                rsvpRepository.save(careerFairRsvp);

                // Cybersecurity Fundamentals Workshop RSVPs (15 users)
                String[] cyberWorkshopUsers = {"emma", "liam", "noah", "william", "ethan", "lucas", "benjamin", "amelia", "luna", "alexander", "sophia", "olivia", "ava", "isabella", "mason"};
                for (String username : cyberWorkshopUsers) {
                    RSVP cyberRsvp = new RSVP();
                    cyberRsvp.setUser(users.get(username));
                    cyberRsvp.setEvent(cyberSecurity);
                    cyberRsvp.setRsvpDate(LocalDateTime.of(2025, 10, 15, 10, 0).plusMinutes((long)(Math.random() * 1440))); // Random time within a day
                    rsvpRepository.save(cyberRsvp);
                }

                // RMIT Battle of the Bands RSVPs (25 users)
                String[] battleOfBandsUsers = {"sarah", "james", "michael", "emma", "liam", "sophia", "noah", "olivia", "william", "ava", "ethan", "isabella", "mason", "mia", "lucas", "charlotte", "oliver", "amelia", "elijah", "harper", "benjamin", "evelyn", "henry", "luna", "alexander"};
                for (String username : battleOfBandsUsers) {
                    RSVP bandRsvp = new RSVP();
                    bandRsvp.setUser(users.get(username));
                    bandRsvp.setEvent(bandNight);
                    bandRsvp.setRsvpDate(LocalDateTime.of(2025, 10, 1, 9, 0).plusMinutes((long)(Math.random() * 23 * 24 * 60))); // Random time between Oct 1-23
                    rsvpRepository.save(bandRsvp);
                }

                // RMIT Chamber Music Society Recital RSVP (fad user)
                RSVP chamberMusicRsvp = new RSVP();
                chamberMusicRsvp.setUser(users.get("priya")); // user with username "fad"
                chamberMusicRsvp.setEvent(classicalRecital);
                chamberMusicRsvp.setRsvpDate(LocalDateTime.of(2025, 8, 20, 14, 30)); // RSVP'd before the event
                rsvpRepository.save(chamberMusicRsvp);

                // RMIT Chamber Music Society Recital RSVP (mason.taylor user)
                RSVP chamberMusicRsvp2 = new RSVP();
                chamberMusicRsvp2.setUser(users.get("mason")); // user with username "mason.taylor"
                chamberMusicRsvp2.setEvent(classicalRecital);
                chamberMusicRsvp2.setRsvpDate(LocalDateTime.of(2025, 8, 21, 16, 0)); // RSVP'd before the event
                rsvpRepository.save(chamberMusicRsvp2);

                // RMIT Chamber Music Society Recital - Additional RSVPs (8 users on Aug 15, 2025)
                String[] chamberUsers = {"liam", "noah", "michael", "sarah", "james", "sophia", "daniel", "emma"};
                for (String username : chamberUsers) {
                    RSVP rsvp = new RSVP();
                    rsvp.setUser(users.get(username));
                    rsvp.setEvent(classicalRecital);
                    rsvp.setRsvpDate(LocalDateTime.of(2025, 8, 15, 10, 0)); // Aug 15, 2025
                    rsvpRepository.save(rsvp);
                }

                // RMIT Annual Winter Ball 2025 RSVP (mason.taylor user)
                RSVP winterBallRsvp = new RSVP();
                winterBallRsvp.setUser(users.get("mason")); // user with username "mason.taylor"
                winterBallRsvp.setEvent(winterBall);
                winterBallRsvp.setRsvpDate(LocalDateTime.of(2025, 8, 6, 10, 0)); // RSVP'd on August 6, 2025
                rsvpRepository.save(winterBallRsvp);

                // RMIT Aquatic Championships RSVPs (5 users with different payment statuses)
                // RSVP 1: michael - Paid
                RSVP swimmingRsvp1 = new RSVP();
                swimmingRsvp1.setUser(users.get("michael"));
                swimmingRsvp1.setEvent(swimming);
                swimmingRsvp1.setRsvpDate(LocalDateTime.of(2025, 10, 1, 10, 15));
                swimmingRsvp1.setPaymentStatus("paid");
                swimmingRsvp1.setAmountPaid(new BigDecimal("30.00"));
                swimmingRsvp1.setStripePaymentIntentId("pi_test_michael_12345");
                rsvpRepository.save(swimmingRsvp1);

                // RSVP 2: emma - Pending
                RSVP swimmingRsvp2 = new RSVP();
                swimmingRsvp2.setUser(users.get("emma"));
                swimmingRsvp2.setEvent(swimming);
                swimmingRsvp2.setRsvpDate(LocalDateTime.of(2025, 10, 2, 14, 20));
                swimmingRsvp2.setPaymentStatus("pending");
                rsvpRepository.save(swimmingRsvp2);

                // RSVP 3: liam - Paid
                RSVP swimmingRsvp3 = new RSVP();
                swimmingRsvp3.setUser(users.get("liam"));
                swimmingRsvp3.setEvent(swimming);
                swimmingRsvp3.setRsvpDate(LocalDateTime.of(2025, 10, 3, 9, 45));
                swimmingRsvp3.setPaymentStatus("paid");
                swimmingRsvp3.setAmountPaid(new BigDecimal("30.00"));
                swimmingRsvp3.setStripePaymentIntentId("pi_test_liam_67890");
                rsvpRepository.save(swimmingRsvp3);

                // RSVP 4: sophia - Pending
                RSVP swimmingRsvp4 = new RSVP();
                swimmingRsvp4.setUser(users.get("sophia"));
                swimmingRsvp4.setEvent(swimming);
                swimmingRsvp4.setRsvpDate(LocalDateTime.of(2025, 10, 5, 16, 30));
                swimmingRsvp4.setPaymentStatus("pending");
                rsvpRepository.save(swimmingRsvp4);

                // RSVP 5: noah - Paid
                RSVP swimmingRsvp5 = new RSVP();
                swimmingRsvp5.setUser(users.get("noah"));
                swimmingRsvp5.setEvent(swimming);
                swimmingRsvp5.setRsvpDate(LocalDateTime.of(2025, 10, 7, 11, 10));
                swimmingRsvp5.setPaymentStatus("paid");
                swimmingRsvp5.setAmountPaid(new BigDecimal("30.00"));
                swimmingRsvp5.setStripePaymentIntentId("pi_test_noah_11223");
                rsvpRepository.save(swimmingRsvp5);
            }
            
            System.out.println("✓ Sample data created successfully!");
            System.out.println("Database initialized with test data:");
            System.out.println("- 35 users created");
            System.out.println("- 8 categories created");
            System.out.println("- 33 events created (8 original + 20 additional + 4 August 2025)");
            System.out.println("- 41 RSVPs created (1 Beach Volleyball + 15 Cybersecurity Workshop + 25 Battle of the Bands)");
            System.out.println("\n🌐 Application is ready!");
            System.out.println("Home: http://localhost:8080/");
        }
    }
}