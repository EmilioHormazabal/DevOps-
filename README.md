# Innovatech Chile — DevOps CI/CD

Proyecto de automatización del ciclo CI/CD para una plataforma multicomponente desplegada en AWS EKS.
Proyecto de automatización del ciclo de integración y entrega continua para una plataforma compuesta por frontend, backend Spring Boot, API Node y base de datos MySQL, desplegada en Amazon EKS.

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

El diagrama completo de la arquitectura AWS está en `docs/arquitectura-aws.svg`.

## Infraestructura AWS

Provisionada con Terraform en `us-east-1`.

| Recurso | Detalle |
|---------|---------|
| VPC | 10.0.0.0/16 |
| Subnet pública frontend | 10.0.1.0/24 |
| Subnet privada backend | 10.0.2.0/24 |
| Subnet pública EKS | 10.0.3.0/24 |
| EKS Cluster | v1.32, 2×t3.medium (min 2, max 4) |
| ECR | 4 repos con scan on push |
| CloudWatch | Log group /eks/innovatech-poc/applications (7 días) |

### Security Groups

| SG | Propósito |
|----|-----------|
| innovatech-poc-sg-front | HTTP (80) desde 0.0.0.0/0, SSH restringido |
| innovatech-poc-sg-back | Tráfico solo desde SG Frontend |
| innovatech-poc-sg-data | BD solo desde SG Backend |
| innovatech-poc-sg-eks | Tráfico interno del cluster |

### Repositorios ECR

| Servicio | URI |
|----------|-----|
| frontend | `219473730351.dkr.ecr.us-east-1.amazonaws.com/innovatech-poc-frontend` |
| back-ventas | `219473730351.dkr.ecr.us-east-1.amazonaws.com/innovatech-poc-back-ventas` |
| back-despachos | `219473730351.dkr.ecr.us-east-1.amazonaws.com/innovatech-poc-back-despachos` |
| api-node | `219473730351.dkr.ecr.us-east-1.amazonaws.com/innovatech-poc-api-node` |

Las imágenes se etiquetan con `latest` y el SHA del commit para trazabilidad.

## Pipelines CI/CD (GitHub Actions)

### CI — ci.yml

Se ejecuta en push o PR a main. Cuatro jobs en paralelo:

| Job | Comando |
|-----|---------|
| api-innovatech | npm ci → npm test |
| front-despacho | npm ci → npm test |
| back-ventas | ./mvnw -B package |
| back-despachos | ./mvnw -B package |

Total: 24 tests (9 api-node + 2 frontend + 10 ventas + 3 despachos).

### CD — deploy.yml

Se ejecuta en push a deploy/main o manual. Cuatro jobs secuenciales:

1. **build** — compila todos los componentes
2. **docker-push** — build y push a ECR (4 servicios en paralelo)
3. **deploy-eks** — aplica manifiestos y espera rollout en EKS
4. **validate** — health checks via port-forward + Load Balancer

Los secretos se gestionan con GitHub Secrets y se inyectan como Kubernetes Secrets en el cluster.

## Entorno local

```bash
docker compose up --build
```

| Servicio | URL |
|----------|-----|
| Frontend | http://localhost:80 |
| API Ventas | http://localhost:8080 |
| API Despachos | http://localhost:8081 |
| API Node | http://localhost:3000 |

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

## Estructura del proyecto

```
├── api-innovatech/            # API Node (Express)
├── back-Ventas_SpringBoot/    # API Ventas (Spring Boot)
├── back-Despachos_SpringBoot/ # API Despachos (Spring Boot)
├── front_despacho/            # Frontend (React + Vite)
├── infra/
│   └── k8s/                   # Manifiestos Kubernetes
│       ├── api-node/
│       ├── back-despachos/
│       ├── back-ventas/
│       ├── configmaps/
│       ├── frontend/
│       ├── mysql/
│       ├── namespace/
│       └── secrets/
├── scripts/                   # Scripts de deploy y validación
├── docs/                      # Documentación
├── init-scripts/              # Inicialización de BD
├── .github/workflows/         # GitHub Actions
├── main.tf                    # Terraform
└── eks.tf                     # Terraform EKS
```

## Documentación

- `docs/INFORME_DESPLIEGUE.md` — Informe completo del despliegue
- `docs/arquitectura-aws.svg` — Diagrama de arquitectura
- `GITFLOW.md` — Flujo de trabajo Git