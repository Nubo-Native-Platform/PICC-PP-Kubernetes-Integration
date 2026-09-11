# Security Policy

## Reporting a vulnerability
Please **do not open a public issue**. Email **contribution@nubons.com** with a
description, impact, and reproduction steps. We aim to acknowledge within 5
working days.

## No secrets in the repository
This repository must never contain secrets, API tokens, cluster access credentials,
passwords, private keys, `.env` files, or deployment kubeconfigs. Configuration is
supplied at runtime via environment variables or secret volumes.
