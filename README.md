# Resource Booking API System

## Overview
The **Resource Booking API System** is a secure RESTful backend application built with **Spring Boot 3** and **Java 17**. It manages bookable items (resources like conference rooms, vehicles, and equipment) and their associated reservations with strict Role-Based Access Control (RBAC), stateless JWT authentication, dynamic query filtering, pagination, and sorting.

---

## Technologies
- **Java**: 17
- **Framework**: Spring Boot 3.3.2
- **Persistence**: Spring Data JPA & Hibernate
- **Database**: MySQL
- **Security**: Spring Security & JJWT (`0.12.6`)
- **Validation**: Jakarta Bean Validation (`spring-boot-starter-validation`)
- **API Specs & Documentation**: Swagger UI / OpenAPI 3 (`springdoc-openapi-starter-webmvc-ui:2.5.0`)
- **Build Tool**: Apache Maven

---

## Key Features
- **Stateless Authentication**: Login issuing signed JWTs with identity and role claims.
- **Password Security**: Passwords stored using `BCryptPasswordEncoder`.
- **Role-Based Access Control (RBAC)**:
  - **`ADMIN`**: Full CRUD access to resources and all reservations across all users.
  - **`USER`**: Can view resources, create reservations, and view/manage ONLY their own reservations.
- **Reservation Ownership Security**: User identity is derived directly from `SecurityContext` / JWT. Impersonation via request body or URL manipulation is strictly blocked.
- **Dynamic Filtering**: Reservations can be filtered by `status`, `minPrice`, and `maxPrice` independently or in any combination.
- **Pagination & Sorting**: Paginated results with `page`, `size`, and flexible field sorting (`sort=price,asc`).
- **Global Exception Handling**: Centralized error responses returning clean JSON (`timestamp`, `status`, `error`, `message`, `path`).
- **Interactive Swagger UI**: Full OpenAPI 3 integration supporting JWT Bearer authentication.

---

## Project Structure
```
com.omkar.resourcebooking
├── config
│   ├── DataInitializer.java        # Auto-seeds ADMIN, USER, and initial resources
│   └── OpenApiConfig.java          # Swagger UI & OpenAPI 3 Bearer JWT configuration
├── controller
│   ├── AuthController.java         # POST /auth/login
│   ├── ResourceController.java     # Resource CRUD endpoints
│   └── ReservationController.java  # Reservation CRUD endpoints with filtering & pagination
├── dto
│   ├── ErrorResponse.java          # Standard error output model
│   ├── LoginRequest.java / LoginResponse.java
│   ├── ReservationRequest.java / ReservationResponse.java
│   └── ResourceRequest.java / ResourceResponse.java
├── entity
│   ├── Reservation.java            # Booking entity
│   ├── ReservationStatus.java      # Enum (PENDING, CONFIRMED, CANCELLED)
│   ├── Resource.java               # Bookable item entity
│   ├── Role.java                   # Enum (USER, ADMIN)
│   └── User.java                   # System user entity
├── exception
│   ├── BadRequestException.java
│   ├── GlobalExceptionHandler.java # RestControllerAdvice handling errors
│   ├── ReservationNotFoundException.java
│   └── ResourceNotFoundException.java
├── repository
│   ├── ReservationRepository.java
│   ├── ReservationSpecification.java# Dynamic JPA specification builder
│   ├── ResourceRepository.java
│   └── UserRepository.java
├── security
│   ├── CustomUserDetails.java
│   ├── CustomUserDetailsService.java
│   ├── JwtAuthenticationFilter.java# Intercepts Bearer tokens
│   ├── JwtService.java             # Token generation & validation
│   └── SecurityConfig.java         # SecurityFilterChain & authorization rules
└── service
    ├── AuthService.java            # Login business logic
    ├── ReservationService.java     # Reservation management logic & ownership enforcement
    └── ResourceService.java        # Resource management logic
```

---

## Database Setup

1. Make sure MySQL server is running.
2. Create the database:
```sql
CREATE DATABASE IF NOT EXISTS resource_booking;
```

---

## Environment Variables

| Variable | Default Value | Description |
| :--- | :--- | :--- |
| `PORT` | `8081` | Server HTTP port |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3306/resource_booking?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true` | JDBC Connection URL |
| `SPRING_DATASOURCE_USERNAME` | `root` | MySQL Database User |
| `SPRING_DATASOURCE_PASSWORD` | `root` | MySQL Database Password |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `update` | Hibernate schema creation policy |
| `JWT_SECRET` | `MyVerySecretKeyForResourceBookingApplicationJwtAuthentication2026` | Secret key for signing JWTs |
| `JWT_EXPIRATION` | `86400000` | Token expiration in milliseconds (24 hours) |

---

## Running the Application

### 1. Build the project
```bash
mvn clean install
```

### 2. Run unit and integration tests
```bash
mvn test
```

### 3. Start the application
```bash
mvn spring-boot:run
```

The application will start on **`http://localhost:8081`**.

---

## Seed Credentials (Development & Testing)

Upon startup, initial test users are automatically created in the database:

| Role | Username | Password | Access Level |
| :--- | :--- | :--- | :--- |
| **ADMIN** | `admin` | `admin123` | Full CRUD on Resources & all Reservations |
| **USER** | `user1` | `user123` | Read Resources, Create & Manage Own Reservations |

---

## Authentication Flow

1. Send `POST /auth/login` with username and password.
2. Obtain `token` string from response JSON.
3. Include the token in the `Authorization` HTTP header for protected endpoints:
```http
Authorization: Bearer <your_jwt_token_here>
```

---

## API Endpoints Summary

### Authentication
- **`POST /auth/login`**: Authenticate and obtain JWT. *(Public)*

### Resources
- **`GET /resources`**: List all resources. *(USER, ADMIN)*
- **`GET /resources/{id}`**: Get resource by ID. *(USER, ADMIN)*
- **`POST /resources`**: Create a new resource. *(ADMIN only)*
- **`PUT /resources/{id}`**: Update an existing resource. *(ADMIN only)*
- **`DELETE /resources/{id}`**: Delete a resource. *(ADMIN only)*

### Reservations
- **`POST /reservations`**: Create a reservation. *(USER, ADMIN)*
- **`GET /reservations`**: List reservations (Supports status, minPrice, maxPrice, page, size, sort). *(USER sees own only, ADMIN sees all)*
- **`GET /reservations/{id}`**: Get reservation by ID. *(USER sees own only, ADMIN sees any)*
- **`PUT /reservations/{id}`**: Update reservation. *(ADMIN only)*
- **`DELETE /reservations/{id}`**: Cancel/Delete reservation. *(ADMIN only)*

---

## Filtering, Pagination, & Sorting Examples

### 1. Filtering
```http
GET /reservations?status=PENDING&minPrice=50&maxPrice=300
```

### 2. Pagination
```http
GET /reservations?page=0&size=5
```

### 3. Sorting
> [!NOTE]
> Allowed sorting fields are strictly validated: `id`, `price`, `startTime`, `endTime`, `status`. Any invalid sort field returns HTTP 400 Bad Request.

```http
GET /reservations?page=0&size=10&sort=price,asc
```

---

## Interactive Swagger Documentation

Swagger UI is available at:
👉 **`http://localhost:8081/swagger-ui.html`** or **`http://localhost:8081/swagger-ui/index.html`**

To test authenticated endpoints in Swagger UI:
1. Execute `POST /auth/login` in Swagger UI.
2. Copy the returned JWT token.
3. Click the **Authorize** button at the top right of Swagger UI.
4. Enter `Bearer <your_token>` and click **Authorize**.

---

## Example JSON Requests

### Login Request (`POST /auth/login`)
```json
{
  "username": "user1",
  "password": "user123"
}
```

### Create Resource (`POST /resources` - ADMIN)
```json
{
  "name": "Executive Boardroom",
  "description": "Premium 30-person conference room with AV equipment",
  "type": "Room",
  "available": true,
  "price": 200.00
}
```

### Create Reservation (`POST /reservations` - USER)
```json
{
  "resourceId": 1,
  "startTime": "2026-09-01T10:00:00",
  "endTime": "2026-09-01T12:00:00",
  "price": 200.00
}
```

---

## Security Architecture & Design Safeguards
- **BCrypt Password Encoding**: User passwords are encrypted using `BCryptPasswordEncoder` before storage.
- **Stateless Sessions**: Session creation policy is set to `STATELESS`.
- **Identity Isolation**: The application extracts identity strictly from the validated JWT token in the `SecurityContext`. Request payloads cannot override user ownership.
- **Granular Ownership Checks**: Attempting to read, update, or delete another user's reservation returns `403 Forbidden` or `404 Not Found`.
