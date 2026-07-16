# ── Etapa 1: instalacion de dependencias ────────────────────────────────────────
FROM node:20-alpine AS build

WORKDIR /app

COPY package*.json ./
RUN npm ci

# ── Etapa 2: produccion ─────────────────────────────────────────────────────────
FROM node:20-alpine

WORKDIR /app

RUN addgroup -S innovatech && adduser -S innovatech -G innovatech

COPY --from=build /app/node_modules ./node_modules
COPY . .

RUN chown -R innovatech:innovatech /app

USER innovatech

EXPOSE 3000

CMD ["node", "src/index.js"]
