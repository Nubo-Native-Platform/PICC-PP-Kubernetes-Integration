# Contributing to PICC-PP-Kubernetes-Integration

This repository — **`PICC-PP-Kubernetes-Integration`** — is part of the **Platform Infrastructure and Core Components (PICC)** area of the **Nubo Native Platform (NNP)**. Contributions are welcome under the **Apache 2.0 License**.

## Before you start
Contribute against an open **Issue**, the published **Roadmap**, or a proposed **enhancement**. Email **contribution@nubons.com** with your approach and category first; we respond within 5 working days.

## Steps
1. Fork & clone the repository.
2. Create a focused topic branch (e.g., `feature/custom-pod-metrics` or `fix/websocket-keepalive`).
3. Ensure all tests and static analysis scans pass (`.\mvnw.cmd clean package` or `./mvnw clean test spotbugs:check`).
4. Open a Pull Request with a clear description and testing notes following the [PR Checklist](DEVELOPMENT_GUIDELINES.md#7-pull-request-pr-checklist).

## Security
**Never commit secrets, tokens, `.env` files, or cluster credentials.** See [SECURITY.md](SECURITY.md). All participation is governed by our [Code of Conduct](CODE_OF_CONDUCT.md).
