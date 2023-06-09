package it.gov.pagopa.project;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.helper.ConditionalHelpers;
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
import it.gov.pagopa.project.service.impl.GeneratePDFServiceImpl;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.logging.Logger;

import static com.microsoft.azure.functions.HttpStatus.BAD_REQUEST;
import static com.microsoft.azure.functions.HttpStatus.INTERNAL_SERVER_ERROR;
import static it.gov.pagopa.project.model.AppErrorCodeEnum.*;

/**
 * Azure Functions with HTTP Trigger.
 */
public class HttpTriggerFunction {

    private static final String INVALID_REQUEST_MESSAGE = "Invalid request";
    private static final String ERROR_GENERATING_PDF_MESSAGE = "An error occurred when generating the PDF";

    private final GeneratePDFService generatePDFService;

    @VisibleForTesting
    public HttpTriggerFunction(GeneratePDFService generatePDFService) {
        this.generatePDFService = generatePDFService;
    }

    public HttpTriggerFunction() {
        this.generatePDFService = new GeneratePDFServiceImpl(buildHandlebars());
    }

    /**
     * This function will be invoked when a Http Trigger occurs.
     * This function listens at endpoint "/api/PDFEngine". To invoke it using "curl" command in bash:
     *  curl -d "HTTP Body" {your host}/api/PDFEngine
     */
    @FunctionName("PDFEngine")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.POST},
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<GeneratePDFInput> request,
            final ExecutionContext context) {
        Logger logger = context.getLogger();

        String message = String.format("PDFEngine function called at %s", LocalDateTime.now());
        logger.info(message);

        GeneratePDFInput generatePDFInput = request.getBody();

        if (generatePDFInput == null) {
            logger.severe("Invalid request the payload is null");
            return request
                    .createResponseBuilder(BAD_REQUEST)
                    .body(buildResponseBody(BAD_REQUEST, PDFE_899, INVALID_REQUEST_MESSAGE))
                    .build();
        }

        if (generatePDFInput.getTemplate() == null || generatePDFInput.getTemplate().isBlank()) {
            String errMsg = String.format("Invalid request the provided HTML template is empty or null: %s", generatePDFInput.getTemplate());
            logger.severe(errMsg);
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

        ByteArrayOutputStream outputStream;
        try {
            outputStream = generatePDFService.generatePDF(generatePDFInput);
        } catch (PDFEngineException e) {
            String errMsg = String.format("Error generating the PDF document: %s", e);
            logger.severe(errMsg);
            return request
                    .createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            buildResponseBody(
                                    INTERNAL_SERVER_ERROR,
                                    e.getErrorCode(),
                                    ERROR_GENERATING_PDF_MESSAGE))
                    .build();
        }

        return request
                .createResponseBuilder(HttpStatus.OK)
                .header("content-type", "application/pdf")
                .header("content-length", String.valueOf(outputStream.size()))
                .header("content-disposition", "attachment; ")
                .body(outputStream.toByteArray())
                .build();
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
        return new Handlebars()
                .registerHelper("eq", ConditionalHelpers.eq)
                .registerHelper("not", ConditionalHelpers.not);
    }
}
