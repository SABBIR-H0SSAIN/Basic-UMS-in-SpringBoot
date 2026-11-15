# University Management System

A CSE 3230 assignment for applying modern software engineering practices using Java 21, Spring Boot, and PostgreSQL. The web interface and REST API let admins manage all accounts, teachers manage teacher accounts, and students manage student accounts.

## Run with Docker

```sh
docker compose up --build
```

Open the login page locally at [localhost:8081/login](http://localhost:8081/login). On a fresh database, sign in as `admin`, `teacher`, or `student` with the password `admin123`.

## Run locally

With JDK 21 and Docker installed, start PostgreSQL and run the app:

```sh
docker compose up -d postgres
./mvnw spring-boot:run
```

Open [the local login page](http://localhost:8080/login).

## Run tests

Run the unit tests with JDK 21:

```sh
./mvnw -Dtest=AccountServiceTest test
```

With Docker running, run the full suite. Testcontainers starts and stops a dedicated PostgreSQL container automatically.

```sh
./mvnw verify
```

GitHub Actions runs the full suite on pushes and pull requests.

## API

CRUD endpoints are available under `/api/admins`, `/api/teachers`, and `/api/students`. To try them, import the [Postman collection](postman/UMS.postman_collection.json) and run it in order. Set `baseUrl` to `http://localhost:8081` when using the full Docker stack.
