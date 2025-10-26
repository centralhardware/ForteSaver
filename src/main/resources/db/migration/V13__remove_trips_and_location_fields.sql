-- Remove trips functionality and location fields from merchants

-- Drop trip_id column from transactions
ALTER TABLE transactions DROP COLUMN IF EXISTS trip_id;

-- Drop trips table
DROP TABLE IF EXISTS trips;

-- Drop location-related columns from merchants
ALTER TABLE merchants DROP COLUMN IF EXISTS country_code;
ALTER TABLE merchants DROP COLUMN IF EXISTS city;

-- Drop location index on merchants (if it exists)
DROP INDEX IF EXISTS idx_merchants_location;
