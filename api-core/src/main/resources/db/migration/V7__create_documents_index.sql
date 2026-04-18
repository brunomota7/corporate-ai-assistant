CREATE INDEX ON tb_documents USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);