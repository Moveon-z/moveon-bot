-- Initialize PostgreSQL database for Moveon Bot
-- This script runs on container startup

-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Create initial admin user
-- Password is BCrypt hashed: "moveon123" (change after first login)
-- Hash generated using BCrypt.withDefaults().hash("moveon123")

-- Note: The actual user table and password hash will be created by the application
-- This script only ensures the database and extension are ready

-- Log initialization
DO $$
BEGIN
    RAISE NOTICE 'Moveon Bot database initialized';
    RAISE NOTICE 'pgvector extension enabled';
END $$;
