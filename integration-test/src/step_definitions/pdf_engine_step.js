const assert = require('assert');
const { Given, When, Then } = require('@cucumber/cucumber');
const {readZipFile, retrieveInputData} = require("./common");
const {generatePDF} = require('./pdf_engine_client');

 Given('a a zip file, that contains a valid HTML template', function () {
   // Write code here that turns the phrase above into concrete actions
   this.zipFile = readZipFile("template.zip");
 });

 Given('a Map of key-values', function () {
   // Write code here that turns the phrase above into concrete actions
   this.inputData = retrieveInputData();
 });

 When('an Http POST request is sent to the PDF Engine with a zip file and the key-values', async function () {
   // Write code here that turns the phrase above into concrete actions
   this.response = await generatePDF(this.zipFile, this.inputData);
 });

 Then('response has a {int} Http status', function (expectedStatus) {
 // Then('response has a {float} Http status', function (float) {
   // Write code here that turns the phrase above into concrete actions
   assert.strictEqual(this.response.status, expectedStatus);
 });
