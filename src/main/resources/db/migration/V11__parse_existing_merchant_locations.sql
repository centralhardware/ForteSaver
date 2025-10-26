-- Parse and populate country_code and city for existing merchants
-- This migration extracts country codes and cities from existing location strings

-- Handle comma-separated format: "MERCHANT,CITY,COUNTRY"
-- Extract country code (last part after last comma, 2 letters)
UPDATE merchants
SET country_code = UPPER(TRIM(SUBSTRING(location FROM '[A-Za-z]{2}$')))
WHERE location IS NOT NULL
  AND location LIKE '%,%'
  AND TRIM(SUBSTRING(location FROM '[A-Za-z]{2}$')) ~ '^[A-Za-z]{2}$';

-- Extract city from comma-separated format (second to last part)
UPDATE merchants
SET city = UPPER(TRIM(SPLIT_PART(location, ',', ARRAY_LENGTH(STRING_TO_ARRAY(location, ','), 1) - 1)))
WHERE location IS NOT NULL
  AND location LIKE '%,%'
  AND city IS NULL
  AND ARRAY_LENGTH(STRING_TO_ARRAY(location, ','), 1) >= 2;

-- Handle space-separated format: "CITY COUNTRY"
-- Extract country code (last 2 characters if they are letters)
UPDATE merchants
SET country_code = UPPER(RIGHT(TRIM(location), 2))
WHERE location IS NOT NULL
  AND location NOT LIKE '%,%'
  AND country_code IS NULL
  AND RIGHT(TRIM(location), 2) ~ '^[A-Za-z]{2}$'
  AND LENGTH(TRIM(location)) > 2;

-- Extract city from space-separated format (everything except last 2 characters)
UPDATE merchants
SET city = UPPER(TRIM(LEFT(TRIM(location), LENGTH(TRIM(location)) - 2)))
WHERE location IS NOT NULL
  AND location NOT LIKE '%,%'
  AND country_code IS NOT NULL
  AND city IS NULL
  AND LENGTH(TRIM(location)) > 2;

-- Clean up city names - remove common suffixes
UPDATE merchants
SET city = REGEXP_REPLACE(city, '\s+(MALL|AIRPORT|STATION|CENTER|CENTRE)$', '', 'i')
WHERE city IS NOT NULL
  AND city ~ '\s+(MALL|AIRPORT|STATION|CENTER|CENTRE)$';

-- Normalize whitespace in city names
UPDATE merchants
SET city = REGEXP_REPLACE(city, '\s+', ' ', 'g')
WHERE city IS NOT NULL;

-- Trim city names
UPDATE merchants
SET city = TRIM(city)
WHERE city IS NOT NULL;
