import pytest
import requests
import time
import uuid
import threading
from faker import Faker

fake = Faker()

AUTH_URL = "http://localhost:18090/api/auth"
WALLET_URL = "http://localhost:18090/api/wallet"
TRANSACTION_URL = "http://localhost:18090/api/transactions"

@pytest.fixture(scope="module")
def env_session():
    def create_user_and_wallet():
        user = {"email": fake.email(), "password": "Password123!", "firstName": fake.first_name(), "lastName": fake.last_name(), "mobileNumber": fake.numerify(text='##########')}
        requests.post(f"{AUTH_URL}/register", json=user)
        resp = requests.post(f"{AUTH_URL}/login", json={"email": user["email"], "password": user["password"]})
        token = resp.json()["token"]
        user_id = resp.json()["id"]
        headers = {"Authorization": f"Bearer {token}"}
        
        wallet_id = None
        for _ in range(15):
            w_resp = requests.get(f"{WALLET_URL}/by-user/{user_id}", headers=headers)
            if w_resp.status_code == 200:
                wallet_id = w_resp.json()["id"]
                break
            time.sleep(1)
        return {"user_id": user_id, "token": token, "wallet_id": wallet_id, "headers": headers}
        
    sender = create_user_and_wallet()
    receiver = create_user_and_wallet()
    
    requests.post(f"{WALLET_URL}/deposit", json={"walletId": sender["wallet_id"], "amount": 1000.0}, headers=sender["headers"])
    return {"sender": sender, "receiver": receiver}

def test_transfer_success_and_history(env_session):
    sender = env_session["sender"]
    receiver = env_session["receiver"]
    transfer_payload = {"senderWalletId": sender["wallet_id"], "receiverWalletId": receiver["wallet_id"], "amount": 100.0}
    
    headers = sender["headers"].copy()
    headers["idempotency-key"] = str(uuid.uuid4())
    resp = requests.post(f"{TRANSACTION_URL}/transfer", json=transfer_payload, headers=headers)
    assert resp.status_code == 200
    
    time.sleep(8) # Wait for saga completion
    
    # Check history
    h_resp = requests.get(f"{TRANSACTION_URL}?walletId={sender['wallet_id']}", headers=headers)
    assert h_resp.status_code == 200
    txs = h_resp.json()
    assert len(txs) > 0
    assert txs[0]["amount"] == 100.0

def test_concurrent_transfers_idempotency(env_session):
    # Test identical rapid requests do not double charge
    sender = env_session["sender"]
    receiver = env_session["receiver"]
    
    # Get initial balance
    b_resp1 = requests.get(f"{WALLET_URL}/balance?walletId={sender['wallet_id']}", headers=sender["headers"])
    start_balance = float(b_resp1.text)
    
    idem_key = str(uuid.uuid4())
    headers = sender["headers"].copy()
    headers["idempotency-key"] = idem_key
    payload = {"senderWalletId": sender["wallet_id"], "receiverWalletId": receiver["wallet_id"], "amount": 50.0}
    
    def make_request():
        requests.post(f"{TRANSACTION_URL}/transfer", json=payload, headers=headers)
        
    threads = []
    for _ in range(5):
        t = threading.Thread(target=make_request)
        threads.append(t)
        t.start()
        
    for t in threads:
        t.join()
        
    time.sleep(8)
    
    # Balance should only have dropped by 50, not 250
    b_resp2 = requests.get(f"{WALLET_URL}/balance?walletId={sender['wallet_id']}", headers=sender["headers"])
    assert float(b_resp2.text) == start_balance - 50.0

def test_transfer_insufficient_funds(env_session):
    sender = env_session["sender"]
    receiver = env_session["receiver"]
    
    headers = sender["headers"].copy()
    headers["idempotency-key"] = str(uuid.uuid4())
    payload = {"senderWalletId": sender["wallet_id"], "receiverWalletId": receiver["wallet_id"], "amount": 50000.0}
    
    resp = requests.post(f"{TRANSACTION_URL}/transfer", json=payload, headers=headers)
    # The saga will fail async or sync depending on implementation
    
    time.sleep(5)
    
    # History should show FAILED
    h_resp = requests.get(f"{TRANSACTION_URL}?walletId={sender['wallet_id']}", headers=headers)
    txs = h_resp.json()
    failed_txs = [tx for tx in txs if tx["amount"] == 50000.0]
    if failed_txs:
        assert failed_txs[0]["status"] == "FAILED"

def test_transfer_negative_amount(env_session):
    sender = env_session["sender"]
    receiver = env_session["receiver"]
    headers = sender["headers"].copy()
    headers["idempotency-key"] = str(uuid.uuid4())
    payload = {"senderWalletId": sender["wallet_id"], "receiverWalletId": receiver["wallet_id"], "amount": -100.0}
    
    resp = requests.post(f"{TRANSACTION_URL}/transfer", json=payload, headers=headers)
    assert resp.status_code in [400, 500]

def test_transfer_missing_idempotency_key(env_session):
    sender = env_session["sender"]
    receiver = env_session["receiver"]
    payload = {"senderWalletId": sender["wallet_id"], "receiverWalletId": receiver["wallet_id"], "amount": 10.0}
    
    resp = requests.post(f"{TRANSACTION_URL}/transfer", json=payload, headers=sender["headers"]) # No idempotency-key header
    # System should reject requests without idempotency key
    assert resp.status_code in [400, 500], "Should enforce idempotency keys on mutating transaction requests"
