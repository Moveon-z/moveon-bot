-- Initialize PostgreSQL database for Moveon Bot
-- This script runs on container startup

-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create index on username for faster lookups
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);

-- Insert initial admin user
-- Username: moveon
-- Password: moveon123 (BCrypt hashed)
-- Hash generated using BCrypt.withDefaults().hash("moveon123")
INSERT INTO users (username, password, status, role, created_at, updated_at)
VALUES ('moveon', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVE', 'ADMIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (username) DO NOTHING;

-- Create documents table
CREATE TABLE IF NOT EXISTS documents (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    file_type VARCHAR(20) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    error_message VARCHAR(1000),
    summary VARCHAR(2000),
    parsed_at TIMESTAMP,
    embedded_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_documents_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create indexes for faster lookups
CREATE INDEX IF NOT EXISTS idx_documents_user_id ON documents(user_id);
CREATE INDEX IF NOT EXISTS idx_documents_status ON documents(status);
CREATE INDEX IF NOT EXISTS idx_documents_created_at ON documents(created_at);
CREATE INDEX IF NOT EXISTS idx_documents_file_type ON documents(file_type);

-- Create document_fragments table
CREATE TABLE IF NOT EXISTS document_fragments (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL,
    fragment_index INT NOT NULL,
    content TEXT NOT NULL,
    char_count INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_fragments_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE
);

-- Create indexes for document_fragments
CREATE INDEX IF NOT EXISTS idx_fragments_document_id ON document_fragments(document_id);
CREATE INDEX IF NOT EXISTS idx_fragments_document_id_index ON document_fragments(document_id, fragment_index);

-- Create document_vectors table (pgvector)
CREATE TABLE IF NOT EXISTS document_vectors (
    id BIGSERIAL PRIMARY KEY,
    fragment_id BIGINT NOT NULL,
    document_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    embedding vector(1536),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_vectors_fragment FOREIGN KEY (fragment_id) REFERENCES document_fragments(id) ON DELETE CASCADE,
    CONSTRAINT fk_vectors_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE
);

-- Create indexes for document_vectors
CREATE INDEX IF NOT EXISTS idx_vectors_fragment_id ON document_vectors(fragment_id);
CREATE INDEX IF NOT EXISTS idx_vectors_document_id ON document_vectors(document_id);
CREATE INDEX IF NOT EXISTS idx_vectors_user_id ON document_vectors(user_id);
CREATE INDEX IF NOT EXISTS idx_vectors_status ON document_vectors(status);

-- Log initialization
DO $$
BEGIN
    RAISE NOTICE 'Moveon Bot database initialized';
    RAISE NOTICE 'pgvector extension enabled';
    RAISE NOTICE 'users table created';
    RAISE NOTICE 'documents table created';
    RAISE NOTICE 'document_fragments table created';
    RAISE NOTICE 'document_vectors table created';
    RAISE NOTICE 'Initial admin user created (username: moveon, password: moveon123)';
END $$;
