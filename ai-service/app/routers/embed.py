from fastapi import APIRouter
from app.schemas.embed_schema import EmbedRequest, EmbedResponse
from app.services.vector_service import  VectorService

router = APIRouter()
vector_service = VectorService()

@router.post("/embed", response_model=EmbedResponse)
async def embed(request: EmbedRequest):
    """Gera embedding de um texto. Usado pelo ingest no api-core."""
    embedding = vector_service.embed(request.text)
    return EmbedResponse(embedding=embedding)