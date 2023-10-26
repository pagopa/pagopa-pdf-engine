package it.gov.pagopa.pdf.engine.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.smallrye.mutiny.Uni;
import it.gov.pagopa.pdf.engine.exception.GeneratePDFException;
import it.gov.pagopa.pdf.engine.exception.RequestBodyParseException;
import it.gov.pagopa.pdf.engine.model.AppErrorCodeEnum;
import it.gov.pagopa.pdf.engine.model.ErrorResponse;
import it.gov.pagopa.pdf.engine.model.GeneratePDFInput;
import it.gov.pagopa.pdf.engine.model.PdfEngineResponse;
import it.gov.pagopa.pdf.engine.service.GeneratePDFService;
import it.gov.pagopa.pdf.engine.service.ParseRequestBodyService;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@QuarkusTest
class GeneratePdfResourceTest {

    @InjectMock
    private GeneratePDFService generatePDFService;

    @InjectMock
    private ParseRequestBodyService parseRequestBodyService;
    private static final String PATTERN_FORMAT = "yyyy.MM.dd.HH.mm.ss";


    @BeforeEach
    public void before() {
        Mockito.reset(generatePDFService, parseRequestBodyService);
    }

    @Test
    @SneakyThrows
    void shouldReturnPdfWhenSuccessfullCallOnValidData() {

        File workingDirectory = new File("workingDirectoryPath");
        if (!workingDirectory.exists()) {
            try {
                Files.createDirectory(workingDirectory.toPath());
            } catch (FileAlreadyExistsException e) {
            }
        }

        try (InputStream input = Objects.requireNonNull(this.getClass().getClassLoader()
                .getResource("valid_pdf.pdf")).openStream();
             BufferedInputStream bufferedInputStream = new BufferedInputStream(input)) {

            PdfEngineResponse pdfEngineResponse = new PdfEngineResponse();
            pdfEngineResponse.setBufferedInputStream(bufferedInputStream);
            pdfEngineResponse.setWorkDirPath(Files.createTempDirectory(
                    workingDirectory.toPath(),
                    DateTimeFormatter.ofPattern(PATTERN_FORMAT)
                            .withZone(ZoneId.systemDefault())
                            .format(Instant.now())));

            when(generatePDFService.generatePDF(Mockito.any(),Mockito.any(),Mockito.any()))
                    .thenReturn(Uni.createFrom().item(pdfEngineResponse));
            when(parseRequestBodyService.retrieveInputData(Mockito.any())).thenCallRealMethod();
            byte[] content =
                    given()
                            .multiPart("template", new File("./src/main/resources/valid_template.html"))
                            .formParam("data", "{\"key1\":\"test\"}")
                            .when().post("/generate-pdf")
                            .then()
                            .statusCode(200)
                            .contentType("application/pdf")
                            .extract()
                            .asByteArray();
            assertNotNull(content);
            verify(parseRequestBodyService).retrieveInputData(Mockito.any());
            verify(generatePDFService).generatePDF(Mockito.any(),Mockito.any(),Mockito.any());

        }
    }

    @Test
    @SneakyThrows
    void shouldReturnErrorWhenCallOnInvalidData() {

        File workingDirectory = new File("workingDirectoryPath");
        if (!workingDirectory.exists()) {
            try {
                Files.createDirectory(workingDirectory.toPath());
            } catch (FileAlreadyExistsException e) {
            }
        }

        try (InputStream input = Objects.requireNonNull(this.getClass().getClassLoader()
                .getResource("valid_pdf.pdf")).openStream();
             BufferedInputStream ignored = new BufferedInputStream(input)) {

            PdfEngineResponse pdfEngineResponse = new PdfEngineResponse();
            pdfEngineResponse.setBufferedInputStream(null);
            pdfEngineResponse.setWorkDirPath(Files.createTempDirectory(
                    workingDirectory.toPath(),
                    DateTimeFormatter.ofPattern(PATTERN_FORMAT)
                            .withZone(ZoneId.systemDefault())
                            .format(Instant.now())));

            when(generatePDFService.generatePDF(Mockito.any(),Mockito.any(),Mockito.any()))
                    .thenReturn(Uni.createFrom().item(pdfEngineResponse));
            when(parseRequestBodyService.retrieveInputData(Mockito.any())).thenCallRealMethod();
            ErrorResponse content =
                    given()
                            .multiPart("template", new File("./src/main/resources/valid_template.html"))
                            .formParam("data", "{\"key1\":\"test\"}")
                            .when().post("/generate-pdf")
                            .then()
                            .statusCode(500)
                            .contentType("application/json")
                            .extract()
                            .as(ErrorResponse.class);
            assertNotNull(content);
            verify(parseRequestBodyService).retrieveInputData(Mockito.any());
            verify(generatePDFService).generatePDF(Mockito.any(),Mockito.any(),Mockito.any());

        }
    }

    @Test
    @SneakyThrows
    void shouldReturnManagedErrorWhenUnsuccessfullCallOnValidData() {

        when(generatePDFService.generatePDF(Mockito.any(),Mockito.any(),Mockito.any()))
                .thenThrow(new GeneratePDFException(AppErrorCodeEnum.PDFE_902,"Error"));
        when(parseRequestBodyService.retrieveInputData(Mockito.any())).thenCallRealMethod();
        ErrorResponse content =
                given()
                        .multiPart("template", new File("./src/main/resources/valid_template.html"))
                        .formParam("data", "{\"key1\":\"test\"}")
                        .when().post("/generate-pdf")
                        .then()
                        .statusCode(400)
                        .contentType("application/json")
                        .extract()
                        .as(ErrorResponse.class);
            assertNotNull(content);
            verify(parseRequestBodyService).retrieveInputData(Mockito.any());
            verify(generatePDFService).generatePDF(Mockito.any(),Mockito.any(),Mockito.any());

    }

    @Test
    @SneakyThrows
    void shouldReturnManagedErrorWhenMissingParsingData() {

        ErrorResponse content =
                given()
                        .multiPart("template", new File("./src/main/resources/valid_template.html"))
                        .formParam("data", "{\"key1\":\"test\"}")
                        .when().post("/generate-pdf")
                        .then()
                        .statusCode(500)
                        .contentType("application/json")
                        .extract()
                        .as(ErrorResponse.class);
        assertNotNull(content);
        verify(parseRequestBodyService).retrieveInputData(Mockito.any());
        verifyNoInteractions(generatePDFService);

    }

    @Test
    @SneakyThrows
    void shouldReturnManagedErrorWhenParsingException() {

        when(parseRequestBodyService.retrieveInputData(Mockito.any())).thenThrow(
                new RequestBodyParseException(AppErrorCodeEnum.PDFE_709, "Parsing Error"));

        ErrorResponse content =
                given()
                        .multiPart("template", new File("./src/main/resources/valid_template.html"))
                        .formParam("data", "{\"key1\":\"test\"}")
                        .when().post("/generate-pdf")
                        .then()
                        .statusCode(500)
                        .contentType("application/json")
                        .extract()
                        .as(ErrorResponse.class);
        assertNotNull(content);
        verify(parseRequestBodyService).retrieveInputData(Mockito.any());
        verifyNoInteractions(generatePDFService);

    }

    @Test
    @SneakyThrows
    void shouldReturnManagedErrorWhenMissingData() {

        GeneratePDFInput generatePDFInput = new GeneratePDFInput();
        generatePDFInput.setData(null);
        when(parseRequestBodyService.retrieveInputData(Mockito.any())).thenReturn(generatePDFInput);

        ErrorResponse content =
                given()
                        .multiPart("template", new File("./src/main/resources/valid_template.html"))
                        .formParam("data", "{\"key1\":\"test\"}")
                        .when().post("/generate-pdf")
                        .then()
                        .statusCode(400)
                        .contentType("application/json")
                        .extract()
                        .as(ErrorResponse.class);
        assertNotNull(content);
        verify(parseRequestBodyService).retrieveInputData(Mockito.any());
        verifyNoInteractions(generatePDFService);

    }

    @Test
    @SneakyThrows
    void shouldReturnErrorWhenServiceReturnsGenericError() {

        File workingDirectory = new File("workingDirectoryPath");
        if (!workingDirectory.exists()) {
            try {
                Files.createDirectory(workingDirectory.toPath());
            } catch (FileAlreadyExistsException e) {
            }
        }

        try (InputStream input = Objects.requireNonNull(this.getClass().getClassLoader()
                .getResource("valid_pdf.pdf")).openStream();
             BufferedInputStream bufferedInputStream = new BufferedInputStream(input)) {

            PdfEngineResponse pdfEngineResponse = new PdfEngineResponse();
            pdfEngineResponse.setBufferedInputStream(bufferedInputStream);
            pdfEngineResponse.setWorkDirPath(Files.createTempDirectory(
                    workingDirectory.toPath(),
                    DateTimeFormatter.ofPattern(PATTERN_FORMAT)
                            .withZone(ZoneId.systemDefault())
                            .format(Instant.now())));

            when(generatePDFService.generatePDF(Mockito.any(),Mockito.any(),Mockito.any()))
                    .thenAnswer(invoke -> new Exception());
            when(parseRequestBodyService.retrieveInputData(Mockito.any())).thenCallRealMethod();
            ErrorResponse content =
                    given()
                            .multiPart("template", new File("./src/main/resources/valid_template.html"))
                            .formParam("data", "{\"key1\":\"test\"}")
                            .when().post("/generate-pdf")
                            .then()
                            .statusCode(400)
                            .contentType("application/json")
                            .extract()
                            .as(ErrorResponse.class);
            assertNotNull(content);
            verify(parseRequestBodyService).retrieveInputData(Mockito.any());
            verify(generatePDFService).generatePDF(Mockito.any(),Mockito.any(),Mockito.any());

        }
    }

}