import os
import pickle
import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestClassifier, RandomForestRegressor
from sklearn.linear_model import LogisticRegression
from sklearn.naive_bayes import MultinomialNB
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.pipeline import Pipeline
from sklearn.model_selection import train_test_split

def train_fraud_model():
    print("Training Fraud Detection Model...")
    # Synthetic data features: amount, txn_1m, txn_10m, loc_change, dev_change, time_of_day
    np.random.seed(42)
    n_samples = 5000
    
    amount = np.random.exponential(scale=1000, size=n_samples)
    txn_1m = np.random.poisson(lam=0.5, size=n_samples)
    txn_10m = np.random.poisson(lam=2, size=n_samples) + txn_1m
    loc_change = np.random.choice([0, 1], p=[0.95, 0.05], size=n_samples)
    dev_change = np.random.choice([0, 1], p=[0.98, 0.02], size=n_samples)
    time_of_day = np.random.uniform(0, 24, size=n_samples)
    
    # Target: Fraud (1) if high txn rate or high amount with location change or odd time
    is_fraud = ((txn_1m > 3) | 
                ((amount > 5000) & (loc_change == 1)) | 
                ((time_of_day < 5) & (amount > 2000))).astype(int)
                
    X = pd.DataFrame({
        'amount': amount, 'txn_1m': txn_1m, 'txn_10m': txn_10m,
        'loc_change': loc_change, 'dev_change': dev_change, 'time_of_day': time_of_day
    })
    y = is_fraud
    
    model = RandomForestClassifier(n_estimators=50, max_depth=10, random_state=42)
    model.fit(X, y)
    
    os.makedirs(os.path.dirname("ml_models/fraud_model.pkl"), exist_ok=True)
    with open("ml_models/fraud_model.pkl", "wb") as f:
        pickle.dump(model, f)
    print("Fraud model saved.")

def train_spending_classifier():
    print("Training Spending Classifier Model...")
    data = [
        ("swiggy order", "Food"), ("zomato food", "Food"), ("dominos pizza", "Food"),
        ("uber ride", "Travel"), ("ola cabs", "Travel"), ("metro card recharge", "Travel"),
        ("amazon shopping", "Shopping"), ("flipkart order", "Shopping"), ("myntra clothes", "Shopping"),
        ("electricity bill", "Bills"), ("water bill", "Bills"), ("jio recharge", "Bills"),
        ("netflix subscription", "Entertainment"), ("spotify premium", "Entertainment")
    ] * 50 # Duplicate to make somewhat larger
    
    X = [item[0] for item in data]
    y = [item[1] for item in data]
    
    # Pipeline with TF-IDF and Naive Bayes
    pipeline = Pipeline([
        ('tfidf', TfidfVectorizer()),
        ('clf', MultinomialNB())
    ])
    
    pipeline.fit(X, y)
    
    with open("ml_models/spending_model.pkl", "wb") as f:
        pickle.dump(pipeline, f)
    print("Spending classifier saved.")

def train_credit_scorer():
    print("Training Credit Scoring Model...")
    # Features: repayment_delay_days, avg_balance, txn_frequency_per_month, failure_rate
    n_samples = 3000
    np.random.seed(42)
    
    delay = np.random.exponential(scale=10, size=n_samples) # 0 to ~50 days
    balance = np.random.lognormal(mean=9, sigma=1, size=n_samples) # 1k to 100k
    freq = np.random.poisson(lam=15, size=n_samples)
    failure = np.random.beta(a=1, b=9, size=n_samples) # 0.0 to 1.0 mostly low
    
    # Target score: 850 max, penalize for delay, reward high balance/freq, penalize failure
    score = 850 - (delay * 5) + (np.log1p(balance) * 5) + (freq * 0.5) - (failure * 200)
    score = np.clip(score, 300, 850) # Cap between 300 and 850
    
    X = pd.DataFrame({
        'delay': delay, 'balance': balance, 'freq': freq, 'failure': failure
    })
    y = score
    
    model = RandomForestRegressor(n_estimators=50, max_depth=10, random_state=42)
    model.fit(X, y)
    
    with open("ml_models/credit_model.pkl", "wb") as f:
        pickle.dump(model, f)
    print("Credit score model saved.")

if __name__ == "__main__":
    train_fraud_model()
    train_spending_classifier()
    train_credit_scorer()
    print("All models trained and saved successfully.")
