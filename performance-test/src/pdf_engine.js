import { SharedArray } from 'k6/data';
import { check } from 'k6';
import { generatePDF } from './modules/pdf_engine_client';
import { retrieveInputData } from './modules/common';

export let options = JSON.parse(open(__ENV.TEST_TYPE));

const varsArray = new SharedArray('vars', function() {
    return JSON.parse(open(`./${__ENV.VARS}`)).environment;
});

const vars = varsArray[0];
const pdfEngineUri = `${vars.pdfEngineUri}`;
const subKey = `${__ENV.SUBSCRIPTION_KEY}`;
const fileName = `${__ENV.TEMPLATE_FILE_NAME}`;
const generateZipped = `${__ENV.GENERATE_ZIPPED}`;

const templateFile   = new SharedArray('template', function () {
  if (fileName) {
        return open('./input_data/zip_files/' + fileName + '.zip', b);
    } else {
        //  default
        return open('./input_data/zip_files/template.zip', b);
  }
});


export function setup() {
    // 2. setup code
  }
  
  export default function (data) {
    // 3. VU code
    let inputData = retrieveInputData();
    let response = generatePDF(pdfEngineUri, subKey, templateFile, inputData, generateZipped);

    console.log("Generate PDF call, Status " + response.status);

    let content_type;
    if (generateZipped) {
        content_type = "application/zip";
    } else {
        content_type = "application/pdf";
    }
    let sentence = `Generate PDF content_type is ${content_type}`;

	check(response, {
		'Generate PDF status is 200': (response) => response.status === 200,
		sentence: (response) => response.headers.content_type === content_type,
        'Generate PDF body not null': (response) => response.body !== null
	});
  }
  
  export function teardown(data) {
    // 4. teardown code
  }