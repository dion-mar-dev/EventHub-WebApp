
# RMIT COSC2299 SEPT Major Project

![Coverage](https://img.shields.io/badge/coverage-70%25-brightgreen) _Last updated: 2025-10-11 21:13:00_

## Group-P2-06 Information

📦 Note: Public replica of the original working codebase used in RMIT COSC2299 Software Engineering project. All functionality remains intact.

### Project landing page

* Dion M   - Scrum master           - (focus: DevOps & architecture)
* Andre W           - Development team member  - (focus: business analyst/technical writer)
* Yuyang S        - Development team member  - (focus: QA/testing)
* Ajay P      - Development team member  - (focus: UI/UX design)
* Kelin L          - Development team member  - (focus: software engineering)

## Navigation Hub/Project Directory

* Github repository: [click me](https://github.com/cosc2299-2025/team-project-group-p02-06)
* Github Project Board : [click me](https://github.com/orgs/cosc2299-2025/projects/179/views/1)
* Communication tool: [click me](https://teams.microsoft.com/l/channel/19%3A3Dp3ArxLupOAWTUj0CpuLdz-K7KuyWocLvuOWrmTVdc1%40thread.tacv2/General?groupId=14f6057f-dcf7-4638-a3e7-5eac71743661&tenantId=d1323671-cdbe-4417-b4d4-bdb24b51316b)

### Running the Application
* Default (dev) profile: H2 in-memory database for quick development
* **devprod profile: MySQL persistence with localhost Stripe URLs (recommended for development/testing) - use `docker-compose.yml`**
* prod profile: MySQL with VM IP (http://34.129.54.215) (TRY LINK, VM is running until 30/11/25), GCS, and production Stripe webhooks for deployed environments on Google Cloud VM - use `docker-compose.remote.yml`
* Local Docker run: `docker-compose up -d` starts MySQL and application with devprod profile at http://localhost:8080
