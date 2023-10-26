package it.gov.pagopa.pdf.engine.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import it.gov.pagopa.pdf.engine.client.PdfEngineClient;
import it.gov.pagopa.pdf.engine.model.InfoResponse;
import jakarta.inject.Inject;
import lombok.SneakyThrows;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class InfoResourceTest {

    @Inject
    private InfoResource sut;

    @InjectMock
    @RestClient
    private PdfEngineClient pdfEngineClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @SneakyThrows
    void shouldReturnStatusOkWhenNodeServiceIsReachable() {
        Mockito.doNothing().when(pdfEngineClient).info();
        String responseString =
                given()
                        .when().get("/info")
                        .then()
                        .statusCode(200)
                        .contentType("application/json")
                        .extract()
                        .asString();


        assertNotNull(responseString);
        InfoResponse response = objectMapper.readValue(responseString, InfoResponse.class);
        assertNotNull(response);
        assertNotNull(response.getName());
        assertNotNull(response.getEnvironment());
        assertNotNull(response.getDescription());
        assertNotNull(response.getVersion());
    }

    @Test
    @SneakyThrows
    void shouldReturnStatusUnavailableWhenNodeServiceIsNotReachable() {
        Mockito.doThrow(new RuntimeException()).when(pdfEngineClient).info();
        String responseString =
                given()
                        .when().get("/info")
                        .then()
                        .statusCode(503)
                        .contentType("application/json")
                        .extract()
                        .asString();


        assertNotNull(responseString);
        InfoResponse response = objectMapper.readValue(responseString, InfoResponse.class);
        assertNotNull(response);
        assertNotNull(response.getName());
        assertNotNull(response.getEnvironment());
        assertNotNull(response.getDescription());
        assertNotNull(response.getVersion());
    }

}