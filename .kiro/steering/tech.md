# Technology Stack

## Backend (Java/Spring Boot)

- **Framework**: Spring Boot 3.2.1 with Java 17
- **Build Tool**: Maven 3.8+
- **Database**: PostgreSQL 15 with Flyway migrations
- **Caching**: Redis 7
- **Vector Database**: ChromaDB 0.4.24 for AI embeddings
- **AI Integration**: Spring AI with Groq LLM (OpenAI-compatible API)

## Key Dependencies

- **Spring Boot Starters**: Web, Data JPA, Validation, Actuator, Data Redis, WebFlux
- **External APIs**: Slack API Client, Vonage Voice API, Twilio SDK, Google Cloud Speech-to-Text
- **Database**: PostgreSQL driver, Hypersistence Utils for JSONB support
- **Testing**: TestContainers, WireMock, Spring Boot Test

## Frontend (React)

- **Framework**: React 18.2.0
- **UI Library**: Material-UI (MUI) 5.15.0
- **Build Tool**: Create React App (react-scripts 5.0.1)

## Infrastructure

- **Containerization**: Docker & Docker Compose
- **Development Environment**: WSL2 (required)
- **Webhook Tunneling**: ngrok for local development

## Common Commands

### Development Setup (WSL2)
```bash
# Start Docker services
sudo service docker start
docker-compose up -d

# Build and run application
mvn clean package
mvn spring-boot:run

# Run end-to-end tests
mvn test-compile exec:java -Dexec.mainClass="com.xlbiz.incident.agent.test.EndToEndIncidentTest" -Dexec.classpathScope=test
```

### Frontend Development
```bash
cd ui
npm install
npm start
```

### Voice Integration Testing
```bash
# Test voice processing
curl -X POST "http://localhost:8080/api/voice/test" \
  -d "transcription=Critical database outage" \
  -d "callerNumber=+1555123456"

# Health check
curl http://localhost:8080/api/voice/health
```

### Database Operations
```bash
# Run Flyway migrations
mvn flyway:migrate

# Check application health
curl http://localhost:8080/actuator/health
```

## Architecture Patterns

- **Layered Architecture**: Controller → Service → Repository pattern
- **DTO Pattern**: Separate DTOs for API requests/responses
- **Configuration Management**: Environment-based configuration with Spring profiles
- **Async Processing**: Non-blocking integrations for Slack/Jira/AI services
- **Event-Driven**: Webhook-based voice integration processing