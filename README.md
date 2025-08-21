# Nahero Backend

Backend API service for the Nahero platform, available at [nahero.site](http://nahero.site).

## Tech Stack

- **Java 21**
- **Spring Boot 3.x**
  - Spring Security with JWT authentication
  - Spring Data JPA
  - Spring JDBC
- **PostgreSQL** database
- **Flyway** for database migrations
- **Kafka** for messaging
- **Lombok** for reducing boilerplate code
- **Docker** and Docker Compose for containerization

## Getting Started

### Prerequisites

- Java 21
- Docker and Docker Compose
- Maven (or use the provided Maven wrapper)

### Running with Docker Compose

The easiest way to run the application locally is using Docker Compose:

```bash
docker compose up -d
```

This will start the application and a PostgreSQL database.

### Running Locally

```bash
# Build the application
./mvnw clean install

# Run the application
./mvnw spring-boot:run
```

## Testing

The project uses JUnit 5 with Testcontainers for integration testing.

```bash
# Run all tests
./mvnw test

# Run a specific test class
./mvnw test -Dtest=TestClassName

# Run a specific test method
./mvnw test -Dtest=TestClassName#methodName
```

## Project Structure

```
src/
├── main/
│   ├── java/br/com/naheroback/
│   │   ├── common/       # Shared components
│   │   ├── modules/      # Feature modules
│   │   └── providers/    # External service integrations
│   └── resources/
│       └── db/migration/ # Flyway database migrations
└── test/
    └── java/            # Test classes
```

## Module Organization

The application follows a modular architecture with features organized into modules:
- Authentication and Authorization
- User Management
- Practice Exams
- Enrollments
- Payments
- Exams

Each module contains its own controllers, entities, repositories, and use cases.
