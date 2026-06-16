import threading
import requests
import time
import json
import statistics

BASE_URL = "http://localhost:8090/api/auth/login"
NUM_REQUESTS = 200
CONCURRENCY = 20

latencies = []
errors = 0

def load_task():
    global errors
    payload = {"email": "loaduser@test.com", "password": "WrongPassword"}
    start_time = time.time()
    try:
        resp = requests.post(BASE_URL, json=payload, timeout=5)
        # We expect 500 or 401, but we just measure latency
        latencies.append((time.time() - start_time) * 1000)
    except Exception:
        errors += 1

def run_load_test():
    threads = []
    print(f"Starting load test with {CONCURRENCY} concurrent threads, total {NUM_REQUESTS} requests...")
    start_time = time.time()
    
    for _ in range(NUM_REQUESTS):
        t = threading.Thread(target=load_task)
        threads.append(t)
        t.start()
        
        while threading.active_count() > CONCURRENCY:
            time.sleep(0.01)
            
    for t in threads:
        t.join()
        
    total_time = time.time() - start_time
    
    if len(latencies) > 0:
        avg_lat = statistics.mean(latencies)
        p95_lat = statistics.quantiles(latencies, n=20)[18] if len(latencies) >= 20 else max(latencies)
    else:
        avg_lat = 0
        p95_lat = 0
        
    print("\n=== LOAD TEST RESULTS ===")
    print(f"Total Time: {total_time:.2f}s")
    print(f"Total Requests: {NUM_REQUESTS}")
    print(f"Errors (Timeout/Conn): {errors}")
    print(f"Avg Latency: {avg_lat:.2f}ms")
    print(f"P95 Latency: {p95_lat:.2f}ms")

if __name__ == "__main__":
    run_load_test()
