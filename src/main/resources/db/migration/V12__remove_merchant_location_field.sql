-- Remove location field from merchants table and update unique constraint
-- The location field is no longer needed as we now store country and city separately
-- The merchant name becomes unique identifier as it contains the full merchant details

-- Drop the old unique constraint (name + location)
ALTER TABLE merchants DROP CONSTRAINT IF EXISTS unique_merchant;

-- Remove the location column
ALTER TABLE merchants DROP COLUMN IF EXISTS location;

-- Create new unique index on just the name
CREATE UNIQUE INDEX unique_merchant_name ON merchants(name);
