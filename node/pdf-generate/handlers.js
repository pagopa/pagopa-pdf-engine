const puppeteer = require('puppeteer');
const path = require('path');
const fs = require('fs');
const readFileSync = require('fs').readFileSync
const rmSync = require('fs').rmSync
const os = require('os');
const {getBrowserSession, closeBrowserSession} = require('./utils/browserManager');
const buildResponseBody = require('./utils/buildUtils');
const multer = require('multer');
const express = require('express');
let handlebars = require("handlebars");
const packageJson = require("../package.json");
var AdmZip = require("adm-zip");
const fse = require('fs-extra');
const appInsights = require("applicationinsights");


const connectionString = process.env.APPLICATIONINSIGHTS_CONNECTION_STRING;


const info = async function (req, res, next) {

    console.log(`INFO : name ${packageJson.name} version ${packageJson.version}`);

    res.send({
        name: packageJson.name,
        version: packageJson.version
    });

}

const shutdown = async function (req, res, server) {
    res.send("Shutdown");
    process.exit(0);
}

function trackCustomEvent(msg) {
    try {
        appInsights.setup(connectionString).start();
        const client = appInsights.defaultClient;
        if (!client) {
            client.trackEvent(msg);
        }
    }
    catch (e) {
        console.error("custom event tracking failed", e);
    }
}

const generatePdf = async function (req, res, next) {


    trackCustomEvent({
        name: "PDF_ENGINE_NODE",
        properties: {
            "type": "PDF_ENGINE_NODE_LOG",
            "title": "Generate PDF NodeJS Function Invoked",
        }
    });

    var workingDir;
    var page;

    let timestampLog = `${Date.now()}`;

    console.time(timestampLog);
    console.info(`Starting generate pdf nodejs function`);

    try {

        try {
            workingDir = fs.mkdtempSync(path.join(os.tmpdir(), 'pdfenginetmp-'));
        } catch (err) {
            trackCustomEvent({
                name: "PDF_ENGINE_NODE",
                properties: {
                    "type": "PDF_ENGINE_NODE_ERROR",
                    "title": "An error occurred on processing the request",
                    "details": "PDFE_908 - Error creating working directory",
                    "cause": err.toString()
                }
            });
            res.status(500);
            res.body(buildResponseBody(500, 'PDFE_908', "An error occurred on processing the request"));
            return;
        }

        console.log(req.file)

        var zip = new AdmZip(req.file.buffer);
        var zipEntries = zip.getEntries();


        for (const zipEntry of zipEntries) {
            if (!zipEntry.entryName.includes("._") && !zipEntry.isDirectory) {
                fse.outputFile(path.join(workingDir, zipEntry.entryName), zipEntry.getData(), err => {
                    if (err) {
                        trackCustomEvent({
                            name: "PDF_ENGINE_NODE",
                            properties: {
                                "type": "PDF_ENGINE_NODE_ERROR",
                                "title": "outputFile error",
                                "details": "An error occurred writing file " + zipEntry.entryName,
                                "cause": err.toString()
                            }
                        });
                        console.error(err);
                    }
                });
            }
        }

        const browser = await getBrowserSession();
        page = await browser.newPage();

        let data = req.body.data;
        let title = req.body.title;
        let renderMode = req.body.renderMode || 'handlebar';

        if (title == undefined) {
            title = "Documento PDF PagoPA";
        }

        if (data == undefined) {
            trackCustomEvent({
                name: "PDF_ENGINE_NODE",
                properties: {
                    "type": "PDF_ENGINE_NODE_ERROR",
                    "title": "Invalid request",
                    "details": "PDFE_898 - Missing data parameter",
                    "cause": err.toString()
                }
            });
            res.status(400);
            res.json(buildResponseBody(400, 'PDFE_898', "Invalid request"));

            return;
        }

        try {

            const jsonData = JSON.parse(data);
            let templateFile = readFileSync(path.join(workingDir, "template.html")).toString();
            let template = handlebars.compile(templateFile);
            jsonData.tempPath = workingDir;
            let html = template(jsonData);
            fs.writeFileSync(path.join(workingDir, "compiledTemplate.html"), html);

        } catch (err) {
            trackCustomEvent({
                name: "PDF_ENGINE_NODE",
                properties: {
                    "type": "PDF_ENGINE_NODE_ERROR",
                    "title": "Error compiling the HTML template",
                    "details": "PDFE_901 - An error occurred compiling the HTML template",
                    "cause": err.toString()
                }
            });
            console.error(err)
            res.status(500);
            res.json(buildResponseBody(400, 'PDFE_901', "Error compiling the HTML template"));

            return;
        }

        try {
            await page.goto('file:' + path.join(workingDir, "compiledTemplate.html"), {
                waitUntil: ['load', 'domcontentloaded']
            });
            // path, can be relative or absolute path
            //await page.addStyleTag({path: path.join(workingDir, "style.css")});
            await waitForRender(page);
            await page.pdf({
                path: path.join(workingDir, "pagopa-receipt.pdf"),
                title: title,
                format: 'A4',
                landscape: false,
                printBackground: true,
            });
        } catch (err) {
            trackCustomEvent({
                name: "PDF_ENGINE_NODE",
                properties: {
                    "type": "PDF_ENGINE_NODE_ERROR",
                    "title": "Error generating the PDF document",
                    "details": "PDFE_902 - An error occurred generating the PDF document",
                    "cause": err.toString()
                }
            });
            console.error(err);
            res.status(500);
            res.json(buildResponseBody(500, 'PDFE_902', "Error generating the PDF document"));

            return;
        }

        trackCustomEvent({
            name: "PDF_ENGINE_NODE",
            properties: {
                "type": "PDF_ENGINE_NODE_LOG",
                "title": "PDF generation process completed",
                "details": "PDF " + title + " generated successfully"
            }
        });
        let content = readFileSync(path.join(workingDir, "pagopa-receipt.pdf"));
        res.setHeader('content-type', 'application/pdf');
        res.send(content);

    } catch (err) {
        trackCustomEvent({
            name: "PDF_ENGINE_NODE",
            properties: {
                "type": "PDF_ENGINE_NODE_ERROR",
                "title": "Error generating the PDF document",
                "details": "PDFE_902 - An error occurred generating the PDF document",
                "cause": err.toString()
            }
        });
        console.error(err);
        res.status(500);
        res.json(buildResponseBody(500, 'PDFE_902', "Error generating the PDF document"));
    } finally {
        if (page) {
            await page.close();
        }

        if (workingDir) {
            rmSync(workingDir, {recursive: true, force: true});
        }

    }

}

const waitForRender = async (page, timeout = 30000) => {
    const checkInterval = process.env.CHECK_SIZE_INTERVAL || 100;
    const maxChecks = timeout / checkInterval;
    let lastSize = 0;
    let checkCounts = 1;
    let countStableSizeIterations = 0;
    const minStableSizeIterations = process.env.MIN_STABLE_SIZE_ITERATIONS || 3;

    while (checkCounts++ <= maxChecks) {
        let html = await page.content();
        let currentSize = html.length;

        if (lastSize != 0 && currentSize == lastSize)
            countStableSizeIterations++;
        else
            countStableSizeIterations = 0;

        if (countStableSizeIterations >= minStableSizeIterations) {
            break;
        }

        lastSize = currentSize;
        await new Promise(r => setTimeout(r, checkInterval))
    }
};

module.exports = {info, generatePdf, shutdown};