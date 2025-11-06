# Damu - NeTEx to GTFS Converter

## Project Overview

Damu is a Spring Boot application that converts NeTEx (Network Timetable Exchange) datasets into GTFS (General Transit Feed Specification) datasets. It serves as a critical integration component in the Entur public transport data pipeline, handling conversion, validation, and aggregation of transit data.

## Architecture

### Technology Stack
- **Java 21** - Core language
- **Spring Boot** - Application framework
- **Apache Camel 4.8.9** - Integration framework and routing
- **Google Cloud Platform**:
  - Cloud Storage (GCS) - File storage
  - Pub/Sub - Messaging
- **Maven** - Build tool

### Key Dependencies
- `netex-gtfs-converter-java` (v2.1.105) - Core NeTEx to GTFS conversion
- `gtfs-validator-main` (v6.0.0) - GTFS validation
- `entur-helpers` (v5.47.0) - Google Cloud integration utilities
- `zt-zip` (v1.17) - ZIP file handling

### Integration Flow
1. **Trigger**: Chouette or Uttu completes a NeTEx export
2. **Notification**: Marduk notifies Damu via Google Pub/Sub
3. **Download**: Damu downloads NeTEx dataset from GCS
4. **Convert**: NeTEx is converted to GTFS
5. **Validate**: GTFS is validated using MobilityData validator
6. **Upload**: GTFS dataset is uploaded to GCS
7. **Notify**: Marduk is notified of completion

## Project Structure

```
damu2/
├── src/main/java/no/entur/damu/
│   ├── App.java                      # Main Spring Boot application
│   ├── Constants.java                 # Application constants
│   ├── config/                        # Configuration classes
│   │   ├── GcsBlobStoreRepositoryConfig.java
│   │   ├── LocalDiskBlobStoreRepositoryConfig.java
│   │   └── InMemoryBlobStoreRepositoryConfig.java
│   ├── routes/                        # Apache Camel routes
│   │   ├── aggregation/               # GTFS aggregation logic
│   │   ├── export/                    # GTFS export handling
│   │   ├── validation/                # GTFS validation
│   │   ├── stop/                      # Stop area processing
│   │   ├── file/                      # File operations
│   │   └── blobstore/                 # Cloud storage operations
│   ├── netex/                         # NeTEx processing
│   ├── gtfs/                          # GTFS handling and utilities
│   ├── pubsub/                        # Pub/Sub integration
│   └── stop/                          # Stop area handling
├── src/main/resources/
│   └── logback.xml                    # Logging configuration
├── src/test/                          # Test suite
├── helm/                              # Kubernetes Helm charts
├── terraform/                         # Infrastructure as Code
├── Dockerfile                         # Container image definition
└── pom.xml                            # Maven build configuration
```

## Key Components

### Routes (Apache Camel)
Damu uses Apache Camel for integration patterns and routing:

- **GtfsExportQueueRouteBuilder** - Handles GTFS export queue processing
- **GtfsAggregationQueueRouteBuilder** - Manages GTFS aggregation across datasets
- **GtfsValidationQueueRouteBuilder** - Validates GTFS feeds
- **StopAreaRepositoryRouteBuilder** - Manages stop area data
- **BlobStore Routes** - Handle cloud storage operations

### Configuration
Multiple blob store configurations support different environments:
- **GCS** (Production) - Google Cloud Storage
- **Local Disk** (Development) - File system storage
- **In-Memory** (Testing) - Memory-based storage

### Processing Modes
- **Basic Aggregation** - Simple GTFS feed merging
- **Extended Aggregation** - Advanced GTFS feed merging with transformations
- **Validation** - GTFS feed validation using industry standards

## Development Setup

### Prerequisites
- Java 21 JDK
- Maven 3.6+
- Docker (optional, for containerized deployment)
- Google Cloud SDK (for GCS/Pub/Sub integration)

### Build Commands

```bash
# Clean and compile
mvn clean compile

# Run tests
mvn test

# Package application
mvn package

# Run with Spring Boot
mvn spring-boot:run

# Format code (Prettier)
mvn prettier:write

# Check code formatting
mvn prettier:check -PprettierCheck

# Skip formatting
mvn clean install -PprettierSkip
```

### Testing
- **Unit Tests**: Standard JUnit 5 tests
- **Integration Tests**: Using Testcontainers for GCloud emulation
- **Camel Tests**: Apache Camel test framework for route testing

Test configuration:
- Memory: `-Xms500m -Xmx7000m -Xss512k`
- Testcontainers used for GCloud Pub/Sub and Storage emulation

## Configuration

### Application Properties
Configuration is managed through Spring Boot properties and environment variables. Key areas:
- Google Cloud project ID and credentials
- Pub/Sub topics and subscriptions
- GCS bucket names
- Camel route configurations
- Logging levels

### Profiles
- **Default** - Development mode with local storage
- **gcs** - Production mode with Google Cloud Storage
- **prettierCheck** - CI mode with code formatting validation
- **prettierSkip** - Skip code formatting

## Deployment

### Docker
```bash
# Build image
docker build -t damu:latest .

# Run container
docker run -p 8080:8080 damu:latest
```

### Kubernetes (Helm)
Helm charts are provided in the `helm/` directory for Kubernetes deployment.

### Terraform
Infrastructure as Code is available in the `terraform/` directory for provisioning cloud resources.

## Monitoring & Observability

### Actuator Endpoints
Spring Boot Actuator provides health checks and metrics:
- `/actuator/health` - Application health
- `/actuator/prometheus` - Prometheus metrics
- `/actuator/info` - Application information

### Logging
- Structured logging with Logback
- Logstash encoder for JSON logging
- SLF4J API with multiple backend support

## CI/CD

### GitHub Actions
Workflow defined in `.github/workflows/push.yml`:
- Build and test on push
- Code formatting validation
- CircleCI integration (legacy)

## Code Quality

### Code Formatting
- **Prettier Java** (v2.1.0) for consistent code formatting
- Automatic formatting on build (validate phase)
- CI enforcement with `prettierCheck` profile

### Security
- OWASP Dependency Check suppression configuration
- CodeQL security scanning support

## Related Projects

- **[netex-gtfs-converter-java](https://github.com/entur/netex-gtfs-converter-java)** - Core conversion library
- **[Marduk](https://github.com/entur/marduk)** - Orchestration service
- **[Chouette](https://github.com/entur/chouette)** - NeTEx export source
- **[Uttu](https://github.com/entur/uttu)** - NeTEx export source

## Working with Claude

### Common Tasks

**Adding a new route:**
Look at existing routes in `src/main/java/no/entur/damu/routes/` for patterns. Routes typically extend `RouteBuilder` and configure Camel DSL endpoints.

**Modifying conversion logic:**
The actual NeTEx to GTFS conversion is handled by the external library. Damu focuses on orchestration, file handling, and validation.

**Testing changes:**
Integration tests use Testcontainers to emulate GCloud services. Tests are in `src/test/java/` mirroring the main package structure.

**Configuration changes:**
Check `src/main/resources/application.properties` or environment-specific property files for runtime configuration.

### Important Notes

1. **Memory Requirements**: Tests require significant memory (7GB heap). Adjust `-Xmx` if needed.

2. **Code Formatting**: Always run `mvn prettier:write` before committing to ensure consistent formatting.

3. **External Dependencies**: The project depends heavily on Entur's ecosystem. Some libraries may require access to Entur's Maven repository.

4. **Cloud Integration**: Full functionality requires GCP credentials and properly configured Pub/Sub topics and GCS buckets.

5. **Large Files**: GTFS and NeTEx datasets can be large. Consider file size limits and memory constraints when processing.

## License

Licensed under EUPL-1.2 (see LICENSE.txt)

## Links

- **Repository**: https://github.com/entur/damu
- **CI**: https://circleci.com/gh/entur/damu/tree/master
- **Parent POM**: org.entur.ror:superpom:4.9.0
