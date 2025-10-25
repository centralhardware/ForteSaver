-- Change deduplication strategy to (currency, daily_sequence, transaction_hash)
-- This solves the problem when statements for different currency accounts have different transaction order
-- Hash ensures same transaction is not imported twice, daily_sequence preserves order within day per currency

-- Step 1: Drop old unique index
DROP INDEX IF EXISTS unique_daily_transaction;

-- Step 2: Make transaction_hash NOT NULL (it's already computed for all transactions)
-- Skip this if already not null

-- Step 3: Create new unique index on (currency, daily_sequence, transaction_hash)
-- This ensures that same transaction in same currency with same sequence is not duplicated
CREATE UNIQUE INDEX unique_currency_sequence_hash
ON transactions(currency, daily_sequence, transaction_hash);
