const path = require('path');
const getPage = require('./getPage');


module.exports = async function (context, request) {

     const transactionID =  'F57E2F8E-25FF-4183-AB7B-4A5EC1A96644-'+process.hrtime.bigint();

     const browser = await getPage();

     const browserContext = await browser.newContext({
         acceptDownloads: true
     });
     const page = await browserContext.newPage();

     const htmlFile = path.resolve('pdf-generate/index.html');

     await page.goto('file:'+htmlFile);
     await page.pdf({
        path: `tests\pagopa-receipt-${transactionID}.pdf`,
        format: 'A4',
        landscape: false,
        printBackground: true,
     });

    await browserContext.close();

    return {
        httpResponse: {
            body: "OK!"
        }
    };
};
