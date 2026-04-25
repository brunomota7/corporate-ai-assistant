from pydantic import BaseModel
from typing import List

class MessageSchema(BaseModel):
    role: str
    content: str

class ChatRequest(BaseModel):
    messages: List[MessageSchema]
    context: List[str]

class ChatResponse(BaseModel):
    answers: str
    tokensUsed: int