-- Consolidate duplicate banks like "BCC" and "BC C" into one
-- Keeps the name with more spaces (more readable: "First Abu Dhabi Bank" instead of "FirstAbuDhabiBank")

-- Step 1: Create a temporary table with normalized bank names for comparison
CREATE TEMP TABLE normalized_banks AS
SELECT
    id,
    name,
    LOWER(REGEXP_REPLACE(name, '\s+', '', 'g')) as normalized_name,
    LENGTH(name) - LENGTH(REPLACE(name, ' ', '')) as space_count,
    ROW_NUMBER() OVER (
        PARTITION BY LOWER(REGEXP_REPLACE(name, '\s+', '', 'g'))
        ORDER BY LENGTH(name) - LENGTH(REPLACE(name, ' ', '')) DESC, id ASC
    ) as rn
FROM banks;

-- Step 2: For each group of duplicates, keep the one with most spaces (more readable)
-- and update transactions to point to it, then delete the duplicates
UPDATE transactions t
SET bank_id = (
    SELECT nb.id
    FROM normalized_banks nb
    WHERE nb.normalized_name = (
        SELECT nb2.normalized_name
        FROM normalized_banks nb2
        WHERE nb2.id = t.bank_id
    )
    AND nb.rn = 1
)
WHERE bank_id IN (
    SELECT id
    FROM normalized_banks
    WHERE rn > 1
);

-- Step 3: Delete duplicate banks (keep the one with most spaces)
DELETE FROM banks
WHERE id IN (
    SELECT id
    FROM normalized_banks
    WHERE rn > 1
);

DROP TABLE normalized_banks;
