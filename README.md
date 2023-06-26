# pagoPA Generate PDF function

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=pagopa_pagopa-pdf-engine&metric=alert_status)](https://sonarcloud.io/dashboard?id=pagopa_pagopa-pdf-engine)

Java Azure Function that exposes REST API to generate a PDFA/2a document based on the provided data and HTML template.

---
## Summary 📖

- [API Documentation 📖](#api-documentation-)
- [Technology Stack](#technology-stack)
- [Start Project Locally 🚀](#start-project-locally-)
  * [Run locally with Docker](#run-locally-with-docker)
      + [Prerequisites](#prerequisites)
      + [Run docker container](#run-docker-container)
  * [Run locally with Maven](#run-locally-with-maven)
      + [Prerequisites](#prerequisites-1)
      + [Run the project](#run-the-project)
  * [Test](#test)
- [Develop Locally 💻](#develop-locally-)
  * [Prerequisites](#prerequisites-2)
  * [Testing 🧪](#testing-)
    + [Unit testing](#unit-testing)
    + [Integration testing](#integration-testing)
    + [Performance testing](#performance-testing)
- [Contributors 👥](#contributors-)
  * [Mainteiners](#mainteiners)

---
## API Documentation 📖
See the [OpenApi 3 here.](https://editor.swagger.io/?url=https://raw.githubusercontent.com/pagopa/pagopa-pdf-engine/main/openapi/openapi.json)

---

## Technology Stack
- Java 11
- IText7
- Handlebars
- Zip4j

---

## Start Project Locally 🚀

> **Warning**
> If you are running the project from macOS you must change the WRITE_FILE_BASE_PATH environment variable with an absolute
> path, such as `/Users/<username>/<working-directory>` (the `<working-directory>` folder must exist).

> **Warning**
> Inside the folder defined by the environment variable WRITE_FILE_BASE_PATH, another folder must exist and must be named as the value of the environment variable WORKING_FILES_FOLDER

### Run locally with Docker

#### Prerequisites
- docker

#### Run docker container
`docker build -t pagopa-pdf-engine .`

`docker run -p 7071:80 pagopa-pdf-engine`

### Run locally with Maven

#### Prerequisites
- maven

#### Run the project
`mvn clean package`

`mvn azure-functions:run`

### Test
```
curl --location 'http://localhost:54078/generate-pdf' \
--form 'template=@"template.zip"' \
--form 'data="{
		\"transaction\": {
			\"id\": \"F57E2F8E-25FF-4183-AB7B-4A5EC1A96644\",
			\"timestamp\": \"2020-07-10 15:00:00.000\",
			\"amount\": 300.00,
			\"psp\": {
				\"name\": \"Nexi\",
				\"fee\": {
					\"amount\": 2.00
				}
			},
			\"rrn\": \"1234567890\",
			\"paymentMethod\": {
				\"name\": \"Visa *1234\",
				\"logo\": \"https://...\",
				\"accountHolder\": \"Marzia Roccaraso\",
				\"extraFee\": false
			},
			\"authCode\": \"9999999999\"
		},
		\"user\": {
			\"data\": {
				\"firstName\": \"Marzia\",
				\"lastName\": \"Roccaraso\",
				\"taxCode\": \"RCCMRZ88A52C409A\"
			},
			\"email\": \"email@test.it\"
		},
		\"cart\": {
			\"items\": [{
				\"refNumber\": {
					\"type\": \"codiceAvviso\",
					\"value\": \"123456789012345678\"
				},
				\"debtor\": {
					\"fullName\": \"Giuseppe Bianchi\",
					\"taxCode\": \"BNCGSP70A12F205X\"
				},
				\"payee\": {
					\"name\": \"Comune di Controguerra\",
					\"taxCode\": \"82001760675\"
				},
				\"subject\": \"TARI 2022\",
				\"amount\": 150.00
			}],
			\"amountPartial\": 300.00
		},
		\"noticeCode\": \"noticeCodeTest\",
		\"amount\": 100
	}"' \
--form 'applySignature="false"'
``` 
As you can see in the provided curl the first field `template` hold a zip file. The zip file contains the HTML template
file and other optional attachments, such as CSS files, that will be used to generate the PDF document.

> **Warning**
> The HTML template file must be named to match the value of the `HTML_TEMPLATE_FILE_NAME` environment variable 
> (default is `template`) (example of HTML template file name: `template.html`)

---

## Develop Locally 💻

### Prerequisites
- git
- maven
- jdk-11

### Testing 🧪

#### Unit testing

To run the **Junit** tests:

`mvn clean verify`

#### Integration testing
From `./integration-test/src`

1. `yarn install`
2. `yarn test`
#### Performance testing

---

## Contributors 👥
Made with ❤️ by PagoPa S.p.A.

### Mainteiners
See `CODEOWNERS` file