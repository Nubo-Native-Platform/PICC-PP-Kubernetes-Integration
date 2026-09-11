# User Manual and Deployment Guide: `PICC-PP-Kubernetes-Integration`

This document provides an operational and deployment manual for the **`PICC-PP-Kubernetes-Integration`** microservice within the **Nubo Native Platform (NNP)**. It covers configuration, API usage, interactive WebSocket terminal protocols, containerization, and production deployment on Kubernetes.

---

## Table of Contents

1. [System Overview](#1-system-overview)
2. [User & Integration Manual](#2-user--integration-manual)
   - [Cluster Connection & Authentication](#cluster-connection--authentication)
   - [Complete API Endpoint Catalog](#complete-api-endpoint-catalog)
   - [Interactive Web Terminal Protocol](#interactive-web-terminal-protocol)
3. [Local Deployment Guideline](#3-local-deployment-guideline)
   - [Method 1: One-Click Docker Compose (Recommended)](#method-1-one-click-docker-compose-recommended)
   - [Method 2: Bare-Metal / Local CLI Setup](#method-2-bare-metal--local-cli-setup)
   - [Local Verification & Swagger Testing](#local-verification--swagger-testing)
4. [Production Deployment Guideline (Kubernetes)](#4-production-deployment-guideline-kubernetes)
   - [Required Cluster RBAC](#required-cluster-rbac)
   - [Kubernetes Manifests (Deployment, Service, ConfigMap, Secret)](#kubernetes-manifests)
   - [Health Probes & Virtual Threads Tuning](#health-probes--virtual-threads-tuning)
5. [Troubleshooting & Operational Runbook](#5-troubleshooting--operational-runbook)

---

## 1. System Overview

The **Kubernetes Integration Service** is an enterprise cluster abstraction layer that bridges interactions between NNP frontends/services and target Kubernetes clusters.

It provides:
- **Real-Time Pod Metrics**: Standardized consumption metrics (CPU in millicores/nanocores, Memory in MB) aggregated from `metrics.k8s.io`.
- **Deployment Resource Discovery**: Complete mapping of associated ReplicaSets, Pods, Services, ConfigMaps, Secrets, PVCs, and Ingresses.
- **Cascading Teardown Orchestration**: Clean, multi-resource deletion pipelines for application environments.
- **In-Browser Interactive Terminal**: Zero-dependency Web terminal powered by xterm.js and WebSocket streaming directly into running container shells.

---

## 2. User & Integration Manual

### Cluster Connection & Authentication
The service authenticates to target Kubernetes clusters using standard Service Account OAuth2 Bearer Tokens:
- **Cluster Admin Queries**: Utilizes `k8s.master.token` for cluster-wide introspection.
- **Environment Scoped Operations**: Dynamically fetches the namespace service account token (`adminK8sNsToken`) from the NNP Dashboard Service (`nnp.dash.baseurl`) and caches it in memory.

### Complete API Endpoint Catalog

| HTTP Method | Endpoint Path | Query / Body Parameters | Description |
|-------------|---------------|-------------------------|-------------|
| `GET` | `/pods/details` | `envName` (query) | Returns live CPU/Memory utilization and pod status for a namespace |
| `GET` | `/associated/deployment/resources` | `envName`, `deploymentName` (query) | Discovers all resources linked to a deployment |
| `DELETE` | `/pod` | `envName`, `podName` (query) | Terminates a specific pod in an environment namespace |
| `POST` | `/delete/resources` | JSON Body (`K8sDeleteResourcesRequest`) | Cascades deletion across deployments, services, configs, secrets, PVCs |
| `GET` | `/admin/pods` | None | Cluster-wide pod listing across all namespaces (admin only) |
| `DELETE` | `/cache/env` | None | Evicts cached environment manifests and tokens |
| `GET` | `/terminal` | `envName`, `pod`, `container` (query) | Renders the HTML5 / xterm.js interactive terminal page |
| `POST` | `/terminal` | JSON Body (`K8sTerminalRequest`) | Renders terminal page with POST body parameters |
| `WS` | `/terminal/exec` | `envName`, `pod`, `container` (query) | Bi-directional WebSocket endpoint streaming TTY shell data |

### Interactive Web Terminal Protocol
1. Navigate to: `http://localhost:8080/terminal?envName=dev&pod=my-app-pod-xyz`
2. The UI initializes `xterm.js` and opens a WebSocket stream to `ws://localhost:8080/terminal/exec?envName=dev&pod=my-app-pod-xyz`.
3. Keystrokes are transmitted via WebSocket binary/text frames into the remote container PTY.
4. Output is rendered directly in the browser terminal console with ANSI escape color code support.

---

## 3. Local Deployment Guideline

### Method 1: One-Click Docker Compose (Recommended)
```bash
# 1. Create your environment configuration
cp .env.example .env

# 2. Start container
docker compose up -d --build

# 3. View application logs
docker compose logs -f kubernetes-integration
```

### Method 2: Bare-Metal / Local CLI Setup
```bash
# Set required environment variables
export K8S_URL=https://127.0.0.1:6443
export K8S_MASTER_TOKEN=<YOUR_CLUSTER_TOKEN>
export NNP_DASH_BASEURL=http://localhost:8082

# Run via Maven Wrapper
./mvnw spring-boot:run
```

### Local Verification & Swagger Testing
- **Swagger UI**: Visit `http://localhost:8080/swagger-ui.html`
- **OpenAPI 3.0 Specs**: `http://localhost:8080/v3/api-docs`
- **Actuator Health Probe**: `http://localhost:8080/actuator/health`

---

## 4. Production Deployment Guideline (Kubernetes)

### Required Cluster RBAC
Deploy the service using a ServiceAccount bound to an appropriate ClusterRole:
```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: picc-kubernetes-integration-role
rules:
  - apiGroups: [""]
    resources: ["pods", "pods/exec", "pods/log", "services", "configmaps", "secrets", "persistentvolumeclaims"]
    verbs: ["get", "list", "watch", "create", "delete"]
  - apiGroups: ["apps"]
    resources: ["deployments", "replicasets", "statefulsets"]
    verbs: ["get", "list", "watch", "delete"]
  - apiGroups: ["metrics.k8s.io"]
    resources: ["pods"]
    verbs: ["get", "list"]
  - apiGroups: ["networking.k8s.io"]
    resources: ["ingresses"]
    verbs: ["get", "list", "delete"]
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: picc-kubernetes-integration-binding
subjects:
  - kind: ServiceAccount
    name: kubernetes-integration-sa
    namespace: nnp-core-components
roleRef:
  kind: ClusterRole
  name: picc-kubernetes-integration-role
  apiGroup: rbac.authorization.k8s.io
```

### Kubernetes Manifests

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: picc-pp-kubernetes-integration
  namespace: nnp-core-components
  labels:
    app: picc-pp-kubernetes-integration
spec:
  replicas: 2
  selector:
    matchLabels:
      app: picc-pp-kubernetes-integration
  template:
    metadata:
      labels:
        app: picc-pp-kubernetes-integration
    spec:
      serviceAccountName: kubernetes-integration-sa
      containers:
        - name: app
          image: ghcr.io/nubo-native-platform/picc-pp-kubernetes-integration:latest
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: 8080
          env:
            - name: PORT
              value: "8080"
            - name: K8S_URL
              value: "https://kubernetes.default.svc"
            - name: NNP_DASH_BASEURL
              value: "http://nnp-portal-backend:8082"
            - name: VAR_BASE_NNP_DOMAIN
              value: "nnp.yourdomain.com"
          resources:
            requests:
              memory: "256Mi"
              cpu: "100m"
            limits:
              memory: "512Mi"
              cpu: "500m"
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 15
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 15
            periodSeconds: 10
---
apiVersion: v1
kind: Service
metadata:
  name: picc-pp-kubernetes-integration
  namespace: nnp-core-components
spec:
  type: ClusterIP
  ports:
    - port: 8080
      targetPort: 8080
      name: http
  selector:
    app: picc-pp-kubernetes-integration
```

---

## 5. Troubleshooting & Operational Runbook

| Symptom | Probable Root Cause | Remediation Step |
|---------|---------------------|------------------|
| `This Environment Does Not Exist In Cache` | NNP Dashboard unreachable or cache stale | Verify `NNP_DASH_BASEURL` connectivity; call `DELETE /cache/env` |
| `WebSocket connection failed: 403 / 500` | Ingress / Gateway missing WebSocket headers | Ensure Ingress annotations include `nginx.org/websocket-services` and headers `Upgrade`, `Connection` |
| `Could not resolve placeholder` | Missing default property resolution | Ensure latest application properties and fallback `${VAR:default}` syntax are active |
| `Exec error: command terminated with exit code 126` | Container lacks shell (`/bin/bash` or `/bin/sh`) | Deploy containers with a standard POSIX shell or distroless debug variant |
