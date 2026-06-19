from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from src.api.routes import health, jobs
from src.shared.config import settings
from src.shared.log import configure_logging, get_logger


configure_logging()
logger = get_logger(__name__)

app = FastAPI(title="omniscribe API")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)
    
app.include_router(health.router, prefix="/health")
app.include_router(jobs.router, tags=["jobs"])

@app.get("/")
async def index():
    return {"service": "omniscribe api", "status": "ok"}

