# Infrastructure

This directory contains the Phase 4 governance and deployment scaffolding.

Phase 4 exists to turn the completed Java Gateway + Java Platform Service + Python AI Service architecture into something that can be operated outside a single developer machine. It adds optional discovery/configuration, traffic protection, health and metrics endpoints, repeatable Kubernetes/Helm deployment shapes, CI/CD image checks, and branch protection guidance without forcing those components into the default local workflow.

## Layout

| Path | Purpose |
| --- | --- |
| `k8s/base` | Kustomize base manifests for frontend, gateway, platform-service, ai-service, Postgres, RabbitMQ, Nacos, and Sentinel |
| `k8s/overlays/dev` | Development overlay with local image tags |
| `helm/smart-ticket-platform` | Helm skeleton for app services with externalized stateful dependencies |
| `github-branch-protection.md` | Recommended branch protection and required checks |

## Local Governance Profile

Default local development does not start Nacos or Sentinel.

```bash
docker compose up --build
```

Enable Phase 4 governance services explicitly:

```bash
SPRING_PROFILES_ACTIVE=governance \
docker compose --profile governance --profile frontend up --build
```

This starts:

- Nacos `v3.0.3` on `http://localhost:8848/nacos`
- Sentinel Dashboard `1.8.9` on `http://localhost:8858`
- Java Gateway and Platform Service with the `governance` Spring profile
- Optional production-style frontend container on `http://localhost:5173`

## Kubernetes

Render or apply the development overlay:

```bash
kubectl kustomize infra/k8s/overlays/dev
kubectl apply -k infra/k8s/overlays/dev
```

The included Postgres, RabbitMQ, Nacos, and Sentinel manifests are development scaffolding. For production, replace them with managed services or dedicated operator-backed deployments.

## Helm

Render the chart:

```bash
helm template smart-ticket infra/helm/smart-ticket-platform
```

The chart intentionally defaults `secrets.create=false`. Create `smart-ticket-secrets` through your cluster secret manager before installing, or override values only in a secure deployment pipeline.

## Images

GitHub Actions builds images for:

- `frontend`
- `gateway`
- `platform-service`
- `ai-service`

Pull request builds validate image construction without pushing. `main` branch pushes publish images to GHCR.
