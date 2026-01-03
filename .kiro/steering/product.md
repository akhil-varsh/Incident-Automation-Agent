# Product Overview

## XLBiz Incident Automation Agent

An AI-powered incident management system that automates the entire incident lifecycle from reporting to resolution. The system accepts incident reports through multiple channels (REST API, voice calls) and automatically processes them using AI classification, creates Slack channels, generates Jira tickets, and provides intelligent suggestions based on a knowledge base.

## Key Features

- **Multi-channel incident reporting**: REST API and voice integration via Vonage
- **AI-powered classification**: Automatic severity assessment and incident categorization using Groq LLM
- **Intelligent knowledge base**: Vector similarity search using ChromaDB for relevant suggestions
- **Automated integrations**: Creates Slack channels and Jira tickets automatically
- **Voice-to-incident processing**: Speech-to-text transcription with incident extraction
- **Real-time dashboard**: React UI for monitoring and managing incidents
- **Comprehensive audit trail**: Full incident lifecycle tracking with metadata

## Target Environment

- Designed for WSL2 (Windows Subsystem for Linux) development
- Production-ready with Docker containerization
- Supports cloud deployment with webhook integrations