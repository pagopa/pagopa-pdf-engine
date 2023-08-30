const packageJson = require("../package.json");

const InfoHandler = async function (context, req) {

    context.res = {
        body: {
            name: packageJson.name,
            version: packageJson.version
        }
    };
}

module.exports = InfoHandler;