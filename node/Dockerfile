FROM mcr.microsoft.com/azure-functions/node:4-node20

ENV AzureWebJobsScriptRoot=/home/site/wwwroot \
    AzureFunctionsJobHost__Logging__Console__IsEnabled=true

ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=true

COPY . /home/site/wwwroot

RUN cd /home/site/wwwroot && \
    npm install && \
    npx playwright install chromium