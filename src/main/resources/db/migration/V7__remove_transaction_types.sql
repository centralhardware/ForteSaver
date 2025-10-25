-- Remove transaction types - we only store card purchases now
-- This migration removes the transaction_type concept from the schema

-- Step 1: Drop foreign key constraint
ALTER TABLE transactions DROP CONSTRAINT IF EXISTS fk_transactions_transaction_type;

-- Step 2: Drop index
DROP INDEX IF EXISTS idx_transactions_transaction_type;

-- Step 3: Remove transaction_type_id column from transactions
ALTER TABLE transactions DROP COLUMN IF EXISTS transaction_type_id;

-- Step 4: Drop transaction_types table
DROP TABLE IF EXISTS transaction_types;
