import pytest
import requests
from faker import Faker

fake = Faker()
GATEWAY_URL = "http://localhost:8090/api/auth"

@pytest.fixture(scope="module")
def new_user():
    return {
        "email": fake.email(),
        "password": "Password123!",
        "firstName": fake.first_name(),
        "lastName": fake.last_name(),
        "mobileNumber": fake.numerify(text='##########')
    }

def test_register_user(new_user):
    resp = requests.post(f"{GATEWAY_URL}/register", json=new_user)
    assert resp.status_code == 200, f"Registration failed: {resp.text}"

def test_login_user(new_user):
    creds = {
        "email": new_user["email"],
        "password": new_user["password"]
    }
    resp = requests.post(f"{GATEWAY_URL}/login", json=creds)
    assert resp.status_code == 200, f"Login failed: {resp.text}"
    
    data = resp.json()
    assert "token" in data
    assert "id" in data
    
    # Save the auth info for other tests if needed
    pytest.auth_token = data["token"]
    pytest.user_id = data["id"]

def test_login_invalid_password(new_user):
    creds = {
        "email": new_user["email"],
        "password": "WrongPassword!"
    }
    resp = requests.post(f"{GATEWAY_URL}/login", json=creds)
    assert resp.status_code in [401, 403], "Should reject invalid password"

def test_get_users():
    headers = {"Authorization": f"Bearer {pytest.auth_token}"}
    resp = requests.get(f"{GATEWAY_URL}/users", headers=headers)
    assert resp.status_code == 200, "Should be able to fetch users with token"
    assert isinstance(resp.json(), list)

def test_get_users_without_token():
    resp = requests.get(f"{GATEWAY_URL}/users")
    assert resp.status_code in [401, 403], "Should block unauthorized access"
