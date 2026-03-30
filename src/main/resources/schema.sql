-- 智能工单系统 Database Schema
-- PostgreSQL 16 + pgvector

-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(200) NOT NULL,
    name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('CUSTOMER', 'ENGINEER', 'ADMIN')),
    department VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Tickets table
CREATE TABLE IF NOT EXISTS tickets (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED')),
    priority VARCHAR(10) NOT NULL DEFAULT 'MEDIUM' CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    priority_reason VARCHAR(500),
    customer_id BIGINT NOT NULL REFERENCES users(id),
    assigned_engineer_id BIGINT REFERENCES users(id),
    resolution_notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    resolved_at TIMESTAMP,
    version BIGINT DEFAULT 0
);

-- Ticket images (separate table for @ElementCollection)
CREATE TABLE IF NOT EXISTS ticket_images (
    ticket_id BIGINT NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    image_url VARCHAR(500) NOT NULL
);

-- Knowledge base with vector embeddings (1024d for text2vec-large-chinese)
CREATE TABLE IF NOT EXISTS knowledge_base (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    content_embedding vector(1024),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'PUBLISHED')),
    source_ticket_id BIGINT,
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- HNSW index for fast similarity search
CREATE INDEX IF NOT EXISTS idx_kb_embedding_hnsw
    ON knowledge_base USING hnsw (content_embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- AI interaction logging table
CREATE TABLE IF NOT EXISTS ai_interactions (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    user_id BIGINT,
    ticket_id BIGINT,
    input_text TEXT,
    output_text TEXT,
    latency_ms BIGINT,
    tokens_used INTEGER,
    success BOOLEAN NOT NULL,
    error_message VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Deflection tracking
CREATE TABLE IF NOT EXISTS deflections (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    matched_kb_id BIGINT,
    search_query TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indexes for common queries
CREATE INDEX IF NOT EXISTS idx_tickets_status ON tickets(status);
CREATE INDEX IF NOT EXISTS idx_tickets_customer ON tickets(customer_id);
CREATE INDEX IF NOT EXISTS idx_tickets_engineer ON tickets(assigned_engineer_id);
CREATE INDEX IF NOT EXISTS idx_tickets_created ON tickets(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_kb_status ON knowledge_base(status);
CREATE INDEX IF NOT EXISTS idx_ai_interactions_type ON ai_interactions(type);
CREATE INDEX IF NOT EXISTS idx_ai_interactions_created ON ai_interactions(created_at DESC);
