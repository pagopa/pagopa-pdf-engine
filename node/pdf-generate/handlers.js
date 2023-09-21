const puppeteer = require('puppeteer');
const path = require('path');
const fs = require('fs').promises;
const readFileSync = require('fs').readFileSync
const rmSync = require('fs').rmSync
const os = require('os');
const getPage = require('./utils/browserManager');
const getBrowserSession = require('./utils/browserManager');
const buildResponseBody = require('./utils/buildUtils');
const multer = require('multer');
const express = require('express');
let handlebars = require("handlebars");
const packageJson = require("../package.json");

const info = async function (req, res, next) {

    res.send({
        name: packageJson.name,
        version: packageJson.version
    });

}

const generatePdf = async function (req, res, next) {
    let timestampLog = `${Date.now()}`;

    console.time(timestampLog);
    console.info(`Starting generate pdf nodejs function`);

    let workingDir = req.body.workingDir;
    let page;

    try {

        console.info(`Initiating browser session after ${console.timeLog(timestampLog)}`);
        console.time("browserSession-"+timestampLog);
        const browser = await getBrowserSession();
        console.info(`TIME to initiate browser session: ${console.timeEnd("browserSession-"+timestampLog)}`);

        console.info(`Initiating browser new page after ${console.timeLog(timestampLog)}`);
        console.time("browserPage-"+timestampLog);
        page = await browser.newPage();
        console.info(`TIME to initiate browser page: ${console.timeEnd("browserPage-"+timestampLog)}`);

        let data = req.body.data;

        if (data == undefined) {
            res.status(400);
            res.json(buildResponseBody(400, 'PDFE_898', "Invalid request"));

            return;
        }

        try {
            console.info(`Reading template from memory after ${console.timeLog(timestampLog)}`);
            console.time("templateRead-"+timestampLog);
            let templateFile = readFileSync(path.join(workingDir, "template.html")).toString();
            console.info(`TIME to read template from memory: ${console.timeEnd("templateRead-"+timestampLog)}`);

            console.info(`Compiling template with handlebars after ${console.timeLog(timestampLog)}`);
            console.time("templateHandlebars-"+timestampLog);
            let template = handlebars.compile(templateFile);
            console.info(`TIME to compile template with handlebars: ${console.timeEnd("templateHandlebars-"+timestampLog)}`);

            console.info(`Filling template with json data after ${console.timeLog(timestampLog)}`);
            console.time("templateData-"+timestampLog);
            let html = template(data);
            console.info(`TIME to fill template with json data: ${console.timeEnd("templateData-"+timestampLog)}`);

            console.info(`Writing compiled template to memory after ${console.timeLog(timestampLog)}`);
            console.time("templateWrite-"+timestampLog);
            await fs.writeFile(path.join(workingDir, "compiledTemplate.html"), html);
            console.info(`TIME to write compiled template to memory: ${console.timeEnd("templateWrite-"+timestampLog)}`);
        } catch (err) {
            console.log(err)
            res.status(500);
            res.json(buildResponseBody(400, 'PDFE_901', "Error compiling the HTML template"));

            return;
        }

        try {
            console.info(`Opening html with browser page after ${console.timeLog(timestampLog)}`);
            console.time("htmlOpen-"+timestampLog);
            await page.goto('file:' + path.join(workingDir, "compiledTemplate.html"));
            console.info(`TIME to open html with browser page: ${console.timeEnd("htmlOpen-"+timestampLog)}`);

            console.info(`Generating pdf through browser page after ${console.timeLog(timestampLog)}`);
            console.time("pdfGenerate-"+timestampLog);
            await page.pdf({
                path: path.join(workingDir, "pagopa-receipt.pdf"),
                format: 'A4',
                landscape: false,
                printBackground: true,
            });
            console.info(`TIME to generate pdf through browser page: ${console.timeEnd("pdfGenerate-"+timestampLog)}`);
        } catch (err) {
            console.log(err);
            res.status(500);
            res.json(buildResponseBody(500, 'PDFE_902', "Error generating the PDF document"));

            return;
        }


        console.info(`Reading pdf from memory after ${console.timeLog(timestampLog)}`);
        console.time("pdfRead-"+timestampLog);
        let content = readFileSync(path.join(workingDir, "pagopa-receipt.pdf"));
        console.info(`TIME to read pdf from memory: ${console.timeEnd("pdfRead-"+timestampLog)}`);

        console.info(`Sending pdf as response after ${console.timeLog(timestampLog)}`);
        console.time("pdfSend-"+timestampLog);
        res.send(content);
        console.info(`TIME to send pdf as response: ${console.timeEnd("pdfSend-"+timestampLog)}`);

    } catch (err) {
        res.status(500);
        res.json(buildResponseBody(500, 'PDFE_902', "Error generating the PDF document"));
    } finally {
        if (page) {
            console.info(`Closing browser page after ${console.timeLog(timestampLog)}`);
            console.time("pageClose-"+timestampLog);
            await page.close();
            console.info(`TIME to close browser page: ${console.timeEnd("pageClose-"+timestampLog)}`);
        }

        console.info(`Ending generate pdf nodejs function after ${console.timeEnd(timestampLog)}`);
    }

}

module.exports = { info, generatePdf };