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
var handlebars = require("handlebars");
const packageJson = require("../package.json");

const info = async function (req, res, next) {

    res.send({
        name: packageJson.name,
        version: packageJson.version
    });

}

const generatePdf = async function (req, res, next) {

    var workingDir = req.body.workingDir;
    var page;

    try {

        const browser = await getBrowserSession();
        page = await browser.newPage();

        var data = req.body.data;

        if (data == undefined) {
            res.status(400);
            res.json(buildResponseBody(400, 'PDFE_898', "Invalid request"));

            return;
        }

        try {
            var templateFile = readFileSync(path.join(workingDir,"template.html")).toString();
            var template = handlebars.compile( templateFile );
            var html = template(data);
            await fs.writeFile(path.join(workingDir,"compiledTemplate.html"), html);
        } catch (err) {
            console.log(err)
            res.status(500);
            res.json(buildResponseBody(400, 'PDFE_901', "Error compiling the HTML template"));

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
            console.log(err);
            res.status(500);
            res.json(buildResponseBody(500, 'PDFE_902', "Error generating the PDF document"));

            return;
        }

       var content = readFileSync(path.join(workingDir,"pagopa-receipt.pdf"));

       res.send(content);

    } catch (err) {
        res.status(500);
        res.json(buildResponseBody(500, 'PDFE_902', "Error generating the PDF document"));
    } finally {
        if (page) {
            await page.close();
        }
    }

}

//setTimeout(() => { process.exit(0) }, 20000)