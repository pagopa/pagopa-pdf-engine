const puppeteer = require('puppeteer');
const path = require('path');
const fs = require('fs').promises;
const createReadStream = require('fs').createReadStream
const readFileSync = require('fs').readFileSync
const rmSync = require('fs').rmSync
const os = require('os');
const unzipper = require('unzipper');
const getPage = require('./browserManager');
const getBrowserSession = require('./browserManager');
const buildResponseBody = require('./utils');
const { default: parseMultipartFormData } = require('@anzp/azure-function-multipart');
var handlebars = require("handlebars");

const generateFunction =
    async function (context, request) {

        var workingDir;
        var page;

        try {

            try {
                workingDir = await fs.mkdtemp(path.join(os.tmpdir(), 'pdfenginetmp-'));
            } catch (err) {
                context.res = {
                    statusCode: 500,
                    body: buildResponseBody(500, 'PDFE_908', "An error occurred on processing the request")
                }
                return;
            }

            const { fields, files } = await parseMultipartFormData(request);

            await fs.writeFile(path.join(workingDir,"zippedFile.zip"), files[0].bufferFile,  "binary");

            createReadStream(path.join(workingDir, "zippedFile.zip"))
                .pipe(unzipper.Extract({ path: workingDir }));

            const browser = await getBrowserSession();
            page = await browser.newPage();

            var data;
            try {
                for (var field in fields) {
                    if (field.name == "data") {
                        data = JSON.parse(field.value);
                    }
                }
            } catch (err) {
                context.res = {
                    statusCode: 400,
                    body: buildResponseBody(500, 'PDFE_707', "Error parsing PDF document input data from output stream")
                }
                return;
            }

            if (data == undefined) {
                context.res = {
                    statusCode: 400,
                    body: buildResponseBody(400, 'PDFE_898', "Invalid request")
                }
                return;
            }

            try {
                var templateFile = readFileSync(path.join(workingDir,"template.html")).toString();
                var template = handlebars.compile( templateFile );
                var html = template(data);
                await fs.writeFile(path.join(workingDir,"compiledTemplate.html"), html);
            } catch (err) {
                context.res = {
                    statusCode: 500,
                    body: buildResponseBody(400, 'PDFE_901', "Error compiling the HTML template")
                }
                return;
            }

            try {
                await page.goto('file:'+path.join(workingDir,"compiledTemplate.html"));
                await page.pdf({
                    path: path.join(workingDir,"pagopa-receipt.pdf"),
                    format: 'A4',
                    landscape: false,
                    printBackground: true,
                });
            } catch (err) {
                context.res = {
                    statusCode: 500,
                    body: buildResponseBody(500, 'PDFE_902', "Error generating the PDF document")
                }
                return;
            }

            var content = readFileSync(path.join(workingDir,"pagopa-receipt.pdf"));

            context.res = { body: content };

        } finally {
            if (workingDir) {
                rmSync(workingDir, { recursive: true, force: true });
            }
            if (page) {
                await page.close();
            }
        }

    };

module.exports = generateFunction;