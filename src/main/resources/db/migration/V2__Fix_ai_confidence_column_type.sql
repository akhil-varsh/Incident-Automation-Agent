-- Fix ai_confidence column type to match Java Double mapping
-- Change from DECIMAL(3,2) to DOUBLE PRECISION

ALTER TABLE incidents 
ALTER COLUMN ai_confidence TYPE DOUBLE PRECISION;

-- Update the constraint to work with DOUBLE PRECISION
ALTER TABLE incidents 
DROP CONSTRAINT IF EXISTS chk_ai_confidence;

ALTER TABLE incidents 
ADD CONSTRAINT chk_ai_confidence 
CHECK (ai_confidence IS NULL OR (ai_confidence >= 0.0 AND ai_confidence <= 1.0));

-- Add comment
COMMENT ON COLUMN incidents.ai_confidence IS 'AI confidence score (0.0 to 1.0) - DOUBLE PRECISION type';