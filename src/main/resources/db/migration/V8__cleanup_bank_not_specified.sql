-- Clean up "Bank not specified" entries
-- This migration removes all variants of "Bank not specified" that may have been saved
-- due to PDF extraction quirks (extra spaces in different positions)

-- Step 1: Set bank_id to NULL for transactions referencing "Bank not specified" banks
-- We normalize whitespace and do case-insensitive comparison
UPDATE transactions
SET bank_id = NULL
WHERE bank_id IN (
    SELECT id
    FROM banks
    WHERE LOWER(REGEXP_REPLACE(name, '\s+', ' ', 'g')) = 'bank not specified'
);

-- Step 2: Delete all "Bank not specified" banks
DELETE FROM banks
WHERE LOWER(REGEXP_REPLACE(name, '\s+', ' ', 'g')) = 'bank not specified';
