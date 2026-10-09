package it.gov.pagopa.pdf.engine.client.impl;

import it.gov.pagopa.pdf.engine.client.PdfEngineClient;
import it.gov.pagopa.pdf.engine.model.AppErrorCodeEnum;
import it.gov.pagopa.pdf.engine.model.PdfEngineErrorResponse;
import it.gov.pagopa.pdf.engine.model.PdfEngineRequest;
import it.gov.pagopa.pdf.engine.model.PdfEngineResponse;
import it.gov.pagopa.pdf.engine.util.ObjectMapperUtils;
import org.apache.commons.io.FileUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Client for the PDF Engine
 */
public class PdfEngineClientImpl implements PdfEngineClient, AutoCloseable {

    private static PdfEngineClientImpl instance = null;

    private final String pdfEngineEndpoint = System.getenv().getOrDefault("PDF_ENGINE_NODE_GENERATE_ENDPOINT", "http://localhost:3000/generate-pdf");
    private final String pdfEngineInfoEndpoint = System.getenv().getOrDefault("PDF_ENGINE_NODE_INFO_ENDPOINT", "http://localhost:3000/info");

    private static final String DATA_KEY = "data";

    private final Header subKeyHeader = new BasicHeader("Ocp-Apim-Subscription-Key", System.getenv().getOrDefault("PDF_ENGINE_NODE_SUBKEY", "NO_SUB_KEY"));

    private final CloseableHttpClient httpClient;

    private PdfEngineClientImpl() {
        this(HttpClientBuilder.create().disableCookieManagement());
    }

    public PdfEngineClientImpl(HttpClientBuilder clientBuilder) {
        this.httpClient = clientBuilder.build();
    }

    public static synchronized PdfEngineClientImpl getInstance() {
        if (instance == null) {
            PdfEngineClientImpl newInstance = new PdfEngineClientImpl();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    newInstance.close();
                } catch (IOException e) {
                    LoggerFactory.getLogger(PdfEngineClientImpl.class)
                            .warn("Error closing PDF Engine HTTP client", e);
                }
            }, "pdf-engine-http-client-shutdown"));

            instance = newInstance;
        }

        return instance;
    }
    
    @Override
    public void close() throws IOException {
        httpClient.close();
    }

    /**
     * Builds the PDF generation request and executes it using the shared HTTP client.
     *
     * @param pdfEngineRequest Data and template for PDF generation
     * @return response containing the PDF location or error details and status
     */
    public PdfEngineResponse generatePDF(PdfEngineRequest pdfEngineRequest) {

        PdfEngineResponse pdfEngineResponse = new PdfEngineResponse();

        // Build and execute the request using the shared HTTP client
        try {
            //Encode template and data
            StringBody dataBody = new StringBody(pdfEngineRequest.getData(), ContentType.APPLICATION_JSON);
            FileBody templateBody = new FileBody(pdfEngineRequest.getTemplate().getFile(), ContentType.DEFAULT_BINARY);

            //Build the multipart request
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            builder.addPart(DATA_KEY, dataBody);
            builder.addPart("template", templateBody);

            HttpEntity entity = builder.build();

            //Set endpoint and auth key
            HttpPost request = new HttpPost(pdfEngineEndpoint);
            request.setHeader(subKeyHeader);
            request.setEntity(entity);

            pdfEngineResponse = handlePdfEngineResponse(httpClient, request);
        } catch (Exception e) {
            handleExceptionErrorMessage(pdfEngineResponse, e);
        }

        return pdfEngineResponse;
    }

    /**
     * Method to contact the underlying service info endpoint
     * @return boolean to determine if the service is available or otherwise
     */
	public boolean info() {
		try {
			HttpGet request = new HttpGet(pdfEngineInfoEndpoint);
			request.setHeader(subKeyHeader);
			return handleInfoResponse(httpClient, request);
		} catch (IOException e) {
			return false;
		}
	}

    /**
     * Calls the PDF Engine and handles its response, updating the PdfEngineResponse accordingly
     *
     * @param client  The previously generated client
     * @param request The request to the PDF engine
     * @return pdf engine response
     */
    private static PdfEngineResponse handlePdfEngineResponse(CloseableHttpClient client, HttpPost request) {
        PdfEngineResponse pdfEngineResponse = new PdfEngineResponse();
        //Execute call
        try (CloseableHttpResponse response = client.execute(request)) {
            //Retrieve response
            HttpEntity entityResponse = response.getEntity();

            //Handles response
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK && entityResponse != null) {
                try (InputStream inputStream = entityResponse.getContent()) {
                    pdfEngineResponse.setStatusCode(HttpStatus.SC_OK);

                    saveTempPdf(pdfEngineResponse, inputStream);
                }
            } else {
                pdfEngineResponse.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);

                handleErrorResponse(pdfEngineResponse, response, entityResponse);
                EntityUtils.consume(entityResponse);
            }
        } catch (Exception e) {
            handleExceptionErrorMessage(pdfEngineResponse, e);
        }

        return pdfEngineResponse;
    }

	private static boolean handleInfoResponse(CloseableHttpClient client, HttpGet request) throws IOException {
		try (CloseableHttpResponse response = client.execute(request)) {
			boolean successful = response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;

			EntityUtils.consume(response.getEntity());

			return successful;
		}
	}

    /**
     * Saves pdf as temporary file
     *
     * @param pdfEngineResponse Pdf engine response
     * @param inputStream       InputStream pdf
     * @throws IOException In case of error to save
     */
    private static void saveTempPdf(PdfEngineResponse pdfEngineResponse, InputStream inputStream) throws IOException {
        File tempDirectory = new File("temp");
        if (!tempDirectory.exists()) {
            Files.createDirectory(tempDirectory.toPath());
        }

        File targetFile = File.createTempFile("tempFile", ".pdf", tempDirectory);

        FileUtils.copyInputStreamToFile(inputStream, targetFile);

        pdfEngineResponse.setTempPdfPath(targetFile.getAbsolutePath());
        pdfEngineResponse.setTempDirectoryPath(tempDirectory.getAbsolutePath());
    }

    /**
     * Handles error message in case of error thrown
     *
     * @param pdfEngineResponse Pdf engine respone
     * @param e                 Error thrown
     */
    private static void handleExceptionErrorMessage(PdfEngineResponse pdfEngineResponse, Exception e) {
        pdfEngineResponse.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        pdfEngineResponse.setErrorMessage(String.format("Exception thrown during pdf generation process: %s", e));
        pdfEngineResponse.setErrorCode(AppErrorCodeEnum.PDFE_902.getErrorCode());
    }

    /**
     * Handles error response from the PDF Engine
     *
     * @param pdfEngineResponse Response to update
     * @param response          Response from the PDF engine
     * @param entityResponse    Response content from the PDF Engine
     * @throws IOException in case of error encoding to string
     */
    private static void handleErrorResponse(
            PdfEngineResponse pdfEngineResponse,
            CloseableHttpResponse response,
            HttpEntity entityResponse
    ) throws IOException {
        //Verify if unauthorized
        if (response != null &&
                response.getStatusLine() != null &&
                response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED
        ) {
            pdfEngineResponse.setErrorMessage("Unauthorized call to PDF engine function");

        } else if (entityResponse != null) {
            //Handle JSON response
            String jsonString = EntityUtils.toString(entityResponse, StandardCharsets.UTF_8);

            if (!jsonString.isEmpty()) {
                PdfEngineErrorResponse errorResponse =
                        ObjectMapperUtils.mapString(jsonString, PdfEngineErrorResponse.class);

                if (errorResponse != null &&
                        errorResponse.getErrors() != null &&
                        !errorResponse.getErrors().isEmpty() &&
                        errorResponse.getErrors().get(0) != null
                ) {
                    pdfEngineResponse.setErrorCode(errorResponse.getAppStatusCode());
                    pdfEngineResponse.setErrorMessage(errorResponse.getErrors().get(0).getMessage());
                }
            }
        }

        if (pdfEngineResponse.getErrorMessage() == null) {
            pdfEngineResponse.setErrorMessage("Unknown error in PDF engine function");
        }
    }
}
