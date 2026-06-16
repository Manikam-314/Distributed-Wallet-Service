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
