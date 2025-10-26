-- Create trips table for tracking travel periods
CREATE TABLE trips (
    id SERIAL PRIMARY KEY,
    account_id INTEGER NOT NULL REFERENCES accounts(id),
    destination_country VARCHAR(2),  -- ISO 2-letter country code (e.g., 'KZ', 'ME')
    destination_city VARCHAR(100),   -- Normalized city name
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    total_amount DECIMAL(15,2),      -- Total spending during trip
    currency VARCHAR(10),             -- Primary currency used
    confidence_score DECIMAL(3,2),   -- 0.00 to 1.00 confidence
    detected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT trips_date_order CHECK (end_date >= start_date),
    CONSTRAINT trips_confidence CHECK (confidence_score >= 0 AND confidence_score <= 1)
);

-- Link transactions to trips (many-to-one relationship)
ALTER TABLE transactions ADD COLUMN trip_id INTEGER REFERENCES trips(id);

-- Indexes for performance
CREATE INDEX idx_trips_account_id ON trips(account_id);
CREATE INDEX idx_trips_dates ON trips(start_date, end_date);
CREATE INDEX idx_trips_destination ON trips(destination_country, destination_city);
CREATE INDEX idx_transactions_trip_id ON transactions(trip_id);

-- Add country and city columns to merchants for easier querying
ALTER TABLE merchants ADD COLUMN country_code VARCHAR(2);
ALTER TABLE merchants ADD COLUMN city VARCHAR(100);

-- Create index for location-based queries
CREATE INDEX idx_merchants_location ON merchants(country_code, city);
