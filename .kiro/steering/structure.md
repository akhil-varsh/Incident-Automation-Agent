# Project Structure

## Root Directory Organization

```
├── src/main/java/com/xlbiz/incident/agent/    # Main Java source code
├── src/test/java/                             # Test source code
├── ui/                                        # React frontend application
├── docker/                                    # Docker configuration files
├── .kiro/                                     # Kiro AI assistant configuration
├── *.sh                                       # Shell scripts for setup/testing
├── *.md                                       # Documentation files
├── pom.xml                                    # Maven build configuration
├── docker-compose.yml                         # Multi-container Docker setup
└── Dockerfile                                 # Application container definition
```

## Java Package Structure

**Base Package**: `com.xlbiz.incident.agent`

- **`config/`** - Spring configuration classes, beans, and application setup
- **`controller/`** - REST API endpoints and web controllers
- **`dto/`** - Data Transfer Objects for API requests/responses
- **`model/`** - JPA entities and domain models
- **`repository/`** - Data access layer (Spring Data JPA repositories)
- **`service/`** - Business logic and service layer implementations
- **`test/`** - Integration tests and test utilities
- **`IncidentAgentApplication.java`** - Main Spring Boot application class

## Frontend Structure (ui/)

```
ui/
├── src/                    # React source code
├── public/                 # Static assets
├── package.json           # Node.js dependencies
└── package-lock.json      # Dependency lock file
```

## Configuration Files

- **`application.yml`** - Spring Boot application configuration
- **`.env`** - Environment variables for secrets and external service configs
- **`docker-compose.yml`** - Multi-service container orchestration
- **`pom.xml`** - Maven dependencies and build configuration

## Documentation Structure

- **`README.md`** - Main project documentation and setup instructions
- **`VOICE_INTEGRATION.md`** - Voice feature setup and usage guide
- **`VONAGE_SETUP_GUIDE.md`** - Vonage API configuration details
- **`FRONTEND_TROUBLESHOOTING.md`** - UI-specific troubleshooting
- **`TWILIO_WEBHOOK_SETUP.md`** - Alternative voice provider setup

## Database Schema

- **`incidents`** - Main incident records with AI analysis and integration data
- **`voice_calls`** - Voice call tracking and transcription processing
- **Flyway migrations** - Located in `src/main/resources/db/migration/`

## Key Architectural Principles

- **Separation of Concerns**: Clear boundaries between web, service, and data layers
- **Domain-Driven Design**: Models reflect business concepts (Incident, VoiceCall)
- **Configuration Externalization**: Environment-specific settings in `.env` and `application.yml`
- **Test Organization**: Integration tests alongside main code for better cohesion
- **Documentation Co-location**: Feature-specific docs at root level for easy discovery