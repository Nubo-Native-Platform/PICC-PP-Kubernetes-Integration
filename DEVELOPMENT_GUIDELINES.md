# Development Guidelines and Contribution Standards: `PICC-PP-Kubernetes-Integration`

This document defines the architectural standards, development workflows, coding conventions, and security requirements for contributors to **`PICC-PP-Kubernetes-Integration`** within the **Nubo Native Platform (NNP)**.

---

## Table of Contents

1. [Architecture & Design Principles](#1-architecture--design-principles)
2. [Project & Package Structure](#2-project--package-structure)
3. [Development Environment Setup](#3-development-environment-setup)
4. [Kubernetes Integration Mechanics](#4-kubernetes-integration-mechanics)
   - [Dynamic Fabric8 Client Factory](#dynamic-fabric8-client-factory)
   - [Pod Metrics Aggregation & Unit Scaling](#pod-metrics-aggregation--unit-scaling)
   - [Interactive WebSocket TTY Terminal Streaming](#interactive-websocket-tty-terminal-streaming)
   - [Cascading Resource Lifecycle Deletion](#cascading-resource-lifecycle-deletion)
5. [Security & Sanitization Guidelines](#5-security--sanitization-guidelines)
   - [Cluster Token Hygiene](#cluster-token-hygiene)
   - [WebSocket Resource Leaks Prevention](#websocket-resource-leaks-prevention)
6. [Code Quality, SAST & SBOM Tooling](#6-code-quality-sast--sbom-tooling)
   - [SpotBugs & FindSecBugs (SAST)](#spotbugs--findsecbugs-sast)
   - [OWASP Dependency-Check (SCA)](#owasp-dependency-check-sca)
   - [CycloneDX SBOM Generation (CNCF Supply Chain)](#cyclonedx-sbom-generation-cncf-supply-chain)
   - [Checkstyle (Google Java Style)](#checkstyle-google-java-style)
7. [Testing & Verification Strategy](#7-testing--verification-strategy)
8. [Open-Source Contribution Guidelines & PR Checklist](#8-open-source-contribution-guidelines--pr-checklist)

---

## 1. Architecture & Design Principles

The Kubernetes Integration Service acts as a high-performance abstraction bridge between NNP platform services (such as portals, backend orchestrators, and automated CI/CD runners) and target Kubernetes clusters.

### Core Architectural Principles

- **Decoupled Cluster Binding**: Uses the `io.fabric8:kubernetes-client` SDK configured dynamically per request/environment using OAuth2 bearer tokens.
- **Bi-Directional Streaming**: Integrates Spring WebSocket with Fabric8 `ExecWatch` to pipe interactive TTY standard input, standard output, and standard error streams in real time to browser-based xterm.js clients.
- **Resilient Fallback Design**: All cluster and dashboard endpoints support graceful fallbacks via environment variables, ensuring the microservice starts seamlessly in standalone, containerized, and mesh topologies without requiring external Spring Cloud Config servers.
- **Fail-Fast Structured Error Translation**: Translates cluster errors into typed `K8sIntgException` responses mapped to HTTP status codes.
- **Supply Chain Security**: Static analysis (SpotBugs), software composition analysis (OWASP), and SBOM generation (CycloneDX) embedded in build and CI pipelines.

---

## 2. Project & Package Structure

```
PICC-PP-Kubernetes-Integration/
├── .env.example                               # Environment variable blueprint
├── .github/
│   └── workflows/
│       └── ci-cd.yml                          # GitHub Actions CI/CD pipeline
├── docs/
│   └── images/
│       ├── architecture.svg                   # Architectural component topology
│       └── workflow.svg                       # WebSocket terminal & exec workflow
├── src/
│   ├── main/
│   │   ├── java/com/nnp/kubernetes_integration/
│   │   │   ├── KubernetesIntegrationApplication.java
│   │   │   ├── configs/                       # Factory & OpenAPI beans
│   │   │   │   ├── K8sIntgClientFactory.java
│   │   │   │   └── OpenApiConfig.java
│   │   │   ├── controllers/                   # REST and Web endpoints
│   │   │   │   ├── CacheController.java
│   │   │   │   ├── NnpK8sAdminController.java
│   │   │   │   ├── NnpK8sIntgRestController.java
│   │   │   │   └── NnpK8sIntgUIController.java
│   │   │   ├── dtos/                          # Request and response models
│   │   │   ├── exceptions/                    # Global exception handling
│   │   │   ├── services/                      # Business logic and Fabric8 execution
│   │   │   │   ├── K8sAdminService.java
│   │   │   │   ├── K8sIntgCommandSvc.java
│   │   │   │   ├── K8sIntgQuerySvc.java
│   │   │   │   ├── NnpK8sSvc.java
│   │   │   │   └── NnpService.java
│   │   │   ├── utils/                         # Conversion math (cores, bytes)
│   │   │   └── websockets/                    # WebSocket handlers and config
│   │   └── resources/
│   │       ├── application.properties         # Parameterized core configuration
│   │       └── templates/terminal.html        # xterm.js UI client
│   └── test/                                  # Unit & Integration test suites
├── application.properties.example              # Local properties reference
├── docker-compose.yml                          # Local Docker compose orchestration
├── Dockerfile                                  # Multi-stage Temurin-21 container
├── pom.xml                                     # Maven POM with security plugins
└── spotbugs-exclude.xml                        # SpotBugs SAST exclusion filter
```

---

## 3. Development Environment Setup

### Prerequisites
- **JDK 21** (e.g. Eclipse Temurin 21)
- **Maven 3.9+** (or use included `mvnw` / `mvnw.cmd`)
- **Docker Engine** (optional, for container runs)
- Target Kubernetes cluster (Minikube, Kind, k3s, or remote EKS/GKE cluster)

### Local Build and Run
```bash
# Clone the repository
git clone https://github.com/Nubo-Native-Platform/PICC-PP-Kubernetes-Integration.git
cd PICC-PP-Kubernetes-Integration

# Copy configuration
cp .env.example .env

# Build and execute tests
./mvnw clean test

# Run the microservice locally
./mvnw spring-boot:run
```

Access Swagger UI at: `http://localhost:8080/swagger-ui.html`

---

## 4. Kubernetes Integration Mechanics

### Dynamic Fabric8 Client Factory
Instead of binding a single static Kubernetes client, `K8sIntgClientFactory` dynamically configures `KubernetesClient` instances:
- In master mode: uses `k8s.master.token` for cluster-wide admin queries (`/admin/pods`).
- In environment mode: retrieves the environment-specific token (`adminK8sNsToken`) from cache or NNP Dashboard, restricting operations to the target namespace.

### Pod Metrics Aggregation & Unit Scaling
`K8sConversionUtils` standardizes raw Kubernetes CPU and Memory metric strings:
- CPU metrics (`m`, `u`, `n`) are converted to millicores or nanocores.
- Memory metrics (`Ki`, `Mi`, `Gi`, `Ti`) are parsed using standard binary prefixes (`1024^n`) into bytes and converted to megabytes (`MB`).

### Interactive WebSocket TTY Terminal Streaming
The service establishes a WebSocket connection at `/terminal/exec`. When a client connects:
1. Validates `envName`, `pod`, and optional `container`.
2. Initiates an `ExecWatch` with Fabric8 using `redirectingInput()`, `writingOutput()`, and `withTTY()`.
3. Streams keystrokes received via `handleTextMessage` to the pod's `stdin`.
4. Streams bytes from `stdout`/`stderr` back to the browser's `WebSocketSession`.
5. Cleans up `ExecWatch` and unregisters sessions upon disconnect.

---

## 5. Security & Sanitization Guidelines

### Cluster Token Hygiene
- **Never commit tokens or secrets.**
- Master tokens must be supplied via Kubernetes Secrets or runtime environment variables (`K8S_MASTER_TOKEN`).
- Environment tokens must be requested on-demand from the secured NNP Dashboard service.

### WebSocket Resource Leaks Prevention
- Always wrap `ExecWatch` instances in thread-safe registry tracking (`ConcurrentHashMap`).
- Implement `afterConnectionClosed` to gracefully close streams and release pod PTY descriptors on the remote node.

---

## 6. Code Quality, SAST & SBOM Tooling

### SpotBugs & FindSecBugs (SAST)
```bash
./mvnw spotbugs:check
```
Ensures 0 bugs and eliminates vulnerabilities such as unclosed streams, injection, and mutable state exposure.

### CycloneDX SBOM Generation
```bash
./mvnw cyclonedx:makeAggregateBom
```
Generates a CNCF-compliant `target/bom.json` documenting all transitive dependencies.

---

## 7. Testing & Verification Strategy

- Run unit and context verification tests:
  ```bash
  ./mvnw test
  ```
- All endpoints must include OpenAPI documentation annotations (`@Operation`, `@ApiResponse`).

---

## 8. Open-Source Contribution Guidelines & PR Checklist

Before submitting a Pull Request:
- [ ] Code compiles with JDK 21 (`./mvnw clean compile`).
- [ ] All tests pass cleanly (`./mvnw test`).
- [ ] SpotBugs reports 0 defects (`./mvnw spotbugs:check`).
- [ ] No hardcoded passwords, internal hostnames, or private tokens.
- [ ] Commit messages follow Conventional Commits (e.g. `feat: ...`, `fix: ...`, `docs: ...`).
