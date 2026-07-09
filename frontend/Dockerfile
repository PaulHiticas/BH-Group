# ---------- Dependencies ----------
FROM node:22-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

# ---------- Build ----------
FROM node:22-alpine AS build
WORKDIR /app
ARG NEXT_PUBLIC_API_BASE_URL
ENV NEXT_PUBLIC_API_BASE_URL=${NEXT_PUBLIC_API_BASE_URL}
COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN npm run build

# ---------- Runtime ----------
FROM node:22-alpine AS runtime
WORKDIR /app
ENV NODE_ENV=production
RUN addgroup -S bhgroup && adduser -S bhgroup -G bhgroup

COPY --from=build /app/public ./public
COPY --from=build --chown=bhgroup:bhgroup /app/.next/standalone ./
COPY --from=build --chown=bhgroup:bhgroup /app/.next/static ./.next/static

USER bhgroup
EXPOSE 3000
ENV PORT=3000
CMD ["node", "server.js"]
