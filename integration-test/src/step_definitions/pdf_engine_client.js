const {postForm} = require("./common");
const FormData = require('form-data');

const pdf_engine_uri = process.env.PDF_ENGINE_URI;

function generatePDF(zipFile, inputData) {
    const form = new FormData();
    form.append('template', zipFile);
    form.append('data', inputData);

    return postForm(pdf_engine_uri, form);
}

module.exports = {
    generatePDF
}