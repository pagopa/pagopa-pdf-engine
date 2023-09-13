const fs = require('fs').promises;
const server = require('../index');
const path = require('path');
const os = require('os');
const axios = require('axios');

afterEach(() => {
  jest.clearAllMocks();
});

const url = 'http://127.0.0.1:3000';

describe("generatePdf", () => {

  it("should return a pdf when passing valid data", async () => {

    workingDir = await fs.mkdtemp(path.join(os.tmpdir(), 'pdfenginetmp-'));
    await fs.writeFile(path.join(workingDir,"template.html"), "<html></html>");

    const formData = new FormData();
    formData.append('data', '{\n' +
        '\t\t"transaction": {\n' +
        '\t\t\t"id": "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz01234567890123456789",\n' +
        '\t\t\t"timestamp": "2020-07-10 15:00:00.000",\n' +
        '\t\t\t"amount": 300.00,\n' +
        '\t\t\t"psp": {\n' +
        '\t\t\t\t"name": "Nexi",\n' +
        '\t\t\t\t"fee": {\n' +
        '\t\t\t\t\t"amount": 2.00\n' +
        '\t\t\t\t}\n' +
        '\t\t\t},\n' +
        '\t\t\t"rrn": "1234567890",\n' +
        '\t\t\t"paymentMethod": {\n' +
        '\t\t\t\t"name": "Visa *1234",\n' +
        '\t\t\t\t"logo": "https://...",\n' +
        '\t\t\t\t"accountHolder": "Marzia Roccaraso",\n' +
        '\t\t\t\t"extraFee": false\n' +
        '\t\t\t},\n' +
        '\t\t\t"authCode": "9999999999"\n' +
        '\t\t},\n' +
        '\t\t"user": {\n' +
        '\t\t\t"data": {\n' +
        '\t\t\t\t"firstName": "Marzia",\n' +
        '\t\t\t\t"lastName": "Roccaraso",\n' +
        '\t\t\t\t"taxCode": "RCCMRZ88A52C409A"\n' +
        '\t\t\t},\n' +
        '\t\t\t"email": "email@test.it"\n' +
        '\t\t},\n' +
        '\t\t"cart": {\n' +
        '\t\t\t"items": [{\n' +
        '\t\t\t\t"refNumber": {\n' +
        '\t\t\t\t\t"type": "codiceAvviso",\n' +
        '\t\t\t\t\t"value": "123456789012345678"\n' +
        '\t\t\t\t},\n' +
        '\t\t\t\t"debtor": {\n' +
        '\t\t\t\t\t"fullName": "Giuseppe Bianchi",\n' +
        '\t\t\t\t\t"taxCode": "BNCGSP70A12F205X"\n' +
        '\t\t\t\t},\n' +
        '\t\t\t\t"payee": {\n' +
        '\t\t\t\t\t"name": "Comune di Controguerra",\n' +
        '\t\t\t\t\t"taxCode": "82001760675"\n' +
        '\t\t\t\t},\n' +
        '\t\t\t\t"subject": "TARI 2022",\n' +
        '\t\t\t\t"amount": 150.00\n' +
        '\t\t\t}],\n' +
        '\t\t\t"amountPartial": 300.00\n' +
        '\t\t},\n' +
        '\t\t"noticeCode": "noticeCodeTest",\n' +
   '\t}');
    formData.append('workingDir', workingDir);

    const res = await axios.post(url+"/pdf-generate", formData,
        {
            headers: {'Content-Type': 'multipart/form-data'}
        })

    expect(res).toBeTruthy()
    expect(res.status).toBe(200)

  });

  it("should return ok when calling info", async () => {

    const res = await axios.get(url+"/info");

    expect(res).toBeTruthy()
    expect(res.status).toBe(200)

  });

  it("should return an error when passing invalid data", async () => {

    workingDir = await fs.mkdtemp(path.join(os.tmpdir(), 'pdfenginetmp-'));
    await fs.writeFile(path.join(workingDir,"template.html"), "<html></html>");

    const formData = new FormData();
    formData.append('workingDir', workingDir);

    const res = await axios.post(url+"/pdf-generate", formData,
        {
            headers: {'Content-Type': 'multipart/form-data'}
        }).catch(function (error) {
            expect(error.response.status).toBe(400)
        });

  });

});

setTimeout(() => { process.exit(1) }, 5000)