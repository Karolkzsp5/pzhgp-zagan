# Etap 1: instalacja zależności i budowanie aplikacji Next.js
FROM node:26-slim AS build
WORKDIR /app
# Program Cypress (testy end-to-end) nie jest potrzebny w obrazie
ENV CYPRESS_INSTALL_BINARY=0 \
    NEXT_TELEMETRY_DISABLED=1
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
# Zmienne NEXT_PUBLIC_* trafiają do kodu dla przeglądarki w czasie budowania, dlatego są argumentami obrazu
ARG NEXT_PUBLIC_API_URL=http://localhost:8080
ARG NEXT_PUBLIC_TURNSTILE_SITE_KEY=1x00000000000000000000AA
ENV NEXT_PUBLIC_API_URL=$NEXT_PUBLIC_API_URL \
    NEXT_PUBLIC_TURNSTILE_SITE_KEY=$NEXT_PUBLIC_TURNSTILE_SITE_KEY \
    NEXT_STANDALONE_OUTPUT=true
RUN npm run build

# Etap 2: uruchomienie samodzielnego serwera Next.js (.next/standalone) bez pełnego node_modules
FROM node:26-slim
WORKDIR /app
ENV NODE_ENV=production \
    NEXT_TELEMETRY_DISABLED=1 \
    PORT=3000 \
    HOSTNAME=0.0.0.0
COPY --from=build --chown=node:node /app/.next/standalone ./
COPY --from=build --chown=node:node /app/.next/static ./.next/static
USER node
EXPOSE 3000
CMD ["node", "server.js"]
