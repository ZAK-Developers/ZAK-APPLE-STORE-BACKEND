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

CREATE TABLE IF NOT EXISTS categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(40) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)@@

CREATE SEQUENCE IF NOT EXISTS products_code_seq START WITH 1 INCREMENT BY 1@@

CREATE TABLE IF NOT EXISTS products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_code VARCHAR(20) UNIQUE NOT NULL,
    category_id UUID NOT NULL REFERENCES categories(id),
    product_name VARCHAR(80) NOT NULL,
    product_description VARCHAR(500) NOT NULL,
    mrp NUMERIC(12, 2) NOT NULL,
    price NUMERIC(12, 2) NOT NULL,
    main_photo TEXT NOT NULL,
    photo_gallery_json TEXT NOT NULL DEFAULT '[]',
    best_seller BOOLEAN NOT NULL DEFAULT FALSE,
    storage VARCHAR(100),
    color VARCHAR(100),
    stock_quantity INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'Active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)@@

DO $$
BEGIN
    IF to_regclass('public.products') IS NOT NULL THEN
        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'products'
              AND column_name = 'best_seller'
        ) THEN
            ALTER TABLE products ADD COLUMN best_seller BOOLEAN NOT NULL DEFAULT FALSE;
        END IF;
        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'products'
              AND column_name = 'storage'
        ) THEN
            ALTER TABLE products ADD COLUMN storage VARCHAR(100);
        END IF;
        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'products'
              AND column_name = 'color'
        ) THEN
            ALTER TABLE products ADD COLUMN color VARCHAR(100);
        END IF;
    END IF;
END $$@@

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email)@@
CREATE INDEX IF NOT EXISTS idx_user_addresses_user_id ON user_addresses(user_id)@@
CREATE INDEX IF NOT EXISTS idx_otp_email_type ON otp_verifications(email, type)@@
CREATE INDEX IF NOT EXISTS idx_otp_mobile_type ON otp_verifications(mobile, type)@@
CREATE INDEX IF NOT EXISTS idx_login_history_user_id ON login_history(user_id)@@
CREATE UNIQUE INDEX IF NOT EXISTS idx_categories_name_lower ON categories ((LOWER(name)))@@
CREATE INDEX IF NOT EXISTS idx_products_category_id ON products(category_id)@@

CREATE TABLE IF NOT EXISTS carts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID UNIQUE NOT NULL REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)@@

CREATE TABLE IF NOT EXISTS cart_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cart_id UUID NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id),
    quantity INTEGER NOT NULL,
    color VARCHAR(50),
    storage VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)@@

CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(100) UNIQUE NOT NULL,
    request_id VARCHAR(100) UNIQUE,
    user_id UUID NOT NULL REFERENCES users(id),
    status VARCHAR(20) NOT NULL,
    payment_method VARCHAR(50),
    shipping_full_name VARCHAR(120),
    shipping_phone VARCHAR(20),
    shipping_address_line1 TEXT,
    shipping_address_line2 TEXT,
    shipping_city VARCHAR(100),
    shipping_state VARCHAR(100),
    shipping_pin_code VARCHAR(10),
    shipping_country VARCHAR(100),
    subtotal NUMERIC(12, 2) NOT NULL,
    tax NUMERIC(12, 2) NOT NULL,
    shipping_fee NUMERIC(12, 2) NOT NULL,
    discount NUMERIC(12, 2) NOT NULL,
    grand_total NUMERIC(12, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)@@

DO $$
BEGIN
    IF to_regclass('public.orders') IS NOT NULL THEN
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = 'orders' AND column_name = 'request_id'
        ) THEN
            ALTER TABLE orders ADD COLUMN request_id VARCHAR(100);
        END IF;
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = 'orders' AND column_name = 'payment_method'
        ) THEN
            ALTER TABLE orders ADD COLUMN payment_method VARCHAR(50);
        END IF;
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = 'orders' AND column_name = 'shipping_full_name'
        ) THEN
            ALTER TABLE orders ADD COLUMN shipping_full_name VARCHAR(120);
        END IF;
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = 'orders' AND column_name = 'shipping_phone'
        ) THEN
            ALTER TABLE orders ADD COLUMN shipping_phone VARCHAR(20);
        END IF;
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = 'orders' AND column_name = 'shipping_address_line1'
        ) THEN
            ALTER TABLE orders ADD COLUMN shipping_address_line1 TEXT;
        END IF;
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = 'orders' AND column_name = 'shipping_address_line2'
        ) THEN
            ALTER TABLE orders ADD COLUMN shipping_address_line2 TEXT;
        END IF;
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = 'orders' AND column_name = 'shipping_city'
        ) THEN
            ALTER TABLE orders ADD COLUMN shipping_city VARCHAR(100);
        END IF;
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = 'orders' AND column_name = 'shipping_state'
        ) THEN
            ALTER TABLE orders ADD COLUMN shipping_state VARCHAR(100);
        END IF;
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = 'orders' AND column_name = 'shipping_pin_code'
        ) THEN
            ALTER TABLE orders ADD COLUMN shipping_pin_code VARCHAR(10);
        END IF;
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = 'orders' AND column_name = 'shipping_country'
        ) THEN
            ALTER TABLE orders ADD COLUMN shipping_country VARCHAR(100);
        END IF;
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = 'orders' AND column_name = 'subtotal'
        ) THEN
            ALTER TABLE orders ADD COLUMN subtotal NUMERIC(12, 2) NOT NULL DEFAULT 0;
        END IF;
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = 'orders' AND column_name = 'tax'
        ) THEN
            ALTER TABLE orders ADD COLUMN tax NUMERIC(12, 2) NOT NULL DEFAULT 0;
        END IF;
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = 'orders' AND column_name = 'shipping_fee'
        ) THEN
            ALTER TABLE orders ADD COLUMN shipping_fee NUMERIC(12, 2) NOT NULL DEFAULT 0;
        END IF;
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = 'orders' AND column_name = 'discount'
        ) THEN
            ALTER TABLE orders ADD COLUMN discount NUMERIC(12, 2) NOT NULL DEFAULT 0;
        END IF;
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = 'orders' AND column_name = 'grand_total'
        ) THEN
            ALTER TABLE orders ADD COLUMN grand_total NUMERIC(12, 2) NOT NULL DEFAULT 0;
        END IF;
    END IF;
END $$@@

CREATE TABLE IF NOT EXISTS order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID NOT NULL,
    product_name VARCHAR(150),
    quantity INTEGER NOT NULL,
    price NUMERIC(12, 2) NOT NULL
)@@

DO $$
BEGIN
    IF to_regclass('public.order_items') IS NOT NULL THEN
        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = 'order_items'
              AND column_name = 'product_name'
        ) THEN
            ALTER TABLE order_items ADD COLUMN product_name VARCHAR(150);
        END IF;
    END IF;
END $$@@

CREATE TABLE IF NOT EXISTS payments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT UNIQUE NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    gateway_order_id VARCHAR(255) NOT NULL,
    gateway_payment_id VARCHAR(255),
    signature VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)@@

CREATE TABLE IF NOT EXISTS product_reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    review_title VARCHAR(180) NOT NULL,
    review_comment TEXT NOT NULL,
    rating INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)@@

CREATE INDEX IF NOT EXISTS idx_product_reviews_product_id ON product_reviews(product_id)@@
CREATE INDEX IF NOT EXISTS idx_product_reviews_user_id ON product_reviews(user_id)@@
