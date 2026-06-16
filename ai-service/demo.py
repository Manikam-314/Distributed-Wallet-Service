import asyncio
from app.models.schemas import FraudCheckRequest, SpendingCategorizeRequest, CreditScoreRequest
from app.services.ml_service import ml_engine
from app.services.llm_service import llm_engine

async def main():
    print("\n--- AI SERVICE FEATURE DEMO ---")
    
    # 1. Load ML Models
    print("\nLoading models...")
    ml_engine.load_models()
    
    # 2. Fraud Check Demo
    print("\n1. FRAUD CHECK DEMO")
    fraud_req = FraudCheckRequest(
        userId="U123", amount=8000, txn_count_last_1min=15, txn_count_last_10min=50,
        location_change_flag=True, device_change_flag=True, time_of_day_hours=3.0
    )
    fraud_res = ml_engine.check_fraud(fraud_req)
    print(f"Input: Rapid 8000 txn at 3 AM with device & location change")
    print(f"Output: {fraud_res.model_dump_json(indent=2)}")

    # 3. Categorization Demo
    print("\n2. SPENDING CATEGORIZATION DEMO")
    cat_req = SpendingCategorizeRequest(description="zomato order dinner", amount=35)
    cat_res = ml_engine.categorize_spending(cat_req)
    print(f"Input: 'zomato order dinner'")
    print(f"Output: {cat_res.model_dump_json(indent=2)}")

    # 4. Credit Score Demo
    print("\n3. CREDIT SCORE DEMO")
    cred_req = CreditScoreRequest(
        userId="U123", repayment_delay_days=0.0, avg_balance=50000.0,
        txn_frequency_per_month=30, failure_rate=0.01
    )
    cred_res = ml_engine.get_credit_score(cred_req)
    print(f"Input: No delays, 50k balance, high frequency")
    print(f"Output: {cred_res.model_dump_json(indent=2)}")

    # 5. Smart Reminder Demo
    print("\n4. SMART REMINDER DEMO")
    text = "bro I will send 500 dollars tomorrow morning"
    rem_res = await llm_engine.extract_reminder(text)
    print(f"Input: '{text}'")
    print(f"Output: {rem_res.model_dump_json(indent=2)}")

if __name__ == "__main__":
    asyncio.run(main())
