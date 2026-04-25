from openai import OpenAI
from sqlalchemy import text
from app.config import settings
from app.database import get_db

class VectorService:
    """
    Responsável por duas operações:
    1. Gerar embeddings de texto via OpenAI (text-embedding-3-small — ADR-007)
    2. Buscar documentos similares no pgvector
    """
    def __init__(self):
        self.client = OpenAI(api_key=settings.openai_api_key)
        self.model = "text-embedding-3-small"

    def embed(self, text: str) -> list[float]:
        """Gera vetor de 1536 dimensões para o texto recebido"""
        response = self.client.embeddings.create(
            model=self.model,
            input=text
        )
        return response.data[0].embedding

    def search(self, query: str, top_k: int = 5) -> list[str]:
        """
        Gera embedding da query e busca os documentos mais
        próximos no pgvector usando distância de cosseno (<->)
        """
        query_embedding = self.embed(query)
        embedding_str = "[" + ",".join(str(x) for x in query_embedding) + "]"

        db = next(get_db())
        result = db.execute(text("""
            SELECT content FROM tb_documents
            ORDER BY embedding <-> CAST(:embedding AS vector)
            LIMIT :limit
        """), {"embedding": embedding_str, "limit": top_k})

        return [row[0] for row in result.fetchall()]

    def store(self, title: str, content: str, source_type: str,
              source_id: str = None, metadata: dict = None) -> None:
        """
        Gera embedding e persiste documento na tb_documents.
        Usado pelo endpoint de ingest.
        """
        embedding = self.embed(content)
        embedding_str = "[" + ",".join(str(x) for x in embedding) + "]"

        db = next(get_db())
        db.execute(text("""
            INSERT INTO tb_documents (title, content, source_type, source_id, embedding, metadata)
            VALUES (:title, :content, :source_type, :source_id,
                    CAST(:embedding AS vector), CAST(:metadata AS jsonb))
        """), {
            "title": title,
            "content": content,
            "source_type": source_type,
            "source_id": source_id,
            "embedding": embedding_str,
            "metadata": metadata or {}
        })
        db.commit()