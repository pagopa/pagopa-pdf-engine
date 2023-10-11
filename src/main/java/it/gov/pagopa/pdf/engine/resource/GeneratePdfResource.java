package it.gov.pagopa.pdf.engine.resource;

import io.smallrye.mutiny.Uni;
import it.gov.pagopa.pdf.engine.exception.PDFEngineException;
import it.gov.pagopa.pdf.engine.model.AppErrorCodeEnum;
import it.gov.pagopa.pdf.engine.model.ErrorMessage;
import it.gov.pagopa.pdf.engine.model.ErrorResponse;
import it.gov.pagopa.pdf.engine.model.GeneratePDFInput;
import it.gov.pagopa.pdf.engine.service.GeneratePDFService;
import it.gov.pagopa.pdf.engine.service.ParseRequestBodyService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.io.FileUtils;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.multipart.MultipartFormDataInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

import static org.jboss.resteasy.reactive.RestResponse.StatusCode.BAD_REQUEST;
import static org.jboss.resteasy.reactive.RestResponse.StatusCode.INTERNAL_SERVER_ERROR;

@Path("/generate-pdf")
public class GeneratePdfResource {

  @Inject
  GeneratePDFService generatePDFService;

  @Inject
  ParseRequestBodyService parseRequestBodyService;


  private final Logger logger = LoggerFactory.getLogger(GeneratePdfResource.class);

  private final String workingDirectoryPath = System.getenv().getOrDefault("WORKING_DIRECTORY_PATH", "");

  private static final String INVALID_REQUEST_MESSAGE = "Invalid request";
  private static final String ERROR_GENERATING_PDF_MESSAGE = "An error occurred when generating the PDF";
  private static final String PATTERN_FORMAT = "yyyy.MM.dd.HH.mm.ss";


  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public Uni<Response> hello(MultipartFormDataInput request) throws IOException {

    logger.debug("Generate PDF function called at {}", LocalDateTime.now());

    Uni<GeneratePDFInput> response = Uni.createFrom().item(() ->  {
      if (request == null) {
        logger.error("Invalid request the payload is null");
//        return Uni.createFrom().item(Response.status(RestResponse.Status.BAD_REQUEST).entity(
//                buildResponseBody(BAD_REQUEST, AppErrorCodeEnum.PDFE_899, INVALID_REQUEST_MESSAGE)).build());
        throw new RuntimeException();
      }

      java.nio.file.Path workingDirPath;
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
//        return Response.status(RestResponse.Status.INTERNAL_SERVER_ERROR)
//                .entity(
//                        buildResponseBody(
//                                INTERNAL_SERVER_ERROR,
//                                AppErrorCodeEnum.PDFE_908,
//                                "An error occurred on processing the request"))
//                .build();
        throw new RuntimeException();
     }


     GeneratePDFInput generatePDFInput;
     try {
       generatePDFInput =
              this.parseRequestBodyService.retrieveInputData(request);
     } catch (PDFEngineException e) {
       logger.error("Error retrieving input data from request body", e);
       Integer status = getHttpStatus(e);
//       return Response.status(status)
//              .entity(buildResponseBody(status, e.getErrorCode(), INVALID_REQUEST_MESSAGE))
//              .build();
       throw new RuntimeException();
     }

     if (generatePDFInput.getTemplateZip() == null) {
       logger.error("Invalid request, template HTML not provided");
//       return Response.status(BAD_REQUEST)
//              .entity(buildResponseBody(BAD_REQUEST, AppErrorCodeEnum.PDFE_897, INVALID_REQUEST_MESSAGE))
//              .build();
       throw new RuntimeException();
     }

     generatePDFInput.setWorkingDir(workingDirectoryPath);

     return generatePDFInput;

    }).onFailure().invoke(error -> Response.status(INTERNAL_SERVER_ERROR)
            .entity(
                    buildResponseBody(
                            INTERNAL_SERVER_ERROR,
                            AppErrorCodeEnum.PDFE_907,
                            ERROR_GENERATING_PDF_MESSAGE))
            .build())
      .onItem().invoke(input -> {

      });

    return response
            .onFailure().invoke(error -> Response.status(INTERNAL_SERVER_ERROR)
                    .entity(
                            buildResponseBody(
                                    INTERNAL_SERVER_ERROR,
                                    AppErrorCodeEnum.PDFE_907,
                                    ERROR_GENERATING_PDF_MESSAGE))
                    .build())
            .onItem().invoke(generatePDFInput -> {
              if (generatePDFInput.getData() == null) {
                logger.error("Invalid request the PDF document input data are null");
                return Response.status(BAD_REQUEST)
                        .entity(buildResponseBody(BAD_REQUEST, AppErrorCodeEnum.PDFE_898, INVALID_REQUEST_MESSAGE))
                        .build();
              }

              try (BufferedInputStream inputStream = generatePDFService.generatePDF(generatePDFInput, workingDirPath, logger)){
                byte[] fileBytes = inputStream.readAllBytes();

                logger.debug("Returning generated pdf at {}", LocalDateTime.now());
                return Response.status(Response.Status.OK)
                        .header("content-type", generatePDFInput.isGenerateZipped() ? "application/zip" : "application/pdf")
                        .header("content-length", String.valueOf(fileBytes.length))
                        .header("content-disposition", "attachment; ")
                        .entity(fileBytes)
                        .build();
              } catch (PDFEngineException e) {
                logger.error("Error generating the PDF document", e);
                return Response.status(INTERNAL_SERVER_ERROR)
                        .entity(
                                buildResponseBody(
                                        INTERNAL_SERVER_ERROR,
                                        e.getErrorCode(),
                                        ERROR_GENERATING_PDF_MESSAGE))
                        .build();
              } catch (IOException e) {
                logger.error("Error handling the generated stream", e);
                return Response.status(INTERNAL_SERVER_ERROR)
                        .entity(
                                buildResponseBody(
                                        INTERNAL_SERVER_ERROR,
                                        AppErrorCodeEnum.PDFE_907,
                                        ERROR_GENERATING_PDF_MESSAGE))
                        .build();
              } finally {
                clearTempDirectory(workingDirPath);
              }
            });

  }

  private static Integer getHttpStatus(PDFEngineException e) {
    int status;
    if (e.getErrorCode().equals(AppErrorCodeEnum.PDFE_703) || e.getErrorCode().equals(AppErrorCodeEnum.PDFE_704) ||
            e.getErrorCode().equals(AppErrorCodeEnum.PDFE_705)) {
      status = INTERNAL_SERVER_ERROR;
    } else {
      status = BAD_REQUEST;
    }
    return status;
  }

  private ErrorResponse buildResponseBody(Integer status, AppErrorCodeEnum appErrorCode, String message) {
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

  private void clearTempDirectory(java.nio.file.Path workingDirPath) {
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