package it.gov.pagopa.pdf.engine.client;

import it.gov.pagopa.pdf.engine.client.impl.PdfEngineClientImpl;
import it.gov.pagopa.pdf.engine.model.PdfEngineRequest;
import it.gov.pagopa.pdf.engine.model.PdfEngineResponse;
import net.lingala.zip4j.ZipFile;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PdfEngineClientImplTest {

    @Test
    void testSingleton() {
        Assertions.assertDoesNotThrow(PdfEngineClientImpl::getInstance);
    }

    @Test
    void runOk() throws IOException {
        PdfEngineRequest pdfEngineRequest = buildPdfEngineRequest();

        CloseableHttpClient mockClient = mockHttpClient(HttpStatus.SC_OK, InputStream.nullInputStream());

        PdfEngineClientImpl client = new PdfEngineClientImpl(mockClient);
        PdfEngineResponse pdfEngineResponse = client.generatePDF(pdfEngineRequest);

        File tempPdf = new File(pdfEngineResponse.getTempPdfPath());
        Assertions.assertTrue(tempPdf.delete());
        Assertions.assertEquals(HttpStatus.SC_OK, pdfEngineResponse.getStatusCode());
    }

    @Test
    void runKoUnauthorized() throws IOException {
        PdfEngineRequest pdfEngineRequest = buildPdfEngineRequest();

        CloseableHttpClient mockClient = mockHttpClient(HttpStatus.SC_UNAUTHORIZED, InputStream.nullInputStream());

        PdfEngineClientImpl client = new PdfEngineClientImpl(mockClient);
        PdfEngineResponse pdfEngineResponse = client.generatePDF(pdfEngineRequest);

        Assertions.assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, pdfEngineResponse.getStatusCode());
        Assertions.assertNotNull(pdfEngineResponse.getErrorMessage());
    }

    @Test
    void runKo400() throws IOException {
        PdfEngineRequest pdfEngineRequest = buildPdfEngineRequest();

        String errorMessage = "\"Invalid request\"";
        String errorBody = "{\n" +
                "  \"errorId\": \"a3779a25-9c8a-4a6f-9272-a052119cfd2e\",\n" +
                "  \"httpStatusCode\": \"BAD_REQUEST\",\n" +
                "  \"httpStatusDescription\": \"Bad Request\",\n" +
                "  \"appErrorCode\": \"PDFE_898\",\n" +
                "  \"errors\": [\n" +
                "    {\n" +
                "      \"message\": " + errorMessage +
                "    }\n" +
                "  ]\n" +
                "}";

        CloseableHttpClient mockClient = mockHttpClient(HttpStatus.SC_BAD_REQUEST,
                new ByteArrayInputStream(errorBody.getBytes()));

        PdfEngineClientImpl client = new PdfEngineClientImpl(mockClient);
        PdfEngineResponse pdfEngineResponse = client.generatePDF(pdfEngineRequest);

        Assertions.assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, pdfEngineResponse.getStatusCode());
        Assertions.assertEquals(errorMessage.replace("\"", ""), pdfEngineResponse.getErrorMessage());
    }

    // -----------------------------------------------------------------
    // Test fixtures
    // -----------------------------------------------------------------

    private static PdfEngineRequest buildPdfEngineRequest() throws IOException {
        File tempDirectory = new File("temp");
        if (!tempDirectory.exists()) {
            Files.createDirectory(tempDirectory.toPath());
        }
        File targetFile = File.createTempFile("tempFile", ".txt", tempDirectory);
        ZipFile zipFile = new ZipFile(targetFile);

        PdfEngineRequest pdfEngineRequest = new PdfEngineRequest();
        try (InputStream inputStream = FileInputStream.nullInputStream()) {
            byte[] template = inputStream.readAllBytes();
            pdfEngineRequest.setTemplate(zipFile);
            pdfEngineRequest.setData(new String(template));
        } finally {
            targetFile.deleteOnExit();
            tempDirectory.deleteOnExit();
        }
        return pdfEngineRequest;
    }

    private static CloseableHttpClient mockHttpClient(int statusCode, InputStream responseBody) throws IOException {
        StatusLine mockStatusLine = mock(StatusLine.class);
        when(mockStatusLine.getStatusCode()).thenReturn(statusCode);

        HttpEntity mockEntity = mock(HttpEntity.class);
        when(mockEntity.getContent()).thenReturn(responseBody);

        CloseableHttpResponse mockResponse = mock(CloseableHttpResponse.class);
        when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);
        when(mockResponse.getEntity()).thenReturn(mockEntity);

        CloseableHttpClient mockClient = mock(CloseableHttpClient.class);
        when(mockClient.execute(any())).thenReturn(mockResponse);
        return mockClient;
    }
}
