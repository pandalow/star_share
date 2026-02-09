# Star Share

A knowledge sharing service built with Java 21 and Spring Boot 3.2.

## Introduction

Star Share is a modern backend service designed for high-concurrency content sharing platforms. It features a robust architecture integrating traditional relational databases with high-performance caching and messaging queues. The system is designed to handle large-scale user interactions such as likes, views, and comments efficiently.

## Technical Architecture

### Tech Stack

| Category | Technology |
|----------|------------|
| **Language** | Java 21 |
| **Framework** | Spring Boot 3.2.4 |
| **Database** | MySQL 9.5 |
| **ORM** | MyBatis 3.0 |
| **Caching & NoSQL** | Redis, Elasticsearch 9.2 |
| **Message Queue** | Apache Kafka |
| **AI & Vector Search** | Spring AI (w/ OpenAI/DeepSeek & ES Vector Store) |
| **Security** | Spring Security + JWT |
| **Tooling** | Maven, Lombok |

### Key Features

#### 1. High-Performance Counter Service
The project implements a "Write-Behind" aggregation strategy for handling high-frequency counter updates (e.g., post views, likes).
- **Event-Driven**: counter events are buffered via Kafka (`CounterAggregationConsumer`).
- **Aggregation**: events are aggregated in Redis bit-fields before being flushed to persistent storage.
- **Efficient Storage**: uses custom Lua scripts and binary-safe Redis strings to store multiple metrics (likes, views, etc.) in a compact memory format.

#### 2. AI Integration
- Built-in support for Large Language Models (LLM) via Spring AI.
- Configured to work with DeepSeek/OpenAI endpoints for chat and completion tasks.
- Vector store integration using Elasticsearch for RAG (Retrieval-Augmented Generation) capabilities.

## Project Structure

The codebase is organized by business domains:

```
src/main/java/com/star/share/
├── auth/           # Authentication, JWT, and Verification Code logic
├── cache/          # Cache configurations and Hotkey detection
├── common/         # Global exceptions, error codes, and utilities
├── config/         # Infrastructure config (ES, Redisson, ThreadPool)
├── counter/        # The high-performance counter service implementation
├── posts/          # Post management API
└── user/           # User entity and management service
```

## Configuration

The application uses `application.yaml` for configuration. Key setups include:
- **Datasource**: MySQL connection settings.
- **Redis & Kafka**: host ports and consumer group configurations.
- **AI**: API keys and base URLs for LLM providers.

## Getting Started

1. Ensure you have Java 21 and Maven installed.
2. Start the required infrastructure (MySQL, Redis, Kafka, Elasticsearch).
3. Configure your `application.yaml` with correct credentials.
4. Run the application: 
   ```bash
   ./mvnw spring-boot:run
   ```
