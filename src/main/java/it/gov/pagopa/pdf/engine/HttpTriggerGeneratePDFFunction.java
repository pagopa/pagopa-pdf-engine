
package it.gov.pagopa.pdf.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import it.gov.pagopa.pdf.engine.exception.PDFEngineException;
import it.gov.pagopa.pdf.engine.model.AppErrorCodeEnum;
import it.gov.pagopa.pdf.engine.model.ErrorMessage;
import it.gov.pagopa.pdf.engine.model.ErrorResponse;
import it.gov.pagopa.pdf.engine.model.GeneratePDFInput;
import it.gov.pagopa.pdf.engine.service.GeneratePDFService;
import it.gov.pagopa.pdf.engine.service.ParseRequestBodyService;
import it.gov.pagopa.pdf.engine.service.impl.GeneratePDFServiceImpl;
import it.gov.pagopa.pdf.engine.service.impl.ParseRequestBodyServiceImpl;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Optional;

import static com.microsoft.azure.functions.HttpStatus.BAD_REQUEST;
import static com.microsoft.azure.functions.HttpStatus.INTERNAL_SERVER_ERROR;

/**
 * Azure Functions with HTTP Trigger.
 */
public class HttpTriggerGeneratePDFFunction {

    private final Logger logger = LoggerFactory.getLogger(HttpTriggerGeneratePDFFunction.class);

    private final String workingDirectoryPath = System.getenv().getOrDefault("WORKING_DIRECTORY_PATH", "");

    private static final String INVALID_REQUEST_MESSAGE = "Invalid request";
    private static final String ERROR_GENERATING_PDF_MESSAGE = "An error occurred when generating the PDF";
    private static final String PATTERN_FORMAT = "yyyy.MM.dd.HH.mm.ss";

    private final GeneratePDFService generatePDFService;
    private final ParseRequestBodyService parseRequestBodyService;

    public HttpTriggerGeneratePDFFunction() {
        this.generatePDFService = new GeneratePDFServiceImpl();
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
     * curl -d "HTTP Body" {your host}/api/generate-pdf
     */
    @FunctionName("generate-pdf")
    public HttpResponseMessage run(
            @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.POST},
                    authLevel = AuthorizationLevel.ANONYMOUS)
            HttpRequestMessage<Optional<byte[]>> request,
            final ExecutionContext context) {

        logger.info("Generate PDF function called at {}", LocalDateTime.now());

        Optional<byte[]> optionalRequestBody = request.getBody();
        if (optionalRequestBody.isEmpty()) {
            logger.error("Invalid request the payload is null");
            return request
                    .createResponseBuilder(BAD_REQUEST)
                    .body(buildResponseBody(BAD_REQUEST, AppErrorCodeEnum.PDFE_899, INVALID_REQUEST_MESSAGE))
                    .build();
        }

        Path workingDirPath;
        try {
            File workingDirectory = createWorkingDirectory();
            workingDirPath = Files.createTempDirectory(
                    workingDirectory.toPath(),
                    DateTimeFormatter.ofPattern(PATTERN_FORMAT)
                            .withZone(ZoneId.systemDefault())
                            .format(Instant.now())
            );
        } catch (IOException e) {
            logger.error(AppErrorCodeEnum.PDFE_908.getErrorMessage(), e);
            return request
                    .createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            buildResponseBody(
                                    INTERNAL_SERVER_ERROR,
                                    AppErrorCodeEnum.PDFE_908,
                                    "An error occurred on processing the request"))
                    .build();
        }

        byte[] requestBody = optionalRequestBody.get();
        GeneratePDFInput generatePDFInput;
        try {
            generatePDFInput = this.parseRequestBodyService.retrieveInputData(requestBody, request.getHeaders(), workingDirPath);
        } catch (PDFEngineException e) {
            logger.error("Error retrieving input data from request body", e);
            HttpStatus status = getHttpStatus(e);
            return request
                    .createResponseBuilder(status)
                    .body(buildResponseBody(status, e.getErrorCode(), INVALID_REQUEST_MESSAGE))
                    .build();
        }

        if (generatePDFInput.getTemplateZip() == null) {
            logger.error("Invalid request, template HTML not provided");
            return request
                    .createResponseBuilder(BAD_REQUEST)
                    .body(buildResponseBody(BAD_REQUEST, AppErrorCodeEnum.PDFE_897, INVALID_REQUEST_MESSAGE))
                    .build();
        }

        if (generatePDFInput.getData() == null) {
            logger.error("Invalid request the PDF document input data are null");
            return request
                    .createResponseBuilder(BAD_REQUEST)
                    .body(buildResponseBody(BAD_REQUEST, AppErrorCodeEnum.PDFE_898, INVALID_REQUEST_MESSAGE))
                    .build();
        }

        try (BufferedInputStream inputStream = generatePDFService.generatePDF(generatePDFInput, workingDirPath, logger)){
            byte[] fileBytes = inputStream.readAllBytes();

            logger.debug("Returning generated pdf at {}", LocalDateTime.now());
            return request
                    .createResponseBuilder(HttpStatus.OK)
                    .header("content-type", generatePDFInput.isGenerateZipped() ? "application/zip" : "application/pdf")
                    .header("content-length", String.valueOf(fileBytes.length))
                    .header("content-disposition", "attachment; ")
                    .body(fileBytes)
                    .build();
        } catch (PDFEngineException e) {
            logger.error("Error generating the PDF document", e);
            return request
                    .createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            buildResponseBody(
                                    INTERNAL_SERVER_ERROR,
                                    e.getErrorCode(),
                                    ERROR_GENERATING_PDF_MESSAGE))
                    .build();
        } catch (IOException e) {
            logger.error("Error handling the generated stream", e);
            return request
                    .createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            buildResponseBody(
                                    INTERNAL_SERVER_ERROR,
                                    AppErrorCodeEnum.PDFE_907,
                                    ERROR_GENERATING_PDF_MESSAGE))
                    .build();
        } finally {
            clearTempDirectory(workingDirPath);
        }


    }

    private static HttpStatus getHttpStatus(PDFEngineException e) {
        HttpStatus status;
        if (e.getErrorCode().equals(AppErrorCodeEnum.PDFE_703) || e.getErrorCode().equals(AppErrorCodeEnum.PDFE_704) || e.getErrorCode().equals(AppErrorCodeEnum.PDFE_705)) {
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

    private void clearTempDirectory(Path workingDirPath) {
        try {
            FileUtils.deleteDirectory(workingDirPath.toFile());
        } catch (IOException e) {
            logger.warn("Unable to clear working directory: {}", workingDirPath, e);
        }
    }

    private File createWorkingDirectory() throws IOException {

        File workingDirectory = new File(workingDirectoryPath);
        if (!workingDirectory.exists()) {
            try {
                Files.createDirectory(workingDirectory.toPath());
            } catch (FileAlreadyExistsException e) {}
        }
        return workingDirectory;
    }

}