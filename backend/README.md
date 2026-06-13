# Sherry Supervision Java Backend

Spring Boot 3 + MyBatis-Plus + LangChain4j backend for the Sherry supervision system.

## Build

```bash
mvn clean package -DskipTests
```

The deployable jar is:

```text
target/sherry-supervision.jar
```

## Run Locally

```bash
java -jar target/sherry-supervision.jar --server.port=8010 --spring.flyway.enabled=false
```

The backend serves the latest built Vue assets from `src/main/resources/static`.
During frontend development, run the Vite server from `../frontend` and use its proxy.

## Run On Server

```bash
export SERVER_PORT=8010
export DATABASE_URL=jdbc:postgresql://127.0.0.1:5432/sherry
export DATABASE_USERNAME=sherry
export DATABASE_PASSWORD=sherry_dev_password
export KIMI_API_KEY=your_key
export KIMI_BASE_URL=https://api.moonshot.cn/v1
export KIMI_MODEL=moonshot-v1-8k

java -jar sherry-supervision.jar --spring.profiles.active=prod
```

## Current Migrated APIs

- `GET /health`
- `GET /`
- `GET /admin`
- `GET /api/v1/admin-menus`
- `GET /api/v1/basic-configs/resources`
- `GET /api/v1/basic-configs/{resourceName}`
- `POST /api/v1/basic-configs/{resourceName}`
- `GET /api/v1/basic-configs/{resourceName}/{id}`
- `PUT /api/v1/basic-configs/{resourceName}/{id}`
- `DELETE /api/v1/basic-configs/{resourceName}/{id}`
- `GET /api/v1/supervision-items`
- `POST /api/v1/supervision-items`
- `PUT /api/v1/supervision-items/{id}`
- `PATCH /api/v1/supervision-items/{id}/status`
- `DELETE /api/v1/supervision-items/{id}`
- `GET /api/v1/progress-feedbacks`
- `POST /api/v1/progress-feedbacks`
- `GET /api/v1/supervision-imports/templates`
- `POST /api/v1/supervision-imports/excel`
- `POST /api/v1/chat/run`
- `POST /api/v1/chat/resume`
- `GET /api/v1/chat/state/{threadId}`
- `GET /api/v1/chat/history/{threadId}`

## Packaging Notes

The Spring Boot Maven plugin builds a layered jar. It can be deployed directly with `java -jar`,
or later packaged into a Docker image with dependency layers cached separately from application code.
