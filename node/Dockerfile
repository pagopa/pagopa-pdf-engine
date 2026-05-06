# Pinned by digest tag: node:24-bookworm
ARG NODE_VERSION=sha256:e9891237dfbb1de60ce19e9ff9fac5d73ad9c37da303ad72ff2a425ad1057e71
ARG OWNER=pagopa
ARG REPO=repo

FROM node@$NODE_VERSION AS builder

WORKDIR /usr/src/build

# Enable Yarn with package.json version
RUN corepack enable

# Copy manifest before to enhance layer caching
COPY package.json yarn.lock ./

# Install only production dependencies (no devDependencies) and freeze lockfile to ensure reproducible builds
RUN yarn install --frozen-lockfile --production

# Copy source code to the build stage
COPY . .

FROM node@$NODE_VERSION
LABEL maintainer="https://pagopa.it"
LABEL org.opencontainers.image.source="https://github.com/pagopa/pagopa-pdf-engine"

# RUN addgroup -S nonroot \
#     && adduser -S nonroot -G nonroot

WORKDIR /usr/src/app

COPY ./package.json /usr/src/app/package.json
COPY --from=builder /usr/src/build/pdf-generate /usr/src/app/pdf-generate
COPY --from=builder /usr/src/build/node_modules /usr/src/app/node_modules

# Chrome headless dependencies (Debian 12 / bookworm)
# https://pptr.dev/troubleshooting#chrome-doesnt-launch-on-linux
RUN apt-get -qq update && apt-get install -y --no-install-recommends \
    ca-certificates fonts-liberation wget xdg-utils lsb-release xvfb \
    libasound2 libatk-bridge2.0-0 libatk1.0-0 libc6 libcairo2 libcups2 \
    libdbus-1-3 libexpat1 libfontconfig1 libgbm1 libgcc-s1 libgdk-pixbuf-2.0-0 \
    libglib2.0-0 libgtk-3-0 libnspr4 libnss3 libpango-1.0-0 libpangocairo-1.0-0 \
    libstdc++6 libx11-6 libx11-xcb1 libxcb1 libxcomposite1 libxcursor1 \
    libxdamage1 libxext6 libxfixes3 libxi6 libxrandr2 libxrender1 libxss1 libxtst6 \
    && rm -rf /var/lib/apt/lists/*

# USER nonroot

EXPOSE 3000
# ENV PORT 9090
ENTRYPOINT ["node", "/usr/src/app/pdf-generate/index.js"]
