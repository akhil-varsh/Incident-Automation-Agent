-- Add voice-specific fields to incidents table
-- This migration adds fields for storing detailed voice call information

-- Add voice-specific columns to incidents table (only if they don't exist)
DO $$ 
BEGIN
    -- Add transcription column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'incidents' AND column_name = 'transcription') THEN
        ALTER TABLE incidents ADD COLUMN transcription TEXT;
    END IF;
    
    -- Add recording_url column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'incidents' AND column_name = 'recording_url') THEN
        ALTER TABLE incidents ADD COLUMN recording_url VARCHAR(500);
    END IF;
    
    -- Add call_duration column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'incidents' AND column_name = 'call_duration') THEN
        ALTER TABLE incidents ADD COLUMN call_duration INTEGER;
    END IF;
    
    -- Add conversation_uuid column if it doesn't exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'incidents' AND column_name = 'conversation_uuid') THEN
        ALTER TABLE incidents ADD COLUMN conversation_uuid VARCHAR(100);
    END IF;
END $$;

-- Create index on conversation_uuid for voice call lookups (only if it doesn't exist)
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_incident_conversation_uuid') THEN
        CREATE INDEX idx_incident_conversation_uuid ON incidents(conversation_uuid);
    END IF;
END $$;

-- Create separate voice_calls table for detailed call tracking (only if it doesn't exist)
CREATE TABLE IF NOT EXISTS voice_calls (
    id BIGSERIAL PRIMARY KEY,
    incident_id BIGINT REFERENCES incidents(id) ON DELETE CASCADE,
    conversation_uuid VARCHAR(100) UNIQUE NOT NULL,
    caller_number VARCHAR(20),
    recording_url VARCHAR(500),
    recording_sid VARCHAR(100),
    transcription TEXT,
    call_duration INTEGER,
    processing_status VARCHAR(20) NOT NULL DEFAULT 'RECEIVED',
    speech_to_text_service VARCHAR(20),
    transcription_confidence DECIMAL(3,2),
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMP
);

-- Create indexes for voice_calls table (only if they don't exist)
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_voice_calls_incident_id') THEN
        CREATE INDEX idx_voice_calls_incident_id ON voice_calls(incident_id);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_voice_calls_conversation_uuid') THEN
        CREATE INDEX idx_voice_calls_conversation_uuid ON voice_calls(conversation_uuid);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_voice_calls_caller_number') THEN
        CREATE INDEX idx_voice_calls_caller_number ON voice_calls(caller_number);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_voice_calls_processing_status') THEN
        CREATE INDEX idx_voice_calls_processing_status ON voice_calls(processing_status);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_voice_calls_created_at') THEN
        CREATE INDEX idx_voice_calls_created_at ON voice_calls(created_at);
    END IF;
END $$;

-- Add constraints for voice_calls (only if they don't exist)
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.check_constraints 
                   WHERE constraint_name = 'chk_voice_processing_status') THEN
        ALTER TABLE voice_calls ADD CONSTRAINT chk_voice_processing_status 
            CHECK (processing_status IN ('RECEIVED', 'DOWNLOADING', 'TRANSCRIBING', 'PROCESSED', 'ERROR', 'DUPLICATE'));
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.check_constraints 
                   WHERE constraint_name = 'chk_transcription_confidence') THEN
        ALTER TABLE voice_calls ADD CONSTRAINT chk_transcription_confidence 
            CHECK (transcription_confidence IS NULL OR (transcription_confidence >= 0.0 AND transcription_confidence <= 1.0));
    END IF;
END $$;

-- Create trigger function if it doesn't exist
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create trigger to automatically update updated_at timestamp for voice_calls (only if it doesn't exist)
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.triggers 
                   WHERE trigger_name = 'update_voice_calls_updated_at') THEN
        CREATE TRIGGER update_voice_calls_updated_at 
            BEFORE UPDATE ON voice_calls 
            FOR EACH ROW 
            EXECUTE FUNCTION update_updated_at_column();
    END IF;
END $$;

-- Add comments for documentation
COMMENT ON COLUMN incidents.transcription IS 'Voice call transcription text';
COMMENT ON COLUMN incidents.recording_url IS 'URL to the voice recording file';
COMMENT ON COLUMN incidents.call_duration IS 'Call duration in seconds';
COMMENT ON COLUMN incidents.conversation_uuid IS 'Unique identifier for the voice conversation';

COMMENT ON TABLE voice_calls IS 'Detailed voice call tracking and processing information';
COMMENT ON COLUMN voice_calls.recording_sid IS 'Twilio recording SID for reference';
COMMENT ON COLUMN voice_calls.speech_to_text_service IS 'Service used for transcription (google, deepgram, etc.)';
COMMENT ON COLUMN voice_calls.transcription_confidence IS 'Confidence score from speech-to-text service';
COMMENT ON COLUMN voice_calls.processing_status IS 'Current processing status of the voice call';