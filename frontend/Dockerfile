# syntax=docker/dockerfile:1
# Angular 21 console → nginx static

FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
ARG API_BASE_URL=https://notification-api.optimizesolux.com
ARG KEYCLOAK_URL=https://auth.optimizesolux.com
ENV API_BASE_URL=$API_BASE_URL
ENV KEYCLOAK_URL=$KEYCLOAK_URL
RUN node <<'EOF'
const fs = require('fs');
const api = process.env.API_BASE_URL || 'https://notification-api.optimizesolux.com';
const auth = process.env.KEYCLOAK_URL || 'https://auth.optimizesolux.com';
const content = `export const environment = {
  production: true,
  apiBaseUrl: '${api}',
  auth: {
    enabled: true,
    url: '${auth}',
    realm: 'notification-hub',
    clientId: 'notification-hub-console',
  },
};
`;
fs.writeFileSync('src/environments/environment.prod.ts', content);
fs.writeFileSync('src/environments/environment.ts', content);
console.log('Baked environment:', { api, auth });
EOF
RUN npm run build -- --configuration=production

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /tmp/dist
RUN set -eux; \
    idx="$(find /tmp/dist -type f -name index.html | head -n 1)"; \
    test -n "$idx"; \
    cp -a "$(dirname "$idx")/." /usr/share/nginx/html/; \
    rm -rf /tmp/dist
EXPOSE 80
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget -qO- http://127.0.0.1/ || exit 1
