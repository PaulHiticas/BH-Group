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
ARG NEXT_PUBLIC_SITE_URL
ENV NEXT_PUBLIC_SITE_URL=${NEXT_PUBLIC_SITE_URL}
ARG NEXT_PUBLIC_GOOGLE_MAPS_API_KEY
ENV NEXT_PUBLIC_GOOGLE_MAPS_API_KEY=${NEXT_PUBLIC_GOOGLE_MAPS_API_KEY}
ARG NEXT_PUBLIC_COMPANY_EMAIL
ENV NEXT_PUBLIC_COMPANY_EMAIL=${NEXT_PUBLIC_COMPANY_EMAIL}
ARG NEXT_PUBLIC_COMPANY_PHONE
ENV NEXT_PUBLIC_COMPANY_PHONE=${NEXT_PUBLIC_COMPANY_PHONE}
COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN npm run build

# ---------- Runtime ----------
FROM node:22-alpine AS runtime
WORKDIR /app
ENV NODE_ENV=production
RUN addgroup -S bhstays && adduser -S bhstays -G bhstays

COPY --from=build /app/public ./public
COPY --from=build --chown=bhstays:bhstays /app/.next/standalone ./
COPY --from=build --chown=bhstays:bhstays /app/.next/static ./.next/static

# Next 15 pins a vulnerable PostCSS release for its build pipeline. CSS is
# already compiled in the build stage, so keep that build-only package out of
# the production filesystem while the compatible Next 15 line remains pinned.
# The container starts with `node server.js`; npm/npx are build tooling and
# must not carry their dependency tree into production either.
RUN rm -rf /app/node_modules/next/node_modules/postcss \
    /usr/local/lib/node_modules/npm \
    /usr/local/bin/npm \
    /usr/local/bin/npx

USER bhstays
EXPOSE 3000
ENV PORT=3000
CMD ["node", "server.js"]
