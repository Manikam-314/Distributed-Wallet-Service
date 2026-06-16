from fastapi import FastAPI, Request
from contextlib import asynccontextmanager
import time
import uuid

from app.config.logging_config import logger
from app.config.settings import settings

# Routers (to be created)
from app.controllers import ai_controller

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup: Load ML models
    logger.info("Initializing AI Service. Loading ML Models...")
    from app.services.ml_service import ml_engine
    ml_engine.load_models()
    
    # Startup: Start Scheduler
    from app.services.scheduler_service import start_scheduler
    start_scheduler()
    
    yield
    # Shutdown
    logger.info("Shutting down AI Service...")

app = FastAPI(title="AI Fintech Service", version="1.0.0", lifespan=lifespan)

# Middleware for generic error handling and correlationId logging
@app.middleware("http")
async def add_correlation_id_and_log(request: Request, call_next):
    correlation_id = request.headers.get("X-Correlation-Id", str(uuid.uuid4()))
    request.state.correlation_id = correlation_id
    
    # Attach correlation id to structured logger locally via ContextVar in real app, 
    # for simplicity we inject into log message context
    start_time = time.time()
    logger.info(f"Request started: {request.method} {request.url.path} (CID: {correlation_id})")
    
    try:
        response = await call_next(request)
        process_time = time.time() - start_time
        response.headers["X-Correlation-Id"] = correlation_id
        logger.info(f"Request completed: {request.method} {request.url.path} - Status: {response.status_code} in {process_time:.3f}s (CID: {correlation_id})")
        return response
    except Exception as e:
        process_time = time.time() - start_time
        logger.error(f"Request failed: {request.method} {request.url.path} - Error: {str(e)} in {process_time:.3f}s (CID: {correlation_id})")
        raise

# API Routers
app.include_router(ai_controller.router, prefix="/ai", tags=["AI Engine"])

@app.get("/health")
def health_check():
    return {"status": "UP", "service": settings.app_name}
