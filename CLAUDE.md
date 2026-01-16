# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Holodeck B2B is a standalone Java-based B2B messaging solution implementing OASIS ebMS3 Messaging and AS4 Profile standards. Built on Apache Axis2, it's designed with extensibility in mind through a well-defined interface layer.

## Build Commands

```bash
# Build all modules
mvn clean install

# Run tests
mvn test

# Build without tests
mvn clean install -DskipTests

# Build specific module
mvn clean install -pl modules/holodeckb2b-core

# Build distribution package (output: modules/holodeckb2b-distribution/target/holodeckb2b-*.zip)
mvn clean install
```

## Module Structure

The project follows a multi-module Maven structure:

- **holodeckb2b-interfaces** (LGPLv3) - Public API interfaces for custom extensions. This is the contract layer that external integrations should depend on.
- **holodeckb2b-core** - Protocol-agnostic core processing engine containing handler chains, validation, submission, and worker pool management.
- **holodeckb2b-ebms3as4** - ebMS3/AS4 protocol implementation. Builds as an Axis2 Archive (AAR): `hb2b-as4-msh.aar`.
- **holodeckb2b-as4secprovider** - Default WS-Security implementation for AS4 using WSS4J and Bouncy Castle.
- **holodeckb2b-certmanager** - Default Certificate Manager for keystore and TLS certificate management.
- **holodeckb2b-default-mds** - Default Metadata Storage Provider using Hibernate + Derby.
- **holodeckb2b-default-psp** - Default Payload Storage Provider.
- **holodeckb2b-ui** - Desktop monitoring application (Swing-based with RMI server).
- **holodeckb2b-distribution** - Assembles the complete distribution package.

## Architecture

### Handler Chain Architecture
Message processing uses Axis2 handler chains. Handlers extend `AbstractBaseHandler` or `AbstractUserMessageHandler` and are organized into inflow/outflow processing pipelines defined in `module.xml`.

### Extension Points
The codebase is designed around interface-based extensibility. Key interfaces in `org.holodeckb2b.interfaces`:

- `IDeliveryMethod` - Custom message delivery backends
- `IMessageProcessingEventHandler` - Event listeners for message lifecycle
- `IMessageValidator` - Custom validation rules
- `IMetadataStorageProvider` / `IPayloadStorageProvider` - Storage abstraction
- `IPModeStorage` - P-Mode configuration storage

### P-Mode Configuration
Processing Modes (P-Modes) are central configuration entities that define how messages are processed, including security settings, delivery specifications, and agreement parameters.

### Event-Driven Processing
The system uses `IMessageProcessingEvent` for lifecycle events (validation failures, delivery events, security events). Custom handlers implement `IMessageProcessingEventHandler`.

## Key Dependencies

- **Axis2 1.8.2** / **AXIOM 1.4.0** - Web services framework and XML processing
- **WSS4J 3.0.4** - WS-Security implementation
- **Bouncy Castle 1.78.1** - Cryptography
- **Hibernate 5.6.15** - ORM (default metadata storage)
- **Log4j 2.23.1** - Logging

## Testing

Uses JUnit 5 (Jupiter) with Mockito. The `holodeckb2b-core` module provides test utilities via its test-jar artifact.

```bash
# Run tests for specific module
mvn test -pl modules/holodeckb2b-core
```

## Contributing

- PRs should target the `next` branch
- Update CHANGELOG.MD with changes
- Reference GitHub issues in commits
