# 🚀 Kafka Outbox Pattern — Distributed Wallet System

## 📌 Overview

This project implements the **Outbox Pattern** using **Spring Boot + Apache Kafka** in a distributed microservices architecture.

The system ensures **reliable event publishing**, **data consistency**, and **failure-safe communication** between services.

The implementation is part of a **Distributed Wallet & Payment Platform**, where:

* Transaction Service creates transactions
* Events are stored safely in a database
* Events are published to Kafka
* Wallet Service processes events asynchronously

This prevents data loss and ensures financial integrity.

---

# 🧠 Problem Solved

In distributed systems, a major issue occurs:

### ❌ Without Outbox Pattern

```
Save transaction → Publish Kafka event
```

If Kafka fails after DB save:

* Transaction saved
* Event NOT published
* Wallet not updated
* System becomes inconsistent ❌

This is called:

👉 **Dual Write Problem**

---

# ✅ Solution — Outbox Pattern

The **Outbox Pattern** ensures:

* Transaction data saved
* Event stored in DB
* Event published reliably later
* No data loss
* Guaranteed delivery

---

# 🏗 Architecture

```
Client Request
     ↓
Transaction Service
     ↓
Save Transaction + Save Event (Outbox Table)
     ↓
Outbox Poller / Publisher
     ↓
Kafka Topic
     ↓
Wallet Service Consumer
     ↓
Wallet Update
```

---

# ⚙️ How It Works

## 1️⃣ Transaction Created

Transaction service:

* Saves transaction
* Stores event in `outbox_events` table

```
Transaction + Event → same DB transaction
```

This guarantees atomicity.

---

## 2️⃣ Event Stored in Outbox Table

Example record:

| id | aggregate_id | payload                 | status  |
| -- | ------------ | ----------------------- | ------- |
| 1  | 101          | TransactionCreatedEvent | PENDING |

Event is NOT immediately sent to Kafka.

---

## 3️⃣ Outbox Publisher

A background process:

* Reads pending events
* Publishes to Kafka
* Marks event as published

```
PENDING → SENT
```

If Kafka fails → retry later.

---

## 4️⃣ Kafka Event Processing

Wallet Service:

* Listens to `transaction-created` topic
* Updates wallet balance
* Uses Inbox pattern to prevent duplicates

---

# 🔑 Key Features Implemented

## ✅ Reliable Event Publishing

Events never lost even if Kafka is down.

---

## ✅ Atomic Transaction + Event Storage

Database transaction guarantees consistency.

---

## ✅ Retry Mechanism

Failed events are retried automatically.

---

## ✅ Eventual Consistency

Services stay consistent asynchronously.

---

## ✅ Inbox Pattern (Consumer Side)

Prevents duplicate processing.

---

## ✅ Dead Letter Queue (DLQ)

Failed messages routed to:

```
transaction-created.DLT
```

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

# 📂 Services in System

## 🧾 Transaction Service

* Creates transactions
* Writes events to outbox table
* Publishes events to Kafka

---

## 💰 Wallet Service

* Consumes transaction events
* Updates wallet balance
* Uses Inbox Pattern for idempotency

---

## 📦 Common Events Module

* Shared event contracts
* Event models
* Metadata structure

---

# 📊 Event Flow Example

```
User → Create Transaction
        ↓
Transaction DB saved
        ↓
Outbox event stored
        ↓
Kafka publish
        ↓
Wallet Service consumes
        ↓
Wallet balance updated
```

---

# ⭐ Benefits

* Prevents dual-write problem
* High reliability
* Failure recovery support
* Scalable microservices communication
* Financial data integrity
* Production-grade event architecture

---

# 🚀 Future Improvements

* Debezium-based CDC instead of polling
* Distributed transaction monitoring
* Event replay support
* Event schema versioning
* Saga orchestration support

---

# 👨‍💻 Author

Manikam — Distributed Systems Learning Project

---

# 📜 License

For educational and learning purposes.
