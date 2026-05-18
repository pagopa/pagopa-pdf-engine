# Bump Node minor version: puppeteer 24 requires Node >= 18.18.
# We stay on the Node 18 line to avoid contaminating the puppeteer
# test with a major Node/V8 change.
ARG NODE_VERSION=18.20.4
ARG OWNER=pagopa
ARG REPO=repo

FROM node:$NODE_VERSION as builder

WORKDIR /usr/src/build

COPY . /usr/src/build

# Puppeteer 24 no longer auto-downloads Chromium reliably in all
# COPY-based builds. Run the official install script so the bundled
# Chrome binary lands inside the project's puppeteer cache.
ENV PUPPETEER_CACHE_DIR=/usr/src/build/.cache/puppeteer
RUN npm install \
    && npx puppeteer browsers install chrome

FROM node:$NODE_VERSION
LABEL maintainer="https://pagopa.it"
LABEL org.opencontainers.image.source https://github.com/$OWNER/$REPO

# RUN addgroup -S nonroot \
#     && adduser -S nonroot -G nonroot

WORKDIR /usr/src/app

COPY ./package.json /usr/src/app/package.json
COPY --from=builder /usr/src/build/pdf-generate /usr/src/app/pdf-generate
COPY --from=builder /usr/src/build/node_modules /usr/src/app/node_modules
# Copy the Chromium cache produced by `puppeteer browsers install`.
COPY --from=builder /usr/src/build/.cache/puppeteer /usr/src/app/.cache/puppeteer
ENV PUPPETEER_CACHE_DIR=/usr/src/app/.cache/puppeteer

RUN apt-get -qq update

# Chrome headless dependencies
# https://github.com/puppeteer/puppeteer/blob/main/docs/troubleshooting.md#chrome-headless-doesnt-launch-on-unix
RUN apt-get install -y xvfb gconf-service libasound2 libatk1.0-0 libc6 libcairo2 libcups2 \
    libdbus-1-3 libexpat1 libfontconfig1 libgbm1 libgcc1 libgconf-2-4 libgdk-pixbuf2.0-0 libglib2.0-0 \
    libgtk-3-0 libnspr4 libpango-1.0-0 libpangocairo-1.0-0 libstdc++6 libx11-6 libx11-xcb1 libxcb1 \
    libxcomposite1 libxcursor1 libxdamage1 libxext6 libxfixes3 libxi6 libxrandr2 libxrender1 libxss1 \
    libxtst6 ca-certificates fonts-liberation libappindicator1 libnss3 lsb-release xdg-utils wget


# Remove the legacy puppeteer postinstall step (no longer needed with v24).
# RUN cd /usr/src/app/node_modules/puppeteer && npm install

EXPOSE 3000
# ENV PORT 9090
ENTRYPOINT ["node", "/usr/src/app/pdf-generate/index.js"]
