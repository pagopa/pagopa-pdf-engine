package it.gov.pagopa.pdf.engine.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.smallrye.mutiny.Uni;
import it.gov.pagopa.pdf.engine.client.PdfEngineClient;
import it.gov.pagopa.pdf.engine.exception.GeneratePDFException;
import it.gov.pagopa.pdf.engine.model.AppErrorCodeEnum;
import it.gov.pagopa.pdf.engine.model.ErrorResponse;
import jakarta.inject.Inject;
import lombok.SneakyThrows;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.InputStream;
import java.util.Objects;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@QuarkusTest
class GeneratePdfResourceTest {

    @Inject
    private InfoResource sut;

    @InjectMock
    @RestClient
    private PdfEngineClient pdfEngineClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @SneakyThrows
    void shouldReturnPdfWhenSuccessfullCallOnValidData() {

        try (InputStream input = Objects.requireNonNull(this.getClass().getClassLoader()
                .getResource("valid_pdf.pdf")).openStream()) {

            when(pdfEngineClient.generatePDF(Mockito.any())).thenReturn(Uni.createFrom().item(input));
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

        }
    }

    @Test
    @SneakyThrows
    void shouldReturnManagedErrorWhenUnsuccessfullCallOnValidData() {

        when(pdfEngineClient.generatePDF(Mockito.any())).thenThrow(new GeneratePDFException(AppErrorCodeEnum.PDFE_902,"Error"));
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


    }

    @Test
    @SneakyThrows
    void shouldReturnManagedErrorWhenCallOnInvalidData() {

        when(pdfEngineClient.generatePDF(Mockito.any())).thenThrow(new GeneratePDFException(AppErrorCodeEnum.PDFE_902,"Error"));
        ErrorResponse content =
                given()
                        .multiPart("template", new File("./src/main/resources/valid_template.html"))
                        .when().post("/generate-pdf")
                        .then()
                        .statusCode(400)
                        .contentType("application/json")
                        .extract()
                        .as(ErrorResponse.class);
        assertNotNull(content);

    }

}