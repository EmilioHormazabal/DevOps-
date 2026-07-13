# Git Flow — Innovatech Chile EV3

## Ramas principales

- **main**: versión estable lista para producción.
- **develop**: rama de integración donde convergen features y fixes.
- **deploy**: rama que dispara el despliegue automático en AWS EKS (solo merges controlados).

## Ramas de trabajo

- **feature/nombre**: nueva funcionalidad.
- **fix/nombre**: corrección de bugs.

## Convención de commits

Mensajes en español, imperativo y descriptivos:

- `Agrega Dockerfile multi-stage para backend ventas`
- `Configura pipeline de despliegue en rama deploy`
- `Corrige healthcheck en docker-compose`

## Flujo de trabajo

1. Crear rama desde `develop`: `git checkout -b feature/mi-cambio develop`
2. Commits incrementales con mensajes claros
3. Pull Request hacia `develop`
4. Al validar CI, merge a `develop`
5. Cuando esté listo para producción: PR `develop` → `main`
6. Para desplegar: merge `main` → `deploy` (dispara GitHub Actions deploy.yml)
