from fastapi import APIRouter
from app.schemas.embed_schema import SearchRequest, SearchResponse
from app.services.vector_service import VectorService

router = APIRouter()
vector_service = VectorService()

@router.post("/search", response_model=SearchResponse)
async def search(request: SearchRequest):
    """Busca chunks relevantes para a query no pgvector"""
    chunks = vector_service.search(request.query)
    return SearchResponse(chunks=chunks)