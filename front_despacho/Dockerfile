# ── Etapa 1: build ────────────────────────────────────────────────────────────
FROM node:20-alpine AS build

WORKDIR /app
COPY package*.json ./
    RUN npm install
COPY . .
RUN npm run build

# ── Etapa 2: producción ────────────────────────────────────────────────────────
FROM nginx:alpine

# Copia el build estático
COPY --from=build /app/dist /usr/share/nginx/html

# Copia la configuración de Nginx con reverse proxy
COPY nginx.conf /etc/nginx/conf.d/default.conf

RUN chown -R nginx:nginx /usr/share/nginx/html && \
    chmod -R 755 /usr/share/nginx/html

USER nginx

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]