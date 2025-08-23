-- Add outbound call support fields to voice_calls table
-- Migration V5: Add outbound call fields

-- Add new columns for outbound call support
ALTER TABLE voice_calls 
ADD COLUMN IF NOT EXISTS call_sid VARCHAR(100),
ADD COLUMN IF NOT EXISTS phone_number VARCHAR(20),
ADD COLUMN IF NOT EXISTS direction VARCHAR(10),
ADD COLUMN IF NOT EXISTS call_type VARCHAR(50),
ADD COLUMN IF NOT EXISTS twilio_status VARCHAR(20),
ADD COLUMN IF NOT EXISTS duration_seconds INTEGER,
ADD COLUMN IF NOT EXISTS ended_at TIMESTAMP;

-- Add index on call_sid for quick lookups
CREATE INDEX IF NOT EXISTS idx_voice_calls_call_sid ON voice_calls(call_sid);

-- Add index on phone_number for outbound call tracking
CREATE INDEX IF NOT EXISTS idx_voice_calls_phone_number ON voice_calls(phone_number);

-- Add index on direction for filtering inbound/outbound calls
CREATE INDEX IF NOT EXISTS idx_voice_calls_direction ON voice_calls(direction);

-- Add index on twilio_status for call status tracking
CREATE INDEX IF NOT EXISTS idx_voice_calls_twilio_status ON voice_calls(twilio_status);

-- Update the processing_status check constraint to include new values
-- First drop the existing constraint, then recreate with new values
DO $$
BEGIN
    -- Drop existing constraint if it exists
    IF EXISTS (SELECT 1 FROM information_schema.check_constraints 
               WHERE constraint_name = 'chk_voice_processing_status') THEN
        ALTER TABLE voice_calls DROP CONSTRAINT chk_voice_processing_status;
    END IF;
    
    -- Add updated constraint with new values
    ALTER TABLE voice_calls ADD CONSTRAINT chk_voice_processing_status 
        CHECK (processing_status IN ('RECEIVED', 'DOWNLOADING', 'TRANSCRIBING', 'PROCESSED', 'ERROR', 'DUPLICATE', 'PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'));
END$$;

-- Add comments for documentation
COMMENT ON COLUMN voice_calls.call_sid IS 'Twilio Call SID for outbound calls';
COMMENT ON COLUMN voice_calls.phone_number IS 'Phone number for outbound calls';
COMMENT ON COLUMN voice_calls.direction IS 'Call direction: inbound or outbound';
COMMENT ON COLUMN voice_calls.call_type IS 'Type of call: INCIDENT_NOTIFICATION, etc.';
COMMENT ON COLUMN voice_calls.twilio_status IS 'Twilio call status: initiated, ringing, answered, completed, etc.';
COMMENT ON COLUMN voice_calls.duration_seconds IS 'Call duration in seconds for outbound calls';
COMMENT ON COLUMN voice_calls.ended_at IS 'Timestamp when the call ended';