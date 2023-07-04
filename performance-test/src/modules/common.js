
function randomString(length, charset) {
    let res = '';
    while (length--) res += charset[(Math.random() * charset.length) | 0];
    return res;
}

export function retrieveInputData() {
    return JSON.stringify({
		"transaction": {
			"id": randomString(36, "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz01234567890123456789"),
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