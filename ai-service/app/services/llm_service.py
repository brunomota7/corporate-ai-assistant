from openai import OpenAI
from app.config import settings

class LlmService:
    """
    Responsável pela integração com o modelo de chat da OpenAI.
    Recebe mensagens já formatadas e retorna resposta + tokens.
    """
    def __init__(self):
        self.client = OpenAI(api_key=settings.openai_api_key)
        self.model = "gpt-4o-mini"

    def chat(self, messages: list[dict]) -> dict:
        response = self.client.chat.completions.create(
            model=self.model,
            messages=messages,
            temperature=0.7,
            max_tokens=500
        )
        return {
            "answer": response.choices[0].message.content,
            "tokens_used": response.usage.completion_tokens
        }