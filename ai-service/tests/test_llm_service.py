from unittest.mock import MagicMock, patch
from app.services.llm_service import LlmService


class TestLlmService:

    def setup_method(self):
        self.llm_service = LlmService()

    def test_chat_returns_answer_and_tokens(self, mocker):
        """
        Caminho feliz — verifica que o retorno do LLM
        é mapeado corretamente para answer e tokens_used
        """
        mock_choice = MagicMock()
        mock_choice.message.content = "O estoque atual é 10 unidades."

        mock_usage = MagicMock()
        mock_usage.completion_tokens = 18

        mock_response = MagicMock()
        mock_response.choices = [mock_choice]
        mock_response.usage = mock_usage

        mocker.patch.object(
            self.llm_service.client.chat.completions,
            "create",
            return_value=mock_response
        )

        messages = [{"role": "user", "content": "Qual o estoque?"}]
        result = self.llm_service.chat(messages)

        assert result["answer"] == "O estoque atual é 10 unidades."
        assert result["tokens_used"] == 18

    def test_chat_sends_correct_messages_to_openai(self, mocker):
        """
        Verifica que as mensagens são enviadas ao modelo correto
        e com os parâmetros esperados — equivalente ao verify() do Mockito
        """
        mock_choice = MagicMock()
        mock_choice.message.content = "Resposta qualquer"

        mock_usage = MagicMock()
        mock_usage.completion_tokens = 5

        mock_response = MagicMock()
        mock_response.choices = [mock_choice]
        mock_response.usage = mock_usage

        mock_create = mocker.patch.object(
            self.llm_service.client.chat.completions,
            "create",
            return_value=mock_response
        )

        messages = [
            {"role": "user", "content": "Pergunta"},
            {"role": "assistant", "content": "Resposta anterior"},
            {"role": "user", "content": "Nova pergunta"}
        ]

        self.llm_service.chat(messages)

        mock_create.assert_called_once_with(
            model="gpt-4o-mini",
            messages=messages,
            temperature=0.7,
            max_tokens=500
        )

    def test_chat_propagates_exception_when_openai_fails(self, mocker):
        """
        Verifica que exceções da OpenAI são propagadas —
        equivalente ao assertThrows do JUnit
        """
        mocker.patch.object(
            self.llm_service.client.chat.completions,
            "create",
            side_effect=Exception("OpenAI API error")
        )

        messages = [{"role": "user", "content": "Pergunta"}]

        try:
            self.llm_service.chat(messages)
            assert False, "Deveria ter lançado exceção"
        except Exception as e:
            assert "OpenAI API error" in str(e)