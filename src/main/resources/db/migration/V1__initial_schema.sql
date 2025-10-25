-- Forte Bank Statement Parser
-- Initial database schema - transactions only

-- Table for transactions with detailed fields
CREATE TABLE transactions (
    id SERIAL PRIMARY KEY,
    transaction_date DATE NOT NULL,
    transaction_type VARCHAR(50) NOT NULL, -- Purchase, Transfer, Refund, etc

    -- Amount in account currency (always present)
    amount DECIMAL(15, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL, -- Account currency (USD, KZT, etc)

    -- Amount in transaction currency (optional, for foreign purchases)
    transaction_amount DECIMAL(15, 2),
    transaction_currency VARCHAR(10),

    -- Merchant/counterparty details
    merchant_name TEXT,
    merchant_location TEXT,

    -- Payment details
    mcc_code VARCHAR(10),
    bank_name VARCHAR(255),
    payment_method VARCHAR(50), -- APPLE PAY, card number, etc

    -- Full description (for any additional info)
    description TEXT,

    -- Deduplication
    transaction_hash VARCHAR(64) NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_transactions_date ON transactions(transaction_date);
CREATE INDEX idx_transactions_type ON transactions(transaction_type);
CREATE INDEX idx_transactions_hash ON transactions(transaction_hash);
CREATE INDEX idx_transactions_mcc ON transactions(mcc_code);

-- Unique constraint for deduplication
CREATE UNIQUE INDEX unique_transaction_hash ON transactions(transaction_hash);
