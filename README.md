# Spring Boot Backend Performance Optimization

## Overview

A Spring Boot REST API backed by AWS RDS (MySQL) with Redis caching, demonstrating query optimization techniques on a 500k+ row smartphone dataset. Built to simulate realistic cloud database workloads including filtering, pagination, projections, indexing, and cloud-to-local sync.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 4.0.6 |
| Database | AWS RDS MySQL 8.4.8 |
| Cache | Redis 8.0.5 (via WSL Ubuntu) |
| ORM | Hibernate 7.2.12 / Spring Data JPA |
| Runtime | Java 17 |
| Build | Maven |

---

## Project Structure

```
com/wcraske/n44/
├── config/
│   └── RedisConfig.java          ← Redis template + serialization setup
├── controller/
│   └── PhoneController.java      ← REST endpoints, timing
├── entity/
│   └── Phone.java                ← JPA entity, @Index annotations, Serializable
├── repository/
│   ├── PhoneRepository.java      ← JPA queries, JPQL, sync query
│   └── PhoneProjection.java      ← DTO for reduced field projection
├── service/
│   └── PhoneService.java         ← Business logic, Redis cache logic
└── N44Application.java
```

---

## AWS Setup

### RDS Instance
- Engine: MySQL 8.4.8
- Instance class: `db.t4g.micro` (free tier)
- Region: `us-east-2` (Ohio)
- Endpoint: `database-1.cpggk8os8ndt.us-east-2.rds.amazonaws.com`
- Port: `3306`

### Security Group
- Inbound rule: MySQL/Aurora TCP 3306 open to local IP
- Outbound: all traffic allowed

### Connect to RDS
```bash
mysql -h database-1.cpggk8os8ndt.us-east-2.rds.amazonaws.com -P 3306 -u admin -p
```

### Create Database
```sql
CREATE DATABASE n44db;
```

---

## Data

- Dataset: `smartphone_dataset_1M.csv` — 500k+ rows, 33 columns
- Loaded via `LOAD DATA LOCAL INFILE` directly from local machine into RDS

```sql
LOAD DATA LOCAL INFILE 'smartphone_dataset_1M.csv'
INTO TABLE phones
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 ROWS;
-- Query OK, 496024 rows affected (1 min 33.55 sec)
```

### Schema — `phones` table

| Column | Type | Notes |
|---|---|---|
| id | BIGINT | Primary key, auto increment |
| brand | VARCHAR | Indexed |
| model_name | VARCHAR | |
| os | VARCHAR | Indexed |
| price_inr | DOUBLE | Indexed |
| launch_year | INT | Indexed |
| fivegsupport | BIT | |
| dual_sim | BIT | |
| ram_gb | INT | |
| storage_gb | INT | |
| battery_mah | INT | |
| updated_at | DATETIME | Indexed — used for sync |
| ... | ... | 33 columns total |

---

## Application Configuration

`src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://database-1.cpggk8os8ndt.us-east-2.rds.amazonaws.com:3306/n44db?useSSL=true&serverTimezone=UTC
spring.datasource.username=admin
spring.datasource.password=your_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.open-in-view=false

spring.cache.type=redis
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.cache.redis.time-to-live=600000
```

---

## Redis Setup (WSL Ubuntu)

```bash
# Launch Ubuntu WSL
wsl -d Ubuntu

# Install Redis
sudo apt update
sudo apt install redis-server -y

# Start Redis in background
redis-server --daemonize yes

# Verify running
redis-cli ping   # → PONG
```

---

## Optimization Techniques

### 1. Pagination
Prevents loading the full 500k dataset into memory. Without pagination, `/full-list` hangs indefinitely. With `Pageable`, only the requested page is fetched.

```java
public List<Phone> getPagedPhones(Pageable pageable) {
    return phoneRepository.findAll(pageable).getContent();
}
```

### 2. Projections
Instead of fetching all 33 columns, a DTO returns only brand, model, storage, and OS — reducing data transfer significantly.

```java
@Query("SELECT new com.wcraske.n44.repository.PhoneProjection(p.brand, p.modelName, p.storageGb, p.os) FROM Phone p")
List<PhoneProjection> findPhoneProjections(Pageable pageable);
```

### 3. Redis Caching
Manual Redis caching via `RedisTemplate` — cache miss hits the DB and stores the result for 10 minutes. Cache hit skips the DB entirely.

```java
List<Phone> cached = (List<Phone>) redisTemplate.opsForValue().get(key);
if (cached != null) return cached;

List<Phone> result = phoneRepository.findAll(pageable).getContent();
redisTemplate.opsForValue().set(key, result, 10, TimeUnit.MINUTES);
```

**Benchmark results:**
| Request | Time |
|---|---|
| First hit (DB) | ~34ms |
| Second hit (Redis) | ~2-5ms |

### 4. Database Indexing
Indexes defined in Java via `@Index` on the `@Table` annotation — Hibernate creates them automatically on startup. No manual SQL required.

```java
@Table(name = "phones", indexes = {
    @Index(name = "idx_brand", columnList = "brand"),
    @Index(name = "idx_os", columnList = "os"),
    @Index(name = "idx_price", columnList = "price_inr"),
    @Index(name = "idx_launch_year", columnList = "launch_year"),
    @Index(name = "idx_brand_os", columnList = "brand, os"),
    @Index(name = "idx_updated_at", columnList = "updated_at")
})
```

**EXPLAIN before indexes:**
```
type: ALL | rows: 606463 | key: NULL   ← full table scan
```

**EXPLAIN after indexes:**
```
type: ref | rows: 124572 | key: idx_brand   ← index used
```

Note: MySQL's query optimizer ignores the price index for `price_inr < 50000` because ~50% of rows match — a full scan is faster at that selectivity. Narrowing to `price_inr < 10000` correctly uses the index.

### 5. Dynamic Filtering with JPQL
Optional query parameters build a dynamic WHERE clause — only filters on params that are provided:

```java
@Query("SELECT p FROM Phone p WHERE " +
       "(:brand IS NULL OR p.brand = :brand) AND " +
       "(:os IS NULL OR p.os = :os) AND " +
       "(:maxPrice IS NULL OR p.priceInr <= :maxPrice)")
List<Phone> searchPhones(@Param("brand") String brand,
                         @Param("os") String os,
                         @Param("maxPrice") Double maxPrice,
                         Pageable pageable);
```

### 6. Cloud-to-Local Sync Endpoint
Simulates delta sync — a local device or external system  calls this endpoint with a timestamp and receives only records updated since that time. Avoids re-syncing the full dataset.

```java
@Query("SELECT p FROM Phone p WHERE p.updatedAt > :lastSyncedAt")
List<Phone> findUpdatedSince(@Param("lastSyncedAt") LocalDateTime lastSyncedAt, Pageable pageable);
```

Seed data for testing:
```sql
UPDATE phones SET updated_at = '2024-01-01 00:00:00';
UPDATE phones SET updated_at = NOW() WHERE id % 1000 = 0;
```

---

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| GET | `/health` | DB connection check |
| GET | `/phones/full-list` | All phones (no limit — use carefully) |
| GET | `/phones/pagination` | Paginated list with Redis cache + timing |
| GET | `/phones/projection` | Reduced fields (brand, model, storage, os) + timing |
| GET | `/phones/search` | Dynamic filter by brand, os, maxPrice + timing |
| GET | `/phones/sync` | Delta sync — records updated since timestamp |
| GET | `/phones/clear-cache` | Flush all Redis cache keys |

### Endpoint Examples

```
# Health check
GET http://localhost:8080/health
→ "DB connected!"

# Paginated list (page 0, 20 results)
GET http://localhost:8080/phones/pagination?pageNo=0&pageSize=20
→ "Fetched 20 records in 39ms"   ← first hit
→ "Fetched 20 records in 3ms"     ← second hit (Redis)

# Search — filter by brand
GET http://localhost:8080/phones/search?brand=Samsung
GET http://localhost:8080/phones/search?brand=Apple&os=iOS
GET http://localhost:8080/phones/search?maxPrice=10000
GET http://localhost:8080/phones/search?brand=Google&maxPrice=50000&pageNo=0&pageSize=10

# Sync — delta since date
GET http://localhost:8080/phones/sync?lastSyncedAt=2024-06-01T00:00:00
→ "Sync: 496 records updated since 2024-06-01T00:00 fetched in 45ms"

# Clear cache
GET http://localhost:8080/phones/clear-cache
→ "Cache cleared!"
```

---

## Separation of Concerns

| Class | Role |
|---|---|
| `Phone.java` | Entity — data structure, maps to DB table, defines indexes |
| `PhoneRepository.java` | Repository — all DB queries, JPQL, Spring Data JPA |
| `PhoneProjection.java` | DTO — reduced field set for projection queries |
| `PhoneService.java` | Service — business logic, Redis cache management |
| `PhoneController.java` | Controller — HTTP routing, request params, response timing |
| `RedisConfig.java` | Config — Redis template, serialization setup |

---

## Key Learnings

- **Full table scans on 500k rows** cause multi-second response times — pagination and indexing are non-negotiable at scale
- **MySQL's query optimizer** is smart enough to skip an index when it would be slower than a full scan (e.g. range queries matching >30% of rows)
- **Redis cache** reduces repeat query time from ~34ms to ~2-5ms — a 12x improvement
- **Projections** reduce network payload by fetching only needed columns instead of all 33
- **Delta sync** via `updatedAt` timestamp allows external systems to pull only changed records, avoiding full dataset transfers
- **Network latency** (BC → AWS Ohio) accounts for ~100ms of every request — in production, co-locating app, Redis, and RDS in the same AWS region eliminates this
