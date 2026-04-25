from pydantic import BaseModel
from typing import List

class EmbedRequest(BaseModel):
    text: str

class EmbedResponse(BaseModel):
    embedding: List[float]

class SearchRequest(BaseModel):
    query: str

class SearchResponse(BaseModel):
    chunks: List[str]