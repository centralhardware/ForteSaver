-- Change deduplication strategy from transaction hash to daily sequence
-- This allows multiple identical transactions while preventing true duplicates
-- Assumption: Bank sorts transactions by time within each day, even if time is not displayed

-- Add new column for daily sequence (order within the same day)
ALTER TABLE transactions
ADD COLUMN daily_sequence INTEGER;

-- For existing records, set sequence based on ID (they will be kept as-is)
UPDATE transactions
SET daily_sequence = id
WHERE daily_sequence IS NULL;

-- Make new column NOT NULL after populating
ALTER TABLE transactions
ALTER COLUMN daily_sequence SET NOT NULL;

-- Drop old unique constraint on transaction_hash
DROP INDEX IF EXISTS unique_transaction_hash;

-- Create new unique constraint: same date + same position within day = duplicate
-- This assumes bank sorts transactions chronologically within each day
CREATE UNIQUE INDEX unique_daily_transaction
ON transactions(transaction_date, daily_sequence);

-- Keep transaction_hash as regular index for analytics (not unique anymore)
-- Index already exists: idx_transactions_hash
