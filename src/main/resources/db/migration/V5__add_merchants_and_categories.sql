-- Add merchants and categories tables
-- Merchants will be extracted from transactions and classified by category

-- Categories table
CREATE TABLE categories (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Insert predefined categories
INSERT INTO categories (name, description) VALUES
('Groceries', 'Supermarkets and grocery stores'),
('Restaurants', 'Restaurants, cafes, and food delivery'),
('Transportation', 'Public transport, taxis, fuel'),
('Shopping', 'Clothing, electronics, and general shopping'),
('Entertainment', 'Movies, games, subscriptions'),
('Healthcare', 'Medical expenses, pharmacies'),
('Utilities', 'Electricity, water, internet, phone bills'),
('Travel', 'Hotels, flights, travel agencies'),
('Education', 'Courses, books, educational materials'),
('Sports', 'Gyms, sports equipment, activities'),
('Home', 'Furniture, home improvement, rent'),
('Beauty', 'Salons, cosmetics, personal care'),
('Pets', 'Pet food, vet services, pet supplies'),
('Gifts', 'Gifts and donations'),
('Transfers', 'Bank transfers and money transfers'),
('Cash', 'Cash withdrawals and deposits'),
('Fees', 'Bank fees and commissions'),
('Other', 'Uncategorized transactions');

-- Merchants table
CREATE TABLE merchants (
    id SERIAL PRIMARY KEY,
    name VARCHAR(500) NOT NULL,
    location TEXT,
    mcc_code VARCHAR(10),
    category_id INTEGER REFERENCES categories(id),

    -- Flag to indicate if merchant needs manual categorization
    needs_categorization BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Unique constraint on merchant name + location combo
    CONSTRAINT unique_merchant UNIQUE (name, location)
);

-- Create index on merchant name for fast lookup
CREATE INDEX idx_merchants_name ON merchants(name);
CREATE INDEX idx_merchants_category ON merchants(category_id);
CREATE INDEX idx_merchants_needs_categorization ON merchants(needs_categorization);

-- Add merchant_id to transactions table (nullable - not all transactions have merchants)
ALTER TABLE transactions ADD COLUMN merchant_id INTEGER REFERENCES merchants(id);

-- Create index on merchant_id for fast joins
CREATE INDEX idx_transactions_merchant ON transactions(merchant_id);

-- Remove merchant-related fields from transactions (they now live in merchants table)
ALTER TABLE transactions DROP COLUMN IF EXISTS merchant_name;
ALTER TABLE transactions DROP COLUMN IF EXISTS merchant_location;
ALTER TABLE transactions DROP COLUMN IF EXISTS mcc_code;
