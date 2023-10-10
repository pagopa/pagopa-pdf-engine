const puppeteer = require('puppeteer');
const path = require('path');
const fs = require('fs');
const readFileSync = require('fs').readFileSync
const rmSync = require('fs').rmSync
const os = require('os');
const { getBrowserSession, closeBrowserSession } = require('./utils/browserManager');
const buildResponseBody = require('./utils/buildUtils');
const multer = require('multer');
const express = require('express');
let handlebars = require("handlebars");
const packageJson = require("../package.json");
var AdmZip = require("adm-zip");
const fse = require('fs-extra');

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

const generatePdf = async function (req, res, next) {

    var workingDir;
    var page;

    let timestampLog = `${Date.now()}`;

    console.time(timestampLog);
    console.info(`Starting generate pdf nodejs function`);

    try {

        console.log('starting!')

        try {
            workingDir = fs.mkdtempSync(path.join(os.tmpdir(), 'pdfenginetmp-'));
        } catch (err) {
            console.log(err);
            res.status(500);
            res.body(buildResponseBody(500, 'PDFE_908', "An error occurred on processing the request"));
            return;
        }

        console.log(req.file)

        fs.writeFileSync(path.join(workingDir,"zippedFile.zip"), req.file.buffer,  "binary");
        const zip = new AdmZip(path.join(workingDir, "zippedFile.zip"));
        zip.extractAllTo(workingDir);

        const browser = await getBrowserSession();

        page = await browser.newPage();

        let data = req.body.data;

        if (data == undefined) {
            res.status(400);
            res.json(buildResponseBody(400, 'PDFE_898', "Invalid request"));

            return;
        }

        try {
            let templateFile = readFileSync(path.join(workingDir, "template.html")).toString();

            let template = handlebars.compile(templateFile);

            let html = template(JSON.parse(data));

            fs.writeFileSync(path.join(workingDir, "compiledTemplate.html"), html);
        } catch (err) {
            console.log(err)
            res.status(500);
            res.json(buildResponseBody(400, 'PDFE_901', "Error compiling the HTML template"));

            return;
        }

        try {
            await page.goto('file:' + path.join(workingDir, "compiledTemplate.html"));


            await page.pdf({
                path: path.join(workingDir, "pagopa-receipt.pdf"),
                format: 'A4',
                landscape: false,
                printBackground: true,
            });
        } catch (err) {
            console.log(err);
            res.status(500);
            res.json(buildResponseBody(500, 'PDFE_902', "Error generating the PDF document"));

            return;
        }

        let content = readFileSync(path.join(workingDir, "pagopa-receipt.pdf"));

        console.time("pdfSend-"+timestampLog);
        res.send(content);

    } catch (err) {
        console.log(err);
        res.status(500);
        res.json(buildResponseBody(500, 'PDFE_902', "Error generating the PDF document"));
    } finally {
        if (page) {
            await page.close();
        }

        if (workingDir) {
            try {
                rmSync(workingDir, { recursive: true, force: true });
            } catch (err) {
                //console.log(err);
            }
        }

    }

}

module.exports = { info, generatePdf, shutdown };