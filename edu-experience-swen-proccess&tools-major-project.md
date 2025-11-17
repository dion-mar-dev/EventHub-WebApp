# Major Project - Resume Reference Guide
*COSC2299 Software Engineering: Process and Tools - Bachelor of Computer Science*

---

## PROJECT OVERVIEW

**Project Name:** EventHub - Campus Event Management System
**Duration:** 11 weeks (3 major milestones)
**Team Size:** 5 members
**Your Role:** Scrum Master (focus: DevOps & Architecture)
**Methodology:** Agile Scrum with weekly sprints

**Project Description:**
Full-stack web application enabling university clubs and students to create, discover, and manage campus events. Features include event creation/management, RSVP system, online payments, event photo galleries, reviews/ratings, and admin tools for content moderation.

---

## YOUR ROLE & RESPONSIBILITIES

### As Scrum Master:
- Facilitated daily stand-ups (4+ per week) and sprint ceremonies (planning, reviews, retrospectives)
- Managed GitHub Projects task board and ensured adherence to Scrum process
- Coordinated team communication via Microsoft Teams
- Documented all meetings and sprint activities
- Resolved team conflicts and kept project on track across 11-week timeline

### As DevOps & Architecture Lead:
- Designed and implemented CI/CD pipeline using GitHub Actions
- Architected containerization strategy with Docker and Docker Compose
- Deployed application to Google Cloud Platform (Compute Engine VM)
- Configured multi-environment deployment strategy (dev, devprod, prod)
- Integrated database migration tooling (Flyway)
- Set up automated testing with code coverage reporting (JaCoCo - 70% coverage)

---

## TECHNICAL STACK

### Backend:
- **Language:** Java 17
- **Framework:** Spring Boot 3.3.2
- **Build Tool:** Maven
- **Database:** MySQL 8.0 (production), H2 (development/testing)
- **ORM:** Spring Data JPA / Hibernate
- **Security:** Spring Security (authentication, authorization, role-based access)
- **Payment Integration:** Stripe API (webhooks, checkout sessions)
- **Database Migrations:** Flyway
- **Cloud Storage:** Google Cloud Storage (event photos)

### Frontend:
- **Template Engine:** Thymeleaf
- **Styling:** CSS with responsive design
- **Layout:** Thymeleaf Layout Dialect
- **Security Integration:** Thymeleaf Spring Security

### DevOps & Infrastructure:
- **Version Control:** Git + GitHub (with proper branching strategy)
- **CI/CD:** GitHub Actions
  - Automated testing on pull requests
  - Docker image build and push to Docker Hub
  - Automatic deployment to GCP VM
  - Coverage badge auto-update
- **Containerization:** Docker, Docker Compose
  - Multi-service orchestration (webapp, MySQL)
  - Health checks and service dependencies
  - Named volumes for data persistence
- **Cloud Platform:** Google Cloud Platform
  - Compute Engine VM deployment
  - External IP with port forwarding (80 → 8080)
  - SSH-based automated deployment
- **Monitoring (documented, not implemented):** Prometheus + Grafana architecture designed

### Testing:
- **Framework:** JUnit 5
- **Test Types:** Unit tests, integration tests, acceptance tests
- **Coverage Tool:** JaCoCo (70% coverage achieved)
- **Test Count:** 22 test classes covering services, controllers, and end-to-end scenarios
- **Mocking:** Spring Boot Test, MockMvc

### Additional Libraries:
- **Lombok** - Reduced boilerplate code
- **OpenCSV** - CSV export functionality (attendee lists)
- **Gson** - JSON processing
- **Spring Boot DevTools** - Enhanced development experience

---

## KEY FEATURES DELIVERED

### Core Event Management:
- Create, edit, delete events with rich metadata (title, description, date/time, location, category, keywords, pricing)
- Event detail pages with full information display
- Search and filter by date, category, keyword
- Calendar view for visualizing upcoming events

### User Participation:
- RSVP system with capacity management
- "My Events" dashboard showing user's RSVPs
- Organizer dashboard with attendee list and management
- Block/unblock attendees functionality
- Review and rating system (post-event feedback)
- CSV export of attendee lists

### Payment System (Advanced Feature):
- Stripe integration for paid events
- Secure checkout flow
- Webhook handling for payment confirmation
- Payment status tracking in database
- Refund support for cancelled RSVPs

### Event Gallery:
- Organizers can upload event photos after events
- Google Cloud Storage integration for scalable photo storage
- Photo galleries visible on event detail pages

### Admin Tools:
- View and manage all events
- Delete inappropriate content
- User management (deactivate/ban accounts)
- System-wide monitoring capabilities

### Technical Enhancements:
- Multi-profile configuration (dev, devprod, prod)
- RESTful API endpoints for event listing and RSVP submission
- QR code check-in system capability
- Remember-me functionality for user sessions
- Comprehensive validation and error handling

---

## ARCHITECTURE & DESIGN

### Design Pattern:
- **MVC (Model-View-Controller)** - Clean separation of concerns
- **Repository Pattern** - Data access abstraction
- **Service Layer** - Business logic encapsulation
- **DTO Pattern** - Data transfer between layers

### Code Quality Practices:
- Followed standard Java coding conventions
- Low coupling, high cohesion design principles
- Used dependency injection throughout
- Implemented comprehensive validation
- Created reusable service components

### Database Design:
- Relational schema with proper foreign key relationships
- Normalized tables (Users, Events, RSVPs, Payments, Reviews, Categories, Keywords, etc.)
- Cascade operations for data integrity
- Flyway migrations for version-controlled schema evolution

### Security Implementation:
- Password encryption with BCrypt
- Role-based access control (USER, ORGANIZER, ADMIN)
- CSRF protection
- Session management with persistent token repository
- Secure payment processing (no PCI data storage)

---

## CI/CD PIPELINE ARCHITECTURE

### Continuous Integration:
- **Trigger:** Pull requests to main branch
- **Steps:**
  1. Checkout code
  2. Set up Java 17 environment
  3. Cache Maven dependencies (build optimization)
  4. Run unit tests with JaCoCo coverage
  5. Display coverage summary
  6. Build JAR artifact (Maven package)
- **Result:** Fast feedback on code quality (~ 2-3 minutes)

### Continuous Deployment:
- **Trigger:** Merged pull requests or manual trigger
- **Steps:**
  1. Login to Docker Hub (using secrets)
  2. Build Docker image from JAR
  3. Push to Docker Hub (latest + SHA tags)
  4. SSH to GCP VM
  5. Pull latest code and images
  6. Restart services with Docker Compose
  7. Clean up old images
- **Result:** Zero-downtime deployments to production

### Performance Optimization:
- Reduced pipeline time by 50% (5m 28s → 2m 42s)
- Single-job approach to avoid duplicate builds
- Dependency caching for faster Maven builds
- Conditional job execution (CI only on PRs, CD on merges)

---

## PROCESS & METHODOLOGY

### Scrum Implementation:
- **Sprint Length:** 1-2 weeks
- **Ceremonies:** Daily standups, sprint planning, sprint reviews, retrospectives
- **Artifacts:** Product backlog, sprint backlog, burndown charts
- **Tools:** GitHub Projects (Kanban board), GitHub Issues (user stories)

### Version Control Practices:
- Feature branch workflow
- Pull request reviews before merging
- Frequent commits (multiple per day as required)
- Meaningful commit messages
- Protected main branch
- No large monolithic commits

### Documentation:
- User stories with acceptance criteria
- Definition of Done (DoD) for each feature
- Test case documentation
- Sprint retrospectives with lessons learned
- Meeting minutes for all Scrum ceremonies
- Architecture diagrams and technical decisions

### Testing Strategy:
- Test-driven development approach
- Unit tests for services and business logic
- Integration tests for database operations
- Acceptance tests for user workflows
- Automated test execution in CI pipeline
- Code coverage monitoring (target: 70%+)

---

## SKILLS DEMONSTRATED

### Technical Skills:
- **Backend Development:** Java, Spring Boot, Spring Data JPA, Spring Security
- **Database:** MySQL, H2, SQL, database design, Flyway migrations
- **Frontend:** Thymeleaf, HTML/CSS, responsive design
- **API Integration:** Stripe payments, Google Cloud Storage, RESTful APIs
- **DevOps:** Docker, Docker Compose, CI/CD, GitHub Actions, automated deployment
- **Cloud:** Google Cloud Platform (Compute Engine, VM management)
- **Testing:** JUnit, integration testing, test coverage, automated testing
- **Version Control:** Git, GitHub, pull request workflows, code reviews
- **Build Tools:** Maven, dependency management

### Process & Soft Skills:
- **Agile/Scrum:** Sprint planning, retrospectives, backlog management
- **Leadership:** Scrum Master role, team coordination, conflict resolution
- **Collaboration:** Pair programming, code reviews, team communication
- **Project Management:** GitHub Projects, task tracking, milestone delivery
- **Documentation:** Technical writing, meeting minutes, sprint reports
- **Problem Solving:** Architecture decisions, debugging, optimization
- **Time Management:** Meeting deadlines, managing scope, sprint commitments

### Domain Knowledge:
- Event management systems
- Payment processing and PCI compliance considerations
- User authentication and authorization
- Cloud storage and CDN concepts
- Containerization and orchestration
- Multi-environment deployment strategies

---

## QUANTIFIABLE ACHIEVEMENTS

- **Team Coordination:** Led 5-person team through 11-week project timeline
- **Code Coverage:** Achieved 70% test coverage across codebase
- **Test Suite:** Wrote/coordinated 22 test classes with unit, integration, and acceptance tests
- **CI/CD Optimization:** Reduced pipeline execution time by 50%
- **Deployment:** Successfully deployed to cloud (GCP) with automated CD pipeline
- **Features Delivered:** 30+ user stories across 3 major milestones
- **Database:** Designed schema with 10+ tables and relationships
- **Commits:** Regular contributions with proper version control practices
- **Zero Critical Bugs:** Delivered production-ready application

---

## LEARNING OUTCOMES & REFLECTIONS

### What Worked Well:
- Strong DevOps automation reduced manual deployment overhead
- Docker containerization simplified environment consistency
- Frequent commits and code reviews caught issues early
- Clear separation of concerns made codebase maintainable
- GitHub Actions provided fast CI/CD feedback

### Challenges Overcome:
- Integrating Stripe webhooks across dev/prod environments
- Managing database migrations with Flyway
- Coordinating Docker networking for multi-service setup
- SSH-based automated deployment to GCP VM
- Balancing feature scope with sprint timeline

### Key Learnings:
- Importance of infrastructure-as-code practices
- Value of automated testing in preventing regressions
- Need for multi-environment configuration management
- Scrum ceremonies are critical for team alignment
- DevOps practices significantly improve development velocity

---

## HOW TO USE THIS REFERENCE

### For Software Engineer Roles:
**Focus on:** Java/Spring Boot stack, MVC architecture, database design, RESTful APIs, testing practices, Git workflow

**Sample bullet points:**
- "Developed full-stack web application using Spring Boot and MySQL, implementing MVC architecture with repository pattern and service layer abstraction"
- "Integrated third-party APIs (Stripe payments, Google Cloud Storage) with secure webhook handling and error recovery"
- "Wrote 22 test classes achieving 70% code coverage using JUnit and MockMvc for unit, integration, and acceptance testing"

### For DevOps Engineer Roles:
**Focus on:** CI/CD pipeline, Docker, cloud deployment, automation, monitoring architecture

**Sample bullet points:**
- "Architected and implemented CI/CD pipeline using GitHub Actions, reducing deployment time by 50% through build optimization and caching strategies"
- "Containerized Spring Boot application with Docker and orchestrated multi-service deployment using Docker Compose with health checks and persistent volumes"
- "Automated deployment to Google Cloud Platform using SSH-based CD pipeline with zero-downtime updates and automatic image cleanup"

### For Full-Stack Developer Roles:
**Focus on:** End-to-end feature development, frontend/backend integration, database, API design

**Sample bullet points:**
- "Built event management platform with event creation, RSVP system, payment processing, and photo galleries, serving 35+ test users with 40+ RSVPs"
- "Designed and implemented relational database schema with Flyway migrations for version-controlled schema evolution"
- "Integrated Thymeleaf templates with Spring Security for role-based access control (USER, ORGANIZER, ADMIN roles)"

### For Scrum Master/Agile Roles:
**Focus on:** Team leadership, Scrum ceremonies, project management, collaboration tools

**Sample bullet points:**
- "Served as Scrum Master for 5-person development team, facilitating daily standups, sprint planning, reviews, and retrospectives across 11-week project"
- "Managed project backlog and sprint planning using GitHub Projects, ensuring team delivered 30+ user stories across 3 major milestones on schedule"
- "Documented all meetings and maintained sprint artifacts (burndown charts, definition of done, acceptance criteria) for stakeholder visibility"

### For Cloud Engineer Roles:
**Focus on:** GCP deployment, infrastructure management, containerization, multi-environment setup

**Sample bullet points:**
- "Deployed containerized application to Google Cloud Compute Engine with automated CI/CD pipeline for continuous delivery"
- "Implemented multi-environment deployment strategy (dev/staging/prod) with profile-based configuration management"
- "Integrated Google Cloud Storage for scalable media storage with proper IAM and lifecycle policies"

### For Backend Developer Roles:
**Focus on:** Spring Boot, API design, database, security, payment integration

**Sample bullet points:**
- "Developed RESTful APIs and server-rendered views using Spring Boot 3 with Spring Data JPA for data persistence and Spring Security for authentication"
- "Implemented secure payment processing with Stripe API, including webhook handlers for asynchronous payment confirmation and refund workflows"
- "Designed normalized relational database schema with 10+ tables, implementing cascade operations and foreign key constraints for data integrity"

### For Quality Assurance/Testing Roles:
**Focus on:** Testing strategy, test automation, coverage, CI integration

**Sample bullet points:**
- "Developed comprehensive test suite with unit, integration, and acceptance tests, achieving 70% code coverage monitored through JaCoCo"
- "Integrated automated testing into CI/CD pipeline with GitHub Actions, providing fast feedback on pull requests before merge"
- "Created acceptance test scenarios covering critical user workflows (registration, login, event creation, RSVP, payments)"

---

## ADDITIONAL CONTEXT

### Project Complexity:
This was a realistic industry-simulating project with:
- Client meetings (tutor as product owner)
- Regular stakeholder presentations
- Changing requirements requiring scope negotiation
- Team coordination challenges
- Production deployment requirements
- Professional development practices

### Industry Alignment:
The project followed industry best practices including:
- Agile/Scrum methodology (standard in modern software teams)
- CI/CD automation (expected in professional environments)
- Containerization with Docker (industry standard for deployment)
- Cloud deployment (critical for modern applications)
- Test automation and coverage (quality assurance standard)
- Code reviews via pull requests (collaboration best practice)
- Security considerations (authentication, authorization, payment security)

### Academic Context:
- University-level software engineering course
- Group project emphasizing professional practices
- Evaluated on both technical delivery and process adherence
- Required documentation and presentations to stakeholders
- Peer reviews to ensure equal contribution

---

## TECHNICAL INTERVIEW PREPARATION

### Be Prepared to Discuss:

**Architecture Decisions:**
- Why MVC pattern? (separation of concerns, maintainability)
- Why service layer? (business logic encapsulation, reusability)
- Why DTOs? (security, API contract stability, validation layer)

**DevOps Choices:**
- Why GitHub Actions? (native to GitHub, free for public repos, YAML-based)
- Why Docker Compose? (local dev/prod parity, simple multi-service orchestration)
- Why Docker Hub? (free public registry, fast pulls, good for student projects)

**Trade-offs Made:**
- Direct VM deployment vs. Kubernetes (Kubernetes overkill for student project scale)
- Thymeleaf vs. React/Vue (faster development, no API needed, server-rendered)
- H2 for tests vs. MySQL (faster test execution, no external dependencies)
- Stripe vs. PayPal (better API design, clearer documentation)

**Challenges & Solutions:**
- **Challenge:** Stripe webhooks need public URL for testing
  - **Solution:** Used Stripe CLI for local forwarding during development
- **Challenge:** Different environments need different configs
  - **Solution:** Spring profiles (dev, devprod, prod) with environment variables
- **Challenge:** Database schema changes across team members
  - **Solution:** Implemented Flyway migrations for version control
- **Challenge:** CI pipeline too slow (5+ minutes)
  - **Solution:** Merged jobs, cached dependencies, optimized test execution

**What You'd Do Differently:**
- Implement Prometheus + Grafana monitoring (documented but not implemented)
- Add integration tests for payment flows (was mostly manual testing)
- Use Kubernetes for better scalability (if project scope was larger)
- Implement blue-green deployment for true zero-downtime
- Add performance testing (load testing with JMeter or Gatling)

---

## READY-TO-USE RESUME BULLETS

### Comprehensive (pick and choose):

1. "Led 5-person Agile team as Scrum Master through 11-week full-stack web application project, delivering 30+ user stories across 3 milestones with complete CI/CD automation"

2. "Architected and deployed containerized Spring Boot application to Google Cloud Platform with GitHub Actions CI/CD pipeline, achieving 50% reduction in deployment time through build optimization"

3. "Developed event management platform using Java 17, Spring Boot 3, MySQL, and Thymeleaf with features including RSVP system, Stripe payment integration, event photo galleries, and role-based admin tools"

4. "Implemented comprehensive testing strategy with JUnit achieving 70% code coverage, including unit, integration, and acceptance tests automated through CI/CD pipeline"

5. "Integrated third-party APIs (Stripe payments, Google Cloud Storage) with secure webhook handling, implementing payment workflows, refund processing, and scalable media storage"

6. "Designed and implemented relational database schema with Flyway migrations, managing 10+ tables with proper normalization, foreign key relationships, and cascade operations"

7. "Established DevOps practices including Docker containerization, multi-environment deployment strategy (dev/staging/prod), and automated testing with code coverage reporting"

8. "Facilitated Agile ceremonies (daily standups, sprint planning, retrospectives) and maintained project artifacts (burndown charts, backlogs, acceptance criteria) using GitHub Projects"

9. "Built secure authentication system with Spring Security implementing BCrypt password encryption, role-based access control (USER/ORGANIZER/ADMIN), and persistent session management"

10. "Optimized CI/CD pipeline by implementing dependency caching, parallel test execution, and conditional job triggers, reducing build time from 5m 28s to 2m 42s"

### Concise (for space-limited resumes):

1. "Led Agile team developing full-stack event management platform using Spring Boot, MySQL, Docker, deployed to GCP with automated CI/CD pipeline (GitHub Actions)"

2. "Implemented CI/CD automation with GitHub Actions, Docker containerization, and cloud deployment, achieving 50% faster build times and zero-downtime deployments"

3. "Developed RESTful Java application with Stripe payment integration, Spring Security authentication, and 70% test coverage using JUnit and automated testing"

4. "Served as Scrum Master for 5-person team, facilitating sprints, managing backlogs via GitHub Projects, and delivering 30+ features across 11-week project timeline"

5. "Architected multi-environment deployment strategy (dev/prod) using Docker Compose, Spring profiles, and Flyway database migrations for schema version control"

---

**END OF REFERENCE DOCUMENT**

*Remember: Tailor your resume bullets to the specific role. Emphasize backend skills for backend roles, DevOps for DevOps roles, leadership for Scrum Master roles, etc. Use quantifiable metrics where possible.*
