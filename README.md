# FinTrack Financial Analytics

FinTrack is a full-stack financial analytics assessment app built with React, TypeScript, Spring Boot, and PostgreSQL. It provides account registration/login, an analytics dashboard, searchable transactions, category budgets, and user settings. Each account's generated sample transactions and budgets are stored in PostgreSQL and remain unchanged across logins and application restarts.

> The financial activity is synthetic. Authentication uses database-backed opaque bearer sessions and BCrypt password hashes, but this assessment app is not a banking product and should not be used to store real financial data without further security review.

## Requirements

- Docker Engine/Desktop with Docker Compose
- For local development: Java 17+, Maven 3.9+, Node.js 20+ and npm

## Run with Docker Compose

Create a local environment file and set separate strong passwords for the PostgreSQL administrator and the application database role:

```sh
cp .env.example .env
```

In PowerShell, use `Copy-Item .env.example .env`.

Edit `.env` and replace both `POSTGRES_PASSWORD` and `APP_DB_PASSWORD`. Start PostgreSQL and the application:

```sh
docker compose up --build -d
```

Open [http://localhost:8080](http://localhost:8080). Compose waits for PostgreSQL's health check before starting the API. Flyway creates and validates the schema automatically. PostgreSQL data is stored in the named `postgres-data` volume and survives container replacement.

To see application logs:

```sh
docker compose logs -f api
```

To stop the services without deleting data:

```sh
docker compose down
```

To reset the database and remove all registered users/sample transactions, stop the services and remove the volume:

```sh
docker compose down -v
```

### Optional demo account

Demo account seeding is disabled by default. For local assessment use only, set these values in `.env`:

```dotenv
APP_DEMO_USER_ENABLED=true
DEMO_USER_EMAIL=demo@fintrack.local
DEMO_USER_PASSWORD=choose-a-local-demo-password
```

Then run `docker compose up --build -d`. The demo user and its sample data are created only when that email is absent; existing data is never reseeded or overwritten. Do not enable a known demo account in a public deployment.

## Local development

Start PostgreSQL using Compose:

```sh
docker compose up -d db
```

Set the application-role database connection values for Spring Boot:

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/fintrack
SPRING_DATASOURCE_USERNAME=fintrack_app
SPRING_DATASOURCE_PASSWORD=<the APP_DB_PASSWORD from .env>
```

On PowerShell, set them in the backend terminal before starting the API:

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/fintrack"
$env:SPRING_DATASOURCE_USERNAME = "fintrack_app"
$env:SPRING_DATASOURCE_PASSWORD = "<the APP_DB_PASSWORD from .env>"
Set-Location backend
mvn spring-boot:run
```

In a second terminal start the frontend:

```sh
cd frontend
npm ci
npm run dev
```

Open [http://localhost:5173](http://localhost:5173). Vite proxies `/api` requests to `http://localhost:8080`; set `VITE_API_PROXY_TARGET` to override that backend address. For a separately hosted frontend, set `VITE_API_URL` to the API base URL ending in `/api/v1` at frontend build time.

The API fails to start if PostgreSQL is unavailable. Schema changes are versioned in `backend/src/main/resources/db/migration`; Hibernate validates the Flyway-managed schema and does not generate tables.

## Build and test

Run backend unit tests:

```sh
cd backend
mvn test
```

Build and type-check the frontend:

```sh
cd frontend
npm ci
npm run build
```

Build the complete production container:

```sh
docker compose build
```

The automated backend unit tests cover generated-data invariants and repeatability. Start the Compose stack and register a user to exercise the PostgreSQL migrations and API integration; the API smoke-tested registration, transaction/budget operations, account isolation, and persistence across restarts. The production API exposes liveness/readiness probes at `/actuator/health/liveness` and `/actuator/health/readiness`.

## REST API

API prefix: `/api/v1`. Login and registration return a bearer token; send it as `Authorization: Bearer <token>` to protected endpoints. Session tokens are random opaque values; only their SHA-256 hashes are stored in the database and each session expires after the configured lifetime.

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/auth/register` | Create a user, persistent account, realistic sample transactions and budgets |
| `POST` | `/auth/login` | Authenticate and issue a persisted expiring session |
| `POST` | `/auth/logout` | Revoke the current session |
| `GET` | `/account` | Return the signed-in user's account and current balance |
| `GET` | `/analytics/dashboard` | Dashboard summary, six-month cash flow, category spend, recent activity, and insights |
| `GET` | `/analytics/cash-flow` | Six-month cash-flow series |
| `GET` | `/analytics/spending` | Current-month spend grouped by budget category |
| `GET` | `/transactions` | Filtered, sorted, paginated transactions |
| `POST` | `/transactions` | Record a positive amount as a `CREDIT` or `DEBIT` |
| `GET` | `/transactions/{id}` | Retrieve an owned transaction |
| `GET` | `/budgets` | Budgets with current-month actual spending |
| `PUT` | `/budgets/{category}` | Update a budget amount, e.g. `{"amount": 6000.00}` |

Transaction list filters: `category`, `type` (`CREDIT` or `DEBIT`), `from`, and `to` (ISO date). Pagination uses `page` (zero-based) and `size` (maximum 100); supported sort fields are `transactionDate`, `amount`, `merchant`, and `category`.

Create a transaction with `{"date":"2026-10-09","merchant":"Grocery Market","category":"Groceries","amount":450.00,"type":"DEBIT"}`. Amounts in create requests are positive; the API stores debit amounts as negative values. Transactions are immutable after creation to preserve a clear financial record.

The dashboard keeps the response shape used by the frontend. Financial amounts are stored as PostgreSQL `NUMERIC(14,2)` and represented as decimal JSON numbers. Credits are positive; debits are negative. Monthly category spending and cash flow are calculated from persisted transactions, not from hard-coded dashboard totals.

## Persistence and sample data

Flyway creates these normalized tables:

- `app_users`: unique email and BCrypt password hash
- `accounts`: one currency/opening balance per user
- `transactions`: signed transaction amounts, category, merchant and transaction date
- `budgets`: per-account category limits
- `auth_sessions`: hashed bearer token and expiry

Registration creates six months of deterministic-for-that-user synthetic salary, supplemental income, and category expenses, plus starting category budgets. The summary, current balance, cash flow, category spend, and insights are derived from those stored records. Registering once and logging in repeatedly returns the same records; sample data is not regenerated at login or restart.

## Operations and security

- Spring Boot emits request method/path, status, duration, and a generated `X-Request-Id`; passwords and bearer tokens are never logged. Backend exceptions are logged with request IDs.
- `/actuator/health` is the only exposed Actuator surface beyond the liveness/readiness probes.
- Compose requires separate database-admin and app-role passwords, waits for PostgreSQL readiness, retains its data in a named volume, applies container hardening to the API, and rotates local Docker logs. The API connects as a non-superuser role that owns only the application schema.
- Keep `.env` out of source control. Use managed secrets, TLS, database backups, appropriate resource limits, and a production identity provider before public deployment.
- PostgreSQL data written by the previous JSON-file version is not imported automatically. Back up and migrate any data you need before switching to this schema.

## Project structure

```text
.
├── Dockerfile
├── compose.yaml
├── backend/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/example/financialanalytics/
│       └── resources/db/migration/
└── frontend/
    ├── package.json
    └── src/
```
