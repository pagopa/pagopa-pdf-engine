const {postForm} = require("./common");
const FormData = require('form-data');

const pdf_engine_uri = process.env.PDF_ENGINE_URI;

function generatePDF(zipFile, inputData) {
    const form = new FormData();
    if (zipFile !== null) {
        form.append('template', zipFile);
    }
    if (inputData !== null) {
        form.append('data', inputData);
    }

    return postForm(pdf_engine_uri, form);
}

module.exports = {
    generatePDF
}