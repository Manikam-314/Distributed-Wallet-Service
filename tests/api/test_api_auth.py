import pytest
import requests
import jwt
from faker import Faker
import time

fake = Faker()
GATEWAY_URL = "http://localhost:18090/api/auth"

@pytest.fixture(scope="module")
def new_user():
    return {
        "email": fake.email(),
        "password": "Password123!",
        "firstName": fake.first_name(),
        "lastName": fake.last_name(),
        "mobileNumber": fake.numerify(text='##########')
    }

def test_register_user_valid(new_user):
    resp = requests.post(f"{GATEWAY_URL}/register", json=new_user)
    assert resp.status_code == 200, f"Registration failed: {resp.text}"

def test_register_duplicate_email(new_user):
    resp = requests.post(f"{GATEWAY_URL}/register", json=new_user)
    assert resp.status_code in [400, 409], f"Duplicate email should fail: {resp.status_code}"

def test_register_missing_fields():
    resp = requests.post(f"{GATEWAY_URL}/register", json={"email": fake.email()})
    assert resp.status_code in [400, 500], "Missing fields should fail validation"

def test_login_success(new_user):
    creds = {"email": new_user["email"], "password": new_user["password"]}
    resp = requests.post(f"{GATEWAY_URL}/login", json=creds)
    assert resp.status_code == 200
    
    data = resp.json()
    assert "token" in data
    assert "id" in data
    pytest.auth_token = data["token"]
    pytest.user_id = data["id"]

def test_login_invalid_credentials(new_user):
    creds = {"email": new_user["email"], "password": "WrongPassword!"}
    resp = requests.post(f"{GATEWAY_URL}/login", json=creds)
    assert resp.status_code in [401, 403], "Should reject invalid credentials"

def test_get_users_with_valid_token():
    headers = {"Authorization": f"Bearer {pytest.auth_token}"}
    resp = requests.get(f"{GATEWAY_URL}/users", headers=headers)
    assert resp.status_code == 200
    assert isinstance(resp.json(), list)

def test_get_users_without_token():
    resp = requests.get(f"{GATEWAY_URL}/users")
    assert resp.status_code in [401, 403], "Unauthorized access should be blocked"

def test_tampered_jwt_token():
    tampered_token = pytest.auth_token[:-5] + "aaaaa"
    headers = {"Authorization": f"Bearer {tampered_token}"}
    resp = requests.get(f"{GATEWAY_URL}/users", headers=headers)
    assert resp.status_code in [401, 403], "Tampered token should be rejected"

def test_expired_jwt_token():
    # Construct an expired token manually to simulate expiry
    payload = jwt.decode(pytest.auth_token, options={"verify_signature": False})
    payload["exp"] = int(time.time()) - 3600
    # Note: Using random secret since we just want it to be structurally valid but unauthorized
    expired_token = jwt.encode(payload, "dummy_secret", algorithm="HS256")
    headers = {"Authorization": f"Bearer {expired_token}"}
    resp = requests.get(f"{GATEWAY_URL}/users", headers=headers)
    assert resp.status_code in [401, 403], "Expired (or invalid signature) token should fail"
