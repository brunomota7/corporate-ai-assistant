
from fastapi import FastAPI
from app.routers import chat, embed, search, health

app = FastAPI(title="ai-service")

app.include_router(health.router)
app.include_router(chat.router)
app.include_router(embed.router)
app.include_router(search.router)