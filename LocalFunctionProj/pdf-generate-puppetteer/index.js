const puppeteer = require('puppeteer');
const path = require('path');
const getPage = require('./getPage');


module.exports = async function (context, request) {

     const transactionID =  'F57E2F8E-25FF-4183-AB7B-4A5EC1A96644-'+process.hrtime.bigint();

     const htmlFile = path.resolve('pdf-generate-puppetteer/index.html');

     const browser = await getPage();

     const page = await browser.newPage();

     await page.goto('file:'+htmlFile);
     await page.pdf({
        path: `tests\pagopa-receipt-${transactionID}.pdf`,
        format: 'A4',
        landscape: false,
        printBackground: true,
     });

    await page.close();

    return {
        httpResponse: {
            body: "OK!"
        }
    };
};
