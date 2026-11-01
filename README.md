# University Management System — CSE 3230

A simple REST API built on the existing Spring Boot 4.1.2 Maven project. It uses Java 21, Spring Web MVC, Spring Security, Spring Data JPA, PostgreSQL and Lombok. Authentication uses a server session and a `JSESSIONID` cookie. There is no JWT. A basic HTML frontend uses vanilla JavaScript and Fetch, with no frontend libraries.

## CSE 3230 fourth-lab assignment checklist

| Assignment requirement | Implementation |
|---|---|
| Spring Boot web application | Existing Java 21 Spring Boot project with five basic HTML pages |
| Admin, Teacher and Student resources | Three entity classes and three REST controllers |
| Complete CRUD for every resource | GET collection, GET by ID, POST, PUT and DELETE under `/api/admins`, `/api/teachers` and `/api/students` |
| Proper REST conventions | Resource URLs, JSON DTOs, 201 with Location for creation, 200 for reads/updates and 204 for deletion |
| Appropriate error statuses | 400 invalid input, 401 unauthenticated, 403 forbidden, 404 missing record, 409 duplicate username |
| Spring Security login and logout | JSON login creates a server session; logout invalidates it |
| Password hashing | BCrypt hashes stored in PostgreSQL; passwords and hashes omitted from responses |
| Admin manages all resources | ADMIN can perform all CRUD operations on Admin, Teacher and Student APIs |
| Teacher manages only Teachers | TEACHER can perform all Teacher CRUD; Admin and Student APIs return 403 |
| Student manages only Students | STUDENT can perform all Student CRUD; Admin and Teacher APIs return 403 |
| Dockerfile and Compose | Java 21 multi-stage application image and PostgreSQL service, with database health check and persistent volume |

The automated HTTP integration test checks every allowed and forbidden CRUD operation for all three roles against real PostgreSQL. The role rules apply to resource types, as the assignment specifies: Teachers can manage all Teacher records, and Students can manage all Student records.

## Run with Docker

Install/start Docker Desktop, then run from this directory:

```sh
docker compose up --build
```

The API runs at `http://localhost:8080`. Ports 8080 and 5432 must be free before starting the full Compose stack; stop your local Spring Boot and PostgreSQL processes if they already use those ports. Compose waits for PostgreSQL to become healthy before starting the application. The Dockerfile builds with Java 21 and runs the packaged application as a non-root user. PostgreSQL data persists in the `postgres_data` volume.

Sample accounts are added individually when missing:

- Admin: `admin` / `admin123`
- Student: `student` / `admin123`
- Teacher: `teacher` / `admin123`

Override the admin credentials using `ADMIN_USERNAME` and `ADMIN_PASSWORD`. `DB_PASSWORD` overrides the lab database password. Existing accounts are never reset at startup, so an admin created by an earlier version still uses its original password. Use these sample credentials for local laboratory work only.

```sh
docker compose logs -f app
docker compose down
```

`docker compose down` preserves data. `docker compose down -v` deletes the database volume and all records.

## Run Java locally

Set `JAVA_HOME` to a JDK 21 installation. Start just PostgreSQL, then run Maven:

```sh
docker compose up -d postgres
./mvnw spring-boot:run
```

Alternatively, use an existing PostgreSQL server and create a database named `ums`. Defaults are `jdbc:postgresql://localhost:5432/ums`, user `ums`, password `ums_password`. Override `DB_URL`, `DB_USERNAME` and `DB_PASSWORD` as needed. Hibernate creates/updates the schema automatically for this lab.

If your existing local PostgreSQL server reports `role "ums" does not exist`, connect as your PostgreSQL administrator and run these once:

```sql
CREATE ROLE ums WITH LOGIN PASSWORD 'ums_password';
CREATE DATABASE ums OWNER ums;
CREATE DATABASE ums_test OWNER ums;
```

In IntelliJ, select Java 21 as the project SDK and run configuration JRE, then run `UmsAssignmentApplication`. When using an existing PostgreSQL server on port 5432, you do not need to start the Compose database.

```sh
./mvnw package -DskipTests
java -jar target/ums_assignment-0.0.1-SNAPSHOT.jar
```

## Basic web frontend

After starting (or restarting) Spring Boot, open **[http://localhost:8080/login.html](http://localhost:8080/login.html)**. Serve the pages through Spring Boot rather than opening the HTML files directly from disk.

There are exactly five pages:

- `login.html`: enter an existing account's username and password.
- `dashboard.html`: welcome message, role, navigation and logout.
- `admins.html`, `teachers.html`, `students.html`: list records, add, edit and delete with a confirmation dialog.

The Admin sees all three management links. A Teacher sees Teachers only; a Student sees Students only. Each role also sees Dashboard and Logout. Opening an unauthorized management page shows a message and hides its form. Spring Security continues to enforce permissions on every API request. The static HTML contains no private records; records load only through authorized API calls.

To edit a record, click **Edit**, change the fields, enter a password, and click **Save changes**. The existing PUT endpoint requires every field, including a password, so the frontend keeps that behavior. Enter the current password to keep it. **Cancel edit** returns the form to Add mode. Passwords are never displayed in tables or filled from server responses.

All frontend files are in `src/main/resources/static/`:

- `common.js`: sends JSON requests, includes session cookies, fetches CSRF tokens, handles HTTP errors, and builds navigation from the server's current session.
- `login.js`: sends the login form to the existing login endpoint.
- `dashboard.js`: displays the welcome message.
- `management.js`: shares the same straightforward table and form logic across the three resource pages. The HTML body's `data-resource` chooses the existing API URL.
- `style.css`: basic spacing and table borders.

The only new API endpoint is authenticated `GET /api/auth/me`, which returns the session's username and role. No passwords, session tokens or roles are stored in browser storage. A 401 on a signed-in page returns the browser to Login. Validation and business rules stay in the backend; errors appear as plain text above the form.

Manual frontend check: log in as Admin and create Teacher and Student accounts. Log out and sign in with each account to check navigation, CRUD and direct visits to other management pages. Java 21 integration tests cover page/assets delivery, the current-session endpoint and the full backend permission matrix. Browser checks also verified login/logout, role navigation, CRUD on all three resource pages, forbidden-page messages and redirection after logout.

## Student academic fields

Students also have optional `year` (study year 1–4), `semester` (1 for 1st or 2 for 2nd), and `cgpa` (0.00–4.00, at most two decimal places). These fields appear only on the Student form and table. Existing records may leave them empty. Teachers have an optional department (up to 100 characters) shown on the Teacher form and table. Admin and Teacher responses do not include student academic fields.

Include the fields in a Student POST or PUT body, for example:

```json
{
  "username": "student1",
  "password": "Student123!",
  "name": "Sabbir Ahmed",
  "email": "student1@university.edu",
  "year": 2,
  "semester": 1,
  "cgpa": 3.75
}
```

A full PUT replaces these fields too; omitted or null values clear them. Hibernate adds the nullable columns to the existing accounts table on startup without deleting records.

## Endpoints and permissions

| Resource | Collection URL | Admin | Teacher | Student |
|---|---|---|---|---|
| Admin | `/api/admins` | All CRUD | Forbidden | Forbidden |
| Teacher | `/api/teachers` | All CRUD | All CRUD | Forbidden |
| Student | `/api/students` | All CRUD | Forbidden | All CRUD |

Permissions apply to the entire resource type: a Teacher may manage **all Teacher records**, and a Student may manage **all Student records**. Roles are determined by the endpoint used to create an account and cannot be changed in the request body. Usernames are unique across all roles and case-sensitive.

Every collection supports:

| Method | URL | Success |
|---|---|---|
| GET | `/api/students` | 200, JSON array |
| GET | `/api/students/{id}` | 200, JSON object |
| POST | `/api/students` | 201, JSON object and Location header |
| PUT | `/api/students/{id}` | 200, updated JSON object |
| DELETE | `/api/students/{id}` | 204, empty body |

Replace `students` with `teachers` or `admins` for the other resources. A resource ID belonging to another role returns 404.

POST and PUT require the same complete JSON body. PUT replaces all editable fields, including the password:

```json
{
  "username": "student1",
  "password": "Student123!",
  "name": "Sabbir Ahmed",
  "email": "student1@university.edu"
}
```

Username: 3–50 letters, digits, underscores, dots or hyphens. Password: at least 8 characters and at most 72 UTF-8 bytes (BCrypt's limit). Name: 1–100 characters. Email: valid basic email format, at most 254 characters.

Example response:

```json
{
  "id": 2,
  "username": "student1",
  "name": "Sabbir Ahmed",
  "email": "student1@university.edu",
  "role": "STUDENT"
}
```

Responses never contain a password or password hash. Errors use JSON, for example `{"status":404,"message":"Resource not found"}`.

| Code | Meaning |
|---|---|
| 200 | Read, update, login or logout succeeded |
| 201 | Record created |
| 204 | Record deleted; intentionally no response body |
| 400 | Invalid fields, malformed JSON or invalid ID format |
| 401 | Login required or incorrect credentials |
| 403 | Role forbidden, or missing/invalid CSRF token |
| 404 | Resource does not exist in the requested resource type |
| 409 | Duplicate username or conflicting stored data |

## Login, CRUD and logout with curl

CSRF protection stays enabled because login uses cookies. Fetch a token before login, and fetch a fresh token after successful login. Send the same cookie jar on every request. The commands below use Python 3 to extract the JSON token.

```sh
BASE=http://localhost:8080
COOKIE_JAR=/tmp/ums-cookies.txt

# 1. Obtain the pre-login session cookie and CSRF token.
TOKEN=$(curl -s -c "$COOKIE_JAR" -b "$COOKIE_JAR" "$BASE/api/auth/csrf" \
  | python3 -c 'import sys,json; print(json.load(sys.stdin)["token"])')

# 2. Log in. The JSON response contains username and role.
curl -i -c "$COOKIE_JAR" -b "$COOKIE_JAR" \
  -H 'Content-Type: application/json' -H "X-CSRF-TOKEN: $TOKEN" \
  -d '{"username":"admin","password":"admin123"}' "$BASE/api/auth/login"

# 3. Login rotates the session ID and CSRF token.
TOKEN=$(curl -s -c "$COOKIE_JAR" -b "$COOKIE_JAR" "$BASE/api/auth/csrf" \
  | python3 -c 'import sys,json; print(json.load(sys.stdin)["token"])')

# 4. Create a Student. Note the returned ID for the next commands.
curl -i -c "$COOKIE_JAR" -b "$COOKIE_JAR" \
  -H 'Content-Type: application/json' -H "X-CSRF-TOKEN: $TOKEN" \
  -d '{"username":"student1","password":"Student123!","name":"Sabbir Ahmed","email":"student1@university.edu"}' \
  "$BASE/api/students"

# 5. List records, then fetch one (replace 2 with the returned ID).
curl -i -b "$COOKIE_JAR" "$BASE/api/students"
curl -i -b "$COOKIE_JAR" "$BASE/api/students/2"

# 6. Replace all editable fields.
curl -i -b "$COOKIE_JAR" -X PUT \
  -H 'Content-Type: application/json' -H "X-CSRF-TOKEN: $TOKEN" \
  -d '{"username":"student1","password":"Student456!","name":"Updated Name","email":"student1@university.edu"}' \
  "$BASE/api/students/2"

# 7. Delete the record.
curl -i -b "$COOKIE_JAR" -X DELETE -H "X-CSRF-TOKEN: $TOKEN" "$BASE/api/students/2"

# 8. Log out; the server invalidates the session.
curl -i -c "$COOKIE_JAR" -b "$COOKIE_JAR" -X POST \
  -H "X-CSRF-TOKEN: $TOKEN" "$BASE/api/auth/logout"
```

## Postman testing

Import `postman/UMS.postman_collection.json`. Keep Postman's cookie jar enabled and clear old localhost cookies before the first run. Set collection variables `baseUrl`, `adminUsername` and `adminPassword` if needed. Run the **whole collection in order** using Collection Runner. Its pre-request script automatically obtains a CSRF token before each POST, PUT and DELETE.

The collection logs in as Admin, creates accounts for all three roles, exercises allowed CRUD and forbidden cross-role requests, checks validation and duplicate usernames, verifies logout and removes its test accounts. It asserts expected response codes. The default admin remains. To run manually, follow the folders in the same order; requests depend on IDs saved by earlier requests.

To test a missing CSRF token manually, send POST `/api/auth/login` with a JSON body from a new request without the collection's pre-request script: expect 403. For incorrect credentials, send a valid CSRF token and incorrect password: expect 401.

## Automated integration test

Tests use a **separate real PostgreSQL database**, never an in-memory replacement. Create it before running tests (with Compose PostgreSQL running):

```sh
docker compose exec postgres createdb -U ums ums_test
./mvnw test
```

If it already exists, skip `createdb`. Use a fresh test database or one containing the seeded `admin` / `Admin123!` account. Do not point tests at a database containing important data.

Override test connection settings if needed:

```sh
TEST_DB_URL=jdbc:postgresql://localhost:5432/ums_test \
TEST_DB_USERNAME=ums TEST_DB_PASSWORD=ums_password ./mvnw verify
```

The integration test starts the application on a random HTTP port and checks real login, BCrypt storage, every allowed/forbidden CRUD operation for all three roles, logout, password omission, Location headers, validation, 404 and 409 responses. It removes records it creates after successful completion.

## Class guide for the lab evaluation

Follow one request through the code in this order: **Controller → Service → Repository → PostgreSQL**. The controller handles HTTP, the service checks input and performs the work, and the repository reads or writes the database. DTOs describe the JSON entering and leaving the API. The three resource controllers share one service because their CRUD steps are the same.

- `entity/Account`: common persisted fields. `Admin`, `Teacher` and `Student` extend it. JPA single-table inheritance stores them in one `accounts` table with a `role` discriminator; usernames have one database-wide unique constraint.
- `repository/AccountRepository`: Spring Data supplies SQL CRUD operations; lookup by username supports login and duplicate checks.
- `service/AccountService`: validates input, selects the resource type, hashes passwords, handles CRUD and maps entities to password-free DTOs. Transactions keep updates together.
- `service/AccountUserDetailsService`: loads an account from PostgreSQL and supplies its role and BCrypt hash to Spring Security.
- `controller/AdminController`, `TeacherController`, `StudentController`: map HTTP verbs and URLs to service methods and select the correct role.
- `controller/AuthController`: provides CSRF tokens, authenticates JSON login, saves the security context in the session, and invalidates sessions on logout. Explicit context saving follows the [Spring Security session documentation](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html).
- `config/SecurityConfig`: BCrypt encoder, authentication manager, session context storage, role URL rules and JSON 401/403 responses. CSRF is Spring Security's default session-based protection.
- `config/DefaultAdminInitializer`: creates the default Admin only when the accounts table is empty.
- `dto`: request and response records. Response DTOs never include passwords; entity password serialization is also disabled.
- `exception`: translates expected application, validation and database errors to HTTP status codes and safe JSON messages.

The original Thymeleaf dependencies remain in the existing POM but this API does not use templates. Spring Boot's automatic Compose integration is disabled because Compose is started explicitly.

## Verification in this workspace

`./mvnw clean verify` passed with Java 21 (Temurin 21.0.13) against local PostgreSQL, including all three roles. The packaged application also passed the Postman collection: 113 HTTP requests and 135 assertions, with zero failures. Compose configuration validation and the Docker image build passed. Spring Boot and PostgreSQL 17 started together successfully under Docker Compose, with the database healthy. The same Postman collection passed against the containerized application: 113 requests and 135 assertions, zero failures. Docker verification used an isolated Compose project, its own database volume, and temporary port overrides (18081 for the app, 55433 for PostgreSQL) to avoid conflicts with local services. The temporary stack was removed afterward.
