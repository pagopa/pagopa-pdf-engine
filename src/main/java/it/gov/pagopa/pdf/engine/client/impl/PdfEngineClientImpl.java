package it.gov.pagopa.pdf.engine.client.impl;

import it.gov.pagopa.pdf.engine.client.PdfEngineClient;
import it.gov.pagopa.pdf.engine.model.AppErrorCodeEnum;
import it.gov.pagopa.pdf.engine.model.PdfEngineErrorResponse;
import it.gov.pagopa.pdf.engine.model.PdfEngineRequest;
import it.gov.pagopa.pdf.engine.model.PdfEngineResponse;
import it.gov.pagopa.pdf.engine.util.Constants;
import it.gov.pagopa.pdf.engine.util.ObjectMapperUtils;
import org.apache.commons.io.FileUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Client for the PDF Engine.
 *
 * <p>A single, pooled {@link CloseableHttpClient} instance is reused for the
 * lifetime of the JVM.</p>
 */
public class PdfEngineClientImpl implements PdfEngineClient {

    // ---------- Endpoints ----------
    private final String pdfEngineEndpoint = System.getenv().getOrDefault(
            "PDF_ENGINE_NODE_GENERATE_ENDPOINT", "http://localhost:3000/generate-pdf");
    private final String pdfEngineInfoEndpoint = System.getenv().getOrDefault(
            "PDF_ENGINE_NODE_INFO_ENDPOINT", "http://localhost:3000/info");

    private final Header subKeyHeader = new BasicHeader(
            "Ocp-Apim-Subscription-Key",
            System.getenv().getOrDefault("PDF_ENGINE_NODE_SUBKEY", "NO_SUB_KEY"));

    // ---------- HTTP timeouts (ms) ----------
    private static final int CONNECT_TIMEOUT_MS = envInt("PDF_ENGINE_HTTP_CONNECT_TIMEOUT_MS", 5_000);
    private static final int CONNECTION_REQUEST_TIMEOUT_MS = envInt("PDF_ENGINE_HTTP_CONN_REQUEST_TIMEOUT_MS", 2_000);
    private static final int SOCKET_TIMEOUT_MS = envInt("PDF_ENGINE_HTTP_SOCKET_TIMEOUT_MS", 30_000);
    private static final int RETRY_COUNT = envInt("PDF_ENGINE_HTTP_RETRY_COUNT", 2);

    // ---------- Connection pool ----------
    private static final int MAX_CONN_PER_ROUTE = envInt("PDF_ENGINE_HTTP_MAX_CONN_PER_ROUTE", 100);
    private static final int MAX_CONN_TOTAL = envInt("PDF_ENGINE_HTTP_MAX_CONN_TOTAL", MAX_CONN_PER_ROUTE);
    private static final long CONN_TTL_SECONDS = envLong("PDF_ENGINE_HTTP_CONN_TTL_SECONDS", 60L);
    private static final long IDLE_EVICT_SECONDS = envLong("PDF_ENGINE_HTTP_IDLE_EVICT_SECONDS", 30L);
    private static final int VALIDATE_AFTER_INACTIVITY_MS = envInt("PDF_ENGINE_HTTP_VALIDATE_AFTER_INACTIVITY_MS", 2_000);

    /**
     * Long-lived, thread-safe pooled HTTP client.
     */
    private final CloseableHttpClient httpClient;

    private static final class Holder {
        private static final PdfEngineClientImpl INSTANCE = new PdfEngineClientImpl();
    }

    public static PdfEngineClientImpl getInstance() {
        return Holder.INSTANCE;
    }

    private PdfEngineClientImpl() {
        this(buildDefaultHttpClient());
    }

    /**
     * Visible for tests: allows injecting a mocked or custom client.
     */
    protected PdfEngineClientImpl(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PdfEngineResponse generatePDF(PdfEngineRequest pdfEngineRequest) {
        try {
            HttpPost request = buildGenerateRequest(pdfEngineRequest);
            return executeGenerate(request);
        } catch (Exception e) {
            return buildExceptionResponse(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean info() {
        HttpGet request = new HttpGet(pdfEngineInfoEndpoint);
        request.setHeader(subKeyHeader);
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
        } catch (IOException e) {
            return false;
        }
    }

    // -----------------------------------------------------------------
    // HTTP client construction
    // -----------------------------------------------------------------

    /**
     * Builds the shared {@link CloseableHttpClient} with a pooled connection
     * manager, sensible timeouts and a retry handler for transient I/O errors.
     */
    private static CloseableHttpClient buildDefaultHttpClient() {
        PoolingHttpClientConnectionManager connectionManager =
                new PoolingHttpClientConnectionManager(CONN_TTL_SECONDS, TimeUnit.SECONDS);
        connectionManager.setMaxTotal(MAX_CONN_TOTAL);
        connectionManager.setDefaultMaxPerRoute(MAX_CONN_PER_ROUTE);
        connectionManager.setValidateAfterInactivity(VALIDATE_AFTER_INACTIVITY_MS);

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(CONNECT_TIMEOUT_MS)
                .setConnectionRequestTimeout(CONNECTION_REQUEST_TIMEOUT_MS)
                .setSocketTimeout(SOCKET_TIMEOUT_MS)
                .build();

        return HttpClientBuilder.create()
                .setConnectionManager(connectionManager)
                .setConnectionManagerShared(false)
                .setDefaultRequestConfig(requestConfig)
                .setRetryHandler(buildRetryHandler())
                .setConnectionTimeToLive(CONN_TTL_SECONDS, TimeUnit.SECONDS)
                .evictExpiredConnections()
                .evictIdleConnections(IDLE_EVICT_SECONDS, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Retry handler: retries on transient I/O failures (connect timeouts,
     * dropped keep-alive connections, read timeouts) but NOT on auth/SSL or
     * unknown-host errors. Critical to absorb single hiccups on APIM without
     * surfacing them as HTTP 500 to the caller.
     */
    private static DefaultHttpRequestRetryHandler buildRetryHandler() {
        return new DefaultHttpRequestRetryHandler(
                RETRY_COUNT,
                true,
                List.of(InterruptedIOException.class, UnknownHostException.class, SSLException.class)
        ) {
            @Override
            public boolean retryRequest(
                    IOException exception,
                    int executionCount,
                    org.apache.http.protocol.HttpContext context
            ) {
                if (executionCount > RETRY_COUNT) {
                    return false;
                }
                if (exception instanceof ConnectTimeoutException
                        || exception instanceof NoHttpResponseException
                        || exception instanceof SocketTimeoutException) {
                    return true;
                }
                return super.retryRequest(exception, executionCount, context);
            }
        };
    }

    // -----------------------------------------------------------------
    // Request / response handling
    // -----------------------------------------------------------------

    private HttpPost buildGenerateRequest(PdfEngineRequest pdfEngineRequest) {
        StringBody dataBody = new StringBody(pdfEngineRequest.getData(), ContentType.APPLICATION_JSON);
        FileBody templateBody = new FileBody(pdfEngineRequest.getTemplate().getFile(), ContentType.DEFAULT_BINARY);

        HttpEntity entity = MultipartEntityBuilder.create()
                .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                .addPart(Constants.DATA_KEY, dataBody)
                .addPart(Constants.TEMPLATE_KEY, templateBody)
                .build();

        HttpPost request = new HttpPost(pdfEngineEndpoint);
        request.setHeader(subKeyHeader);
        request.setEntity(entity);
        return request;
    }

    private PdfEngineResponse executeGenerate(HttpPost request) {
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            HttpEntity entityResponse = response.getEntity();

            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK && entityResponse != null) {
                try (InputStream inputStream = entityResponse.getContent()) {
                    return buildSuccessResponse(inputStream);
                }
            }
            return buildErrorResponse(response, entityResponse);
        } catch (Exception e) {
            return buildExceptionResponse(e);
        }
    }

    /**
     * Persists the PDF returned by the engine to a temporary file and returns
     * a successful {@link PdfEngineResponse} pointing at it.
     */
    private PdfEngineResponse buildSuccessResponse(InputStream pdfStream) throws IOException {
        File tempDirectory = new File("temp");
        if (!tempDirectory.exists()) {
            Files.createDirectory(tempDirectory.toPath());
        }
        File targetFile = File.createTempFile("tempFile", ".pdf", tempDirectory);
        FileUtils.copyInputStreamToFile(pdfStream, targetFile);

        PdfEngineResponse pdfEngineResponse = new PdfEngineResponse();
        pdfEngineResponse.setStatusCode(HttpStatus.SC_OK);
        pdfEngineResponse.setTempPdfPath(targetFile.getAbsolutePath());
        pdfEngineResponse.setTempDirectoryPath(tempDirectory.getAbsolutePath());
        return pdfEngineResponse;
    }

    /**
     * Builds an error {@link PdfEngineResponse} from an exception raised
     * while contacting the PDF engine.
     */
    private PdfEngineResponse buildExceptionResponse(Exception e) {
        PdfEngineResponse pdfEngineResponse = new PdfEngineResponse();
        pdfEngineResponse.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        pdfEngineResponse.setErrorMessage(String.format("Exception thrown during pdf generation process: %s", e));
        pdfEngineResponse.setErrorCode(AppErrorCodeEnum.PDFE_902.getErrorCode());
        return pdfEngineResponse;
    }

    /**
     * Builds an error {@link PdfEngineResponse} from a non-2xx response
     * returned by the PDF engine.
     */
    private PdfEngineResponse buildErrorResponse(CloseableHttpResponse response, HttpEntity entityResponse)
            throws IOException {
        PdfEngineResponse pdfEngineResponse = new PdfEngineResponse();
        pdfEngineResponse.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);

        if (response != null
                && response.getStatusLine() != null
                && response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
            pdfEngineResponse.setErrorMessage("Unauthorized call to PDF engine function");
        } else if (entityResponse != null) {
            String jsonString = EntityUtils.toString(entityResponse, StandardCharsets.UTF_8);
            if (!jsonString.isEmpty()) {
                PdfEngineErrorResponse errorResponse =
                        ObjectMapperUtils.mapString(jsonString, PdfEngineErrorResponse.class);
                if (errorResponse != null
                        && errorResponse.getErrors() != null
                        && !errorResponse.getErrors().isEmpty()
                        && errorResponse.getErrors().get(0) != null) {
                    pdfEngineResponse.setErrorCode(errorResponse.getAppStatusCode());
                    pdfEngineResponse.setErrorMessage(errorResponse.getErrors().get(0).getMessage());
                }
            }
        }

        if (pdfEngineResponse.getErrorMessage() == null) {
            pdfEngineResponse.setErrorMessage("Unknown error in PDF engine function");
        }
        return pdfEngineResponse;
    }

    // -----------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------

    private static int envInt(String name, int defaultValue) {
        return Integer.parseInt(System.getenv().getOrDefault(name, Integer.toString(defaultValue)));
    }

    private static long envLong(String name, long defaultValue) {
        return Long.parseLong(System.getenv().getOrDefault(name, Long.toString(defaultValue)));
    }
}
