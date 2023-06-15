const fs = require("fs");
const axios = require("axios").default;

axios.defaults.headers.common['Ocp-Apim-Subscription-Key'] = process.env.SUBKEY // for all requests
if (process.env.canary) {
  axios.defaults.headers.common['X-CANARY'] = 'canary' // for all requests
}

function postForm(url, body) {
    return axios.post(url, body,  {headers: {'Content-Type': 'multipart/form-data'}})
        .then(res => {
            return res;
        })
        .catch(error => {
			if (error.response) {
				// The request was made and the server responded with a status code
				// that falls out of the range of 2xx
				console.log(error.response.status);
			  } else {
				// Something happened in setting up the request that triggered an Error
				console.log('Error', error.message);
			  }
        });
}

function readZipFile(fileName) {
   return fs.createReadStream("./input_data/zip_files/" + fileName);
}

function retrieveInputData() {
    return JSON.stringify({
		"transaction": {
			"id": "F57E2F8E-25FF-4183-AB7B-4A5EC1A96644",
			"timestamp": "2020-07-10 15:00:00.000",
			"amount": 300.00,
			"psp": {
				"name": "Nexi",
				"fee": {
					"amount": 2.00
				}
			},
			"rrn": "1234567890",
			"paymentMethod": {
				"name": "Visa *1234",
				"logo": "https://...",
				"accountHolder": "Marzia Roccaraso",
				"extraFee": false
			},
			"authCode": "9999999999"
		},
		"user": {
			"data": {
				"firstName": "Marzia",
				"lastName": "Roccaraso",
				"taxCode": "RCCMRZ88A52C409A"
			},
			"email": "email@test.it"
		},
		"cart": {
			"items": [{
				"refNumber": {
					"type": "codiceAvviso",
					"value": "123456789012345678"
				},
				"debtor": {
					"fullName": "Giuseppe Bianchi",
					"taxCode": "BNCGSP70A12F205X"
				},
				"payee": {
					"name": "Comune di Controguerra",
					"taxCode": "82001760675"
				},
				"subject": "TARI 2022",
				"amount": 150.00
			}],
			"amountPartial": 300.00
		},
		"noticeCode": "noticeCodeTest",
		"amount": 100
	});
}

module.exports = {
    postForm, readZipFile, retrieveInputData
}