const assert = require('assert');
const { Given, When, Then } = require('@cucumber/cucumber');
const {readZipFile, retrieveInputData} = require("./common");
const {generatePDF} = require('./pdf_engine_client');

 Given('a zip file, that contains a valid HTML template', function () {
  this.zipFile = readZipFile("template.zip");
});

 Given('a Map of key-values', function () {
  this.inputData = retrieveInputData();
});

When('an Http POST request is sent to the PDF Engine with a zip file and the key-values', async function () {
  this.response = await generatePDF(this.zipFile, this.inputData, "ITEXT");
});

When('an Http POST request is sent to the PDF Engine with a zip file and without key-values', async function () {
  this.response = await generatePDF(this.zipFile, null, "ITEXT");
});

When('an Http POST request is sent to the PDF Engine with key-values and without a zip file', async function () {
  this.response = await generatePDF(null, this.inputData, "ITEXT");
});

Then('response has a {int} Http status', function (expectedStatus) {
  assert.strictEqual(this.response.status, expectedStatus);
});

Then('application error code is {string}', function (expectedAppErrorCode) {
  assert.strictEqual(this.response.data.appErrorCode, expectedAppErrorCode);
});

 Given('a zip file, that contains a valid HTML template to use in Playwright', function () {
  this.zipFile = readZipFile("template.zip");
});

When('an Http POST request is sent to the PDF Engine with a zip file and the key-values and with Playwright Engine', async function () {
  this.response = await generatePDF(this.zipFile, this.inputData, "PLAYWRIGHT");
});