import http from 'k6/http';

export function generatePDF(pdfEngineUri, subKey, zipFile, inputData, generateZipped) {
    const form = {
        data: inputData,
        template: http.file(zipFile, 'template.zip'),
        generateZipped: generateZipped
      };

      let headers = { 
        'Content-Type': 'multipart/form-data',
        'Ocp-Apim-Subscription-Key': subKey
    };

    return http.post(pdfEngineUri, form, {headers});
}