# Informe de Despliegue — Innovatech Chile

## Resumen

Proyecto DevOps que automatiza el ciclo CI/CD de una plataforma compuesta por frontend React, backend Spring Boot, API Node y base de datos MySQL, desplegada en Amazon EKS mediante GitHub Actions.

## 1. Arquitectura del Sistema

### 1.1 Componentes

| Componente | Tecnologia | Puerto |
|------------|-----------|--------|
| Frontend | React + Vite + Nginx | 80 |
| API Ventas | Spring Boot 17 | 8080 |
| API Despachos | Spring Boot 17 | 8081 |
| API Node (mock) | Express | 3000 |
| Base de datos | MySQL 8.0 | 3306 |

### 1.2 Metodo de Integracion

El frontend se sirve mediante Nginx, que actua como proxy reverso. Las rutas `/api/ventas/` se redirigen a back-ventas:8080 y `/api/despachos/` a back-despachos:8081. La comunicacion entre backends y MySQL usa variables de entorno para la URL JDBC, permitiendo cambiar la configuracion sin reconstruir las imagenes.

El flujo de comunicacion es:

Usuario → LoadBalancer :80 → Nginx (proxy reverso) → /api/ventas/ → back-ventas :8080 → MySQL :3306
                                                              /api/despachos/ → back-despachos :8081 → MySQL :3306

El API Node funciona como servicio mock independiente sin conexion a base de datos, utilizado para pruebas de integracion.

## 2. Contenedores

### 2.1 Dockerfiles

Cada componente tiene su propio Dockerfile con las siguientes optimizaciones:

**Frontend (React + Nginx):** Multi-etapa con node:20-alpine para build y nginx:alpine para produccion. El build genera archivos estaticos que se copian a la imagen final de Nginx.

**Backend Ventas y Despachos (Spring Boot):** Multi-etapa con maven:3.9-eclipse-temurin-17 para compilacion y eclipse-temurin:17-jre-alpine para ejecucion. Solo el JRE en produccion reduce significativamente el tamano de la imagen.

**API Node (Express):** Multi-etapa con node:20-alpine. Usa usuario no-root `innovatech` por seguridad.

Todas las imagenes incluyen:
- Multi-stage: build tools separados del runtime
- Imagenes base Alpine (minimalistas)
- Usuarios no-root en produccion
- Exposicion de puertos minimos
- .dockerignore en cada componente

### 2.2 Orquestacion Local (Docker Compose)

El archivo `docker-compose.yml` define 5 servicios (mysql, back-ventas, back-despachos, api-node, frontend) con:

- Red interna `innovatech-network` para aislamiento
- Healthcheck en MySQL con `mysqladmin ping`
- Dependencias condicionales (`condition: service_healthy`)
- Volumen persistente `mysql_data` para la base de datos
- Variables de entorno para configuracion

Comando para entorno local:

```bash
docker compose up --build
```

## 3. Registro de Imagenes (ECR)

Se crearon 4 repositorios en Amazon ECR mediante Terraform:

- `innovatech-poc-frontend`
- `innovatech-poc-back-ventas`
- `innovatech-poc-back-despachos`
- `innovatech-poc-api-node`

El flujo de publicacion en el pipeline CI/CD:

1. Build de la imagen con `docker/build-push-action@v6`
2. Etiquetado con dos tags: `latest` y `${{ github.sha }}`
3. Push automatico a ECR
4. Cache de capas entre ejecuciones via GitHub Actions cache

El tag por SHA permite trazabilidad directa entre el codigo y la imagen desplegada.

## 4. Pipeline CI/CD (GitHub Actions)

### 4.1 Pipeline de Integracion Continua (ci.yml)

Se ejecuta en push o PR a main/master con 4 jobs paralelos:

| Job | Tecnologia | Comando |
|-----|-----------|---------|
| api-innovatech | Node 20 | npm ci → npm test |
| front-despacho | Node 20 | npm ci → npm test |
| back-ventas | Java 17 (Temurin) | ./mvnw -B package |
| back-despachos | Java 17 (Temurin) | ./mvnw -B package |

Total: **24 tests** (9 api-node + 2 frontend + 10 ventas + 3 despachos)

### 4.2 Pipeline de Despliegue Continuo (deploy.yml)

Disparado por push a las ramas `deploy` o `main`, o manualmente via `workflow_dispatch`. Consta de 4 jobs secuenciales:

1. **build**: Compila todos los componentes
2. **docker-push**: Build y push a ECR (matrix de 4 servicios en paralelo)
3. **deploy-eks**: kubectl apply + rollout wait en EKS
4. **validate**: Healthchecks via port-forward + Load Balancer

Los secretos se manejan mediante GitHub Secrets, inyectandolos como `--from-literal` en kubectl para crear el secret `innovatech-db-secret` directamente en el cluster.

## 5. Infraestructura en la Nube (AWS)

### 5.1 Arquitectura

La infraestructura se provisiona con Terraform e incluye:

| Recurso | Detalle |
|---------|---------|
| VPC | CIDR 10.0.0.0/16 con DNS habilitado |
| Subnet publica frontend | 10.0.1.0/24, AZ 1 |
| Subnet privada backend | 10.0.2.0/24, AZ 1 |
| Subnet publica EKS | 10.0.3.0/24, AZ 2 |
| Internet Gateway | Acceso a Internet para subnets publicas |
| NAT Gateway | Acceso a Internet para subnets privadas |
| EKS Cluster | v1.32, rol LabRole |
| Node Group | 2× t3.medium (min 2, max 4) |
| ECR | 4 repos con force_delete y scan_on_push |
| CloudWatch | Log group con 7 dias de retencion |

### 5.2 Seguridad y Grupos de Seguridad

- **SG Frontend**: HTTP (80) desde 0.0.0.0/0, SSH (22) restringido
- **SG Backend**: Trafico solo desde SG Frontend
- **SG Data**: Base de datos solo desde SG Backend
- **SG EKS**: Trafico interno del cluster + HTTPS API server

### 5.3 Kubernetes

Los manifiestos en `infra/k8s/` definen:

| Componente | Replicas | Tipo | Probes |
|------------|:--------:|------|--------|
| MySQL | 1 | ClusterIP | readiness + liveness (mysqladmin) |
| back-ventas | 3 | ClusterIP | HTTP /actuator/health |
| back-despachos | 3 | ClusterIP | HTTP /actuator/health |
| api-node | 2 | ClusterIP | HTTP /health |
| frontend (Nginx) | 2 | LoadBalancer | HTTP / |

Los deployments incluyen resource limits, imagePullPolicy Always, y variables de entorno desde Secrets y ConfigMaps.

## 6. Configuracion y Secretos

### 6.1 Gestion de Secretos

Los secretos se manejan en dos niveles:

- **GitHub Secrets**: Almacenan AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, AWS_SESSION_TOKEN, MYSQL_ROOT_PASSWORD, MYSQL_PASSWORD
- **Kubernetes Secrets**: El pipeline crea `innovatech-db-secret` dinamicamente usando `kubectl create secret --from-literal` con los valores de GitHub Secrets

Esto evita tener secretos hardcodeados en el repositorio (el archivo `infra/k8s/secrets/secret.yaml` existe solo como template de referencia para desarrollo local).

### 6.2 Principio de Minimo Privilegio (IAM)

Se usa el rol `LabRole` proporcionado por AWS Academy para EKS y EC2. Los Security Groups restringen el trafico solo a los puertos y origenes necesarios para cada capa.

## 7. Seguridad Basica

- **Imagenes base Alpine/slim**: Reducen superficie de ataque
- **Usuarios no-root**: Contenedores ejecutan procesos como usuario `innovatech`
- **Puertos minimos expuestos**: Solo los necesarios para cada servicio
- **Security Groups restrictivos**: Trafico permitido solo entre capas autorizadas
- **Scan de vulnerabilidades**: ECR con `scan_on_push = true`
- **Multi-stage**: Herramientas de build no estan en imagen de produccion

## 8. Orquestacion y Escalabilidad

Se eligio Amazon EKS sobre ECS por las siguientes razones:

- **Control fino**: Kubernetes permite configurar probes, HPA, affinity y tolerations
- **Portabilidad**: Los manifiestos funcionan en cualquier cluster K8s (multi-cloud)
- **Ecosistema**: Amplia comunidad y herramientas (Helm, Prometheus, ArgoCD)
- **Autoescalado**: HPA al 70% de CPU con 2-5 replicas

Frente a un despliegue manual, la orquestacion automatizada ofrece:

- **Auto-recuperacion**: Kubernetes reinicia contenedores fallidos automaticamente
- **Rolling updates**: Actualizaciones sin downtime
- **Balanceo de carga**: Servicios distribuidos entre replicas via ClusterIP/LoadBalancer
- **Escalabilidad horizontal**: Aumento/reduccion de replicas segun demanda

## 9. Observabilidad

- **Logs del pipeline**: GitHub Actions registra cada paso del CI/CD
- **CloudWatch Logs**: Grupo `/eks/innovatech-poc/applications` con 7 dias de retencion
- **Health probes**: Liveness y readiness en todos los deployments
- **Metricas de cluster**: Metrics Server instalado para HPA y kubectl top

## 10. Tests

Total: **24 tests** distribuidos:

| Componente | Tests | Framework |
|------------|-------|-----------|
| back-ventas | 10 | JUnit 5 + Mockito |
| back-despachos | 3 | JUnit 5 + Mockito |
| api-node | 9 | Jest |
| front_despacho | 2 | Vitest + React Testing Library |

## Conclusion

El proyecto implementa un pipeline CI/CD completo que automatiza el ciclo commit → build → test → push ECR → deploy EKS → validacion, siguiendo buenas practicas de contenerizacion, seguridad y orquestacion en la nube AWS.
