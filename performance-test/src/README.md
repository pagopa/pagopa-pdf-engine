# K6 tests for _PDFEngine_ project

[k6](https://k6.io/) is a load testing tool. 👀 See [here](https://k6.io/docs/get-started/installation/) to install it.

  - [01. PDF engine](#01-pdf-engine)

This is a set of [k6](https://k6.io) tests related to the _PDF Engine_ initiative.

To invoke k6 test passing parameter use -e (or --env) flag:

```
-e MY_VARIABLE=MY_VALUE
```

## 01. PDF engine

Call to test the PDF generator:

```
k6 run --env VARS=local.environment.json --env TEST_TYPE=./test-types/load.json --env SUBSCRIPTION_KEY=<your-subscription-key> --env GENERATE_ZIPPED=false --env TEMPLATE_FILE_NAME=./zip_files/template.zip pdf_engine.js 
```

where the mean of the environment variables is:

```json
{
  "environment": [
    {
      "env": "dev",
      "pdfEngineUri": "https://api.dev.platform.pagopa.it/shared/pdf-engine/v1/generate-pdf"
    }
  ]
}  
```

`pdfEngineUri`: PDF Engine url to access the PDF Engine REST API