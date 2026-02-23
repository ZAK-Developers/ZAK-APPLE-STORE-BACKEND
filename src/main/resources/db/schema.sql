-- ZAK Apple Store - PostgreSQL Schema
-- Run this script to create the database and users table manually (optional)
-- With spring.jpa.hibernate.ddl-auto=update, Hibernate will auto-create/update tables on startup

-- Create database (run as postgres superuser):
-- CREATE DATABASE "ZAKapplestoreDB";

-- Connect to ZAKapplestoreDB and run:

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    mobile VARCHAR(50),
    role VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    CONSTRAINT chk_role CHECK (role IN ('ADMIN', 'CUSTOMER'))
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
