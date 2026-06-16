import pytest
import requests
import time
import uuid
from faker import Faker

fake = Faker()

AUTH_URL = "http://localhost:8090/api/auth"
WALLET_URL = "http://localhost:8090/api/wallet"
TRANSACTION_URL = "http://localhost:8090/api/transactions"

@pytest.fixture(scope="module")
def env_session():
    def create_user_and_wallet():
        user = {
            "email": fake.email(),
            "password": "Password123!",
            "firstName": fake.first_name(),
            "lastName": fake.last_name(),
            "mobileNumber": fake.numerify(text='##########')
        }
        requests.post(f"{AUTH_URL}/register", json=user)
        
        resp = requests.post(f"{AUTH_URL}/login", json={"email": user["email"], "password": user["password"]})
        data = resp.json()
        token = data["token"]
        user_id = data["id"]
        
        headers = {"Authorization": f"Bearer {token}"}
        
        wallet_id = None
        for _ in range(10):
            w_resp = requests.get(f"{WALLET_URL}/by-user/{user_id}", headers=headers)
            if w_resp.status_code == 200:
                wallet_id = w_resp.json()["id"]
                break
            time.sleep(1)
            
        return {"user_id": user_id, "token": token, "wallet_id": wallet_id, "headers": headers}
        
    sender = create_user_and_wallet()
    receiver = create_user_and_wallet()
    
    # Fund sender
    deposit_payload = {"walletId": sender["wallet_id"], "amount": 1000.0}
    requests.post(f"{WALLET_URL}/deposit", json=deposit_payload, headers=sender["headers"])
    
    return {"sender": sender, "receiver": receiver}

def test_successful_transfer(env_session):
    sender = env_session["sender"]
    receiver = env_session["receiver"]
    
    transfer_payload = {
        "senderWalletId": sender["wallet_id"],
        "receiverWalletId": receiver["wallet_id"],
        "amount": 200.0
    }
    
    idey_key = str(uuid.uuid4())
    headers = sender["headers"].copy()
    headers["idempotency-key"] = idey_key
    
    resp = requests.post(f"{TRANSACTION_URL}/transfer", json=transfer_payload, headers=headers)
    assert resp.status_code == 200, f"Transfer failed: {resp.text}"
    
    # Time for Kafka async processing to settle
    time.sleep(10)
    
    # Verify sender balance
    s_resp = requests.get(f"{WALLET_URL}/balance?walletId={sender['wallet_id']}", headers=sender["headers"])
    assert float(s_resp.text) == 800.0
    
    # Verify receiver balance
    r_resp = requests.get(f"{WALLET_URL}/balance?walletId={receiver['wallet_id']}", headers=receiver["headers"])
    assert float(r_resp.text) == 200.0

def test_insufficient_funds_transfer(env_session):
    sender = env_session["sender"]
    receiver = env_session["receiver"]
    
    # Sender currently has 800. Try sending 1000.
    transfer_payload = {
        "senderWalletId": sender["wallet_id"],
        "receiverWalletId": receiver["wallet_id"],
        "amount": 1000.0
    }
    headers = sender["headers"].copy()
    headers["idempotency-key"] = str(uuid.uuid4())
    
    resp = requests.post(f"{TRANSACTION_URL}/transfer", json=transfer_payload, headers=headers)
    
    # Transaction service typically triggers this asynchronously but since it intercepts it initially, 
    # check if there's a 4xx / 5xx or if it is queued and failed async. 
    # If 200 OK, verify wallet balance HAS NOT CHANGED
    time.sleep(10)
    
    s_resp = requests.get(f"{WALLET_URL}/balance?walletId={sender['wallet_id']}", headers=sender["headers"])
    assert float(s_resp.text) == 800.0, "Balance should remain unchanged if funds were insufficient"

def test_idempotency_duplicate_request(env_session):
    sender = env_session["sender"]
    receiver = env_session["receiver"]
    
    transfer_payload = {
        "senderWalletId": sender["wallet_id"],
        "receiverWalletId": receiver["wallet_id"],
        "amount": 150.0
    }
    
    idey_key = f"idempotent-key-{fake.uuid4()}"
    headers = sender["headers"].copy()
    headers["idempotency-key"] = idey_key
    
    # First request
    resp1 = requests.post(f"{TRANSACTION_URL}/transfer", json=transfer_payload, headers=headers)
    assert resp1.status_code == 200
    
    time.sleep(10)
    s_resp_1 = requests.get(f"{WALLET_URL}/balance?walletId={sender['wallet_id']}", headers=headers)
    balance_1 = float(s_resp_1.text)
    
    # Duplicate request
    resp2 = requests.post(f"{TRANSACTION_URL}/transfer", json=transfer_payload, headers=headers)
    assert resp2.status_code == 409
    
    time.sleep(10)
    s_resp_2 = requests.get(f"{WALLET_URL}/balance?walletId={sender['wallet_id']}", headers=headers)
    balance_2 = float(s_resp_2.text)
    
    # Balance should not drop twice
    assert balance_1 == balance_2, "Idempotency failed, transaction processed twice!"
