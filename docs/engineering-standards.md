# Engineering Standards

This project uses the Superpowers workflow mindset and Alibaba-style engineering discipline:

- Understand first, then change.
- Keep layers clear.
- Let tests and verification define completion.
- Let AI call interfaces; do not let AI bypass service boundaries.

## Stack Boundary

| Layer | Technology | Responsibility |
|---|---|---|
| Frontend | Vue 3 + Vite | Management console, chat page, CRUD pages |
| API | Spring MVC Controller | HTTP boundary, request validation, response formatting |
| AI Tools | LangChain4j | Tool definitions and model-call boundary |
| Service | Spring services | Business validation, transactions, orchestration |
| Mapper | MyBatis-Plus | Database access and persistence mapping |
| Database | PostgreSQL | Durable business and configuration data |

## Backend Rules

### Controller Layer

- Only parse requests, call services, and return responses.
- No SQL in controller methods.
- No long business branches in controller methods.
- Convert domain errors to HTTP errors at the boundary.

### Service Layer

- Own business rules and transaction decisions.
- Validate state transitions before writing data.
- Be callable by both REST API and LangChain4j tools.
- Keep external side effects explicit, for example IM sending or user-service lookup.

### Mapper Layer

- Own persistence mapping.
- Do not decide business meaning.
- Query methods must not write.
- Write methods must return the latest saved record or affected count through the service layer.

### AI Tool Layer

- Tools call service interfaces instead of directly changing tables.
- Tool names and descriptions must be understandable by the model.
- Every tool that mutates business data should be auditable when logging tables are added.

## Frontend Rules

- Vue owns page state and interactions.
- Keep API resource names aligned with backend routes.
- Do not hardcode business table behavior in many places; use menu metadata and field configs where possible.
- CRUD pages should expose clear create/edit/delete states.
- Avoid showing debug traces on user-facing pages unless a dedicated diagnostics page is added.
- Keep the bundle lean; avoid large UI libraries unless their value is clear.
- API calls must go through `src/api/client.js`, which unwraps the standard response envelope.
- User-facing success and error messages should use the shared Toast host instead of scattered `alert`.

## Database Rules

- Tables and columns use `snake_case`.
- Every business table has a UUID primary key.
- Core mutable tables include `created_at` and `updated_at`.
- Status fields use controlled dictionary values.
- Soft delete can be added later when audit requirements are clear; do not mix soft and hard delete casually.

## API Rules

- Prefix public backend APIs with `/api/v1`.
- Resource paths use lowercase plural kebab-case.
- JSON fields use `snake_case`.
- JSON APIs return the standard envelope:
  ```json
  {
    "success": true,
    "code": 0,
    "message": "ok",
    "data": {},
    "trace_id": "request-trace-id"
  }
  ```
- Error responses keep the same envelope and set `success=false`; `data` must be `null`.
- HTTP status codes express transport/category result, while `code` expresses business result.
- Current business code ranges:
  - `0`: success
  - `40000`: invalid parameter
  - `40100`: unauthenticated
  - `40300`: forbidden
  - `40400`: resource not found
  - `40900`: business conflict
  - `50000`: system error
- Backend exceptions should use explicit business exception types:
  - `InvalidRequestException` for malformed JSON, bad Excel files, unknown handler codes, and invalid parameters.
  - `ResourceNotFoundException` for missing items, templates, or config resources.
  - `BusinessConflictException` or `DataIntegrityViolationException` handling for duplicate keys and state conflicts.
  - Unknown exceptions are handled as `50000` and must not leak stack traces.
- CRUD pattern:
  - `GET /resources`
  - `POST /resources`
  - `GET /resources/{id}`
  - `PUT /resources/{id}`
  - `DELETE /resources/{id}`
- Return JSON objects, not bare arrays.
- Conflict uses HTTP `409`.
- Missing resources use HTTP `404`.
- Validation errors use HTTP `422`.

## Coding Rules

- Name things by business meaning, not implementation tricks.
- Avoid magic strings. Promote repeated values into constants or dictionaries.
- Avoid global mutable state for business data.
- Keep functions short enough that their responsibility is obvious.
- Prefer explicit field mapping over implicit reflection when business meaning matters.
- Catch specific exceptions when possible.
- Comments explain why, not what obvious code already says.

## Test Rules

- CRUD resources need full create/list/get/update/delete tests as they mature.
- Database-backed behavior should be tested against PostgreSQL when Docker is available.
- AI workflow tests should verify service calls and tool-call payloads.
- Frontend changes need at least build verification and browser verification for primary pages.

## Completion Checklist

Before saying a task is done:

- Relevant code paths have been read.
- Directory placement follows the project structure.
- API boundaries are clear.
- Tests or verification were run.
- Database schema changes were applied or documented.
- The user-facing page was checked if frontend changed.
