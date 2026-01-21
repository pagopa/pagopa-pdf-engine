const appInsights = require("applicationinsights");

const connectionString = process.env.APPLICATIONINSIGHTS_CONNECTION_STRING;

if (connectionString) {
    try {
        appInsights.setup(connectionString).start();
    } catch (e) {
        console.error("Failed to start Application Insights", e);
    }
}

module.exports = appInsights.defaultClient;