-- Add account number to transactions table
ALTER TABLE transactions
ADD COLUMN account_number VARCHAR(50);

-- Create index on account_number for better query performance
CREATE INDEX idx_account_number ON transactions(account_number);
