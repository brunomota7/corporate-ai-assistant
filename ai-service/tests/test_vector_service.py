from unittest.mock import MagicMock, patch
from app.services.vector_service import VectorService


class TestVectorService:

    def setup_method(self):
        self.vector_service = VectorService()

    def test_embed_returns_float_list(self, mocker):
        """
        Verifica que embed() retorna a lista de floats
        gerada pela OpenAI Embeddings API
        """
        fake_embedding = [0.1, 0.2, 0.3, 0.4, 0.5]

        mock_data = MagicMock()
        mock_data.embedding = fake_embedding

        mock_response = MagicMock()
        mock_response.data = [mock_data]

        mocker.patch.object(
            self.vector_service.client.embeddings,
            "create",
            return_value=mock_response
        )

        result = self.vector_service.embed("texto de teste")

        assert result == fake_embedding
        assert isinstance(result, list)

    def test_search_returns_chunks_from_database(self, mocker):
        """
        Verifica que search() gera o embedding da query,
        consulta o banco e retorna a lista de conteúdos
        """
        mocker.patch.object(
            self.vector_service,
            "embed",
            return_value=[0.1] * 1536
        )

        mock_row_1 = MagicMock()
        mock_row_1.__getitem__ = lambda self, idx: "Chunk sobre estoque" if idx == 0 else None

        mock_row_2 = MagicMock()
        mock_row_2.__getitem__ = lambda self, idx: "Chunk sobre preços" if idx == 0 else None

        mock_result = MagicMock()
        mock_result.fetchall.return_value = [mock_row_1, mock_row_2]

        mock_db = MagicMock()
        mock_db.execute.return_value = mock_result

        mocker.patch("app.services.vector_service.get_db", return_value=iter([mock_db]))

        result = self.vector_service.search("Qual o estoque?", top_k=2)

        assert len(result) == 2
        assert result[0] == "Chunk sobre estoque"
        assert result[1] == "Chunk sobre preços"

    def test_search_returns_empty_list_when_no_results(self, mocker):
        """
        Verifica que search() retorna lista vazia
        quando não há documentos similares no banco
        """
        mocker.patch.object(
            self.vector_service,
            "embed",
            return_value=[0.0] * 1536
        )

        mock_result = MagicMock()
        mock_result.fetchall.return_value = []

        mock_db = MagicMock()
        mock_db.execute.return_value = mock_result

        mocker.patch("app.services.vector_service.get_db", return_value=iter([mock_db]))

        result = self.vector_service.search("query sem resultado")

        assert result == []

    def test_store_generates_embedding_and_persists(self, mocker):
        """
        Verifica que store() gera o embedding do conteúdo
        e executa o INSERT no banco com os parâmetros corretos
        """
        fake_embedding = [0.5] * 1536

        mocker.patch.object(
            self.vector_service,
            "embed",
            return_value=fake_embedding
        )

        mock_db = MagicMock()
        mocker.patch("app.services.vector_service.get_db", return_value=iter([mock_db]))

        self.vector_service.store(
            title="Manual de produtos",
            content="Conteúdo do manual",
            source_type="MANUAL",
            source_id=None,
            metadata={"autor": "admin"}
        )

        mock_db.execute.assert_called_once()
        mock_db.commit.assert_called_once()