-- Normalize reference data: Banks, Transaction Types, and Accounts
-- This migration extracts repeated data into separate reference tables

-- Step 1: Create Banks table
CREATE TABLE banks (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Step 2: Create Transaction Types table
CREATE TABLE transaction_types (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Step 3: Create Accounts table
-- Note: All accounts are from Forte bank, so no bank_id needed here
CREATE TABLE accounts (
    id SERIAL PRIMARY KEY,
    account_number VARCHAR(50) NOT NULL UNIQUE,
    currency VARCHAR(10) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Step 4: Populate Banks table from existing transaction data
INSERT INTO banks (name, created_at)
SELECT DISTINCT bank_name, CURRENT_TIMESTAMP
FROM transactions
WHERE bank_name IS NOT NULL
ON CONFLICT (name) DO NOTHING;

-- Step 5: Populate Transaction Types table from existing data
INSERT INTO transaction_types (name, created_at)
SELECT DISTINCT transaction_type, CURRENT_TIMESTAMP
FROM transactions
ON CONFLICT (name) DO NOTHING;

-- Step 6: Populate Accounts table from existing transaction data
INSERT INTO accounts (account_number, currency, created_at)
SELECT DISTINCT
    account_number,
    currency,
    CURRENT_TIMESTAMP
FROM transactions
WHERE account_number IS NOT NULL
ON CONFLICT (account_number) DO NOTHING;

-- Step 7: Add new foreign key columns to transactions table
ALTER TABLE transactions ADD COLUMN transaction_type_id INTEGER;
ALTER TABLE transactions ADD COLUMN account_id INTEGER;
ALTER TABLE transactions ADD COLUMN bank_id INTEGER;

-- Step 8: Populate the new foreign key columns
UPDATE transactions t
SET transaction_type_id = tt.id
FROM transaction_types tt
WHERE tt.name = t.transaction_type;

UPDATE transactions t
SET account_id = a.id
FROM accounts a
WHERE a.account_number = t.account_number;

UPDATE transactions t
SET bank_id = b.id
FROM banks b
WHERE b.name = t.bank_name;

-- Step 9: Make foreign key columns NOT NULL and add constraints
ALTER TABLE transactions ALTER COLUMN transaction_type_id SET NOT NULL;
ALTER TABLE transactions ALTER COLUMN account_id SET NOT NULL;
-- bank_id stays nullable as not all transactions have bank info

ALTER TABLE transactions ADD CONSTRAINT fk_transactions_transaction_type
    FOREIGN KEY (transaction_type_id) REFERENCES transaction_types(id);

ALTER TABLE transactions ADD CONSTRAINT fk_transactions_account
    FOREIGN KEY (account_id) REFERENCES accounts(id);

ALTER TABLE transactions ADD CONSTRAINT fk_transactions_bank
    FOREIGN KEY (bank_id) REFERENCES banks(id);

-- Step 10: Drop old unique constraint and create new one
ALTER TABLE transactions DROP CONSTRAINT IF EXISTS unique_currency_sequence_hash;
CREATE UNIQUE INDEX unique_account_sequence_hash ON transactions(account_id, daily_sequence, transaction_hash);

-- Step 11: Remove redundant columns from transactions
ALTER TABLE transactions DROP COLUMN transaction_type;
ALTER TABLE transactions DROP COLUMN currency;
ALTER TABLE transactions DROP COLUMN bank_name;
ALTER TABLE transactions DROP COLUMN account_number;

-- Step 12: Create indexes for better query performance
CREATE INDEX idx_transactions_account ON transactions(account_id);
CREATE INDEX idx_transactions_transaction_type ON transactions(transaction_type_id);
CREATE INDEX idx_transactions_bank ON transactions(bank_id);
