import requests
import time

services = {
    "API Gateway (8090)": "http://localhost:8090/actuator/health",
    "Auth Service (8093)": "http://localhost:8093/actuator/health",
    "Wallet Service (8091)": "http://localhost:8091/actuator/health",
    "Transaction Service (8092)": "http://localhost:8092/actuator/health",
    "Notification Service (8094)": "http://localhost:8094/actuator/health",
    "AI Service (8095)": "http://localhost:8095/health"
}

def wait_for_services():
    print("Waiting for services to become healthy...")
    ready_services = set()
    
    timeout = 180  # Max 3 minutes
    start_time = time.time()
    
    while len(ready_services) < len(services):
        if time.time() - start_time > timeout:
            print("Timeout reached! Some services did not start:")
            for name in services:
                if name not in ready_services:
                    print(f"FAILED: {name}")
            exit(1)
            
        for name, url in services.items():
            if name in ready_services:
                continue
                
            try:
                resp = requests.get(url, timeout=2)
                if resp.status_code == 200:
                    data = resp.json()
                    status = data.get("status", "")
                    if status == "UP":
                        print(f"[{name}] is UP!")
                        ready_services.add(name)
            except Exception:
                pass
                
        if len(ready_services) < len(services):
            time.sleep(5)
            
    print("All backend services are UP AND RUNNING!")

if __name__ == "__main__":
    wait_for_services()
