from fastapi import APIRouter
from app.schemas.chat_schema import ChatRequest, ChatResponse
from app.services.llm_service import LlmService

router = APIRouter()
llm_service = LlmService()

@router.post("/chat", response_model=ChatResponse)
async def chat(request: ChatRequest):
    """
    Recebe histórico de mensagens e contexto RAG
    Injeta o contexto na última mensagem antes de chamar o LLM
    """
    messages = [{"role": m.role, "content": m.content} for m in request.messages]

    if messages and messages[-1]["role"] == "user" and request.context:
        context_text = "\n".join(request.context)
        messages[-1]["content"] += f"\n\nContexto relevante:\n{context_text}"

    result = llm_service.chat(messages)

    return ChatResponse(
        answers=result["answer"],
        tokensUsed=result["tokens_used"]
    )