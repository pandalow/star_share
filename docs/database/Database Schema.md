# Database Schema Design

## 1. Overview

### Technical Stack
- **Database**: MySQL 8.0
- **Character Set**: utf8mb4
- **Collation**: utf8mb4_unicode_ci
- **Storage Engine**: InnoDB

### Core Entities
- **users**: User accounts and profiles
- **login_logs**: Authentication audit logs
- **know_posts**: User-generated posts/articles
- **outbox**: Event sourcing for reliable message publishing (Outbox Pattern)
- **following**: Follow relationships (write-optimized)
- **follower**: Follower relationships (read-optimized, denormalized)

---

## 2. ER Diagram

![ER Diagram](Star%20Share%20Schema%20ERD.png)

---

## 3. Table Structures

### 3.1 `users`

**Description**: Stores user account information and profile data.

| Field | Type | Constraints | Default | Description |
|-------|------|-------------|---------|-------------|
| `id` | BIGINT UNSIGNED | PK, AUTO_INCREMENT | - | Primary key |
| `phone` | VARCHAR(32) | UNIQUE, NULL | - | Phone number (optional) |
| `email` | VARCHAR(128) | UNIQUE, NULL | - | Email address (optional) |
| `password_hash` | VARCHAR(128) | NULL | - | Bcrypt password hash |
| `nickname` | VARCHAR(64) | NOT NULL | - | Display name |
| `avatar` | TEXT | NULL | - | Avatar URL |
| `bio` | VARCHAR(512) | NULL | - | User biography |
| `zg_id` | VARCHAR(64) | UNIQUE, NULL | - | Custom user ID |
| `gender` | VARCHAR(16) | NULL | - | Gender |
| `birthday` | DATE | NULL | - | Date of birth |
| `school` | VARCHAR(128) | NULL | - | School name |
| `tags_json` | JSON | NULL | - | User interests/tags |
| `created_at` | TIMESTAMP | NOT NULL | CURRENT_TIMESTAMP | Account creation time |
| `updated_at` | TIMESTAMP | NOT NULL | CURRENT_TIMESTAMP ON UPDATE | Last update time |

**Indexes**:
- `PRIMARY KEY (id)`
- `UNIQUE KEY uk_users_phone (phone)` - Ensures unique phone numbers
- `UNIQUE KEY uk_users_email (email)` - Ensures unique email addresses
- `UNIQUE KEY uk_users_zg_id (zg_id)` - Ensures unique custom IDs

**Notes**:
- At least one of `phone`, `email`, or `zg_id` must be provided for authentication
- `password_hash` uses bcrypt algorithm with cost factor 10

---

### 3.2 `login_logs`

**Description**: Audit trail for authentication attempts and sessions.

| Field | Type | Constraints | Default | Description |
|-------|------|-------------|---------|-------------|
| `id` | BIGINT UNSIGNED | PK, AUTO_INCREMENT | - | Primary key |
| `user_id` | BIGINT UNSIGNED | NULL | - | User ID (NULL for failed login attempts) |
| `identifier` | VARCHAR(128) | NOT NULL | - | Login identifier (email/phone) |
| `channel` | VARCHAR(32) | NOT NULL | - | Login channel (web/mobile/api) |
| `ip` | VARCHAR(45) | NULL | - | Client IP address (IPv4/IPv6) |
| `user_agent` | VARCHAR(512) | NULL | - | Client user agent string |
| `status` | VARCHAR(16) | NOT NULL | - | Login status (success/failed/blocked) |
| `created_at` | TIMESTAMP | NOT NULL | CURRENT_TIMESTAMP | Login attempt time |

**Indexes**:
- `PRIMARY KEY (id)`
- `KEY ix_login_logs_user_created_at (user_id, created_at)` - Query user login history

**Common Queries**:
```sql
-- Get recent login history for a user
SELECT * FROM login_logs 
WHERE user_id = ? 
ORDER BY created_at DESC 
LIMIT 20;

-- Detect suspicious login patterns
SELECT ip, COUNT(*) as attempts
FROM login_logs
WHERE status = 'failed' 
  AND created_at >= DATE_SUB(NOW(), INTERVAL 1 HOUR)
GROUP BY ip
HAVING attempts > 5;
```

---

### 3.3 `know_posts`

**Description**: User-generated posts/articles with rich content support.

| Field | Type | Constraints | Default | Description |
|-------|------|-------------|---------|-------------|
| `id` | BIGINT UNSIGNED | PK | - | Snowflake ID (generated in application layer) |
| `tag_id` | BIGINT UNSIGNED | NULL | - | Primary category/tag ID |
| `tags` | JSON | NULL | - | Tag array, e.g. `["java", "programming"]` |
| `title` | VARCHAR(256) | NULL | - | Post title |
| `description` | VARCHAR(500) | NULL | - | Short description/summary (max 500 chars) |
| `content_url` | TEXT | NULL | - | OSS signed URL for content access |
| `content_object_key` | VARCHAR(512) | NULL | - | OSS object key (e.g., `uploads/2026/02/123.md`) |
| `content_etag` | VARCHAR(128) | NULL | - | OSS ETag for content verification |
| `content_size` | BIGINT UNSIGNED | NULL | - | Content size in bytes |
| `content_sha256` | CHAR(64) | NULL | - | SHA-256 hash of content (hex) |
| `creator_id` | BIGINT UNSIGNED | NOT NULL | - | Post creator user ID |
| `is_top` | TINYINT(1) | NOT NULL | 0 | Whether post is pinned |
| `type` | VARCHAR(32) | NOT NULL | `'image_text'` | Post type (image_text/video/etc) |
| `visible` | VARCHAR(32) | NOT NULL | `'public'` | Visibility (public/private/friends) |
| `img_urls` | JSON | NULL | - | Image URL array |
| `video_url` | TEXT | NULL | - | Video URL (reserved for future use) |
| `status` | VARCHAR(16) | NOT NULL | `'draft'` | Post status (draft/published/deleted) |
| `create_time` | TIMESTAMP | NOT NULL | CURRENT_TIMESTAMP | Creation timestamp |
| `update_time` | TIMESTAMP | NOT NULL | CURRENT_TIMESTAMP ON UPDATE | Last update timestamp |
| `publish_time` | TIMESTAMP | NULL | NULL | Publication timestamp |

**Indexes**:
- `PRIMARY KEY (id)`
- `KEY ix_know_posts_creator_ct (creator_id, create_time)` - Query user's posts
- `KEY ix_know_posts_status_ct (status, create_time)` - Query posts by status
- `KEY ix_know_posts_tag_ct (tag_id, create_time)` - Query posts by category
- `KEY ix_know_posts_top_ct (is_top, create_time)` - Query pinned posts
- `KEY ix_know_posts_creator_status_pub (creator_id, status, publish_time)` - Complex queries

**Foreign Keys**:
- `CONSTRAINT fk_know_posts_creator FOREIGN KEY (creator_id) REFERENCES users(id)`

**Design Notes**:
- Content is stored in OSS (Object Storage Service), not in database
- `id` uses Snowflake algorithm for distributed ID generation
- Status flow: `draft` → `published` → `deleted`
- Soft delete: status changed to `deleted` instead of actual deletion

**Common Queries**:
```sql
-- Get user's published posts
SELECT id, title, description, publish_time
FROM know_posts
WHERE creator_id = ? AND status = 'published'
ORDER BY publish_time DESC
LIMIT 20;

-- Get public feed
SELECT p.id, p.title, u.nickname
FROM know_posts p
JOIN users u ON p.creator_id = u.id
WHERE p.status = 'published' AND p.visible = 'public'
ORDER BY p.publish_time DESC
LIMIT 20;
```

---

### 3.4 `outbox`

**Description**: Implements the Outbox Pattern for reliable event publishing in distributed systems.

| Field | Type | Constraints | Default | Description |
|-------|------|-------------|---------|-------------|
| `id` | BIGINT UNSIGNED | PK | - | Event ID (Snowflake) |
| `aggregate_type` | VARCHAR(64) | NOT NULL | - | Aggregate root type (User/Post/Follow) |
| `aggregate_id` | BIGINT UNSIGNED | NULL | - | Aggregate root ID |
| `type` | VARCHAR(64) | NOT NULL | - | Event type (e.g., `post.published`) |
| `payload` | JSON | NOT NULL | - | Event data payload |
| `created_at` | TIMESTAMP(3) | NOT NULL | CURRENT_TIMESTAMP(3) | Event creation time (millisecond precision) |

**Indexes**:
- `PRIMARY KEY (id)`
- `KEY ix_outbox_agg (aggregate_type, aggregate_id)` - Query events by aggregate
- `KEY ix_outbox_ct (created_at)` - Query events by time (for polling)

**Purpose**:
The `outbox` table ensures atomicity between database operations and event publishing:
1. Business operation and event are saved in the same transaction
2. Background job polls the table and publishes events to message queue
3. After successful publishing, records are deleted or marked as published

**Example Events**:
```json
// User created event
{
  "aggregate_type": "User",
  "aggregate_id": 123,
  "type": "user.created",
  "payload": {"userId": 123, "nickname": "John"}
}

// Post published event
{
  "aggregate_type": "Post",
  "aggregate_id": 456,
  "type": "post.published",
  "payload": {"postId": 456, "title": "Hello World", "creatorId": 123}
}
```

**Relationship in ERD**:
The `outbox` table has logical (not physical) relationships with multiple tables through `(aggregate_type, aggregate_id)`:
- `aggregate_type = "User"` → `users.id`
- `aggregate_type = "Post"` → `know_posts.id`
- `aggregate_type = "Follow"` → `following.id`

No foreign key constraint is created due to the polymorphic nature of the relationship.

---

### 3.5 `following` and `follower` Tables

**⚠️ IMPORTANT: Dual-Write Strategy for Follow Relationships**

These two tables implement a **denormalized, dual-write strategy** to optimize both write and read operations for social graph queries.

#### 3.5.1 `following` Table

**Description**: Stores "who I am following" relationships, optimized for write operations and querying a user's following list.

| Field | Type | Constraints | Default | Description |
|-------|------|-------------|---------|-------------|
| `id` | BIGINT UNSIGNED | PK | - | Relationship ID (Snowflake) |
| `from_user_id` | BIGINT UNSIGNED | NOT NULL | - | Follower (the user who follows) |
| `to_user_id` | BIGINT UNSIGNED | NOT NULL | - | Followee (the user being followed) |
| `rel_status` | TINYINT | NOT NULL | 1 | Relationship status (1=active, 0=unfollowed) |
| `created_at` | DATETIME(3) | NOT NULL | - | Relationship creation time |
| `updated_at` | DATETIME(3) | NOT NULL | - | Last update time |

**Indexes**:
- `PRIMARY KEY (id)`
- `UNIQUE KEY uk_from_to (from_user_id, to_user_id)` - Prevent duplicate follows
- `KEY idx_from_created (from_user_id, created_at, to_user_id, rel_status)` - Query following list
- `KEY idx_to (to_user_id, from_user_id, rel_status)` - Reverse lookup

**Optimized Query**:
```sql
-- Get all users that User A is following
SELECT to_user_id, created_at
FROM following
WHERE from_user_id = ? AND rel_status = 1
ORDER BY created_at DESC;
```

---

#### 3.5.2 `follower` Table

**Description**: Stores "who follows me" relationships, optimized for querying a user's follower list.

| Field | Type | Constraints | Default | Description |
|-------|------|-------------|---------|-------------|
| `id` | BIGINT UNSIGNED | PK | - | Relationship ID (Snowflake) |
| `to_user_id` | BIGINT UNSIGNED | NOT NULL | - | The user being followed |
| `from_user_id` | BIGINT UNSIGNED | NOT NULL | - | The follower |
| `rel_status` | TINYINT | NOT NULL | 1 | Relationship status (1=active, 0=unfollowed) |
| `created_at` | DATETIME(3) | NOT NULL | - | Relationship creation time |
| `updated_at` | DATETIME(3) | NOT NULL | - | Last update time |

**Indexes**:
- `PRIMARY KEY (id)`
- `UNIQUE KEY uk_to_from (to_user_id, from_user_id)` - Prevent duplicate entries
- `KEY idx_to_created (to_user_id, created_at, from_user_id, rel_status)` - Query follower list
- `KEY idx_from (from_user_id, to_user_id, rel_status)` - Reverse lookup

**Optimized Query**:
```sql
-- Get all followers of User B
SELECT from_user_id, created_at
FROM follower
WHERE to_user_id = ? AND rel_status = 1
ORDER BY created_at DESC;
```

---

#### Why Dual-Write Strategy?

**Problem with Single Table Approach**:
If we only had one table (e.g., `following`), querying "User B's followers" would require:
```sql
-- Slow: must scan by secondary column
SELECT from_user_id FROM following WHERE to_user_id = ?;
```
This forces a secondary index scan which is inefficient for high-traffic social networks.

**Benefits of Dual-Write**:

| Aspect | Single Table | Dual-Write (following + follower) |
|--------|--------------|-----------------------------------|
| **Write Performance** | ✅ Fast (1 insert) | ⚠️ Moderate (2 inserts) |
| **Read Performance** | ❌ Slow for follower list | ✅ Fast for both queries |
| **Storage** | ✅ Minimal | ❌ 2x storage |
| **Consistency** | ✅ Always consistent | ⚠️ Requires transactional writes |

**Implementation Pattern**:
```java
@Transactional
public void follow(Long fromUserId, Long toUserId) {
    long relationshipId = snowflakeIdGenerator.nextId();
    Instant now = Instant.now();
    
    // 1. Insert into following table
    Following following = Following.builder()
        .id(relationshipId)
        .fromUserId(fromUserId)
        .toUserId(toUserId)
        .relStatus((byte) 1)
        .createdAt(now)
        .updatedAt(now)
        .build();
    followingRepository.save(following);
    
    // 2. Insert into follower table (same transaction)
    Follower follower = Follower.builder()
        .id(relationshipId) // Same ID for consistency
        .toUserId(toUserId)
        .fromUserId(fromUserId)
        .relStatus((byte) 1)
        .createdAt(now)
        .updatedAt(now)
        .build();
    followerRepository.save(follower);
    
    // Both inserts are atomic due to @Transactional
}
```

**Key Design Decisions**:
1. **Same ID**: Both tables use the same `id` for the same relationship, enabling consistency checks
2. **Transactional Writes**: Both inserts must succeed or fail together
3. **Soft Delete**: `rel_status = 0` for unfollows instead of deletion, preserving history
4. **Covering Indexes**: Indexes include all fields needed for queries to avoid table lookups

---

## 4. Design Decisions

### 4.1 Why JSON for Tags?

**Scenario**: Tags are limited (typically 3-5 per post) and don't require complex queries.

**Advantages**:
- Avoids JOIN complexity with separate `tags` and `post_tags` tables
- Flexible schema evolution
- Faster writes (no junction table)

**Limitations**:
- Cannot efficiently query "all posts with tag X" using plain MySQL
- Consider ElasticSearch or MySQL 8.0 JSON indexes for tag-based search

### 4.2 Why Store Content in OSS Instead of Database?

**Reasons**:
- **Scalability**: Post content can be large (markdown + images)
- **Performance**: Database focused on metadata, CDN serves content
- **Cost**: Object storage is cheaper than database storage
- **Versioning**: OSS supports built-in version control

### 4.3 Soft Delete vs Hard Delete

**Current Approach**: Soft delete (`status = 'deleted'`)

**Reasons**:
- Users can recover accidentally deleted posts
- Audit trail and analytics
- Archival strategy: periodically move old deleted posts to cold storage

### 4.4 Snowflake ID Generation

**Why not AUTO_INCREMENT?**:
- **Distributed systems**: Multiple application instances can generate IDs independently
- **No single point of failure**: No need for centralized ID generator
- **Time-ordered**: IDs contain timestamp, enabling efficient range queries
- **Privacy**: Random-looking IDs don't expose business metrics

---

## 5. Common Query Patterns

### 5.1 User Feed (Homepage)
```sql
SELECT p.id, p.title, p.description, p.publish_time,
       u.nickname, u.avatar
FROM know_posts p
JOIN users u ON p.creator_id = u.id
WHERE p.status = 'published' 
  AND p.visible = 'public'
ORDER BY p.publish_time DESC
LIMIT 20 OFFSET 0;
```
**Index Used**: `ix_know_posts_status_ct`

### 5.2 User Profile Posts
```sql
SELECT id, title, description, publish_time
FROM know_posts
WHERE creator_id = ? 
  AND status = 'published'
ORDER BY publish_time DESC;
```
**Index Used**: `ix_know_posts_creator_status_pub`

### 5.3 Following List
```sql
SELECT f.to_user_id, u.nickname, u.avatar, f.created_at
FROM following f
JOIN users u ON f.to_user_id = u.id
WHERE f.from_user_id = ? AND f.rel_status = 1
ORDER BY f.created_at DESC;
```
**Index Used**: `idx_from_created`

### 5.4 Follower Count (Aggregation)
```sql
SELECT COUNT(*) as follower_count
FROM follower
WHERE to_user_id = ? AND rel_status = 1;
```
**Index Used**: `idx_to_created`

---

## 6. Performance Optimization

### 6.1 Index Strategy
- **High-frequency queries**: Covered indexes for `creator_id`, `status`, `publish_time`
- **Avoid**: Indexes on JSON fields (use ElasticSearch instead)
- **Partitioning**: Consider partitioning `know_posts` by `create_time` if data exceeds 10M rows

### 6.2 Caching Strategy
- **Hot data**: Post details cached for 30 seconds (Caffeine + Redis)
- **Feed stream**: Cached by hour segment (`feed:public:20:2026022710`)
- **Counter data**: Like/favorite counts cached separately

### 6.3 Archival Strategy
- **Condition**: `status='deleted' AND update_time < NOW() - INTERVAL 90 DAY`
- **Action**: Move to `know_posts_archive` table or cold storage

---

## 7. Migration History

| Version | Date | Changes | Impact |
|---------|------|---------|--------|
| v1.0.0 | 2026-01-15 | Initial schema | N/A |
| v1.1.0 | 2026-02-10 | Added `is_top` field to `know_posts` | Rebuild index `ix_know_posts_top_ct` |
| v1.2.0 | 2026-02-20 | Added `outbox` table for event sourcing | New infrastructure table |
| v1.3.0 | 2026-03-01 | Implemented dual-write for follow relationships | Added `follower` table |

**Migration Scripts**: `/src/main/resources/db/migration/`

---

## 8. Security Considerations

### 8.1 Password Storage
- Uses **bcrypt** with cost factor 10
- Never store plaintext passwords
- Rotate salt on password change

### 8.2 Sensitive Data
- `email` and `phone` should be encrypted at application layer for GDPR compliance
- Consider separate PII (Personally Identifiable Information) table with stricter access control

### 8.3 SQL Injection Prevention
- Always use **parameterized queries** (MyBatis `#{param}` syntax)
- Never concatenate user input into SQL strings

---

## 9. Backup and Recovery

### 9.1 Backup Strategy
- **Full backup**: Daily at 2 AM UTC
- **Incremental backup**: Every 6 hours
- **Binlog**: 7-day retention for point-in-time recovery

### 9.2 Recovery Procedures
- **Disaster recovery**: RTO (Recovery Time Objective) < 4 hours
- **Data validation**: Automated integrity checks post-recovery

---

## 10. Monitoring and Alerts

### 10.1 Key Metrics
- **Query response time**: P99 < 100ms
- **Slow query log**: Queries > 1 second
- **Table growth rate**: Monitor `know_posts` size
- **Index usage**: Identify unused indexes

### 10.2 Alert Thresholds
- Connection pool exhaustion: > 80% utilization
- Replication lag: > 5 seconds
- Disk usage: > 85%