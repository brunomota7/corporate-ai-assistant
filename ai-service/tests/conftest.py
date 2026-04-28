import os
import pytest

os.environ.setdefault("OPENAI_API_KEY", "sk-test-fake-key")
os.environ.setdefault("DATABASE_URL", "postgresql://fake:fake@localhost:5432/fake_db")