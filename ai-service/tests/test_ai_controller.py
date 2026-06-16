from fastapi.testclient import TestClient
from main import app
from app.models.schemas import FraudCheckRequest, FraudCheckResponse

client = TestClient(app)

def test_health_check():
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json()["status"] == "UP"

def test_fraud_check_clean():
    payload = {
        "userId": "U12345",
        "amount": 5.0,
        "txn_count_last_1min": 0,
        "txn_count_last_10min": 1,
        "location_change_flag": False,
        "device_change_flag": False,
        "time_of_day_hours": 14.5
    }
    
    response = client.post("/ai/fraud-check", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert "fraudScore" in data
    assert data["action"] == "ALLOW"

def test_fraud_check_malicious():
    payload = {
        "userId": "U12345",
        "amount": 8000.0,
        "txn_count_last_1min": 15,  # Bot attack
        "txn_count_last_10min": 50, # Bot attack
        "location_change_flag": True, # Suspicious
        "device_change_flag": True, # Suspicious
        "time_of_day_hours": 3.0 # Midnight
    }
    
    response = client.post("/ai/fraud-check", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["action"] == "BLOCK" or data["action"] == "FLAG"

def test_spending_categorize():
    payload = {"description": "uber ride to work", "amount": 25.0}
    response = client.post("/ai/categorize", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert "category" in data

def test_credit_scoring():
    payload = {
        "userId": "U123",
        "repayment_delay_days": 0.0,
        "avg_balance": 50000.0,
        "txn_frequency_per_month": 30,
        "failure_rate": 0.05
    }
    response = client.post("/ai/credit-score", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert "creditScore" in data
    assert data["creditScore"] >= 300 and data["creditScore"] <= 850
package com.wallet.integrations;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.time.LocalDateTime;

/**
 * Example Spring Boot Service connecting to the Python AI service.
 * Inject this into the TransactionService or RequestService.
 */
@Service
public class AiServiceClient {

    private final RestTemplate restTemplate;

    @Value("${ai.service.url:http://localhost:8000/ai}")
    private String aiServiceUrl;

    public AiServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Data Transfer Object for Fraud Check API Request
     */
    public record FraudCheckReq(
            String userId,
            double amount,
            int txn_count_last_1min,
            int txn_count_last_10min,
            boolean location_change_flag,
            boolean device_change_flag,
            double time_of_day_hours
    ) {}

    /**
     * Data Transfer Object for Fraud Check API Response
     */
    public record FraudCheckRes(
            double fraudScore,
            String action,
            String reason
    ) {}

    /**
     * Calls the ML /fraud-check endpoint cleanly with timeouts and Correlation Headers.
     * Uses RestTemplate but can be swapped for WebClient.
     */
    public FraudCheckRes checkTransactionFraud(String userId, double amount, int transactionsLast10Min) {
        String url = aiServiceUrl + "/fraud-check";

        // Current hour of day
        LocalDateTime now = LocalDateTime.now();
        double hourOfDay = now.getHour() + (now.getMinute() / 60.0);

        FraudCheckReq requestDto = new FraudCheckReq(
                userId,
                amount,
                1, // Simplified count for the last 1min
                transactionsLast10Min,
                false, // Need to implement location diffing in standard geo logic
                false,
                hourOfDay
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Correlation-Id", java.util.UUID.randomUUID().toString());

        HttpEntity<FraudCheckReq> entity = new HttpEntity<>(requestDto, headers);

        try {
            ResponseEntity<FraudCheckRes> response = restTemplate.postForEntity(url, entity, FraudCheckRes.class);
            return response.getBody();
        } catch (Exception e) {
            // Important: Failsafe mechanism.
            // If AI is offline, allow and log, or fail-closed based on risk appetite.
            System.err.println("AI Fraud Check Failed: " + e.getMessage());
            return new FraudCheckRes(0.0, "ALLOW", "AI Service Unreachable - Bypass");
        }
    }
}
CREATE TABLE IF NOT EXISTS `Reminders` (
    `id` VARCHAR(36) NOT NULL PRIMARY KEY,            -- UUID
    `senderId` VARCHAR(50) NOT NULL,                  -- User ID who created the reminder
    `receiverId` VARCHAR(50) NOT NULL,                -- User ID who is supposed to receive/pay
    `amount` DECIMAL(15,2),                           -- Amount extracted, NULL if missing
    `dueDate` DATETIME,                               -- Scheduled time
    `message` TEXT NOT NULL,                          -- Original natural language text
    `status` ENUM('PENDING', 'COMPLETED', 'FAILED', 'PROCESSING') DEFAULT 'PENDING',
    `confidence` DECIMAL(5,4),                        -- AI Confidence score (0.0 to 1.0)
    `createdAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updatedAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX `idx_reminder_status_due` (`status`, `dueDate`) -- Optimized for the Scheduler Worker
);

CREATE TABLE IF NOT EXISTS `FraudEvents` (
    `id` VARCHAR(36) NOT NULL PRIMARY KEY,
    `transactionId` VARCHAR(50) NOT NULL,
    `userId` VARCHAR(50) NOT NULL,
    `amount` DECIMAL(15,2),
    `fraudScore` DECIMAL(5,4) NOT NULL,               -- The AI Score
    `actionTaken` ENUM('ALLOW', 'FLAG', 'BLOCK') NOT NULL,
    `reason` VARCHAR(255),                            -- Why it blocked/flagged
    `createdAt` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX `idx_fraud_user` (`userId`)
);
from apscheduler.schedulers.background import BackgroundScheduler
from apscheduler.triggers.cron import CronTrigger
from app.config.logging_config import logger
from app.config.settings import settings
from app.utils.kafka_producer import publish_notification

def process_reminders_job():
    """
    Simulates checking the database for reminders that are due and pushing to Kafka.
    In a real system, you would query the 'Reminder' table where dueDate <= NOW() and status == 'PENDING'.
    """
    logger.info("Scheduler Triggered: Checking for due reminders...")
    
    # Example simulated payload:
    # 1. Query: SELECT * FROM Reminder WHERE status = 'PENDING' AND dueDate <= NOW()
    # 2. Iterate:
    # 3. Mark status = 'PROCESSING'
    # 4. Push to notification queue
    # 5. Mark status = 'COMPLETED'
    
    # Simulated pending reminder found:
    simulated_reminder = {
        "reminderId": "123e4567-e89b-12d3-a456-426614174000",
        "senderId": "U1001",
        "receiverId": "U2005",
        "amount": 500,
        "message": "Payment reminder for amount 500",
        "dueDate": "2024-05-15T12:00:00Z"
    }
    
    # Try sending notification via Kafka (if enabled)
    success = publish_notification("reminder.due", simulated_reminder)
    
    if success:
        logger.info(f"Successfully processed and published reminder {simulated_reminder['reminderId']}")
    
def start_scheduler():
    scheduler = BackgroundScheduler()
    # Run at the top of every minute
    scheduler.add_job(
        process_reminders_job,
        trigger=CronTrigger(minute='*'),
        id='process_reminders',
        name='Process Pending Reminders',
        replace_existing=True
    )
    scheduler.start()
    logger.info("Background Scheduler started. Jobs scheduled.")
import pickle
import os
import pandas as pd
import numpy as np
from app.config.settings import settings
from app.config.logging_config import logger
from app.models.schemas import (
    FraudCheckRequest, FraudCheckResponse,
    SpendingCategorizeRequest, SpendingCategorizeResponse,
    CreditScoreRequest, CreditScoreResponse
)
from app.utils.exceptions import ModelNotLoadedException

class MLEngine:
    def __init__(self):
        self.fraud_model = None
        self.spending_model = None
        self.credit_model = None

    def load_models(self):
        try:
            if os.path.exists(settings.fraud_model_path):
                with open(settings.fraud_model_path, "rb") as f:
                    self.fraud_model = pickle.load(f)
            else:
                logger.warning("Fraud model not found. Using fallback logic.")

            if os.path.exists(settings.spending_model_path):
                with open(settings.spending_model_path, "rb") as f:
                    self.spending_model = pickle.load(f)
            else:
                logger.warning("Spending model not found. Using fallback logic.")

            if os.path.exists(settings.credit_model_path):
                with open(settings.credit_model_path, "rb") as f:
                    self.credit_model = pickle.load(f)
            else:
                logger.warning("Credit model not found. Using fallback logic.")

            logger.info("ML Models loaded successfully.")
        except Exception as e:
            logger.error(f"Failed to load ML models: {e}")

    def check_fraud(self, req: FraudCheckRequest) -> FraudCheckResponse:
        # Fallback logic if model fails/missing
        if not self.fraud_model:
            logger.info("Fraud Check: Using rule-based fallback.")
            if req.txn_count_last_1min > 5 or (req.amount > 10000 and req.location_change_flag):
                return FraudCheckResponse(fraudScore=0.9, action="BLOCK", reason="Rule: High velocity or large foreign txn")
            return FraudCheckResponse(fraudScore=0.1, action="ALLOW", reason="Rule: Looks normal")

        try:
            # Inference expects: amount, txn_1m, txn_10m, loc_change, dev_change, time_of_day
            features = pd.DataFrame([{
                'amount': req.amount,
                'txn_1m': req.txn_count_last_1min,
                'txn_10m': req.txn_count_last_10min,
                'loc_change': int(req.location_change_flag),
                'dev_change': int(req.device_change_flag),
                'time_of_day': req.time_of_day_hours
            }])
            
            # Predict probability of class 1 (Fraud)
            prob = self.fraud_model.predict_proba(features)[0][1]
            
            action = "ALLOW"
            reason = "Normal"
            if prob > 0.8:
                action = "BLOCK"
                reason = "High fraud score detected"
            elif prob > 0.4:
                action = "FLAG"
                reason = "Suspicious activity detected"
                
            return FraudCheckResponse(fraudScore=round(prob, 4), action=action, reason=reason)
        except Exception as e:
            logger.error(f"Fraud model inference failed: {e}")
            return FraudCheckResponse(fraudScore=0.0, action="ALLOW", reason="Inference error fallback")

    def categorize_spending(self, req: SpendingCategorizeRequest) -> SpendingCategorizeResponse:
        if not self.spending_model:
            return SpendingCategorizeResponse(category="Other", confidence=0.0)
            
        try:
            desc = req.description.lower()
            category = self.spending_model.predict([desc])[0]
            prob = max(self.spending_model.predict_proba([desc])[0])
            
            return SpendingCategorizeResponse(category=str(category), confidence=round(float(prob), 4))
        except Exception as e:
            logger.error(f"Spending model inference failed: {e}")
            return SpendingCategorizeResponse(category="Other", confidence=0.0)

    def get_credit_score(self, req: CreditScoreRequest) -> CreditScoreResponse:
        if not self.credit_model:
            # Fallback
            score = 650
            return CreditScoreResponse(creditScore=score, riskLevel="MEDIUM")
            
        try:
            features = pd.DataFrame([{
                'delay': req.repayment_delay_days,
                'balance': req.avg_balance,
                'freq': req.txn_frequency_per_month,
                'failure': req.failure_rate
            }])
            
            score = float(self.credit_model.predict(features)[0])
            score = int(np.clip(score, 300, 850)) # Strictly enforce credit score bounds
            
            level = "MEDIUM"
            if score >= 720:
                level = "LOW"
            elif score < 600:
                level = "HIGH"
                
            return CreditScoreResponse(creditScore=score, riskLevel=level)
        except Exception as e:
            logger.error(f"Credit model inference failed: {e}")
            return CreditScoreResponse(creditScore=650, riskLevel="MEDIUM")

ml_engine = MLEngine()
import json
import re
import dateparser
from datetime import datetime
import google.generativeai as genai
from pydantic import ValidationError
from typing import Dict, Any, Optional

from app.config.settings import settings
from app.config.logging_config import logger
from app.models.schemas import ReminderExtractionResponse

# Configure LLM provider statically for this example, focusing on Gemini as per the prompt request.
# In a real app we could use a factory pattern based on `settings.llm_provider`
if settings.gemini_api_key:
    genai.configure(api_key=settings.gemini_api_key)
    # Using the standard genai model. You could swap for pro/flash as needed.
    _model = genai.GenerativeModel('gemini-1.5-flash-latest')
else:
    _model = None
    logger.warning("No Gemini API key supplied. LLM fallback mode will be highly active.")

PROMPT_TEMPLATE = """
You are a production-grade NLP and AI extraction engine for a fintech application.
Your task is to extract structured payment reminder information from informal user messages.

Input text: "{text}"

Extraction Rules:
1. AMOUNT: Extract numeric monetary values (₹, $, etc.). If multiple amounts, choose the most relevant OR mark confidence low. If no amount, return null.
2. DATE (CRITICAL): You MUST extract relative and absolute dates ("tomorrow", "today", "next week", "next friday", "tonight", "by evening", "later"). Convert to exact ISO 8601 format (YYYY-MM-DDTHH:MM:SS) or null if ambiguous. NEVER ignore clear words like "tomorrow". 
3. INTENT: "I will send", "I'll give" -> "repay". "send me", "pay me" -> "request". Otherwise -> "unknown".
4. CONFIDENCE SCORING:
   - amount only -> 0.5
   - amount + intent -> 0.6
   - amount + date -> 0.75
   - amount + date + intent -> 0.85-0.95
   - ambiguous inputs -> reduce confidence
5. VALIDATION: valid = true ONLY IF amount is present AND dueDate is valid (not null). Otherwise valid = false.
6. EDGE CASES:
   - "send later" -> dueDate = null, valid = false
   - "garbage input" -> safe defaults (nulls, unknown, false)
7. STRICT RULES: DO NOT hallucinate values. DO NOT skip date extraction. ONLY reply with valid JSON format.

Output JSON Format:
{{
  "amount": number|null,
  "dueDate": "string"|null,
  "intent": "repay"|"request"|"unknown",
  "confidence": number,
  "valid": boolean
}}
"""

class LLMEngine:
    
    async def extract_reminder(self, message: str) -> ReminderExtractionResponse:
        logger.info(f"Extracting reminder from text: {message}")

        # ALWAYS RUN FALLBACK CHECK FIRST
        fallback = self._fallback_extract(message)

        llm_result = None
        if _model:
            try:
                prompt = PROMPT_TEMPLATE.format(text=message)
                response = await _model.generate_content_async(prompt)

                raw_text = response.text.strip('` \n').removeprefix('json')
                parsed_json = json.loads(raw_text)

                llm_result = parsed_json

            except Exception as e:
                logger.error(f"LLM failed: {e}")

        # MERGE LOGIC
        amount = fallback.amount
        due_date = fallback.dueDate
        intent = fallback.intent
        
        llm_success = False

        if llm_result:
            llm_success = True
            if llm_result.get("amount") is not None:
                amount = llm_result.get("amount")
            if llm_result.get("dueDate") is not None:
                due_date = llm_result.get("dueDate")
            if llm_result.get("intent") and llm_result.get("intent") != "unknown":
                intent = llm_result.get("intent")

        # VALIDATION RULE (STRICT)
        valid = bool(amount is not None and due_date is not None)

        # CONFIDENCE FIX
        confidence = 0.0
        if amount is not None and due_date is not None and intent != "unknown":
            confidence = 0.85
            if llm_success and llm_result.get("amount") is not None and llm_result.get("dueDate") is not None:
                confidence = 0.95
        elif amount is not None and due_date is not None:
            confidence = 0.75
        elif amount is not None and intent != "unknown":
            confidence = 0.6
        elif amount is not None:
            confidence = 0.5
        else:
            confidence = 0.2

        if fallback.dueDate and (llm_result is None or llm_result.get("dueDate") is None):
            confidence += 0.05
            confidence = min(confidence, 1.0)

        final = {
            "amount": amount,
            "dueDate": due_date,
            "intent": intent,
            "confidence": confidence,
            "valid": valid
        }

        res = ReminderExtractionResponse(**final)

        logger.info(f"FINAL RESULT: {res.model_dump_json()}")

        return res
    def _fallback_extract(self, message: str) -> ReminderExtractionResponse:
        """Rule-based extraction when LLM fails or is unavailable."""
        logger.info("Using Regex/Rule-based fallback for reminder extraction.")
        
        # 1. Simple amount extraction
        amount_match = re.search(r'(?:₹|\$|rs\.?)?\s*(\d+(?:\.\d{1,2})?)', message.lower())
        amount = float(amount_match.group(1)) if amount_match else None
        
        # 2. Simple intent guessing
        intent = "unknown"
        if any(w in message.lower() for w in ['pay', 'send', 'give', 'repay', 'will give']):
            intent = "repay"
        elif any(w in message.lower() for w in ['request', 'need', 'ask']):
            intent = "request"
            
        # 3. Date extraction fallback using dateparser
        due_date = None
        # Remove amount to prevent dateparser confusion (e.g. "500 tomorrow")
        msg_for_date = re.sub(r'(?:₹|\$|rs\.?)?\s*(\d+(?:\.\d{1,2})?)', '', message.lower())
        
        import dateparser.search
        dates = dateparser.search.search_dates(msg_for_date, settings={'TIMEZONE': 'UTC', 'PREFER_DATES_FROM': 'future'})
        if dates:
            # Pick the first matched date and zero out the time component
            due_date = dates[0][1].replace(hour=0, minute=0, second=0, microsecond=0).isoformat()
            
        # Strict validation: Only true if amount AND dueDate are present
        valid = bool(amount is not None and due_date is not None)
        
        # Calculate confidence using the new specific strict rules
        confidence = 0.0
        if amount is not None and due_date is not None and intent != "unknown":
            confidence = 0.85
        elif amount is not None and due_date is not None:
            confidence = 0.75
        elif amount is not None and intent != "unknown":
            confidence = 0.6
        elif amount is not None:
            confidence = 0.5
        else:
            confidence = 0.2
            
        return ReminderExtractionResponse(
            amount=amount,
            dueDate=due_date,
            intent=intent,
            confidence=confidence,
            valid=valid
        )

llm_engine = LLMEngine()
# Empty init
from pydantic import BaseModel, Field, validator
from typing import Optional, Literal
from datetime import datetime

# --- Feature 1: Smart Reminders ---
class ReminderExtractionRequest(BaseModel):
    message: str = Field(..., description="Raw text message from user")

class ReminderExtractionResponse(BaseModel):
    amount: Optional[float] = None
    dueDate: Optional[str] = Field(None, description="ISO Format date string")
    intent: Literal["repay", "request", "unknown"] = "unknown"
    confidence: float = Field(..., ge=0, le=1)
    valid: bool = False

# --- Feature 2: Fraud Detection ---
class FraudCheckRequest(BaseModel):
    userId: str
    amount: float
    txn_count_last_1min: int
    txn_count_last_10min: int
    location_change_flag: bool
    device_change_flag: bool
    time_of_day_hours: float = Field(..., description="Hour of current transaction, e.g., 14.5 for 2:30 PM")

class FraudCheckResponse(BaseModel):
    fraudScore: float = Field(..., ge=0, le=1)
    action: Literal["ALLOW", "FLAG", "BLOCK"]
    reason: Optional[str] = None

# --- Feature 3: Spending Analysis ---
class SpendingCategorizeRequest(BaseModel):
    description: str = Field(..., description="Transaction description/merchant name")
    amount: float

class SpendingCategorizeResponse(BaseModel):
    category: str
    confidence: float = Field(..., ge=0, le=1)

# --- Feature 4: Credit Scoring ---
class CreditScoreRequest(BaseModel):
    userId: str
    repayment_delay_days: float
    avg_balance: float
    txn_frequency_per_month: int
    failure_rate: float = Field(..., ge=0, le=1)

class CreditScoreResponse(BaseModel):
    creditScore: int = Field(..., ge=300, le=850)
    riskLevel: Literal["LOW", "MEDIUM", "HIGH"]
from fastapi import APIRouter, HTTPException, Depends
from typing import Dict, Any

from app.models.schemas import (
    ReminderExtractionRequest, ReminderExtractionResponse,
    FraudCheckRequest, FraudCheckResponse,
    SpendingCategorizeRequest, SpendingCategorizeResponse,
    CreditScoreRequest, CreditScoreResponse
)
from app.services.llm_service import llm_engine
from app.services.ml_service import ml_engine

router = APIRouter()

@router.post("/extract-reminder", response_model=ReminderExtractionResponse)
async def extract_reminder(request: ReminderExtractionRequest):
    """
    Feature 1: Extracts amount, due date, and intent from a messy text message.
    """
    try:
        return await llm_engine.extract_reminder(request.message)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/fraud-check", response_model=FraudCheckResponse)
def fraud_check(request: FraudCheckRequest):
    """
    Feature 2: Real-time fraud detection using ML inference.
    """
    try:
        return ml_engine.check_fraud(request)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/categorize", response_model=SpendingCategorizeResponse)
def categorize_spending(request: SpendingCategorizeRequest):
    """
    Feature 3: Categorize transactions for spending analysis.
    """
    try:
        return ml_engine.categorize_spending(request)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@router.post("/credit-score", response_model=CreditScoreResponse)
def get_credit_score(request: CreditScoreRequest):
    """
    Feature 4: Predict user credit score and risk level.
    """
    try:
        return ml_engine.get_credit_score(request)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
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
