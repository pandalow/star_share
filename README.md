# Star Share

A knowledge sharing service built with Java 21 and Spring Boot 3.2.

## Introduction

Star Share is a modern backend service designed for high-concurrency content sharing platforms. It features a robust architecture integrating traditional relational databases with high-performance caching and messaging queues. The system is designed to handle large-scale user interactions such as likes, views, and comments efficiently.

## Related Docs
API: [API GUIDE](/docs/api/API.md)


## Technical Architecture

### Tech Stack

| Category | Technology |
|----------|------------|
| **Language** | Java 21 |
| **Framework** | Spring Boot 3.2.4 |
| **Database** | MySQL 9.5 |
| **ORM** | MyBatis 3.0 |
| **Caching & NoSQL** | Redis, Caffeine |
| **Message Queue** | Apache Kafka |
| **Security** | Spring Security + JWT |
| **Tooling** | Maven, Lombok |

### Key Features

#### 1. High-Performance Counter Service


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
