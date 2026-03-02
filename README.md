# Star Share

Star Share is a domain-driven backend for collaborative knowledge sharing platforms. It combines transactional guarantees from MySQL + MyBatis with high-throughput counters, Redis-backed caching, Kafka-based outbox pipelines, Elasticsearch search, and Spring AI powered assistants to keep authoring, discovery, and social interactions fast even under spiky load.

## Highlights
- **Post lifecycle**: Draft → confirm OSS uploads → metadata edits → publish/delete endpoints exposed under `api/v1/posts/**`.
- **Auth & identity**: Email verification codes, password + code login, refresh-token rotation, password reset, and `/me` profile in [docs/api/Auth.md](docs/api/Auth.md).
- **Engagement graph**: Follow/unfollow, follower/following lists, mutual-state checks, and cached counters described in [docs/api/Relation.md](docs/api/Relation.md).
- **Actions & counters**: Atomic like/fav toggles (Action API) feed Kafka-backed counter aggregation served via [docs/api/Counter.md](docs/api/Counter.md).
- **Storage pipeline**: Aliyun OSS pre-signed uploads (see [docs/api/OSS.md](docs/api/OSS.md)) plus content hashing before publish.
- **AI integrations**: Spring AI (OpenAI + DeepSeek) with Elasticsearch vector store enables RAG-style assistants for knowledge surfacing and moderation workflows.

## Architecture
- **Service core**: Spring Boot 3.2 (Java 21) REST service with layered modules under `com.star.share`.
- **Persistence**: MyBatis + MySQL 9.5 primary store; Alibaba Canal subscribers keep Kafka topics/outbox tables in sync.
- **Caching**: Redis for shared counters, verification codes, and distributed locks (Redisson); Caffeine for local near-cache.
- **Messaging**: Apache Kafka for write decoupling, counter fan-out, and async projections.
- **Search & AI**: Elasticsearch both for keyword search and Spring AI vector store; LLM calls routed via Spring AI starters.
- **Observability**: Spring Boot Actuator provides `/actuator/**` health, metrics, and readiness probes.

## Module Map
```
src/main/java/com/star/share/
├── auth/        # Verification, JWT, refresh-token, and auditing flows
├── cache/       # Redis/Caffeine config plus hot-key detection
├── common/      # Error model, exceptions, utility helpers
├── config/      # Infrastructure beans (Elasticsearch, Redisson, thread pools)
├── counter/     # Counter schemas, Kafka consumers, aggregation services
├── posts/       # Draft/publish pipeline, feeds, and metadata APIs
├── relation/    # Follow graph commands, queries, and outbox processors
├── oss/         # OSS presign + upload verification adapters
└── user/        # Core user entity, mapper, and profile services
```

## Tech Stack

| Area | Technology |
|------|------------|
| Language | Java 21 |
| Framework | Spring Boot 3.2.4 |
| Persistence | MyBatis 3.0, MySQL 9.5, Canal |
| Messaging | Apache Kafka |
| Cache & Locking | Redis, Redisson, Caffeine |
| Search & Vector Store | Elasticsearch Java Client 9.2.1 |
| Object Storage | Aliyun OSS SDK |
| AI | Spring AI (OpenAI + DeepSeek) |
| Security | Spring Security, OAuth2 Resource Server (JWT) |
| Tooling | Maven Wrapper, Lombok, Actuator |

## Prerequisites
1. Java 21 (JDK) and Maven Wrapper (`./mvnw`) available.
2. Running infrastructure: MySQL, Redis, Kafka, Elasticsearch cluster, and Aliyun OSS bucket.
3. Optional: Mail server for verification emails, external LLM keys for Spring AI.

## Configuration
Edit `src/main/resources/application.yaml` (or override via environment variables) to supply:
- `spring.datasource.*` for MySQL credentials.
- `spring.data.redis.*` and Redisson nodes for caching/locks.
- `spring.kafka.*` consumer groups and bootstrap servers.
- `spring.elasticsearch.*` endpoints plus credentials.
- `oss.*` bucket/endpoint/signature configuration.
- `spring.ai.*` provider keys, base URLs, and vector store settings.
- `share.*` domain-specific toggles (rate limits, counter thresholds, thread pools).

## Run Locally
```bash
# Start with dev profile (recommended)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Package an executable jar
./mvnw clean package
java -jar target/share-1.0-SNAPSHOT.jar --spring.profiles.active=prod
```

## Testing & Quality Gates
```bash
# Unit + integration tests
./mvnw test

# Static checks (SpotBugs/Checkstyle if configured)
./mvnw verify
```

Actuator endpoints such as `/actuator/health`, `/actuator/metrics`, and `/actuator/prometheus` can be wired into your monitoring stack.

## API Reference
- Entry index: [docs/api/API.md](docs/api/API.md)
- Post workflow: [docs/api/Post.md](docs/api/Post.md)
- Auth & identity: [docs/api/Auth.md](docs/api/Auth.md)
- Relation graph: [docs/api/Relation.md](docs/api/Relation.md)
- Counter read model: [docs/api/Counter.md](docs/api/Counter.md)
- Action toggles: [docs/api/Action.md](docs/api/Action.md)
- OSS presign: [docs/api/OSS.md](docs/api/OSS.md)

## Deployment Notes
- Configure JVM memory (`JAVA_OPTS`) and thread pools (`ThreadPoolConfig`) per environment.
- Enable Kafka idempotence and DLQs for counter/action topics.
- Secure secrets through Spring Cloud Config, Vault, or environment variables; never commit plaintext keys.
- Run database migrations before deploying (Flyway/Liquibase scripts to be added as needed).

## Contributing
1. Create a feature branch.
2. Keep modules cohesive (one domain per package).
3. Add/extend API docs in `docs/api/` when new controllers are introduced.
4. Ensure tests and CI checks pass before opening a PR.
