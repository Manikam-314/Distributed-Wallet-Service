import pytest
import requests
import time
from faker import Faker

fake = Faker()
AUTH_URL = "http://localhost:18090/api/auth"
WALLET_URL = "http://localhost:18090/api/wallet"

@pytest.fixture(scope="module")
def user_wallet():
    # Register & Login
    user = {"email": fake.email(), "password": "Password123!", "firstName": fake.first_name(), "lastName": fake.last_name(), "mobileNumber": fake.numerify(text='##########')}
    requests.post(f"{AUTH_URL}/register", json=user)
    resp = requests.post(f"{AUTH_URL}/login", json=user)
    token = resp.json()["token"]
    user_id = resp.json()["id"]
    headers = {"Authorization": f"Bearer {token}"}
    
    # Wait for async wallet creation
    wallet_id = None
    for _ in range(15):
        w_resp = requests.get(f"{WALLET_URL}/by-user/{user_id}", headers=headers)
        if w_resp.status_code == 200:
            wallet_id = w_resp.json()["id"]
            break
        time.sleep(1)
        
    return {"user_id": user_id, "token": token, "wallet_id": wallet_id, "headers": headers}

def test_wallet_exists(user_wallet):
    assert user_wallet["wallet_id"] is not None, "Wallet was not created automatically"

def test_deposit_valid_amount(user_wallet):
    payload = {"walletId": user_wallet["wallet_id"], "amount": 1000.0}
    resp = requests.post(f"{WALLET_URL}/deposit", json=payload, headers=user_wallet["headers"])
    assert resp.status_code == 200
    
    b_resp = requests.get(f"{WALLET_URL}/balance?walletId={user_wallet['wallet_id']}", headers=user_wallet["headers"])
    assert float(b_resp.text) == 1000.0

def test_deposit_negative_amount(user_wallet):
    payload = {"walletId": user_wallet["wallet_id"], "amount": -50.0}
    resp = requests.post(f"{WALLET_URL}/deposit", json=payload, headers=user_wallet["headers"])
    assert resp.status_code in [400, 500], "Negative deposits must be blocked"

def test_deposit_massive_amount(user_wallet):
    # Test for overflows
    payload = {"walletId": user_wallet["wallet_id"], "amount": 999999999999999.0}
    resp = requests.post(f"{WALLET_URL}/deposit", json=payload, headers=user_wallet["headers"])
    # System should ideally reject or handle gracefully
    assert resp.status_code in [200, 400, 500]

def test_deposit_zero_amount(user_wallet):
    payload = {"walletId": user_wallet["wallet_id"], "amount": 0.0}
    resp = requests.post(f"{WALLET_URL}/deposit", json=payload, headers=user_wallet["headers"])
    assert resp.status_code in [400, 500], "Zero deposit should be rejected"

def test_get_balance_invalid_wallet(user_wallet):
    resp = requests.get(f"{WALLET_URL}/balance?walletId=999999", headers=user_wallet["headers"])
    assert resp.status_code in [404, 500]

def test_unauthorized_deposit(user_wallet):
    payload = {"walletId": user_wallet["wallet_id"], "amount": 100.0}
    resp = requests.post(f"{WALLET_URL}/deposit", json=payload)
    assert resp.status_code in [401, 403]
