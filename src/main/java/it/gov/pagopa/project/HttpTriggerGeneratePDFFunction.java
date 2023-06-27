/*
Copyright (C)

This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public
License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
details.

You should have received a copy of the GNU Affero General Public License along with this program.
If not, see https://www.gnu.org/licenses/.
*/

package it.gov.pagopa.project;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.helper.ConditionalHelpers;
import com.github.jknack.handlebars.io.FileTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import com.google.common.annotations.VisibleForTesting;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import it.gov.pagopa.project.exception.PDFEngineException;
import it.gov.pagopa.project.model.AppErrorCodeEnum;
import it.gov.pagopa.project.model.ErrorMessage;
import it.gov.pagopa.project.model.ErrorResponse;
import it.gov.pagopa.project.model.GeneratePDFInput;
import it.gov.pagopa.project.service.GeneratePDFService;
import it.gov.pagopa.project.service.ParseRequestBodyService;
import it.gov.pagopa.project.service.impl.GeneratePDFServiceImpl;
import it.gov.pagopa.project.service.impl.ParseRequestBodyServiceImpl;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.microsoft.azure.functions.HttpStatus.BAD_REQUEST;
import static com.microsoft.azure.functions.HttpStatus.INTERNAL_SERVER_ERROR;
import static it.gov.pagopa.project.model.AppErrorCodeEnum.*;
import static it.gov.pagopa.project.service.impl.GeneratePDFServiceImpl.WORKING_DIR;

/**
 * Azure Functions with HTTP Trigger.
 */
public class HttpTriggerGeneratePDFFunction {

    private static final String INVALID_REQUEST_MESSAGE = "Invalid request";
    private static final String ERROR_GENERATING_PDF_MESSAGE = "An error occurred when generating the PDF";

    private final String writeFileBasePath = System.getenv().getOrDefault("WRITE_FILE_BASE_PATH", "/tmp");
    private final String unzippedFilesFolder = System.getenv().getOrDefault("UNZIPPED_FILES_FOLDER", "/unzipped");
    private final String zipFileName = System.getenv().getOrDefault("ZIP_FILE_NAME", "/input.zip");

    private final GeneratePDFService generatePDFService;
    private final ParseRequestBodyService parseRequestBodyService;

    public HttpTriggerGeneratePDFFunction() {
        this.generatePDFService = new GeneratePDFServiceImpl(buildHandlebars());
        this.parseRequestBodyService = new ParseRequestBodyServiceImpl(new ObjectMapper());
    }

    @VisibleForTesting
    public HttpTriggerGeneratePDFFunction(GeneratePDFService generatePDFService, ParseRequestBodyService parseRequestBodyService) {
        this.generatePDFService = generatePDFService;
        this.parseRequestBodyService = parseRequestBodyService;
    }

    /**
     * This function will be invoked when a Http Trigger occurs.
     * This function listens at endpoint "/api/generate-pdf". To invoke it using "curl" command in bash:
     *  curl -d "HTTP Body" {your host}/api/generate-pdf
     */
    @FunctionName("generate-pdf")
    public HttpResponseMessage run(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.POST},
                    authLevel = AuthorizationLevel.ANONYMOUS)
                    HttpRequestMessage<Optional<byte[]>> request,
            final ExecutionContext context) {
        Logger logger = context.getLogger();

        String message = String.format("Generate PDF function called at %s", LocalDateTime.now());
        logger.info(message);

        Optional<byte[]> optionalRequestBody = request.getBody();
        if (optionalRequestBody.isEmpty()) {
            logger.severe("Invalid request the payload is null");
            return request
                    .createResponseBuilder(BAD_REQUEST)
                    .body(buildResponseBody(BAD_REQUEST, PDFE_899, INVALID_REQUEST_MESSAGE))
                    .build();
        }

        byte[] requestBody = optionalRequestBody.get();
        GeneratePDFInput generatePDFInput;
        try {
            generatePDFInput = this.parseRequestBodyService.retrieveInputData(requestBody, request.getHeaders());
        } catch (PDFEngineException e) {
            logger.log(Level.SEVERE, "Error retrieving input data from request body", e);
            HttpStatus status = getHttpStatus(e);
            return request
                    .createResponseBuilder(status)
                    .body(buildResponseBody(status, e.getErrorCode(), INVALID_REQUEST_MESSAGE))
                    .build();
        }

        if (!generatePDFInput.isTemplateSavedOnFileSystem()) {
            logger.severe("Invalid request, template HTML not provided");
            return request
                    .createResponseBuilder(BAD_REQUEST)
                    .body(buildResponseBody(BAD_REQUEST, PDFE_897, INVALID_REQUEST_MESSAGE))
                    .build();
        }

        if (generatePDFInput.getData() == null) {
            logger.severe("Invalid request the PDF document input data are null");
            return request
                    .createResponseBuilder(BAD_REQUEST)
                    .body(buildResponseBody(BAD_REQUEST, PDFE_898, INVALID_REQUEST_MESSAGE))
                    .build();
        }

        try (BufferedInputStream inputStream = generatePDFService.generatePDF(generatePDFInput)){
            byte[] fileBytes = inputStream.readAllBytes();
            return request
                    .createResponseBuilder(HttpStatus.OK)
                    .header("content-type", generatePDFInput.isGenerateZipped() ? "application/zip" : "application/pdf")
                    .header("content-length", String.valueOf(fileBytes.length))
                    .header("content-disposition", "attachment; ")
                    .body(fileBytes)
                    .build();
        } catch (PDFEngineException e) {
            logger.log(Level.SEVERE, "Error generating the PDF document", e);
            return request
                    .createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            buildResponseBody(
                                    INTERNAL_SERVER_ERROR,
                                    e.getErrorCode(),
                                    ERROR_GENERATING_PDF_MESSAGE))
                    .build();
        } catch (IOException e) {
            return request
                    .createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            buildResponseBody(
                                    INTERNAL_SERVER_ERROR,
                                    PDFE_907,
                                    ERROR_GENERATING_PDF_MESSAGE))
                    .build();
        } finally {
            clearTempFiles(logger);
        }


    }

    private static HttpStatus getHttpStatus(PDFEngineException e) {
        HttpStatus status;
        if (e.getErrorCode().equals(PDFE_703) || e.getErrorCode().equals(PDFE_704) || e.getErrorCode().equals(PDFE_705)) {
           status = INTERNAL_SERVER_ERROR;
        } else {
            status = BAD_REQUEST;
        }
        return status;
    }

    private ErrorResponse buildResponseBody(HttpStatus status, AppErrorCodeEnum appErrorCode, String message) {
        return new ErrorResponse(
                status,
                appErrorCode,
                Collections.singletonList(
                        ErrorMessage.builder()
                                .message(message)
                                .build()
                )
        );
    }

    private Handlebars buildHandlebars() {
        TemplateLoader loader = new FileTemplateLoader(writeFileBasePath + unzippedFilesFolder, ".html");
        return new Handlebars(loader)
                .registerHelper("eq", ConditionalHelpers.eq)
                .registerHelper("not", ConditionalHelpers.not);
    }


    private void clearTempFiles(Logger logger) {
        try {
            FileUtils.deleteDirectory(new File(writeFileBasePath + WORKING_DIR));
            FileUtils.deleteDirectory(new File(writeFileBasePath + unzippedFilesFolder));
            Files.delete(Path.of(writeFileBasePath + zipFileName));
        } catch (IOException e) {
            logger.log(Level.WARNING, "Unable to clear temp files", e);
        }
    }
}
