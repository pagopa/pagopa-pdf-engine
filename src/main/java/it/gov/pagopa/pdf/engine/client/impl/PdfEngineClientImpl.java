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
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.SocketConfig;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Client for the PDF Engine
 */
public class PdfEngineClientImpl implements PdfEngineClient, AutoCloseable {

	private static PdfEngineClientImpl instance = null;

	private final String pdfEngineEndpoint = System.getenv().getOrDefault("PDF_ENGINE_NODE_GENERATE_ENDPOINT",
			"http://localhost:3000/generate-pdf");
	private final String pdfEngineInfoEndpoint = System.getenv().getOrDefault("PDF_ENGINE_NODE_INFO_ENDPOINT",
			"http://localhost:3000/info");

	private static final String DATA_KEY = "data";
	private static final String HTTP_DEADLINE_ATTRIBUTE = PdfEngineClientImpl.class.getName() + ".deadline";

	private final Header subKeyHeader = new BasicHeader("Ocp-Apim-Subscription-Key",
			System.getenv().getOrDefault("PDF_ENGINE_NODE_SUBKEY", "NO_SUB_KEY"));

	private final CloseableHttpClient httpClient;
	private final ScheduledThreadPoolExecutor deadlineExecutor;

	// Maximum number of pooled HTTP connections across all routes.
	private final int maxConnectionsTotal = readPositiveIntEnv("PDF_ENGINE_HTTP_MAX_TOTAL", 32);
	// Maximum number of pooled HTTP connections per route (per target host).
	private final int maxConnectionsPerRoute = readPositiveIntEnv("PDF_ENGINE_HTTP_MAX_PER_ROUTE", 16);
	// Maximum wait, in milliseconds, to lease a connection from the pool.
	private final int poolAcquireTimeoutMs = readPositiveIntEnv("PDF_ENGINE_HTTP_POOL_TIMEOUT_MS", 1_000);
	// Timeout, in milliseconds, for a TCP connection attempt.
	private final int connectTimeoutMs = readPositiveIntEnv("PDF_ENGINE_HTTP_CONNECT_TIMEOUT_MS", 3_000);
	// Maximum wait for data, in milliseconds, during a blocking socket read.
	private final int socketTimeoutMs = readPositiveIntEnv("PDF_ENGINE_HTTP_SOCKET_TIMEOUT_MS", 15_000);
	// Maximum connection age, in milliseconds, after which it cannot be reused.
	private final long connectionTtlMs = readPositiveLongEnv("PDF_ENGINE_HTTP_TTL_MS", 600_000L);
	// Maximum idle time, in milliseconds, after which a connection is evicted from
	// the pool.
	private final long idleConnectionTimeoutMs = readPositiveLongEnv("PDF_ENGINE_HTTP_IDLE_TIMEOUT_MS", 60_000L);
	// Maximum additional attempts for recoverable transport failures. Zero value
	// disables retries.
	private final int maxRetries = readNonNegativeIntEnv("PDF_ENGINE_HTTP_MAX_RETRIES", 1);
	// Base wait before each allowed retry, in milliseconds.
	private final int retryBackoffMs = readNonNegativeIntEnv("PDF_ENGINE_HTTP_RETRY_BACKOFF_MS", 200);
	// Maximum random delay added to the backoff, in milliseconds (inclusive). Zero
	// disables jitter.
	private final int retryJitterMs = readNonNegativeIntEnv("PDF_ENGINE_HTTP_RETRY_JITTER_MS", 200);
	// Total HTTP exchange budget, including pool wait, retries and response body
	// reads.
	private final int requestTimeoutMs = readPositiveIntEnv("PDF_ENGINE_HTTP_REQUEST_TIMEOUT_MS", 20_000);

	private PdfEngineClientImpl() {
		this(HttpClientBuilder.create().disableCookieManagement());
	}

	public PdfEngineClientImpl(HttpClientBuilder clientBuilder) {
		if (maxConnectionsPerRoute > maxConnectionsTotal) {
			throw new IllegalArgumentException(
					"PDF_ENGINE_HTTP_MAX_PER_ROUTE must not exceed " + "PDF_ENGINE_HTTP_MAX_TOTAL");
		}

		RequestConfig requestConfig = RequestConfig.custom().setConnectionRequestTimeout(poolAcquireTimeoutMs)
				.setConnectTimeout(connectTimeoutMs).setSocketTimeout(socketTimeoutMs).build();

		SocketConfig socketConfig = SocketConfig.custom().setSoTimeout(socketTimeoutMs).build();

		clientBuilder.setMaxConnTotal(maxConnectionsTotal);
		clientBuilder.setMaxConnPerRoute(maxConnectionsPerRoute);

		clientBuilder.setDefaultRequestConfig(requestConfig);
		clientBuilder.setDefaultSocketConfig(socketConfig);

		clientBuilder.setConnectionTimeToLive(connectionTtlMs, TimeUnit.MILLISECONDS);

		clientBuilder.disableCookieManagement();
		clientBuilder.setRetryHandler(createRetryHandler());

		clientBuilder.evictExpiredConnections();
		clientBuilder.evictIdleConnections(idleConnectionTimeoutMs, TimeUnit.MILLISECONDS);

		this.httpClient = clientBuilder.build();

		// One shared scheduler per client; each request schedules only its own
		// cancellation.
		this.deadlineExecutor = new ScheduledThreadPoolExecutor(1, runnable -> {
			Thread thread = new Thread(runnable, "pdf-engine-http-deadline");
			thread.setDaemon(true);
			return thread;
		});
		this.deadlineExecutor.setRemoveOnCancelPolicy(true);
	}

	public static synchronized PdfEngineClientImpl getInstance() {
		if (instance == null) {
			PdfEngineClientImpl newInstance = new PdfEngineClientImpl();

			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				try {
					newInstance.close();
				} catch (IOException e) {
					LoggerFactory.getLogger(PdfEngineClientImpl.class).warn("Error closing PDF Engine HTTP client", e);
				}
			}, "pdf-engine-http-client-shutdown"));

			instance = newInstance;
		}

		return instance;
	}

	@Override
	public void close() throws IOException {
		try {
			httpClient.close();
		} finally {
			deadlineExecutor.shutdownNow();
		}
	}

	/**
	 * Builds the PDF generation request and executes it using the shared HTTP
	 * client.
	 *
	 * @param pdfEngineRequest Data and template for PDF generation
	 * @return response containing the PDF location or error details and status
	 */
	public PdfEngineResponse generatePDF(PdfEngineRequest pdfEngineRequest) {

		PdfEngineResponse pdfEngineResponse = new PdfEngineResponse();

		// Build and execute the request using the shared HTTP client
		try {
			// Encode template and data
			StringBody dataBody = new StringBody(pdfEngineRequest.getData(), ContentType.APPLICATION_JSON);
			FileBody templateBody = new FileBody(pdfEngineRequest.getTemplate().getFile(), ContentType.DEFAULT_BINARY);

			// The caller owns this directory and removes it after processing the request.
			String configuredWorkingDir = pdfEngineRequest.getWorkingDirPath();
			if (configuredWorkingDir == null || configuredWorkingDir.isBlank()) {
				throw new IllegalArgumentException("workingDirPath must not be blank");
			}

			Path workingDirPath = Path.of(configuredWorkingDir).toAbsolutePath().normalize();
			if (!Files.isDirectory(workingDirPath)) {
				throw new IOException("workingDirPath must refer to an existing directory");
			}

			// Build the multipart request
			MultipartEntityBuilder builder = MultipartEntityBuilder.create();
			builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
			builder.addPart(DATA_KEY, dataBody);
			builder.addPart("template", templateBody);

			HttpEntity entity = builder.build();

			// Set endpoint and auth key
			HttpPost request = new HttpPost(pdfEngineEndpoint);
			request.setHeader(subKeyHeader);
			request.setEntity(entity);

			pdfEngineResponse = handlePdfEngineResponse(httpClient, request, workingDirPath);
		} catch (Exception e) {
			handleExceptionErrorMessage(pdfEngineResponse, e);
		}

		return pdfEngineResponse;
	}

	/**
	 * Method to contact the underlying service info endpoint
	 *
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
	 * Calls the PDF Engine and handles its response, updating the PdfEngineResponse
	 * accordingly
	 *
	 * @param client         The previously generated client
	 * @param request        The request to the PDF engine
	 * @param workingDirPath The existing directory owned by this request
	 * @return pdf engine response
	 */
	private PdfEngineResponse handlePdfEngineResponse(CloseableHttpClient client, HttpPost request,
			Path workingDirPath) {
		PdfEngineResponse pdfEngineResponse = new PdfEngineResponse();

		// This method owns the deadline and closes it on every exit path.
		try (HttpRequestDeadline deadline = new HttpRequestDeadline(deadlineExecutor, request::abort,
				requestTimeoutMs)) {
			HttpClientContext context = HttpClientContext.create();
			context.setAttribute(HTTP_DEADLINE_ATTRIBUTE, deadline);

			// The helper consumes and closes the entire response before completing the
			// deadline.
			executePdfRequest(client, request, context, deadline, pdfEngineResponse, workingDirPath);

			// A delayed timer must not allow a result that arrived after the budget
			// expired.
			deadline.complete();
		} catch (Exception e) {
			if (pdfEngineResponse.getTempPdfPath() != null) {
				deleteTempPdf(new File(pdfEngineResponse.getTempPdfPath()), e);
				pdfEngineResponse.setTempPdfPath(null);
				pdfEngineResponse.setTempDirectoryPath(null);
			}
			handleExceptionErrorMessage(pdfEngineResponse, e);
		}

		return pdfEngineResponse;
	}

	/**
	 * Executes the POST and owns its HTTP response; the deadline is borrowed from
	 * the caller.
	 */
	private static void executePdfRequest(CloseableHttpClient client, HttpPost request, HttpClientContext context,
			HttpRequestDeadline deadline, PdfEngineResponse pdfEngineResponse, Path workingDirPath) throws IOException {
		try (CloseableHttpResponse response = client.execute(request, context)) {
			consumePdfResponse(response, pdfEngineResponse, workingDirPath);
		} catch (Exception e) {
			checkDeadlineAfterFailure(deadline, e);
			throw e;
		}
	}

	/**
	 * Reads the body of a borrowed response; executePdfRequest closes the response
	 * afterwards.
	 */
	private static void consumePdfResponse(CloseableHttpResponse response, PdfEngineResponse pdfEngineResponse,
			Path workingDirPath) throws IOException {
		HttpEntity entityResponse = response.getEntity();

		if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK && entityResponse != null) {
			try (InputStream inputStream = entityResponse.getContent()) {
				saveTempPdf(pdfEngineResponse, inputStream, workingDirPath);
				pdfEngineResponse.setStatusCode(HttpStatus.SC_OK);
			}
		} else {
			pdfEngineResponse.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			handleErrorResponse(pdfEngineResponse, response, entityResponse);
			EntityUtils.consume(entityResponse);
		}
	}

	private boolean handleInfoResponse(CloseableHttpClient client, HttpGet request) throws IOException {
		// This method owns the deadline, including any retries performed by the HTTP
		// client.
		try (HttpRequestDeadline deadline = new HttpRequestDeadline(deadlineExecutor, request::abort,
				requestTimeoutMs)) {
			HttpClientContext context = HttpClientContext.create();
			context.setAttribute(HTTP_DEADLINE_ATTRIBUTE, deadline);

			boolean successful = executeInfoRequest(client, request, context, deadline);
			deadline.complete();
			return successful;
		}
	}

	/**
	 * Executes the GET and closes its response while the caller's deadline is still
	 * active.
	 */
	private static boolean executeInfoRequest(CloseableHttpClient client, HttpGet request, HttpClientContext context,
			HttpRequestDeadline deadline) throws IOException {
		try (CloseableHttpResponse response = client.execute(request, context)) {
			boolean successful = response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
			EntityUtils.consume(response.getEntity());
			return successful;
		} catch (IOException e) {
			checkDeadlineAfterFailure(deadline, e);
			throw e;
		}
	}

	private static void checkDeadlineAfterFailure(HttpRequestDeadline deadline, Exception failure)
			throws SocketTimeoutException {
		try {
			deadline.complete();
		} catch (SocketTimeoutException timeout) {
			timeout.initCause(failure);
			throw timeout;
		}
	}

	/**
	 * Saves the original PDF in the request directory, for cleanup by the caller.
	 *
	 * @param pdfEngineResponse Response receiving the temporary PDF location
	 * @param inputStream       Original PDF response stream
	 * @param workingDirPath    Existing directory owned by this request
	 * @throws IOException If saving the PDF fails
	 */
	private static void saveTempPdf(PdfEngineResponse pdfEngineResponse, InputStream inputStream, Path workingDirPath)
			throws IOException {
		File targetFile = Files.createTempFile(workingDirPath, "tempFile", ".pdf").toFile();
		try {
			FileUtils.copyInputStreamToFile(inputStream, targetFile);
			pdfEngineResponse.setTempPdfPath(targetFile.getAbsolutePath());
			pdfEngineResponse.setTempDirectoryPath(workingDirPath.toString());
		} catch (IOException | RuntimeException e) {
			deleteTempPdf(targetFile, e);
			throw e;
		}
	}

	private static void deleteTempPdf(File targetFile, Exception failure) {
		try {
			Files.deleteIfExists(targetFile.toPath());
		} catch (IOException cleanupFailure) {
			failure.addSuppressed(cleanupFailure);
			LoggerFactory.getLogger(PdfEngineClientImpl.class)
					.warn("Error deleting temporary PDF after a failed HTTP exchange", cleanupFailure);
		}
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
	private static void handleErrorResponse(PdfEngineResponse pdfEngineResponse, CloseableHttpResponse response,
			HttpEntity entityResponse) throws IOException {
		// Verify if unauthorized
		if (response != null && response.getStatusLine() != null
				&& response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
			pdfEngineResponse.setErrorMessage("Unauthorized call to PDF engine function");

		} else if (entityResponse != null) {
			// Handle JSON response
			String jsonString = EntityUtils.toString(entityResponse, StandardCharsets.UTF_8);

			if (!jsonString.isEmpty()) {
				PdfEngineErrorResponse errorResponse = ObjectMapperUtils.mapString(jsonString,
						PdfEngineErrorResponse.class);

				if (errorResponse != null && errorResponse.getErrors() != null && !errorResponse.getErrors().isEmpty()
						&& errorResponse.getErrors().get(0) != null) {
					pdfEngineResponse.setErrorCode(errorResponse.getAppStatusCode());
					pdfEngineResponse.setErrorMessage(errorResponse.getErrors().get(0).getMessage());
				}
			}
		}

		if (pdfEngineResponse.getErrorMessage() == null) {
			pdfEngineResponse.setErrorMessage("Unknown error in PDF engine function");
		}
	}

	/**
	 * Applies a configurable delay only to retries allowed by HttpClient's rules.
	 */
	private HttpRequestRetryHandler createRetryHandler() {
		// false prevents retrying a fully sent non-idempotent request such as our POST.
		DefaultHttpRequestRetryHandler retryRules = new DefaultHttpRequestRetryHandler(maxRetries, false);

		return (exception, executionCount, context) -> canRetryAfterBackoff(exception, executionCount, context,
				retryRules, (HttpRequestDeadline) context.getAttribute(HTTP_DEADLINE_ATTRIBUTE));
	}

	/**
	 * The deadline parameter is borrowed from the HTTP context. It is owned and
	 * closed by handlePdfEngineResponse or handleInfoResponse, after the whole
	 * exchange ends.
	 */
	private boolean canRetryAfterBackoff(IOException exception, int executionCount, HttpContext context,
			HttpRequestRetryHandler retryRules, HttpRequestDeadline deadline) {
		if (Thread.currentThread().isInterrupted() || (deadline != null && deadline.isExpired())
				|| !retryRules.retryRequest(exception, executionCount, context)) {
			return false;
		}

		long delayMs = retryBackoffMs + ThreadLocalRandom.current().nextLong(retryJitterMs + 1L);

		try {
			if (!awaitRetryDelay(deadline, delayMs)) {
				return false;
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}

		return !Thread.currentThread().isInterrupted() && (deadline == null || !deadline.isExpired())
				&& retryRules.retryRequest(exception, executionCount, context);
	}

	/**
	 * Waits using a borrowed deadline, without closing the caller's request budget.
	 */
	private static boolean awaitRetryDelay(HttpRequestDeadline deadline, long delayMs) throws InterruptedException {
		if (deadline != null) {
			return deadline.awaitRetryDelay(delayMs);
		}
		// Preserve the configured delay for contexts without an HTTP request deadline.
		TimeUnit.MILLISECONDS.sleep(delayMs);
		return true;
	}

	private static long readPositiveLongEnv(String name, long defaultValue) {
		String configuredValue = System.getenv(name);

		if (configuredValue == null) {
			return defaultValue;
		}

		final long parsedValue;
		try {
			parsedValue = Long.parseLong(configuredValue.trim());
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(name + " must contain a positive integer", e);
		}

		if (parsedValue <= 0) {
			throw new IllegalArgumentException(name + " must be greater than zero");
		}

		return parsedValue;
	}

	private static int readPositiveIntEnv(String name, int defaultValue) {
		long parsedValue = readPositiveLongEnv(name, defaultValue);

		if (parsedValue > Integer.MAX_VALUE) {
			throw new IllegalArgumentException(name + " must not exceed " + Integer.MAX_VALUE);
		}

		return (int) parsedValue;
	}

	private static int readNonNegativeIntEnv(String name, int defaultValue) {
		String configuredValue = System.getenv(name);
		if (configuredValue == null) {
			return defaultValue;
		}

		final int parsedValue;
		try {
			parsedValue = Integer.parseInt(configuredValue.trim());
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(name + " must contain an integer between 0 and " + Integer.MAX_VALUE, e);
		}

		if (parsedValue < 0) {
			throw new IllegalArgumentException(name + " must be greater than or equal to zero");
		}

		return parsedValue;
	}
}