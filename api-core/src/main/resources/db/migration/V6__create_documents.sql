CREATE TABLE tb_documents (
    id UUID PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    source_type VARCHAR(50) NOT NULL,
    source_id UUID NULL,
    embedding VECTOR(1536) NULL,
    metadata JSONB NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);