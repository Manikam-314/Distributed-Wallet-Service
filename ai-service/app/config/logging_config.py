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
