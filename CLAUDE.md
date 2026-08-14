# Damu - NeTEx to GTFS Converter

## Project Overview

Damu converts NeTEx datasets into GTFS datasets for the Entur public transport data pipeline. It also
merges the per-provider GTFS exports into the national datasets, validates GTFS, and publishes a GTFS
export of the stop places.

## Architecture

### Technology Stack
- **Java 25**
- **Spring Boot** (no integration framework; PubSub consumers come from `entur-google-pubsub`)
- **Google Cloud Platform**: Cloud Storage for files, Pub/Sub for messaging
- **Maven**, parent `org.entur.ror:superpom`

### Key Dependencies
- `netex-gtfs-converter-java` - the NeTEx to GTFS conversion itself
- `gtfs-validator-main` - MobilityData GTFS validation
- `entur-helpers` - `AbstractEnturGooglePubSubConsumer`, GCS blob store
- `zt-zip` - ZIP handling

Versions live in pom.xml; do not trust versions written in this file.

## Project Structure

```
damu/
├── src/main/java/no/entur/damu/
│   ├── App.java              # Spring Boot application
│   ├── Constants.java        # Wire names and blob path fragments
│   ├── DamuMdc.java          # correlationId and codespace for structured logging
│   ├── pubsub/               # Consumer, publisher, attribute handling, destination names
│   ├── export/               # GTFS export and stop place GTFS export
│   ├── aggregation/          # National GTFS merge
│   ├── validation/           # GTFS validation and report upload
│   ├── stop/                 # Stop area repository, registry fetchers, refresh job
│   ├── gtfs/                 # GTFS merging and validation helpers
│   ├── netex/                # Entur customisations of the converter library
│   ├── services/             # Blob store access
│   └── config/               # Blob store profiles
├── helm/                     # Kubernetes deployment configs
├── terraform/                # Infrastructure as code
└── pom.xml
```

### How a request runs

`GtfsRouteDispatcherConsumer` reads a message off `GtfsRouteDispatcherTopic` and switches on its
`Action` attribute:

| `Action` | Body | Job |
| --- | --- | --- |
| `Export` | codespace | `GtfsExportService` - download the NeTEx export, convert, validate, upload both, notify marduk on `DamuExportGtfsStatusQueue` |
| `Aggregation` | comma-separated GTFS file names | `GtfsAggregationService` - download each, merge into the extended and basic national datasets, upload both, notify marduk on `MardukAggregateGtfsStatusQueue` |

Two jobs run on a schedule instead:

| When | Job |
| --- | --- |
| startup, then 01:00 and 14:00 | `StopAreaRefreshService` - reload the stop area repository from `tiamat/CurrentAndFuture_latest.zip` |
| 03:30 | `GtfsStopExportService` - export the stop places to `tiamat/Current_latest-gtfs.zip` |

The ack deadline is managed by the PubSub streaming pull client, which extends it for up to an hour
while a message is being processed. Nothing in the application touches it.

## Common Tasks

### Building
Requires JDK 25; the enforcer fails the build on anything older.
```bash
mvn clean package
```

### Running tests
```bash
mvn test
```
Use `clean` after changing a `static final` constant: those inline into call sites and an incremental
build can pass against the old value.

Only `PubSubWiringTest` needs Docker; everything else runs in one JVM.

### Formatting
Prettier runs in `validate` and gates the build.
```bash
mvn prettier:write
mvn prettier:check -PprettierCheck
```

### Running locally
Use the compose stack in the parent `marduk-pipeline` repository, which brings up the PubSub and GCS
emulators and creates the topics.

## Configuration

- `helm/damu/templates/configmap.yaml` is the production configuration.
- `src/test/resources/application.properties` is the test configuration.
- Profiles: `gcs-blobstore` (deployed), `local-disk-blobstore`, `in-memory-blobstore` (tests).

## Key Files to Review

- `docs/camel-removal.md`: what the move off Camel changed, and the behaviour differences it
  introduced. Read the *Wire contract*, *Retries and failure handling* and *The release* sections before
  changing anything under `pubsub/`, `export/` or `aggregation/`.
- `pom.xml`: dependencies and build configuration
- `README.md`: user-facing documentation

## Common Pitfalls

1. **JDK 25 is required**: the Maven enforcer rejects anything older, and the failure at `validate`
   names the JDK range rather than the cause. Set `JAVA_HOME` before `mvn`.
2. **Message attributes are a wire format.** Damu echoes the whole request attribute map back to marduk
   on every status notification, and marduk matches its pending job on what comes back. `WireContractTest`
   pins the names and values against hard-coded strings; if it fails, check marduk before changing it.
3. **`maxDeliveryAttempts=5` is duplicated in marduk's `terraform/pubsub.tf`.** Change them together.
4. **The exporter's output stream can only be read once**: it is a stream over a temp file that is
   deleted on close. `GtfsExportService` copies it to a file of its own because validation and upload
   both need it.
5. **A missing NeTEx export leaves the job with no terminal status.** Preserved from the Camel version
   on purpose, and pinned by a test. Changing it changes what marduk records for the provider.
6. **Removing a ConfigMap key is a rollout hazard**, not a cleanup: helm applies the ConfigMap before the
   first pod is replaced, and kubelet re-syncs it into pods still running the old image.

## Making Changes

- Run the tests first, so a failure afterwards is attributable.
- Prettier runs in `validate` and gates the build; let `mvn prettier:write` do the formatting.
- Update this file when the architecture moves, and `docs/camel-removal.md` when the behaviour does.

## Related Projects

- **[netex-gtfs-converter-java](https://github.com/entur/netex-gtfs-converter-java)** - the conversion library
- **[Marduk](https://github.com/entur/marduk)** - orchestration service, and damu's only client
- **[Chouette](https://github.com/entur/chouette)** / **[Uttu](https://github.com/entur/uttu)** - NeTEx export sources

## License

Licensed under EUPL-1.2 (see LICENSE.txt)
