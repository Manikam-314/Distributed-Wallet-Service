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
