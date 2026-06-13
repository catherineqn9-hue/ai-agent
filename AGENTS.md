# Sherry Project Agent Rules

This project follows a strict engineering workflow inspired by Superpowers and Alibaba-style coding conventions.

## Workflow

1. Read the existing code and related docs before changing code.
2. Clarify input, output, persistence, interface, and acceptance criteria.
3. Keep changes small and scoped to the requested module.
4. Add or update tests for backend behavior and business rules.
5. Run verification before reporting completion.
6. AI must call backend services or declared tools. Do not bypass service APIs to mutate business data.

## Current Architecture

- Frontend: Vue 3 + Vite management console in `frontend/`.
- Backend: Spring Boot 3 + MyBatis-Plus + LangChain4j in `backend/`.
- Persistence: PostgreSQL.
- Legacy Python: archived in `legacy-python/` and no longer the main development path.

## Backend Layering

```text
backend/src/main/java/com/sherry/supervision/
  controller/       HTTP boundary
  dto/              request/response contracts
  entity/           database entities
  mapper/           MyBatis-Plus data access
  service/          business interfaces
  service/impl/     business implementations
  ai/               LangChain4j tools
  excel/            Excel import strategies
  configresource/   dynamic CRUD resource registry
```

Rules:

- Controllers must not contain SQL.
- Mappers must not contain business decisions.
- Services own validation, transaction-oriented business flow, and persistence orchestration.
- AI tools call services, not raw database writes.
- Shared constants must be named, not embedded as magic strings.

## Frontend Layering

```text
frontend/src/
  api/          backend API clients
  components/   Vue pages and business components
  styles/       global layout and visual styles
```

Rules:

- Menus are loaded from backend/database configuration.
- CRUD pages should stay configuration-driven when possible.
- Keep the frontend lightweight; do not add a large UI library without a clear reason.
- Use dialogs for create/edit flows and list tables for management pages.

## Naming

- Java classes: `PascalCase`.
- Java methods and fields: `lowerCamelCase`.
- Database tables and columns: `snake_case`.
- API JSON fields: `snake_case`.
- API resources: lowercase plural kebab-case, for example `/api/v1/basic-configs/agent-configs`.
- Vue component names: `PascalCase.vue`.

## Tests

- Backend business behavior should have JUnit tests.
- Every CRUD resource should have create/list/get/update/delete coverage as it matures.
- AI tool and chat behavior must verify that business services are called.
- Bug fixes should include a regression test or a documented verification step.

## Verification

Use:

```powershell
rtk docker compose up -d postgres
rtk mvn -q test
rtk npm run build
```

Run Maven commands from `backend/` and npm commands from `frontend/`.
