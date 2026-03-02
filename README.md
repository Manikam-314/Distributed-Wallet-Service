# 🚀 Kafka Integration — Advanced DLQ & Replay Pipeline

## 📌 Overview

This module implements an **Advanced Dead Letter Queue (DLQ) + Replay Pipeline** using **Apache Kafka + Spring Boot** in a distributed microservices architecture.

It extends basic retry and DLQ handling by introducing:

* Failure tracking
* Event replay mechanism
* Manual recovery support
* Message reprocessing pipeline
* Production-grade fault handling

This is part of the **Distributed Wallet & Payment Platform**.

---

# 🎯 Problem Solved

Basic retry and DLQ solve temporary failures, but production systems also need:

* Recovery of failed events
* Manual reprocessing
* Replay of messages
* Failure debugging
* Long-term reliability

### ❌ Without Replay System

```text
Event fails → Sent to DLQ → Stays there forever ❌
```

This causes:

* Data inconsistency
* Financial issues
* Manual intervention complexity

---

# ✅ Solution — Advanced DLQ + Replay Pipeline

This implementation provides:

* Automatic retry handling
* Dead letter queue storage
* DLQ consumer monitoring
* Event replay pipeline
* Recovery-based processing
* End-to-end fault tolerance

---

# 🏗 Architecture

```text
Producer Service
      ↓
Kafka Main Topic (transaction-created)
      ↓
Consumer Processing
      ↓
Success → Completed
Failure → Retry
      ↓
Still fails → Dead Letter Topic
      ↓
DLQ Consumer
      ↓
Replay Pipeline
      ↓
Reprocess Event → Main Topic
```

---

# ⚙️ How It Works

## 1️⃣ Event Production

Transaction service publishes event:

```java
kafkaTemplate.send("transaction-created", event);
```

Kafka stores the message.

---

## 2️⃣ Consumer Processing

Wallet service processes event:

```java
@KafkaListener(topics = "transaction-created")
```

* Updates wallet balance
* Handles transaction logic

---

## 3️⃣ Retry Handling

If failure occurs:

* Message retried automatically
* Temporary errors resolved
* Prevents data loss

---

## 4️⃣ Dead Letter Queue (DLQ)

If retries fail:

```text
transaction-created.DLT
```

Message moved to DLQ.

Purpose:

* Failure isolation
* Debugging
* Recovery support

---

## 5️⃣ DLQ Consumer

Separate consumer listens to DLQ topic:

```text
transaction-created.DLT
```

Used for:

* Logging failed events
* Monitoring failures
* Triggering replay

---

## 6️⃣ Replay Pipeline ⭐ (Advanced Feature)

This is the major enhancement.

### Replay Pipeline Flow

```text
DLQ Event → Validation → Replay → Main Topic
```

Replay process:

* Reads failed event
* Validates data
* Fixes transient issue
* Republishes to original topic
* Consumer processes again

---

## 7️⃣ Event Reprocessing

After replay:

```text
transaction-created.DLT → transaction-created
```

Event processed again successfully.

This ensures:

* No permanent failure
* Event recovery
* System consistency

---

# 🔑 Key Features Implemented

## ✅ Automatic Retry

Temporary failures retried automatically.

---

## ✅ Dead Letter Queue

Failed messages stored safely.

---

## ✅ DLQ Consumer

Dedicated failure monitoring system.

---

## ✅ Replay Pipeline ⭐

Reprocess failed events from DLQ.

---

## ✅ Failure Recovery System

Allows system self-healing.

---

## ✅ Event Reprocessing

Replays failed messages to main topic.

---

## ✅ Production-Grade Fault Handling

Reliable messaging system.

---

# 📦 Topics Used

| Topic                         | Purpose                    |
| ----------------------------- | -------------------------- |
| `transaction-created`         | Main event stream          |
| `transaction-created.DLT`     | Failed events              |
| `transaction-created.DLT.DLT` | Secondary failure handling |
| `test-topic`                  | Testing                    |

---

# 📂 Services in System

## 🧾 Transaction Service

* Publishes events
* Initiates transaction workflow

---

## 💰 Wallet Service

* Processes transactions
* Handles retry logic
* Consumes DLQ events
* Supports replay pipeline

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
Kafka main topic
        ↓
Wallet consumer processes
        ↓
✔ Success → Done
❌ Failure → Retry
❌ Still fails → DLQ
        ↓
Replay pipeline
        ↓
Reprocessed successfully
```

---

# ⭐ Benefits

* Zero message loss
* Failure recovery support
* Replay capability
* System reliability
* Event consistency
* Production-ready architecture
* Self-healing system design

---

# 🧠 Real-World Use Cases

* Banking systems
* Payment platforms
* Order processing
* Financial transactions
* Distributed microservices
* High reliability systems

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

* Automated replay scheduler
* Failure monitoring dashboard
* Alert system for DLQ events
* Admin UI for replay control
* Exponential backoff retry strategy

---

# 👨‍💻 Author

Manikam — Distributed Systems Learning Project

---

# 📜 License

For educational and learning purposes.
