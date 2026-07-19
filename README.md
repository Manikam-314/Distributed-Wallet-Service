🌐 Distributed Fintech Wallet System

📖 Overview

Distributed Fintech Wallet System is a microservices-based digital wallet platform built using Spring Boot, Spring Cloud, Apache Kafka, Redis, React, and PostgreSQL/MySQL.

The project demonstrates modern backend engineering concepts including secure authentication, distributed transactions, event-driven communication, Redis caching, AI integration, observability, and containerized deployment.

🎯 Project Highlights
Microservices Architecture using Spring Boot
Secure Authentication with Spring Security & JWT
Wallet-to-Wallet Money Transfer
Distributed Transactions using Saga Pattern
Apache Kafka Event-Driven Communication
Transactional Outbox & Consumer Inbox Patterns
Redis Caching & Distributed Locking
AI-powered Wallet Assistant using Spring AI & Groq
Fraud Detection Service using Python FastAPI
Monitoring using Prometheus, Grafana & OpenTelemetry
Docker & Kubernetes Deployment
AWS EC2 Deployment Support
🏗️ System Architecture

Architecture Diagram

✨ Features
🔐 Authentication & Security
User Registration & Login
JWT Authentication
Spring Security
Password Encryption
Email OTP Verification
SMS OTP Verification
Role-based Authorization
💳 Wallet Management
Create Wallet
Deposit Money
Withdraw Money
Wallet Balance
Transaction History
Wallet-to-Wallet Transfer
🔄 Distributed Transactions
Saga Orchestration
Compensation Transactions
Event-driven Processing
Reliable Message Delivery
⚡ Event-Driven Architecture
Apache Kafka Producer
Apache Kafka Consumer
Asynchronous Notifications
Decoupled Microservices
🚀 Performance
Redis Caching
Redis Distributed Locks
API Gateway Routing
Scalable Microservices Architecture
🤖 AI Features
Spring AI Agent
Groq Llama 3.3 Integration
Natural Language Wallet Assistant
Fraud Detection using FastAPI
Transaction Categorization
📊 Monitoring
Prometheus Metrics
Grafana Dashboards
OpenTelemetry Tracing
Grafana Tempo
🧩 Microservices
Service	Description
API Gateway	Request routing, JWT validation, rate limiting
Auth Service	User authentication and authorization
Wallet Service	Wallet management, balance updates and Redis caching
Transaction Service	Money transfers, Saga orchestration and Kafka publishing
Notification Service	Email and SMS notifications
AI Service	Fraud detection using FastAPI
Agent Service	Spring AI conversational assistant
🛠 Technology Stack
Backend
Java 17
Spring Boot
Spring Security
Spring Cloud Gateway
Spring Data JPA
Hibernate
REST APIs
Frontend
React
TypeScript
Vite
Tailwind CSS
Zustand
React Query
Database
PostgreSQL
MySQL
Redis
Messaging
Apache Kafka
Zookeeper
AI
Spring AI
Groq API
OpenAI API
FastAPI
Scikit-Learn
DevOps
Docker
Docker Compose
Kubernetes
AWS EC2
Monitoring
Prometheus
Grafana
OpenTelemetry
Grafana Tempo
⚙️ Design Patterns
Saga Pattern

Coordinates distributed money transfers across multiple microservices while maintaining consistency through compensation workflows.

Transactional Outbox Pattern

Ensures reliable event publishing to Kafka by storing events in the database before publishing.

Consumer Inbox Pattern

Prevents duplicate Kafka message processing using unique event identifiers.

Redis Distributed Lock

Ensures concurrent wallet updates are processed safely without race conditions.

Redis Caching

Caches frequently accessed wallet and transaction data to reduce unnecessary database reads.

📁 Project Structure
distributed-wallet-system/

├── api-gateway/
├── auth-service/
├── wallet-service/
├── transaction-service/
├── notification-service/
├── ai-service/
├── agent-service/
├── consumer-wallet-v2/
├── common-events/
├── monitoring/
├── k8s/
├── docker-compose.yml
└── README.md
📸 Screenshots

Add screenshots for:

Login Page
Dashboard
Wallet Page
Transfer Money
Transaction History
Swagger UI
Grafana Dashboard
Prometheus Metrics
Kafka UI
AI Assistant

Example:

## Dashboard

![Dashboard](docs/dashboard.png)

## Swagger

![Swagger](docs/swagger.png)

## Grafana

![Grafana](docs/grafana.png)
🚀 Getting Started
Prerequisites
Java 17+
Maven 3.8+
Node.js 18+
Python 3.10+
Docker Desktop
Clone Repository
git clone https://github.com/yourusername/distributed-wallet-system.git

cd distributed-wallet-system
Start Infrastructure
docker compose up -d
Run Backend Services

Run each Spring Boot service individually:

mvn spring-boot:run

or use your automation script:

.\start_all_services.ps1
Run Frontend
cd consumer-wallet-v2

npm install

npm run dev

Frontend:

http://localhost:5173
🐳 Deployment

The project supports deployment using:

Docker Compose
Kubernetes
AWS EC2
📊 Monitoring

Application metrics are collected using Spring Boot Actuator, visualized with Prometheus and Grafana, and distributed traces are captured using OpenTelemetry and Grafana Tempo.

🔮 Future Improvements
Event Sourcing
CQRS
Keycloak Authentication
ELK Stack Integration
GitHub Actions CI/CD
Kubernetes Horizontal Pod Autoscaler
API Versioning
Multi-region Deployment
👨‍💻 Author

Manikam

Java Backend Developer

Tech Stack

Java
Spring Boot
Spring Security
Spring Cloud
Kafka
Redis
PostgreSQL
MySQL
React
Docker
Kubernetes
AWS
Spring AI
