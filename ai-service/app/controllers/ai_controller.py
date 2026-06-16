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
