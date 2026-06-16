import pytest
import requests
import time
from faker import Faker

fake = Faker()

AUTH_URL = "http://localhost:8090/api/auth"
WALLET_URL = "http://localhost:8090/api/wallet"

@pytest.fixture(scope="module")
def wallet_user_session():
    # Register user
    user = {
        "email": fake.email(),
        "password": "Password123!",
        "firstName": fake.first_name(),
        "lastName": fake.last_name(),
        "mobileNumber": fake.numerify(text='##########')
    }
    requests.post(f"{AUTH_URL}/register", json=user)
    
    # Login to get token and id
    resp = requests.post(f"{AUTH_URL}/login", json={"email": user["email"], "password": user["password"]})
    data = resp.json()
    token = data["token"]
    user_id = data["id"]
    
    headers = {"Authorization": f"Bearer {token}"}
    
    # Wait for Kafka event to create wallet asynchronously
    wallet_id = None
    for _ in range(10):
        w_resp = requests.get(f"{WALLET_URL}/by-user/{user_id}", headers=headers)
        if w_resp.status_code == 200:
            wallet_id = w_resp.json()["id"]
            break
        time.sleep(1)
        
    assert wallet_id is not None, "Wallet was not created after user registration"
    
    return {
        "user_id": user_id,
        "token": token,
        "wallet_id": wallet_id,
        "headers": headers
    }

def test_wallet_creation_event(wallet_user_session):
    headers = wallet_user_session["headers"]
    user_id = wallet_user_session["user_id"]
    
    resp = requests.get(f"{WALLET_URL}/by-user/{user_id}", headers=headers)
    assert resp.status_code == 200
    data = resp.json()
    assert data["userId"] == user_id
    assert data["balance"] == 0.0

def test_deposit(wallet_user_session):
    headers = wallet_user_session["headers"]
    wallet_id = wallet_user_session["wallet_id"]
    
    payload = {
        "walletId": wallet_id,
        "amount": 500.0
    }
    # Test valid deposit
    resp = requests.post(f"{WALLET_URL}/deposit", json=payload, headers=headers)
    assert resp.status_code == 200, f"Deposit failed: {resp.text}"
    
    # Test balance directly
    b_resp = requests.get(f"{WALLET_URL}/balance?walletId={wallet_id}", headers=headers)
    assert b_resp.status_code == 200
    assert float(b_resp.text) == 500.0

def test_deposit_negative_amount(wallet_user_session):
    headers = wallet_user_session["headers"]
    wallet_id = wallet_user_session["wallet_id"]
    
    payload = {
        "walletId": wallet_id,
        "amount": -100.0
    }
    resp = requests.post(f"{WALLET_URL}/deposit", json=payload, headers=headers)
    # The application should reject negative deposits natively, either 400 Bad Request or 500 Server Error depending on implementation
    assert resp.status_code in [400, 500], "Negative deposits should not be allowed"

def test_unauthorized_wallet_access():
    resp = requests.get(f"{WALLET_URL}/balance?walletId=1")
    assert resp.status_code in [401, 403], "Should reject without auth token"
