const puppeteer = require('puppeteer');
const path = require('path');
const fs = require('fs').promises;
const createReadStream = require('fs').createReadStream
const readFileSync = require('fs').readFileSync
const os = require('os');
const unzipper = require('unzipper');
const getPage = require('./browserManager');
const getBrowserSession = require('./browserManager');
const buildResponseBody = require('./utils');
const { default: parseMultipartFormData } = require('@anzp/azure-function-multipart');
var handlebars = require("handlebars");


module.exports = async function (context, request) {

    var workingDir;

    try {
        workingDir = await fs.mkdtemp(path.join(os.tmpdir(), 'pdfenginetmp-'));
    } catch (err) {
        return buildResponseBody(500, 'PDFE_908', "An error occurred on processing the request");
    }   
    
    const { fields, files } = await parseMultipartFormData(request);

    await fs.writeFile(path.join(workingDir,"zippedFile.zip"), files[0].bufferFile,  "binary");

    createReadStream(path.join(workingDir, "zippedFile.zip"))
        .pipe(unzipper.Extract({ path: workingDir }));

    const browser = await getBrowserSession();
    const page = await browser.newPage();

    var data;
    for (var field in fields) {
        if (field.name == "data") {
            data = JSON.parse(field.value);
        }
    }
    var templateFile = readFileSync(path.join(workingDir,"template.html")).toString();
    var template = handlebars.compile( templateFile );
    var html = template(data);

    await fs.writeFile(path.join(workingDir,"compiledTemplate.html"), html);

    await page.goto('file:'+path.join(workingDir,"compiledTemplate.html"));
    await page.pdf({
       path: path.join(workingDir,"pagopa-receipt.pdf"),
       format: 'A4',
       landscape: false,
       printBackground: true,
    });

    await page.close();

    var content = readFileSync(path.join(workingDir,"pagopa-receipt.pdf"));

    context.res = { body: content };

};
