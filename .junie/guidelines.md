# Development Guidelines for Utenlandsadresser

This document provides guidelines and instructions for developing and maintaining the Utenlandsadresser project.

## Build/Configuration Instructions

### Prerequisites

- JDK 21 (as specified in the build.gradle.kts file)
- Gradle (wrapper included in the project)

### Project Structure

The project consists of three main modules:

- `app`: The main application module
- `hent-utenlandsadresser`: Module for retrieving foreign addresses
- `sporingslogg-cleanup`: Module for cleaning up tracking logs

### Building the Project

To build the entire project:

```bash
./gradlew build
```

To build a specific module:

```bash
./gradlew :app:build
./gradlew :hent-utenlandsadresser:build
./gradlew :sporingslogg-cleanup:build
```

### Running the Application

To run the main application:

```bash
./gradlew :app:run
```

For development mode:

```bash
./gradlew :app:run -Pdevelopment=true
```

## Testing Information

### Test Framework

The project uses Kotest as the primary testing framework, along with several supporting libraries:

- Kotest: Main testing framework with various spec styles (WordSpec, etc.)
- TestContainers: For integration tests with PostgreSQL and Kafka
- WireMock: For mocking HTTP services
- Mockk: For mocking Kotlin classes

### Running Tests

To run all tests:

```bash
./gradlew test
```

To run tests for a specific module:

```bash
./gradlew :app:test
./gradlew :hent-utenlandsadresser:test
./gradlew :sporingslogg-cleanup:test
```

### Example Test

Here's an example of a simple test using Kotest's WordSpec style:

The project contains a test file `KtorEnvTest.kt` that tests the `AppEnv.getFromEnvVariable` method. This method
determines the application environment based on environment variables.

The test structure follows Kotest's WordSpec style:

1. Define a test class that extends WordSpec
2. Group related tests using descriptive strings
3. Write individual test cases with clear descriptions
4. Use assertions like `shouldBe` to verify expected behavior

For example, the test verifies:

- When no environment variable exists, it returns LOCAL
- When the environment variable is empty, it returns LOCAL
- When the environment variable has specific values, it returns the corresponding environment

To run this specific test:

```bash
./gradlew :app:test --tests "no.nav.utenlandsadresser.KtorEnvTest"
```

### Adding New Tests

1. Create a new test file in the appropriate module's test directory
2. Use one of the Kotest spec styles (WordSpec, FunSpec, etc.)
3. Follow the existing test patterns for consistency
4. Run the test to verify it works correctly

## Additional Development Information

### Dependency Management

The project uses Gradle's version catalog for dependency management. Key dependencies include:

- Ktor: Web framework for building the API
- Exposed: SQL framework for database access
- Arrow: Functional programming library
- Kafka: For message streaming
- PostgreSQL: Database for storing data

### Environment Configuration

The application uses environment variables for configuration, with the `AppEnv` enum determining the current
environment:

- LOCAL: Local development environment (default)
- DEV_GCP: Development environment in Google Cloud Platform
- PROD_GCP: Production environment in Google Cloud Platform

### Database

The application uses PostgreSQL for data storage, with Flyway for database migrations. For testing
TestContainers are used.

### API Documentation

The API is documented using OpenAPI/Swagger UI, accessible when running the application.

### Logging

The application uses SLF4J with Logback for logging.

### Continuous Integration

GitHub Actions are used for CI/CD, including automatic dependency updates via Dependabot.
