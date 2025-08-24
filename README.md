# Incident Automation Agent

An AI-powered incident management system that automates the entire incident lifecycle from reporting to resolution. The system accepts incident reports through multiple channels (REST API, voice calls) and automatically processes them using AI classification, creates Slack channels and sends messages, and provides intelligent suggestions based on Ai Classification/knowledge base.

## 🎯 Key Features

### 🤖 AI-Powered Intelligence
- **Automatic Severity Classification**: Uses Groq LLM to analyze incident descriptions and assign appropriate severity levels (LOW, MEDIUM, HIGH)
- **Intelligent Suggestions**: Provides step-by-step remediation recommendations based on incident analysis
- **Vector Knowledge Base**: ChromaDB-powered similarity search for finding relevant solutions from past incidents
- **Confidence Scoring**: AI provides confidence levels for its classifications and suggestions

### 📞 Multi-Channel Incident Reporting
- **REST API**: Direct incident submission via HTTP endpoints
- **Voice Integration**: Phone-based incident reporting with speech-to-text processing
- **Twilio Integration**: Outbound calls for incident notifications and escalations
- **DTMF Support**: Interactive voice response for incident acknowledgment

### 🔗 Enterprise Integrations
- **Slack Integration**: Automatic channel creation, notifications, and stakeholder alerts
- **Jira Integration**: Automatic ticket creation with proper priority mapping
- **Real-time Updates**: Bidirectional sync between systems for status updates

### 📊 Comprehensive Dashboard
- **React UI**: Modern web interface for incident management
- **Real-time Monitoring**: Live incident status and metrics
- **Outbound Call Center**: Interface for making incident notification calls
- **Knowledge Management**: Browse and manage the incident knowledge base

### 🔍 Advanced Analytics
- **Incident Tracking**: Complete audit trail from creation to resolution
- **Performance Metrics**: Response times, resolution rates, and trend analysis
- **Voice Call Analytics**: Call duration, success rates, and transcription accuracy

## 🏗️ Architecture Design

### System Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Input Layer   │    │  Processing     │    │   Integration   │
│                 │    │     Layer       │    │     Layer       │
├─────────────────┤    ├─────────────────┤    ├─────────────────┤
│ • REST API      │───▶│ • Spring Boot   │───▶│ • Slack API     │
│ • Voice Calls   │    │ • AI Classifier │    │ • Jira API      │
│ • React UI      │    │ • Knowledge Base│    │ • Twilio API    │
│ • Webhooks      │    │ • Vector Search │    │ • Groq AI       │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                     Data Layer                                  │
├─────────────────┬─────────────────┬─────────────────┬───────────┤
│   PostgreSQL    │    ChromaDB     │     Redis       │  Ollama   │
│   (Primary)     │   (Vectors)     │   (Cache)       │(Embeddings)│
└─────────────────┴─────────────────┴─────────────────┴───────────┘
```

### Component Overview
- **Spring Boot Application**: Central orchestrator with REST API endpoints
- **AI Classification Service**: Groq LLM integration for intelligent incident analysis
- **Vector Knowledge Base**: ChromaDB with Ollama embeddings for similarity search
- **Voice Integration**: Twilio APIs with Google Cloud Speech-to-Text
- **Enterprise Integrations**: Slack channels, Jira tickets, and real-time notifications
- **React Dashboard**: Modern UI for incident management and monitoring
- **Multi-Database Architecture**: PostgreSQL (primary), ChromaDB (vectors)

## 🛠️ Implemented Tools & APIs

### 🎯 Core Incident Management
- **Incident Creation API**: `POST /api/v1/incidents` - Create new incidents with automatic AI classification
- **Incident Retrieval**: `GET /api/v1/incidents` - List all incidents with filtering and pagination
- **Incident Details**: `GET /api/v1/incidents/{id}` - Get detailed incident information
- **Status Updates**: Real-time incident status tracking and notifications

### 🤖 AI & Machine Learning
- **Groq LLM Integration**: Advanced language model for incident classification and analysis
- **AI Classification Service**: Automatic severity assessment with confidence scoring
- **Intelligent Suggestions**: Context-aware remediation recommendations
- **Ollama Embeddings**: Local embedding generation for vector similarity search
- **ChromaDB Vector Store**: Semantic search across incident knowledge base
- **Knowledge Base Management**: Store and retrieve solutions from past incidents

### 📞 Voice & Communication
- **Twilio Voice Integration**: 
  - Inbound call handling with speech-to-text
  - Outbound notification calls with DTMF support
  - Call recording and transcription
  - Interactive voice response (IVR) system
- **Google Cloud Speech-to-Text**: High-accuracy voice transcription
- **Deepgram Fallback**: Alternative speech-to-text service
- **Voice Call Analytics**: Call duration, success rates, and quality metrics

### 🔗 Enterprise Integrations
- **Slack Integration**:
  - Automatic channel creation for incidents
  - Real-time notifications to stakeholders
  - Incident status updates and threading
  - Custom message formatting with severity indicators
- **Jira Integration**:
  - Automatic ticket creation with proper priority mapping
  - Bidirectional status synchronization
  - Custom field mapping and project configuration
  - Attachment and comment synchronization

### 📊 User Interface
- **React Dashboard**: Modern web interface built with Material-UI
- **Incident Management Panel**: Create, view, and manage incidents
- **Outbound Call Center**: Interface for making notification calls
- **Knowledge Base Browser**: Search and manage incident solutions
- **Real-time Updates**: Live incident status and metrics
- **Responsive Design**: Mobile-friendly interface

### 🗄️ Data Management
- **PostgreSQL Database**: Primary data storage with JSONB support
- **Flyway Migrations**: Database schema versioning and updates
- **ChromaDB Vector Database**: Semantic search and similarity matching
- **Data Export/Import**: Backup and restore capabilities

### 🔍 Monitoring & Analytics
- **Health Check Endpoints**: System status and component health monitoring
- **Actuator Integration**: Spring Boot metrics and monitoring
- **Prometheus Metrics**: Performance and usage statistics
- **Audit Logging**: Complete incident lifecycle tracking
- **Error Handling**: Comprehensive error reporting and recovery

### 🔧 Development & Testing Tools
- **End-to-End Testing**: Automated incident workflow testing
- **Integration Tests**: Component interaction validation
- **Mock Services**: WireMock for external API testing
- **TestContainers**: Isolated database testing
- **Docker Compose**: Multi-service development environment

### 🌐 API Endpoints

#### Incident Management
```
POST   /api/v1/incidents              - Create incident
GET    /api/v1/incidents              - List incidents
GET    /api/v1/incidents/{id}         - Get incident details
PUT    /api/v1/incidents/{id}         - Update incident
DELETE /api/v1/incidents/{id}         - Delete incident
```

#### Voice Integration
```
POST   /api/voice/test                - Test voice processing
GET    /api/voice/health              - Voice system health
POST   /api/twilio/webhook            - Twilio webhook handler
POST   /api/twilio/outbound/call/incident-notification - Make outbound call
POST   /api/twilio/outbound/call/incident-update       - Send incident update
```

#### Knowledge Base
```
GET    /api/knowledge/search          - Search knowledge base
POST   /api/knowledge/entries         - Add knowledge entry
GET    /api/knowledge/entries/{id}    - Get knowledge entry
PUT    /api/knowledge/entries/{id}    - Update knowledge entry
```

#### System Health
```
GET    /actuator/health               - Application health
GET    /actuator/metrics              - System metrics
GET    /actuator/info                 - Application info
```

## 🚀 Setup Instructions

### Prerequisites
- **WSL2** (Windows Subsystem for Linux) - Required for this project
- Java 17+
- Maven 3.8+
- Docker & Docker Compose
- (Optional) Node.js 18+ for React UI

### 0. WSL Setup (Windows Users)
This project is designed to run in WSL2. If you're on Windows:

```bash
# Install WSL2 (run in PowerShell as Administrator)
wsl --install

# Or if WSL is already installed, ensure you're using WSL2
wsl --set-default-version 2

# Install Ubuntu (recommended)
wsl --install -d Ubuntu

# Start WSL and update packages
wsl
sudo apt update && sudo apt upgrade -y
```

**Install required tools in WSL:**
```bash
# Install Java 17
sudo apt install openjdk-17-jdk -y

# Install Maven
sudo apt install maven -y

# Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER

# Install Docker Compose
sudo apt install docker-compose -y

# Optional: Install Node.js for React UI
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt install nodejs -y
```

**Start Docker service:**
```bash
sudo service docker start
# Or enable auto-start
sudo systemctl enable docker
```

### 1. Clone the Repository
```bash
git clone <your-repo-url>
cd biz
```

### 2. Configure Environment
Create a `.env` file with your configuration:

```bash
# AI Service Configuration
GROQ_API_KEY=your_groq_api_key
GROQ_BASE_URL=https://api.groq.com/openai
GROQ_MODEL=gpt-oss-120b

# Slack Integration
SLACK_BOT_TOKEN=xoxb-your-slack-bot-token
SLACK_SIGNING_SECRET=your-slack-signing-secret

# Jira Integration
JIRA_BASE_URL=https://your-domain.atlassian.net
JIRA_EMAIL=your-email@domain.com
JIRA_API_TOKEN=your-jira-api-token
JIRA_PROJECT_KEY=YOUR_PROJECT

# Database Configuration
DB_HOST=localhost
DB_PORT=5432
DB_NAME=incident_db
DB_USER=incident_user
DB_PASSWORD=secure_password

# Voice Integration (Twilio)
TWILIO_ACCOUNT_SID=your-twilio-account-sid
TWILIO_AUTH_TOKEN=your-twilio-auth-token
TWILIO_PHONE_NUMBER=+1234567890
TWILIO_WEBHOOK_BASE_URL=https://your-ngrok-url.ngrok-free.app

# Google Cloud Speech-to-Text
GOOGLE_CLOUD_PROJECT_ID=your-project-id
GOOGLE_APPLICATION_CREDENTIALS=/path/to/credentials.json

# ChromaDB & Vector Search
CHROMA_BASE_URL=http://localhost:8000
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_ENABLED=true
```


### 3. Start Dependencies
```bash
# Make sure Docker is running in WSL
sudo service docker start

# Start the services
docker-compose up -d
```
- This will start PostgreSQL, ChromaDB, and Redis.

### 4. Build & Run the Application
```bash
# Build the project
mvn clean package

# Run the application
mvn spring-boot:run
```

**WSL Network Access:**
- Application will be available at `http://localhost:8080` from both WSL and Windows
- If you have issues accessing from Windows, use your WSL IP: `http://<wsl-ip>:8080`
- Find WSL IP with: `ip addr show eth0 | grep inet`

### 5. (Optional) Run End-to-End Test
```bash
mvn test-compile exec:java -Dexec.mainClass="com.xlbiz.incident.agent.test.EndToEndIncidentTest" -Dexec.classpathScope=test
```

### 6. (Optional) Start the React UI
```bash
cd ui
npm install
npm start
```

### 7. (Optional) Test Voice Integration
```bash
# Test voice processing endpoint
curl -X POST "http://localhost:8080/api/voice/test" \
  -d "transcription=Critical database outage in production affecting all users" \
  -d "callerNumber=+1555123456"

# Check voice integration health
curl http://localhost:8080/api/voice/health

# Test outbound call
curl -X POST "http://localhost:8080/api/twilio/outbound/call/incident-notification" \
  -H "Content-Type: application/json" \
  -d '{
    "toPhoneNumber": "+1234567890",
    "incidentId": "INCIDENT-123",
    "severity": "HIGH",
    "message": "Critical incident requires immediate attention"
  }'
```

## 💻 Technology Stack

### Backend Technologies
- **Java 17**: Modern Java with enhanced performance and features
- **Spring Boot 3.2.1**: Enterprise-grade application framework
- **Spring AI**: AI integration framework with Groq LLM support
- **Spring Data JPA**: Database abstraction and ORM
- **Spring WebFlux**: Reactive programming for async operations
- **Maven 3.8+**: Build automation and dependency management

### Databases & Storage
- **PostgreSQL 15**: Primary relational database with JSONB support
- **ChromaDB 0.4.24**: Vector database for AI embeddings and similarity search
- **Ollama**: Local embedding generation service using nomic-embed-text:v1.5
- **Flyway**: Database migration and versioning

### AI & Machine Learning
- **Groq API**: High-performance LLM for incident classification
- **OpenAI-Compatible API**: Flexible AI service integration
- **ChromaDB Embeddings**: Vector similarity search
- **Ollama Embeddings**: Local embedding generation
- **Spring AI Framework**: Unified AI service abstraction

### Communication & Integration
- **Twilio API**: Voice calls, SMS, and webhook handling
- **Slack API Client**: Real-time messaging and channel management
- **Google Cloud Speech-to-Text**: High-accuracy voice transcription
- **Deepgram API**: Alternative speech-to-text service

### Frontend Technologies
- **React 18.2.0**: Modern JavaScript UI framework
- **Material-UI (MUI) 5.15.0**: Professional React component library
- **Create React App**: Build tooling and development server
- **Responsive Design**: Mobile-friendly interface

### DevOps & Infrastructure
- **Docker & Docker Compose**: Containerization and orchestration
- **WSL2**: Windows Subsystem for Linux development environment
- **ngrok**: Webhook tunneling for local development
- **TestContainers**: Integration testing with real databases
- **WireMock**: HTTP service mocking for tests

### Monitoring & Observability
- **Spring Boot Actuator**: Health checks and metrics
- **Prometheus**: Metrics collection and monitoring
- **Structured Logging**: JSON-formatted logs with correlation IDs
- **Error Tracking**: Comprehensive error handling and reporting

## 📖 Usage Examples

### Creating an Incident via API
```bash
curl -X POST "http://localhost:8080/api/v1/incidents" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "INCIDENT-001",
    "type": "DATABASE_ERROR",
    "description": "PostgreSQL connection pool exhausted, users unable to login",
    "source": "monitoring-system",
    "metadata": {
      "environment": "production",
      "affected_users": 1500,
      "database_host": "db-prod-01"
    }
  }'
```

### Voice-Based Incident Reporting
1. Call the configured Twilio phone number
2. Describe the incident when prompted
3. System automatically transcribes and processes the incident
4. AI classifies severity and creates Slack/Jira tickets

### Making Outbound Notification Calls
```bash
curl -X POST "http://localhost:8080/api/twilio/outbound/call/incident-notification" \
  -H "Content-Type: application/json" \
  -d '{
    "toPhoneNumber": "+1234567890",
    "incidentId": "INCIDENT-001",
    "severity": "HIGH",
    "message": "Database connection pool exhausted - immediate attention required"
  }'
```

### Searching Knowledge Base
```bash
curl -X GET "http://localhost:8080/api/knowledge/search?query=database+connection+timeout&maxResults=5"
```

### Using the React Dashboard
1. Navigate to `http://localhost:3000`
2. View active incidents on the main dashboard
3. Use the "Outbound Call Center" to make notification calls
4. Browse the knowledge base for solutions
5. Monitor real-time incident status updates

## ⚖️ Trade-offs & Assumptions

### 🎯 Design Decisions
- **AI-First Approach**: Groq LLM provides intelligent classification with rule-based fallback
- **Multi-Database Architecture**: PostgreSQL (primary), ChromaDB (vectors) for optimal performance
- **Async Processing**: Non-blocking integrations prevent system bottlenecks
- **Microservice-Ready**: Modular design supports future service decomposition
- **API-First Design**: All functionality accessible via REST API, UI is optional

### 🔒 Security Considerations
- **No Authentication by Default**: Add Spring Security or API Gateway for production
- **Environment Variables**: Sensitive data stored in `.env` files
- **HTTPS Required**: Voice webhooks require secure endpoints
- **Token Management**: Secure storage of API tokens and credentials

### 🚀 Performance Optimizations
- **Redis Caching**: Frequent queries cached for improved response times
- **Async Operations**: Slack/Jira/AI calls don't block main workflow
- **Connection Pooling**: Database connections optimized for concurrent access
- **Vector Search**: ChromaDB provides fast semantic similarity matching

### 🔧 Extensibility Features
- **Plugin Architecture**: Easy addition of new integrations (PagerDuty, ServiceNow)
- **Configurable Workflows**: Customizable incident processing pipelines
- **Voice Providers**: Twilio API & Communication
- **Flexible AI Models**: Swappable LLM providers through Spring AI

### 📊 Monitoring & Observability
- **Health Checks**: Comprehensive system health monitoring
- **Metrics Collection**: Prometheus-compatible metrics
- **Structured Logging**: JSON logs with correlation IDs
- **Error Tracking**: Detailed error reporting and recovery

### 🧪 Testing Strategy
- **End-to-End Tests**: Complete incident workflow validation
- **Integration Tests**: Component interaction testing with TestContainers
- **Mock Services**: WireMock for external API testing
- **Voice Testing**: Automated speech-to-text accuracy validation

---

## 🏆 Features Summary

✅ **AI-Powered Classification** - Automatic severity assessment and intelligent suggestions  
✅ **Multi-Channel Reporting** - REST API, voice calls, and web interface  
✅ **Enterprise Integrations** - Slack channels, Jira tickets, and notifications  
✅ **Voice Processing** - Speech-to-text with interactive voice response  
✅ **Vector Knowledge Base** - Semantic search for relevant solutions  
✅ **Real-time Dashboard** - Modern React UI with live updates  
✅ **Outbound Calling** - Automated incident notifications via phone  
✅ **Comprehensive Analytics** - Performance metrics and audit trails  
✅ **Docker Support** - Containerized deployment with Docker Compose  
✅ **Production Ready** - Health checks, monitoring, and error handling  


