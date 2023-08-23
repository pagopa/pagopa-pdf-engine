'use strict';

const { app } = require('@azure/functions');
const puppeteer = require('puppeteer');
const path = require('path');

app.http('generatePdf', {
    route: 'generate-pdf',
    handler: async (request, context) => {

        const transactionID = 'F57E2F8E-25FF-4183-AB7B-4A5EC1A96644';

          const browser = await puppeteer.launch();
          const page = await browser.newPage();

          const htmlFile = path.resolve('index.html');

          await page.goto(`file://${htmlFile}`, { waitUntil: 'networkidle2' });
          await page.pdf({
            path: `pagopa-receipt-${transactionID}.pdf`,
            format: 'A4',
            landscape: false,
            printBackground: true,
          });

          await browser.close();

    }
});