const buildResponseBody = (statusCode, appStatusCode, errorMessage) => {
    return {
        statusCode: statusCode,
        body: {
            status: statusCode,
            appStatusCode: appStatusCode,
            errors: [errorMessage]
        }
    }
}

module.exports = buildResponseBody;