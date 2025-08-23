-- Create incidents table for XLBiz.AI Incident Automation Agent
-- This migration creates the main incidents table with all required fields

CREATE TABLE incidents (
    id BIGSERIAL PRIMARY KEY,
    external_id VARCHAR(255) UNIQUE NOT NULL,
    type VARCHAR(50) NOT NULL,
    description TEXT,
    severity VARCHAR(20) DEFAULT 'UNKNOWN',
    status VARCHAR(20) NOT NULL DEFAULT 'RECEIVED',
    source VARCHAR(100),
    incident_timestamp TIMESTAMP,
    metadata JSONB DEFAULT '{}',
    
    -- Integration tracking fields
    slack_channel_id VARCHAR(50),
    slack_message_ts VARCHAR(50),
    jira_ticket_key VARCHAR(20),
    
    -- AI analysis results
    ai_suggestion TEXT,
    ai_reasoning TEXT,
    ai_confidence DECIMAL(3,2),
    
    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMP
);

-- Create indexes for better query performance
CREATE INDEX idx_incident_external_id ON incidents(external_id);
CREATE INDEX idx_incident_type ON incidents(type);
CREATE INDEX idx_incident_severity ON incidents(severity);
CREATE INDEX idx_incident_status ON incidents(status);
CREATE INDEX idx_incident_created_at ON incidents(created_at);
CREATE INDEX idx_incident_source ON incidents(source);

-- Create index on JSONB metadata for common queries
-- Using B-tree indexes for text fields extracted from JSONB
CREATE INDEX idx_incident_metadata_service ON incidents ((metadata->>'service'));
CREATE INDEX idx_incident_metadata_environment ON incidents ((metadata->>'environment'));

-- Create GIN index on the entire JSONB column for general JSON queries
CREATE INDEX idx_incident_metadata_gin ON incidents USING GIN (metadata);

-- Add constraints
ALTER TABLE incidents ADD CONSTRAINT chk_incident_type 
    CHECK (type IN ('DATABASE_CONNECTION_ERROR', 'HIGH_CPU', 'DISK_FULL', 'MEMORY_LEAK', 
                   'NETWORK_ISSUE', 'SERVICE_DOWN', 'SECURITY_BREACH', 'DATA_CORRUPTION', 
                   'API_FAILURE', 'DEPLOYMENT_FAILURE', 'OTHER'));

ALTER TABLE incidents ADD CONSTRAINT chk_incident_severity 
    CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'UNKNOWN'));

ALTER TABLE incidents ADD CONSTRAINT chk_incident_status 
    CHECK (status IN ('RECEIVED', 'CLASSIFYING', 'PROCESSING', 'PROCESSED', 
                     'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'FAILED'));

ALTER TABLE incidents ADD CONSTRAINT chk_ai_confidence 
    CHECK (ai_confidence IS NULL OR (ai_confidence >= 0.0 AND ai_confidence <= 1.0));

-- Create trigger to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_incidents_updated_at 
    BEFORE UPDATE ON incidents 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

-- Add comments for documentation
COMMENT ON TABLE incidents IS 'Main table storing incident data and processing status';
COMMENT ON COLUMN incidents.external_id IS 'Unique identifier from the source monitoring system';
COMMENT ON COLUMN incidents.metadata IS 'JSON metadata from source system (service, environment, etc.)';
COMMENT ON COLUMN incidents.ai_suggestion IS 'AI-generated remediation suggestions';
COMMENT ON COLUMN incidents.ai_reasoning IS 'AI explanation for severity classification';
COMMENT ON COLUMN incidents.ai_confidence IS 'AI confidence score (0.0 to 1.0)';
COMMENT ON COLUMN incidents.slack_channel_id IS 'Slack channel ID created for this incident';
COMMENT ON COLUMN incidents.jira_ticket_key IS 'Jira ticket key created for this incident';