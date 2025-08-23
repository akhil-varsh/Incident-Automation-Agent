-- Initialize PostgreSQL database for Incident Automation Agent
-- This script runs automatically when the PostgreSQL container starts

-- Create database if it doesn't exist (handled by POSTGRES_DB environment variable)
-- Create user if it doesn't exist (handled by POSTGRES_USER environment variable)

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Grant necessary permissions
GRANT ALL PRIVILEGES ON DATABASE incident_db TO incident_user;

-- Create schema for application tables (Flyway will handle table creation)
-- This ensures the database is ready for Flyway migrations