CREATE EXTENSION IF NOT EXISTS "pgcrypto"@@

-- If legacy schema exists (BIGINT user id / missing required columns), reset auth tables.
DO $$
BEGIN
    IF to_regclass('public.users') IS NOT NULL THEN
        IF EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'users'
              AND column_name = 'id'
              AND udt_name <> 'uuid'
        )
        OR EXISTS (
            SELECT 1
            FROM (VALUES
                ('username'),
                ('provider'),
                ('email_verified'),
                ('mobile_verified'),
                ('status'),
                ('created_at'),
                ('updated_at')
            ) AS required_columns(column_name)
            WHERE NOT EXISTS (
                SELECT 1
                FROM information_schema.columns c
                WHERE c.table_schema = 'public'
                  AND c.table_name = 'users'
                  AND c.column_name = required_columns.column_name
            )
        ) THEN
            DROP TABLE IF EXISTS login_history CASCADE;
            DROP TABLE IF EXISTS user_addresses CASCADE;
            DROP TABLE IF EXISTS otp_verifications CASCADE;
            DROP TABLE IF EXISTS users CASCADE;
        END IF;
    END IF;
END $$@@

-- Defensive reset if child table foreign key types are incompatible.
DO $$
BEGIN
    IF to_regclass('public.login_history') IS NOT NULL AND EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'login_history'
          AND column_name = 'user_id'
          AND udt_name <> 'uuid'
    ) THEN
        DROP TABLE IF EXISTS login_history CASCADE;
    END IF;
END $$@@

DO $$
BEGIN
    IF to_regclass('public.user_addresses') IS NOT NULL AND EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'user_addresses'
          AND column_name = 'user_id'
          AND udt_name <> 'uuid'
    ) THEN
        DROP TABLE IF EXISTS user_addresses CASCADE;
    END IF;
END $$@@

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    mobile VARCHAR(20),
    password VARCHAR(255),
    provider VARCHAR(20) DEFAULT 'LOCAL',
    provider_id VARCHAR(255),
    email_verified BOOLEAN DEFAULT FALSE,
    mobile_verified BOOLEAN DEFAULT FALSE,
    profile_image TEXT,
    role VARCHAR(20) DEFAULT 'CUSTOMER',
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)@@

CREATE TABLE IF NOT EXISTS user_addresses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    full_name VARCHAR(120),
    phone VARCHAR(20),
    address_line1 TEXT,
    address_line2 TEXT,
    city VARCHAR(100),
    state VARCHAR(100),
    pin_code VARCHAR(10),
    country VARCHAR(100),
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)@@

CREATE TABLE IF NOT EXISTS otp_verifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(150),
    mobile VARCHAR(20),
    otp VARCHAR(10),
    type VARCHAR(50),
    expires_at TIMESTAMP,
    verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)@@

CREATE TABLE IF NOT EXISTS login_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id),
    login_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(100),
    device VARCHAR(200),
    status VARCHAR(50)
)@@

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email)@@
CREATE INDEX IF NOT EXISTS idx_user_addresses_user_id ON user_addresses(user_id)@@
CREATE INDEX IF NOT EXISTS idx_otp_email_type ON otp_verifications(email, type)@@
CREATE INDEX IF NOT EXISTS idx_otp_mobile_type ON otp_verifications(mobile, type)@@
CREATE INDEX IF NOT EXISTS idx_login_history_user_id ON login_history(user_id)@@
