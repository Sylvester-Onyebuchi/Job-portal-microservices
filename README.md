# Spring Microservice Project

A Spring Boot microservices backend for a job application platform. The system is split into independent services for authentication, users, companies, jobs, applications, notifications, service discovery, centralized configuration, and API gateway routing.

## Architecture

| Service | Purpose | Default port |
| --- | --- | --- |
| `api-gateway` | Gateway entry point, security filters, Redis-backed rate limiting | `8084` |
| `spring-config` | Spring Cloud Config Server backed by a remote Git config repository | `8080` |
| `discovery-server` | Eureka service registry | `8761` |
| `auth-service` | Keycloak-backed authentication and user management | `8085` |
| `company-service` | Company registration and company profile management | `8086` |
| `job-service` | Job posting, lookup, search, and company job management | `8087` |
| `application-service` | Candidate job applications and CV upload/download | `8088` |
| `notification-service` | Email notifications from RabbitMQ events | `8083` |

> Note: most services import configuration from `spring-config` at `http://localhost:8080`. The effective ports, datasource credentials, RabbitMQ settings, JWT settings, and route configuration may be defined in the remote config repository configured by `spring-config`.

## Tech Stack

- Java 21
- Spring Boot 4.x
- Spring Cloud 2025.x
- Spring Cloud Gateway
- Spring Cloud Config
- Netflix Eureka
- Spring Security and OAuth2 Resource Server
- Keycloak
- PostgreSQL
- RabbitMQ
- Redis
- Bucket4j
- Flyway
- Maven
- Docker and Docker Compose

## Project Structure

```text
.
├── api-gateway/
├── application-service/
├── auth-service/
├── company-service/
├── discovery-server/
├── job-service/
├── notification-service/
├── postgres/
├── spring-config/
└── docker-compose.yml
```

## Prerequisites

- JDK 21
- Docker Desktop or Docker Engine
- Maven, or use each service's included `mvnw`
- Access to the config repository used by `spring-config`:
  `https://github.com/Sylvester-Onyebuchi/spring-config-server`

## Environment Variables

Create a root `.env` file before starting Docker Compose. Use the committed example file as a template:

```bash
cp .env.example .env
```

`spring-config` has its own environment example for Config Server credentials and service URLs:

```bash
cp spring-config/.env.example spring-config/.env
```

The Config Server currently imports local properties from `spring-config/.env.local.properties`, so either export the variables from `spring-config/.env` before running the service or copy the same values there:

```bash
cp spring-config/.env.example spring-config/.env.local.properties
```

Do not commit real credentials. `.env`, `.env.*`, and `spring-config/.env*` are ignored, while `.env.example` files are intended to be committed.

## Start Infrastructure

The root `docker-compose.yml` starts the backing infrastructure: PostgreSQL databases, RabbitMQ, Redis, RedisInsight, Keycloak, and the Keycloak database.

```bash
docker compose up -d
```

Useful infrastructure URLs:

| Tool | URL |
| --- | --- |
| Keycloak | `http://localhost:8079` |
| RabbitMQ Management UI | `http://localhost:15671` |
| RedisInsight | `http://localhost:5540` |
| Eureka Dashboard | `http://localhost:8761` |
| Config Server | `http://localhost:8080` |

Infrastructure ports:

| Dependency | Host port |
| --- | --- |
| Auth PostgreSQL | `5440` |
| Company PostgreSQL | `5441` |
| Job PostgreSQL | `5442` |
| Application PostgreSQL | `5443` |
| Keycloak PostgreSQL | `5444` |
| RabbitMQ | `5671` |
| RabbitMQ Management | `15671` |
| Redis | `6380` |
| RedisInsight | `5540` |
| Keycloak | `8079` |

## Run the Services Locally

Start the services in this order so discovery and centralized configuration are available before the domain services boot.

```bash
cd discovery-server
./mvnw spring-boot:run
```

```bash
cd spring-config
set -a
source .env.local.properties
set +a
./mvnw spring-boot:run
```

Then start the remaining services in separate terminal sessions:

```bash
cd auth-service && ./mvnw spring-boot:run
cd company-service && ./mvnw spring-boot:run
cd job-service && ./mvnw spring-boot:run
cd application-service && ./mvnw spring-boot:run
cd notification-service && ./mvnw spring-boot:run
cd api-gateway && ./mvnw spring-boot:run
```

## Build and Test

Each service is an independent Maven project.

Build one service:

```bash
cd job-service
./mvnw clean package
```

Run tests for one service:

```bash
cd job-service
./mvnw test
```

Build all services from the project root:

```bash
for service in api-gateway application-service auth-service company-service discovery-server job-service notification-service spring-config; do
  if [ "$service" = "spring-config" ]; then
    (cd "$service" && ./mvnw clean package -Dmaven.test.skip=true)
  else
    (cd "$service" && ./mvnw clean package -DskipTests)
  fi
done
```

## CI Pipelines

GitHub Actions workflows live in the root `.github/workflows` directory. Each service has a basic CI pipeline that builds the jar, starts the application briefly, verifies that the process stays alive, and then stops it.

| Workflow | Service |
| --- | --- |
| `.github/workflows/api-gateway-ci.yml` | `api-gateway` |
| `.github/workflows/application-service-ci.yml` | `application-service` |
| `.github/workflows/auth.yml` | `auth-service` |
| `.github/workflows/company-service-ci.yml` | `company-service` |
| `.github/workflows/discovery-server-ci.yml` | `discovery-server` |
| `.github/workflows/job-service-ci.yml` | `job-service` |
| `.github/workflows/notification-service-ci.yml` | `notification-service` |
| `.github/workflows/spring-config-ci.yml` | `spring-config` |

The pipelines do not build or push Docker images. They are intentionally limited to basic build-and-run validation.

## API Overview

Public and protected routes are defined per service. In normal usage, call these through the API Gateway when routes are configured in Spring Config.

### Authentication

`auth-service` Keycloak-backed endpoints:

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/api/v1/auth/public/create` | Create a user |
| `POST` | `/api/v1/auth/public/login` | Log in |
| `POST` | `/api/v1/auth/public/forgot-password` | Request password reset |
| `POST` | `/api/v1/auth/public/resend` | Resend verification email |
| `POST` | `/api/v1/auth/refresh` | Refresh access token |
| `POST` | `/api/v1/auth/logout` | Log out |
| `GET` | `/api/v1/users/user` | Get authenticated user |
| `PUT` | `/api/v1/users/user/update` | Update authenticated user |
| `DELETE` | `/api/v1/users/user/delete` | Delete authenticated user |

### Companies

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/api/v1/companies/create` | Create a company |
| `GET` | `/api/v1/companies/company/owner` | Get the authenticated owner's company |
| `PUT` | `/api/v1/companies/company/update` | Update a company |
| `DELETE` | `/api/v1/companies/company/delete/{id}` | Delete a company |

### Jobs

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/api/v1/jobs/post-job` | Post a job |
| `GET` | `/api/v1/jobs/company` | List jobs for the authenticated company |
| `GET` | `/api/v1/jobs/job/{id}` | Get a job with authorization context |
| `GET` | `/api/v1/jobs/{id}` | Get a public job by ID |
| `GET` | `/api/v1/jobs` | List jobs with pagination |
| `GET` | `/api/v1/jobs/search?title={title}` | Search jobs by title |
| `DELETE` | `/api/v1/jobs/job/{id}/delete` | Delete a job |

### Applications

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/api/v1/applications/apply` | Submit an application with CV multipart upload |
| `GET` | `/api/v1/applications/candidate` | List applications for the authenticated candidate |
| `GET` | `/api/v1/applications/cv` | Download the authenticated candidate's CV |
| `DELETE` | `/api/v1/applications/{id}/delete` | Delete an application |

## Keycloak

The Compose setup runs Keycloak in development mode:

```yaml
command: start-dev
KC_HTTP_ENABLED: "true"
KC_HOSTNAME_STRICT: "false"
```

Create or import the realm, client, roles, and users expected by the services. The commented local configuration in `auth-service/src/main/resources/application.yml` references a `spring-auth` realm and an `auth-cli` client.

## Database Migrations

Flyway migrations live under each service's resources directory:

```text
company-service/src/main/resources/db/migration/
job-service/src/main/resources/db/migration/
application-service/src/main/resources/db/migration/
```

Migrations run automatically when the relevant service starts, provided the datasource and Flyway settings are available from Spring Config.

## Messaging

RabbitMQ is used for asynchronous events:

- User registration and password reset notification events
- Company creation events
- Application submission and company alert events

`notification-service` consumes these events and sends email notifications.

## Troubleshooting

- If services fail to start with missing properties, verify that `spring-config` is running and can clone the remote config repository.
- If services cannot register, verify that `discovery-server` is running at `http://localhost:8761`.
- If protected endpoints return `401`, verify Keycloak realm/client configuration and the JWT issuer/public key settings in Spring Config.
- If rate limiting fails, verify Redis is running on `localhost:6380` and the password matches `REDIS_PASSWORD`.
- If email notifications fail, verify RabbitMQ credentials and mail settings in Spring Config.
