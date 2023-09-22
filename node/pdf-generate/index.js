const puppeteer = require('puppeteer');
const path = require('path');
const fs = require('fs');
const createReadStream = require('fs').createReadStream
const readFileSync = require('fs').readFileSync
const rmSync = require('fs').rmSync
const os = require('os');
const unzipper = require('unzipper');
const getPage = require('./utils/browserManager');
const getBrowserSession = require('./utils/browserManager');
const buildResponseBody = require('./utils/buildUtils');
const multer = require('multer');
const express = require('express');
var handlebars = require("handlebars");
const packageJson = require("../package.json");
const AdmZip = require("adm-zip");



var app = express();

const storage = multer.memoryStorage()
const upload = multer({ storage: storage })

app.use(function(err, req, res, next) {
  res.status(err.status || 500);
  res.json({'errors': {
    message: err.message,
    error: {}
  }});
});

app.get('/info', async function (req, res, next) {

    res.send({
        name: packageJson.name,
        version: packageJson.version
    });

})

app.post('/pdf-generate', upload.single('template'), async function (req, res, next) {

        var workingDir;
        var page;

        try {

            try {
                workingDir = fs.mkdtempSync(path.join(os.tmpdir(), 'pdfenginetmp-'));
            } catch (err) {
                res.status(500);
                res.body(buildResponseBody(500, 'PDFE_908', "An error occurred on processing the request"));

                return;
            }

            fs.writeFileSync(path.join(workingDir,"zippedFile.zip"), req.file.buffer,  "binary");

            const zip = new AdmZip(path.join(workingDir, "zippedFile.zip"));
            zip.extractAllTo(workingDir);

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
                fs.writeFileSync(path.join(workingDir,"compiledTemplate.html"), html);
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

        } finally {
            if (workingDir) {
                rmSync(workingDir, { recursive: true, force: true });
            }
            if (page) {
                await page.close();
            }
        }

});

var server = app.listen( process.env.PORT || 3000, function(){
  console.log('Listening on port ' + server.address().port);
});