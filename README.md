## Booking System - REST API Application

### Overview
A comprehensive Booking System built as a Spring Boot monolithic REST API application that allows users to manage accommodation unit bookings with automated payment processing and real-time availability tracking.

### Key Capabilities

Unit Management: Create, update, and search accommodation units (apartments, flats, homes)
Smart Booking: Reserve units with automatic 15-minute payment window
Payment Processing: Emulated payment system with deadline enforcement
Real-time Availability: Cached statistics for instant availability checks
Automatic Cleanup: Scheduled job to expire unpaid bookings


### Unit Management

Create units with custom properties (rooms, type, floor, cost)
15% system markup automatically applied to base cost
Search units by multiple criteria with pagination and sorting
Filter by date range, cost, accommodation type, and number of rooms
Real-time availability status tracking

### Booking System

Three-stage booking process:
```
Create Booking → Units marked as RESERVED
15-Minute Payment Window → Automatic deadline creation
Process Payment → Units marked as BOOKED
```

Cancel unpaid bookings (owner only)
Update bookings before payment (swap units)
Automatic expiration of unpaid bookings after 15 minutes

### Payment Processing
```
Emulated payment system with validation
Payment deadline enforcement
Automatic booking cancellation on expiration
Payment status tracking (PENDING / COMPLETED)
```
### Status Transitions
```
AVAILABLE → RESERVED → BOOKED     (Successful payment)
AVAILABLE → RESERVED → AVAILABLE  (Cancelled or expired)
```

### Timeline Example
```
00:00 - User creates booking → Units become RESERVED
00:00 - Payment deadline set to 00:15
00:10 - User processes payment → Units become BOOKED ✅
OR
00:16 - Scheduler runs → Booking expired → Units become AVAILABLE again ❌
```
### Status Transitions
```
AVAILABLE → RESERVED → BOOKED     (Successful payment)
AVAILABLE → RESERVED → AVAILABLE  (Cancelled or expired)
```
### Access Application
```
API Base URL: http://localhost:8080/api
Swagger UI: http://localhost:8080/swagger-ui/index.html
Health Check: http://localhost:8080/actuator/health
```

### Initial Data
On startup, the application automatically creates:
```
10 units via Liquibase changelog (predefined)
90 units with random parameters (application startup)
Total: 100 units ready for testing
```
## Technology Stack
### Core Framework

- Java 21 - LTS version
- Lombok - Boilerplate reduction
- Spring Boot 3.5.6 - Application framework
- Spring Web - REST API
- Spring Data JPA - Data persistence
- Hibernate - ORM implementation

### Database & Migration

- PostgreSQL - Primary database (Docker)
- Liquibase - Database schema versioning (SQL format)

### Caching

- Redis - Distributed cache

### Mapping

- MapStruct - Entity-DTO mapping

### API Documentation

- Swagger/OpenAPI 3.0 - Interactive API documentation

### Testing

- JUnit 5 - Unit testing framework
- Spring Test - Integration testing
- Mockito - Mocking framework
- Testcontainers - Container-based integration tests

#### Build & DevOps

- Gradle - Build automation
- Docker Compose - Containerization

#### Start Docker Services (PostgreSQL & Redis)
```bash
    docker-compose up
```

#### Stop Docker Services (PostgreSQL & Redis)
```bash
    docker-compose down
```

#### Build Application
```bash
    ./gradlew clean build
```
