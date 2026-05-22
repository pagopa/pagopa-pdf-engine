# Pinned by digest tag: node:24-bookworm
ARG NODE_VERSION=sha256:e9891237dfbb1de60ce19e9ff9fac5d73ad9c37da303ad72ff2a425ad1057e71

FROM node@$NODE_VERSION AS builder

WORKDIR /usr/src/build

# Store Puppeteer's Chromium inside the project, so it is propagated
# to the runtime stage with the rest of the app (since Puppeteer >= 19
# the browser lives in ~/.cache/puppeteer, NOT in node_modules/puppeteer).
ENV PUPPETEER_CACHE_DIR=/usr/src/build/.cache/puppeteer

# --- Performance pinning ---------------------------------------------------
# Puppeteer 25.x bundles Chrome 138+, which exhibits a measurable PDF
# generation slowdown under load compared to Chrome 127 (last version
# bundled with puppeteer 22.x, used by app v2.10.23 baseline).
# We therefore:
#   1) skip the automatic Chrome download during `yarn install`;
#   2) install the older Chrome 127.0.6533.88 explicitly via @puppeteer/browsers
#      into the same PUPPETEER_CACHE_DIR;
#   3) tell Puppeteer to use that binary at runtime (PUPPETEER_EXECUTABLE_PATH
#      is set on the runtime stage below).
# Change PINNED_CHROME_VERSION to override.
ENV PUPPETEER_SKIP_DOWNLOAD=true
ARG PINNED_CHROME_VERSION=127.0.6533.88
ENV PINNED_CHROME_VERSION=${PINNED_CHROME_VERSION}
# ---------------------------------------------------------------------------

# Enable Yarn with package.json version
RUN corepack enable

# Copy manifest before to enhance layer caching
COPY package.json yarn.lock ./

# Install only production dependencies (no devDependencies) and freeze lockfile to ensure reproducible builds
RUN yarn install --frozen-lockfile --production

# Install the pinned Chrome version into PUPPETEER_CACHE_DIR.
# @puppeteer/browsers is already a transitive dependency of puppeteer.
RUN npx --yes @puppeteer/browsers install chrome@${PINNED_CHROME_VERSION} \
        --path ${PUPPETEER_CACHE_DIR} \
    && ls -la ${PUPPETEER_CACHE_DIR}/chrome/

# Copy source code to the build stage
COPY . .

FROM node@$NODE_VERSION
LABEL maintainer="https://pagopa.it"
LABEL org.opencontainers.image.source="https://github.com/pagopa/pagopa-pdf-engine"

# RUN addgroup -S nonroot \
#     && adduser -S nonroot -G nonroot

WORKDIR /usr/src/app

# Tell Puppeteer where to find the Chromium copied from the builder stage.
ENV PUPPETEER_CACHE_DIR=/usr/src/app/.cache/puppeteer

# Pin runtime to the legacy Chrome installed in the builder stage.
# The exact path depends on the version installed above.
ARG PINNED_CHROME_VERSION=127.0.6533.88
ENV PUPPETEER_EXECUTABLE_PATH=/usr/src/app/.cache/puppeteer/chrome/linux-${PINNED_CHROME_VERSION}/chrome-linux64/chrome

COPY ./package.json /usr/src/app/package.json
COPY --from=builder /usr/src/build/pdf-generate /usr/src/app/pdf-generate
COPY --from=builder /usr/src/build/node_modules /usr/src/app/node_modules
COPY --from=builder /usr/src/build/.cache/puppeteer /usr/src/app/.cache/puppeteer

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
