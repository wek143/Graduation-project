-- H2 compatible version
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS active BIT NOT NULL DEFAULT TRUE;
