import { SharedArray } from 'k6/data';
import { check } from 'k6';
import { generatePDF } from './modules/pdf_engine_client.js';
import { retrieveInputData } from './modules/common.js';

export let options = JSON.parse(open(__ENV.TEST_TYPE));

const varsArray = new SharedArray('vars', function () {
  return JSON.parse(open(`./${__ENV.VARS}`)).environment;
});

const vars = varsArray[0];
const pdfEngineUri = `${vars.pdfEngineUri}`;
const subKey = `${__ENV.SUBSCRIPTION_KEY}`;
const fileName = `${__ENV.TEMPLATE_FILE_NAME}`;
// Default to PDF when the variable is missing or empty; reject other invalid values.
const generateZippedValue = (__ENV.GENERATE_ZIPPED || '').trim().toLowerCase() || 'false';

if (generateZippedValue !== 'true' && generateZippedValue !== 'false') {
  throw new Error('GENERATE_ZIPPED must be "true" or "false"');
}

const generateZipped = generateZippedValue === 'true';

// Optional explicit paths allow smoke tests to reuse the validated integration fixtures.
const templateFilePath = __ENV.TEMPLATE_FILE_PATH ||
  './zip_files/' + ((fileName === 'undefined' || fileName === null) ? 'template' : fileName) + '.zip';

const templateFile = open(templateFilePath, 'b');

// Read and validate a fixed JSON input once during initialization, when configured.
const fixedInputData = __ENV.DATA_FILE_PATH
  ? JSON.stringify(JSON.parse(open(__ENV.DATA_FILE_PATH)))
  : null;

export function setup() {
  // 2. setup code
}

export default function (data) {
  // 3. VU code
  const inputData = fixedInputData !== null ? fixedInputData : retrieveInputData();
  let response = generatePDF(pdfEngineUri, subKey, templateFile, inputData, generateZipped);

  console.log("Generate PDF call, Status " + response.status);

  let content_type;
  if (generateZipped) {
    content_type = "application/zip";
  } else {
    content_type = "application/pdf";
  }

  check(response, {
    'Generate PDF status is 200': (response) => response.status === 200,
    'Generate PDF content_type is the expected one based on the generateZipped field': (response) => response.headers["Content-Type"] === content_type,
	'Generate PDF body is not empty': (response) => response.body !== null && response.body.byteLength > 0
  });
}

export function teardown(data) {
  // 4. teardown code
}