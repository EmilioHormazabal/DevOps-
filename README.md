# Innovatech Chile — DevOps CI/CD

Proyecto de automatización del ciclo CI/CD para una plataforma multicomponente desplegada en AWS EKS.

## Stack

| Componente | Tecnología | Puerto |
|------------|-----------|--------|
| Frontend | React + Vite + Nginx | 80 |
| API Ventas | Spring Boot 17 | 8080 |
| API Despachos | Spring Boot 17 | 8081 |
| API Node (mock) | Express | 3000 |
| Base de datos | MySQL 8.0 | 3306 |

## Arquitectura

```
[Usuario] → LoadBalancer :80 → Nginx (proxy reverso)
                                  ├── /api/ventas/ → back-ventas :8080 → MySQL :3306
                                  └── /api/despachos/ → back-despachos :8081 → MySQL :3306
```

## Requisitos

- Docker + Docker Compose
- Node.js 20
- Java 17 (Temurin)
- AWS CLI + cuenta AWS (para deploy en nube)

## Entorno local

```bash
docker compose up --build
```

Servicios disponibles:
- Frontend: http://localhost:80
- API Ventas: http://localhost:8080
- API Despachos: http://localhost:8081
- API Node: http://localhost:3000

## Tests

```bash
# API Node
cd api-innovatech && npm test

# Frontend
cd front_despacho && npx vitest run

# Backend Ventas
cd back-Ventas_SpringBoot/Springboot-API-REST && ./mvnw test

# Backend Despachos
cd back-Despachos_SpringBoot/Springboot-API-REST-DESPACHO && ./mvnw test
```

**Total: 24 tests** (9 api-node + 2 frontend + 10 ventas + 3 despachos)

## Pipelines CI/CD (GitHub Actions)

| Pipeline | Trigger | Jobs |
|----------|---------|------|
| `ci.yml` | push/PR a main | build + test (4 jobs paralelos) |
| `deploy.yml` | push a deploy/main + manual | build → push ECR → deploy EKS → validate |

## Infraestructura AWS (Terraform)

- VPC con subnets pública y privada
- EKS Cluster v1.32, 2 nodos t3.medium (autoescalable 2-4)
- ECR: 4 repositorios con scan on push
- CloudWatch Logs (7 días retención)

## Documentación

- `docs/TORPEDO.md` — Resumen técnico para la defensa
- `docs/INFORME_DESPLIEGUE.md` — Base del informe Word
- `docs/GUIA_DE_ESTUDIO.md` — Guía de estudio
- `docs/PIPELINE-ANALISIS.md` — Análisis del pipeline
- `docs/arquitectura-aws.svg` — Diagrama de arquitectura
- `GITFLOW.md` — Flujo de trabajo Git
