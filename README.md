# Innovatech Chile — DevOps EV3

Guía rápida para la evaluación. Despliegue de aplicación full-stack en **Amazon EKS** con pipeline **CI/CD** automatizado.

---

## 1. Levantar Infraestructura (Terraform)
Crea la red (VPC), clúster EKS y repositorios ECR.

```bash
terraform init
terraform apply --auto-approve
```
*(Tiempo estimado: ~15 minutos)*

Conectar tu terminal al clúster recién creado:
```bash
aws eks update-kubeconfig --name innovatech-cluster --region us-east-1
kubectl get nodes
```

---

## 2. Actualizar Credenciales de AWS
Las credenciales de AWS Academy expiran cada 4 horas. Antes de ejecutar el pipeline, actualízalas en GitHub:
1. Ve a **Settings > Secrets and variables > Actions** en tu repositorio.
2. Actualiza los siguientes secretos con los datos de tu *AWS Details*:
   - `AWS_ACCESS_KEY_ID`
   - `AWS_SECRET_ACCESS_KEY`
   - `AWS_SESSION_TOKEN`
   - `AWS_ACCOUNT_ID` (Tu ID de cuenta de 12 dígitos)

---

## 3. Ejecutar Pipeline CI/CD (Despliegue)
El pipeline compila el código, crea imágenes Docker, las sube a ECR y despliega en Kubernetes.

1. Ve a la pestaña **Actions** en GitHub.
2. Selecciona **Deploy Innovatech — EKS** en el menú izquierdo.
3. Haz clic en **Run workflow** -> selecciona la rama `deploy` -> **Run workflow**.

*(Tiempo estimado: ~10 minutos)*

---

## 4. Verificar Despliegue
Una vez que el pipeline termine con éxito, ejecuta:

```bash
# Ver que todos los servicios estén corriendo (Running)
kubectl get pods,svc -n innovatech

# Obtener la URL pública (Load Balancer) del Frontend para probar en el navegador
kubectl get svc frontend -n innovatech -o jsonpath='{.status.loadBalancer.ingress[0].hostname}'
```

---

## 5. Análisis del Pipeline (Evaluación y Mejora)
Para la parte teórica de la evaluación, los tiempos, optimizaciones implementadas y oportunidades de mejora están documentados en:
**[Ver Documento de Análisis (docs/PIPELINE-ANALISIS.md)](docs/PIPELINE-ANALISIS.md)**

---

## 6. Destruir Infraestructura (Al finalizar)
Para no consumir más créditos de AWS Academy:
```bash
terraform destroy --auto-approve
```