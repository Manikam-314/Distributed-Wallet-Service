# Empty init
from pydantic_settings import BaseSettings
from typing import Optional

class Settings(BaseSettings):
    app_name: str = "ai-service"
    environment: str = "development"
    log_level: str = "INFO"
    app_port: int = 8000

    enable_kafka: bool = False
    enable_redis: bool = False

    openai_api_key: Optional[str] = None
    gemini_api_key: Optional[str] = None
    llm_provider: str = "gemini"

    fraud_model_path: str = "ml_models/fraud_model.pkl"
    spending_model_path: str = "ml_models/spending_model.pkl"
    credit_model_path: str = "ml_models/credit_model.pkl"

    kafka_bootstrap_servers: str = "localhost:9092"
    kafka_notifications_topic: str = "notifications.events"

    redis_host: str = "localhost"
    redis_port: int = 6379

    class Config:
        env_file = ".env"

settings = Settings()
import logging
import sys
from pydantic import BaseModel
from typing import Any, Dict
from datetime import datetime
import json

class JSONFormatter(logging.Formatter):
    def format(self, record):
        log_obj = {
            "timestamp": datetime.utcnow().isoformat() + "Z",
            "level": record.levelname,
            "message": record.getMessage(),
            "module": record.module,
            "line": record.lineno,
        }
        
        # Add correlation check
        if hasattr(record, "correlation_id"):
            log_obj["correlation_id"] = record.correlation_id
            
        return json.dumps(log_obj)

def setup_logging(log_level: str = "INFO"):
    logger = logging.getLogger("ai_service")
    logger.setLevel(getattr(logging, log_level.upper(), logging.INFO))
    
    # Prevent adding handlers multiple times
    if not logger.handlers:
        handler = logging.StreamHandler(sys.stdout)
        handler.setFormatter(JSONFormatter())
        logger.addHandler(handler)
        
    return logger

logger = setup_logging()
