# Kubernetes manifests

Basic manifests for the core of the stack (app + Postgres), showing the same
docker-compose setup (`docker/docker-compose.yml`) expressed as Kubernetes objects.

**Scope (deliberately limited for now):** app + Postgres only. Kafka, Ollama, and
Prometheus/Grafana aren't manifested yet - they'd follow the same recipe (Kafka as a
StatefulSet + headless Service since it needs stable per-broker identity, unlike this
Postgres's simpler single-replica Deployment; Ollama/Prometheus/Grafana as plain
Deployments + Services, same shape as Postgres here). Left as a next step, not because
it's hard, just to keep this gap's scope focused.

## What's here

| File | What |
|---|---|
| `namespace.yaml` | `tattoo-supply-manager` namespace, everything else lives in it |
| `db-credentials-secret.yaml` | Postgres user/password/db (same dev placeholders already committed in docker-compose.yml) |
| `app-secret.yaml` | `JWT_SECRET` (same dev placeholder as docker-compose.yml) |
| `app-configmap.yaml` | Non-secret app config (datasource URL, CORS) |
| `postgres-pvc.yaml` | 1Gi persistent volume claim for Postgres data |
| `postgres-deployment.yaml` | Postgres (pgvector/pgvector:pg17, same image as docker-compose), 1 replica |
| `postgres-service.yaml` | ClusterIP Service, DNS name `postgres` inside the cluster |
| `app-deployment.yaml` | The Spring Boot app, waits for Postgres via an initContainer, health probes on `/actuator/health` |
| `app-service.yaml` | ClusterIP Service for the app |

## How to run it locally (Docker Desktop's Kubernetes)

1. Enable Kubernetes: Docker Desktop → Settings → Kubernetes → Enable Kubernetes → Apply & Restart.
2. Build the app image (Docker Desktop's Kubernetes shares the same image cache as Docker itself, no registry needed):
   ```
   docker build -f docker/Dockerfile -t tattoo-supply-manager:local .
   ```
3. Apply everything:
   ```
   kubectl apply -f k8s/
   ```
4. Watch it come up:
   ```
   kubectl get pods -n tattoo-supply-manager -w
   ```
5. Reach the app (no Ingress/LoadBalancer in this scope):
   ```
   kubectl port-forward -n tattoo-supply-manager svc/app 8080:8080
   ```
   Then hit `http://localhost:8080/actuator/health` or any other endpoint as usual.

**Expected warnings in the logs:** Kafka connection retries and Ollama-related logs -
those services aren't deployed in this scope yet (see "Scope" above). The app boots
fine anyway; Spring Kafka/Spring AI don't hard-fail startup when their broker/model
server isn't reachable, they just log and retry in the background (same behavior
already relied on in CI, which also doesn't run Kafka/Ollama as service containers).

## Tear down

```
kubectl delete namespace tattoo-supply-manager
```
