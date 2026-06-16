import http from 'k6/http';
import { check, sleep } from 'k6';
import { randomString } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

export const options = {
    vus: 50,
    duration: '30s',
    thresholds: {
        http_req_duration: ['p(95)<500'], // 95% of requests must be under 500ms
        http_req_failed: ['rate<0.01'],    // Error rate must be less than 1%
    },
};

const BASE_URL = 'http://localhost:8090/api';

export default function () {
    // 1. Simulate Auth checking/routing under load
    const loginPayload = JSON.stringify({
        email: `loaduser_${randomString(5)}@test.com`,
        password: 'Password123!'
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
    };

    // Just hit a login endpoint to stress Auth Service
    const loginRes = http.post(`${BASE_URL}/auth/login`, loginPayload, params);
    
    // It's expected to return 401 or 404 because user doesn't exist, 
    // but we use it to test Gateway -> Auth routing and DB query performance
    check(loginRes, {
        'status is 401 or 400 or 200': (r) => [401, 400, 200].includes(r.status),
    });

    sleep(1);
}
