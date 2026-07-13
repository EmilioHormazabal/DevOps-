#!/usr/bin/env bash
# deploy-evaluacion.sh — Innovatech Chile EV3
# Uso:
#   ./scripts/deploy-evaluacion.sh deploy
#   ./scripts/deploy-evaluacion.sh destroy
#   ./scripts/deploy-evaluacion.sh status

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

AWS_REGION="${AWS_REGION:-us-east-1}"
PROJECT_NAME="${PROJECT_NAME:-innovatech-poc}"
CLUSTER_NAME="${CLUSTER_NAME:-innovatech-cluster}"
NAMESPACE="${NAMESPACE:-innovatech}"

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'
log()  { echo -e "${GREEN}[+]${NC} $*"; }
warn() { echo -e "${YELLOW}[!]${NC} $*"; }
err()  { echo -e "${RED}[ERROR]${NC} $*" >&2; }

need_cmd() { command -v "$1" >/dev/null || { err "Falta: $1"; exit 1; }; }

check_prereqs() {
  need_cmd aws
  need_cmd terraform
  need_cmd docker
  aws sts get-caller-identity --region "${AWS_REGION}" >/dev/null
  log "Cuenta: $(aws sts get-caller-identity --query Account --output text)"
}

tf_apply() {
  terraform init -input=false
  terraform apply -auto-approve
}

tf_destroy() {
  terraform init -input=false
  terraform destroy -auto-approve
}

ecr_login_build_push() {
  local account registry
  account="$(aws sts get-caller-identity --query Account --output text)"
  registry="${account}.dkr.ecr.${AWS_REGION}.amazonaws.com"
  aws ecr get-login-password --region "${AWS_REGION}" | docker login --username AWS --password-stdin "${registry}"

  for svc in frontend back-ventas back-despachos api-node; do
    log "Build ${svc}..."
    docker build --platform linux/amd64 -t "${registry}/${PROJECT_NAME}-${svc}:latest" "${ROOT_DIR}/${svc}"
    docker push "${registry}/${PROJECT_NAME}-${svc}:latest"
  done
}

ecs_redeploy() {
  "${SCRIPT_DIR}/deploy-k8s.sh"
}

cmd_deploy() {
  check_prereqs
  log "=== Terraform Apply ==="
  (cd "${ROOT_DIR}" && tf_apply)
  log "=== Imagenes + EKS ==="
  ecr_login_build_push
  ecs_redeploy
  log "URL aplicacion: http://$(kubectl get svc frontend -n ${NAMESPACE} -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')"
}

cmd_destroy() {
  check_prereqs
  log "Limpiando Kubernetes..."
  kubectl delete namespace "${NAMESPACE}" --ignore-not-found=true --wait=false 2>/dev/null || true
  log "Destruyendo infraestructura..."
  (cd "${ROOT_DIR}" && tf_destroy)
  log "Infraestructura eliminada."
}

cmd_status() {
  check_prereqs
  echo "=== Servicios EKS ==="
  kubectl get pods,svc -n "${NAMESPACE}" -o wide 2>/dev/null || echo "Namespace ${NAMESPACE} no encontrado"
  echo ""
  echo "=== HPA ==="
  kubectl get hpa -n "${NAMESPACE}" 2>/dev/null || echo "No hay HPA"
  echo ""
  echo "=== Load Balancer ==="
  kubectl get svc frontend -n "${NAMESPACE}" -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || echo "No disponible"
}

case "${1:-}" in
  deploy)  cmd_deploy ;;
  destroy) cmd_destroy ;;
  status)  cmd_status ;;
  *) echo "Uso: $0 {deploy|destroy|status}"; exit 1 ;;
esac
