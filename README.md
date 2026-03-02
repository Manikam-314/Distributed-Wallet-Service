# 🚀 Kafka Integration — Retry & Dead Letter Queue (DLQ)

## 📌 Overview

This project implements **Apache Kafka Retry and Dead Letter Queue (DLQ) mechanisms** in a distributed microservices architecture using **Spring Boot + Kafka**.

The system ensures:

* Reliable message processing
* Automatic retry on failure
* Failure isolation using DLQ
* No message loss
* Fault-tolerant event-driven communication

This implementation is part of a **Distributed Wallet & Payment Platform**.

---

# 🧠 Problem Solved

In event-driven systems, message processing may fail due to:

* Network issues
* Service downtime
* Database errors
* Invalid data
* Temporary failures

### ❌ Without Retry/DLQ

```text
Kafka Event → Consumer fails → Message lost ❌
```

This causes:

* Data inconsistency
* Financial errors
* System instability

---

# ✅ Solution — Retry + Dead Letter Queue

This implementation provides:

* Automatic retry mechanism
* Error handling
* Dead letter topic for failed events
* Safe message processing

---

# 🏗 Architecture

```text
Producer Service
     ↓
Kafka Topic (transaction-created)
     ↓
Wallet Service Consumer
     ↓
Success → Process message
Failure → Retry
     ↓
Still fails → Dead Letter Queue
```

---

# ⚙️ How It Works

## 1️⃣ Event Published to Kafka

Transaction Service publishes event:

```java
kafkaTemplate.send("transaction-created", event);
```

Kafka stores the event.

---

## 2️⃣ Consumer Processes Event

Wallet Service listens:

```java
@KafkaListener(topics = "transaction-created")
```

* Updates wallet balance
* Processes transaction

---

## 3️⃣ Retry Mechanism

If processing fails:

* Kafka retries message automatically
* Multiple retry attempts allowed
* Temporary errors can recover

### Example failures handled:

* DB connection issue
* Service temporary crash
* Network timeout

---

## 4️⃣ Dead Letter Queue (DLQ)

If message fails after all retries:

```text
transaction-created.DLT
```

Message moved to DLQ.

This ensures:

* No message loss
* Failure isolation
* Debugging support

---

## 5️⃣ DLQ Consumer

Separate consumer listens to:

```text
transaction-created.DLT
```

Used for:

* Error logging
* Manual recovery
* Monitoring failed events

---

# 🔑 Key Features Implemented

## ✅ Kafka Producer

Transaction service publishes events.

---

## ✅ Kafka Consumer

Wallet service processes events.

---

## ✅ Retry Mechanism

Automatic reprocessing on failure.

---

## ✅ Dead Letter Queue

Failed events stored safely.

---

## ✅ Error Handling Strategy

Prevents system crash due to bad messages.

---

## ✅ Event-Driven Architecture

Loose coupling between services.

---

# 📦 Topics Used

| Topic                     | Purpose                 |
| ------------------------- | ----------------------- |
| `transaction-created`     | Main transaction events |
| `transaction-created.DLT` | Failed message storage  |
| `test-topic`              | Testing events          |

---

# 📂 Services in System

## 🧾 Transaction Service

* Produces Kafka events
* Sends transaction events

---

## 💰 Wallet Service

* Consumes events
* Updates wallet balance
* Handles retries
* Processes DLQ messages

---

## 📦 Common Events Module

* Shared event models
* Event metadata
* Contract definitions

---

# 📊 Event Flow Example

```text
User creates transaction
        ↓
Transaction Service publishes event
        ↓
Kafka topic receives message
        ↓
Wallet Service consumes
        ↓
✔ Success → Wallet updated
❌ Failure → Retry
❌ Still fails → DLQ
```

---

# ⭐ Benefits

* Prevents message loss
* Improves system reliability
* Supports failure recovery
* Isolates problematic events
* Production-grade messaging
* Scalable architecture

---

# 🧩 Technologies Used

* Java 17
* Spring Boot
* Spring Kafka
* Apache Kafka
* MySQL
* Docker
* Maven

---

# 🚀 Future Improvements

* Configurable retry backoff strategy
* Monitoring dashboard for DLQ
* Alerting system for failures
* Event replay mechanism
* Exponential retry policy

---

# 👨‍💻 Author

Manikam — Distributed Systems Learning Project

---

# 📜 License

For educational and learning purposes.
