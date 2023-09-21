const puppeteer = require('puppeteer');
const path = require('path');
const fs = require('fs').promises;
const readFileSync = require('fs').readFileSync
const rmSync = require('fs').rmSync
const os = require('os');
const {getBrowserSession, getPage} = require('./utils/browserManager');
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
        console.timeLog(timestampLog, "At initiating browser session");
        console.time("browserSession-"+timestampLog);
        const browser = await getBrowserSession();
        console.timeEnd("browserSession-"+timestampLog, "TIME to initiate browser session");

        console.timeLog(timestampLog, "At initiating browser new page");
        console.time("browserPage-"+timestampLog);
        page = await getPage();
        console.timeEnd("browserPage-"+timestampLog, "TIME to initiate browser page");
        console.timeEnd("browserPage-"+timestampLog, "TIME to initiate browser page");

        let data = req.body.data;

        if (data == undefined) {
            res.status(400);
            res.json(buildResponseBody(400, 'PDFE_898', "Invalid request"));

            return;
        }

        let html;

        try {
            console.timeLog(timestampLog, "At reading template from memory after");
            console.time("templateRead-"+timestampLog);
            let templateFile = readFileSync(path.join(workingDir, "template.html")).toString();
            console.timeEnd("templateRead-"+timestampLog, "TIME to read template from memory");

            console.timeLog(timestampLog, "At compiling template with handlebars");
            console.time("templateHandlebars-"+timestampLog);
            let template = handlebars.compile(templateFile);
            console.timeEnd("templateHandlebars-"+timestampLog, "TIME to compile template with handlebars");

            console.timeLog(timestampLog, "At filling template with json data");
            console.time("templateData-"+timestampLog);
            html = template(data);
            console.timeEnd("templateData-"+timestampLog, "TIME to fill template with json data");

            console.timeLog(timestampLog, "At writing compiled template to memory");
            console.time("templateWrite-"+timestampLog);
            await fs.writeFile(path.join(workingDir, "compiledTemplate.html"), html);
            console.timeEnd("templateWrite-"+timestampLog, "TIME to write compiled template to memory");
        } catch (err) {
            console.log(err)
            res.status(500);
            res.json(buildResponseBody(400, 'PDFE_901', "Error compiling the HTML template"));

            return;
        }

        try {
            console.timeLog(timestampLog, "At opening html with browser page")
            console.time("htmlOpen-"+timestampLog);
            await page.setContent(html);
            console.timeEnd("htmlOpen-"+timestampLog, "TIME to open html with browser page");

            console.timeLog(timestampLog, "At generating pdf through browser page");
            console.time("pdfGenerate-"+timestampLog);
            console.timeEnd("htmlOpen-"+timestampLog, "TIME to open html with browser page");

            console.timeLog(timestampLog, "At generating pdf through browser page");
            console.time("pdfGenerate-"+timestampLog);
            await page.pdf({
                path: path.join(workingDir, "pagopa-receipt.pdf"),
                format: 'A4',
                landscape: false,
                printBackground: true,
            });
            console.timeEnd("pdfGenerate-"+timestampLog, "TIME to generate pdf through browser page");
        } catch (err) {
            console.log(err);
            res.status(500);
            res.json(buildResponseBody(500, 'PDFE_902', "Error generating the PDF document"));

            return;
        }

        console.timeLog(timestampLog, "At reading pdf from memory");
        console.time("pdfRead-"+timestampLog);
        let content = readFileSync(path.join(workingDir, "pagopa-receipt.pdf"));
        console.timeEnd("pdfRead-"+timestampLog, "TIME to read pdf from memory");

        console.timeLog(timestampLog, "At sending pdf as response");
        console.time("pdfSend-"+timestampLog);
        res.send(content);
        console.timeEnd("pdfSend-"+timestampLog, "TIME to send pdf as response");

    } catch (err) {
        res.status(500);
        res.json(buildResponseBody(500, 'PDFE_902', "Error generating the PDF document"));
    } finally {
        if (page) {
            console.timeLog(timestampLog, "At closing browser page");
            console.time("pageClose-"+timestampLog);
            await page.close();
            console.timeEnd("pageClose-"+timestampLog, "TIME to close browser page");
        }

        console.timeEnd(timestampLog, "At ending generate pdf nodejs function");
    }

}

module.exports = { info, generatePdf };