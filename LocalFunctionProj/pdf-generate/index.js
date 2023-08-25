const puppeteer = require('playwright');
const path = require('path');

module.exports = async function (context, request) {

     const transactionID =  'F57E2F8E-25FF-4183-AB7B-4A5EC1A96644-'+process.hrtime.bigint();

      const browser = await chromium.launch({
             headless: true,
     });
     const page = await context.newPage();


     const htmlFile = path.resolve('pdf-generate/index.html');

     await page.goto('file:'+htmlFile, { waitUntil: 'networkidle2' });
     await page.pdf({
        path: `tests\pagopa-receipt-${transactionID}.pdf`,
        format: 'A4',
        landscape: false,
        printBackground: true,
     });

     await browser.close();

    return {
        httpResponse: {
            body: message
        },
        queueOutput: message
    };
};
