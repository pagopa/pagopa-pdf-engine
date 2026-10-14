package it.gov.pagopa.pdf.engine.client;

import it.gov.pagopa.pdf.engine.client.impl.PdfEngineClientImpl;
import it.gov.pagopa.pdf.engine.model.PdfEngineRequest;
import it.gov.pagopa.pdf.engine.model.PdfEngineResponse;
import net.lingala.zip4j.ZipFile;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;

import com.sun.net.httpserver.HttpServer;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpClientConnection;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.NoHttpResponseException;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PdfEngineClientImplTest {

	// JUnit creates and removes an isolated directory for each test.
	@TempDir
	Path workingDir;

	private static final String[] HTTP_SETTING_NAMES = { "PDF_ENGINE_HTTP_MAX_TOTAL", "PDF_ENGINE_HTTP_MAX_PER_ROUTE",
			"PDF_ENGINE_HTTP_POOL_TIMEOUT_MS", "PDF_ENGINE_HTTP_CONNECT_TIMEOUT_MS",
			"PDF_ENGINE_HTTP_SOCKET_TIMEOUT_MS", "PDF_ENGINE_HTTP_REQUEST_TIMEOUT_MS", "PDF_ENGINE_HTTP_TTL_MS",
			"PDF_ENGINE_HTTP_IDLE_TIMEOUT_MS" };

	@Test
	void testSingleton() {
		Assertions.assertDoesNotThrow(PdfEngineClientImpl::getInstance);
	}

	@Test
	void runOk() throws IOException {

		File targetFile = Files.createTempFile(workingDir, "template-", ".zip").toFile();
		try (ZipFile zipFile = new ZipFile(targetFile)) {
			byte[] template;
			PdfEngineRequest pdfEngineRequest = new PdfEngineRequest();
			pdfEngineRequest.setWorkingDirPath(workingDir.toString());
			try (InputStream inputStream = FileInputStream.nullInputStream()) {
				template = inputStream.readAllBytes();

				pdfEngineRequest.setTemplate(zipFile);
				pdfEngineRequest.setData(new String(template));
			}

			HttpClientBuilder mockBuilder = mock(HttpClientBuilder.class);
			CloseableHttpClient mockClient = mock(CloseableHttpClient.class);

			CloseableHttpResponse mockResponse = mock(CloseableHttpResponse.class);
			StatusLine mockStatusLine = mock(StatusLine.class);
			when(mockStatusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
			when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);

			HttpEntity mockEntity = mock(HttpEntity.class);
			when(mockEntity.getContent()).thenAnswer(invocation -> InputStream.nullInputStream());
			when(mockResponse.getEntity()).thenReturn(mockEntity);

			when(mockClient.execute(any(HttpPost.class), any(HttpClientContext.class))).thenReturn(mockResponse);
			when(mockBuilder.build()).thenReturn(mockClient);

			try (PdfEngineClientImpl client = new PdfEngineClientImpl(mockBuilder)) {
				for (int i = 0; i < 2; i++) {
					PdfEngineResponse response = client.generatePDF(pdfEngineRequest);

					Assertions.assertEquals(HttpStatus.SC_OK, response.getStatusCode());

					File tempPdf = new File(response.getTempPdfPath());
					Path expectedDirectory = workingDir.toAbsolutePath().normalize();
					Assertions.assertEquals(expectedDirectory, tempPdf.toPath().getParent());
					Assertions.assertEquals(expectedDirectory.toString(), response.getTempDirectoryPath());
					Assertions.assertTrue(tempPdf.delete());
					Assertions.assertTrue(Files.isDirectory(workingDir),
							"The request directory belongs to the caller and must remain available");
				}

				verify(mockBuilder, times(1)).build();
				verify(mockClient, times(2)).execute(any(HttpPost.class), any(HttpClientContext.class));
				verify(mockResponse, times(2)).close();

				// Il client deve restare aperto tra le richieste.
				verify(mockClient, never()).close();
			}

			// La chiusura avviene al termine del suo ciclo di vita.
			verify(mockClient, times(1)).close();
		}
	}

	@Test
	void runKoUnauthorized() throws IOException {

		File targetFile = Files.createTempFile(workingDir, "template-", ".zip").toFile();
		try (ZipFile zipFile = new ZipFile(targetFile)) {
			byte[] template;
			PdfEngineRequest pdfEngineRequest = new PdfEngineRequest();
			pdfEngineRequest.setWorkingDirPath(workingDir.toString());
			try (InputStream inputStream = FileInputStream.nullInputStream()) {
				template = inputStream.readAllBytes();

				pdfEngineRequest.setTemplate(zipFile);
				pdfEngineRequest.setData(new String(template));
			}

			HttpClientBuilder mockBuilder = mock(HttpClientBuilder.class);
			CloseableHttpClient mockClient = mock(CloseableHttpClient.class);

			CloseableHttpResponse mockResponse = mock(CloseableHttpResponse.class);
			StatusLine mockStatusLine = mock(StatusLine.class);
			when(mockStatusLine.getStatusCode()).thenReturn(HttpStatus.SC_UNAUTHORIZED);
			when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);

			HttpEntity mockEntity = mock(HttpEntity.class);
			when(mockEntity.getContent()).thenReturn(InputStream.nullInputStream());
			when(mockResponse.getEntity()).thenReturn(mockEntity);

			when(mockClient.execute(any(HttpPost.class), any(HttpClientContext.class))).thenReturn(mockResponse);
			when(mockBuilder.build()).thenReturn(mockClient);

			try (PdfEngineClientImpl client = new PdfEngineClientImpl(mockBuilder)) {
				PdfEngineResponse pdfEngineResponse = client.generatePDF(pdfEngineRequest);

				Assertions.assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, pdfEngineResponse.getStatusCode());
				Assertions.assertEquals("Unauthorized call to PDF engine function",
						pdfEngineResponse.getErrorMessage());
			}

		}
	}

	@Test
	void runKo400() throws IOException {
		File targetFile = Files.createTempFile(workingDir, "template-", ".zip").toFile();
		try (ZipFile zipFile = new ZipFile(targetFile)) {
			byte[] template;
			PdfEngineRequest pdfEngineRequest = new PdfEngineRequest();
			pdfEngineRequest.setWorkingDirPath(workingDir.toString());
			try (InputStream inputStream = FileInputStream.nullInputStream()) {
				template = inputStream.readAllBytes();
				pdfEngineRequest.setTemplate(zipFile);
				pdfEngineRequest.setData(new String(template));
			}

			HttpClientBuilder mockBuilder = mock(HttpClientBuilder.class);
			CloseableHttpClient mockClient = mock(CloseableHttpClient.class);

			CloseableHttpResponse mockResponse = mock(CloseableHttpResponse.class);
			StatusLine mockStatusLine = mock(StatusLine.class);
			when(mockStatusLine.getStatusCode()).thenReturn(HttpStatus.SC_BAD_REQUEST);
			when(mockResponse.getStatusLine()).thenReturn(mockStatusLine);

			HttpEntity mockEntity = mock(HttpEntity.class);
			String errorMessage = "\"Invalid request\"";
			String errorResponseBody = "{\n" + "  \"errorId\": \"a3779a25-9c8a-4a6f-9272-a052119cfd2e\",\n"
					+ "  \"httpStatusCode\": \"BAD_REQUEST\",\n" + "  \"httpStatusDescription\": \"Bad Request\",\n"
					+ "  \"appErrorCode\": \"PDFE_898\",\n" + "  \"errors\": [\n" + "    {\n" + "      \"message\": "
					+ errorMessage + "    }\n" + "  ]\n" + "}";
			when(mockEntity.getContent()).thenReturn(new ByteArrayInputStream(errorResponseBody.getBytes()));
			when(mockResponse.getEntity()).thenReturn(mockEntity);

			when(mockClient.execute(any(HttpPost.class), any(HttpClientContext.class))).thenReturn(mockResponse);
			when(mockBuilder.build()).thenReturn(mockClient);

			try (PdfEngineClientImpl client = new PdfEngineClientImpl(mockBuilder)) {
				PdfEngineResponse pdfEngineResponse = client.generatePDF(pdfEngineRequest);

				Assertions.assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, pdfEngineResponse.getStatusCode());
				Assertions.assertEquals(errorMessage.replace("\"", ""), pdfEngineResponse.getErrorMessage());
			}

		}
	}

	@Test
	void infoReusesTheSameTcpConnection() throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);

		Set<Integer> sourcePorts = ConcurrentHashMap.newKeySet();
		AtomicInteger receivedRequests = new AtomicInteger();

		server.createContext("/info", exchange -> {
			try (exchange) {
				sourcePorts.add(exchange.getRemoteAddress().getPort());
				receivedRequests.incrementAndGet();

				byte[] body = "OK".getBytes(StandardCharsets.UTF_8);
				exchange.sendResponseHeaders(HttpStatus.SC_OK, body.length);

				try (OutputStream output = exchange.getResponseBody()) {
					output.write(body);
				}
			}
		});

		server.start();

		try {
			String endpoint = "http://127.0.0.1:" + server.getAddress().getPort() + "/info";

			EnvironmentVariables environment = new EnvironmentVariables().set("PDF_ENGINE_NODE_INFO_ENDPOINT", endpoint)
					.set("PDF_ENGINE_HTTP_MAX_TOTAL", "4").set("PDF_ENGINE_HTTP_MAX_PER_ROUTE", "2")
					.set("PDF_ENGINE_HTTP_POOL_TIMEOUT_MS", "1000").set("PDF_ENGINE_HTTP_CONNECT_TIMEOUT_MS", "2000")
					.set("PDF_ENGINE_HTTP_SOCKET_TIMEOUT_MS", "2000").set("PDF_ENGINE_HTTP_TTL_MS", "60000")
					.set("PDF_ENGINE_HTTP_IDLE_TIMEOUT_MS", "60000");

			environment.execute(() -> {
				try (PdfEngineClientImpl client = new PdfEngineClientImpl(HttpClientBuilder.create())) {

					for (int i = 0; i < 20; i++) {
						Assertions.assertTrue(client.info(), "Call " + i + " failed");
					}

					Assertions.assertEquals(20, receivedRequests.get());

					Assertions.assertEquals(1, sourcePorts.size(),
							"Sequential calls must reuse the same TCP connection");
				}
			});
		} finally {
			server.stop(0);
		}
	}

	@Test
	void exhaustedPoolFailsPromptlyAndRecovers() throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);

		ExecutorService serverExecutor = Executors.newFixedThreadPool(2);
		ExecutorService callerExecutor = Executors.newSingleThreadExecutor();
		server.setExecutor(serverExecutor);

		CountDownLatch firstRequestReceived = new CountDownLatch(1);
		CountDownLatch releaseFirstRequest = new CountDownLatch(1);
		AtomicInteger receivedRequests = new AtomicInteger();

		server.createContext("/info", exchange -> {
			try (exchange) {
				int requestNumber = receivedRequests.incrementAndGet();

				if (requestNumber == 1) {
					firstRequestReceived.countDown();

					try {
						if (!releaseFirstRequest.await(10, TimeUnit.SECONDS)) {
							exchange.sendResponseHeaders(HttpStatus.SC_SERVICE_UNAVAILABLE, -1);
							return;
						}
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return;
					}
				}

				byte[] body = "OK".getBytes(StandardCharsets.UTF_8);
				exchange.sendResponseHeaders(HttpStatus.SC_OK, body.length);

				try (OutputStream output = exchange.getResponseBody()) {
					output.write(body);
				}
			}
		});

		server.start();

		try {
			String endpoint = "http://127.0.0.1:" + server.getAddress().getPort() + "/info";

			EnvironmentVariables environment = new EnvironmentVariables().set("PDF_ENGINE_NODE_INFO_ENDPOINT", endpoint)
					.set("PDF_ENGINE_HTTP_MAX_TOTAL", "2").set("PDF_ENGINE_HTTP_MAX_PER_ROUTE", "1")
					.set("PDF_ENGINE_HTTP_POOL_TIMEOUT_MS", "250").set("PDF_ENGINE_HTTP_CONNECT_TIMEOUT_MS", "2000")
					.set("PDF_ENGINE_HTTP_SOCKET_TIMEOUT_MS", "10000").set("PDF_ENGINE_HTTP_TTL_MS", "60000")
					.set("PDF_ENGINE_HTTP_IDLE_TIMEOUT_MS", "60000");

			environment.execute(() -> {
				try (PdfEngineClientImpl client = new PdfEngineClientImpl(HttpClientBuilder.create())) {

					Future<Boolean> firstCall = callerExecutor.submit(client::info);

					Assertions.assertTrue(firstRequestReceived.await(3, TimeUnit.SECONDS),
							"The first call must reach the server");

					long started = System.nanoTime();
					boolean secondCall = client.info();
					long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);

					Assertions.assertFalse(secondCall);

					Assertions.assertTrue(elapsedMs < 2000, "Pool acquisition took " + elapsedMs + " ms");

					Assertions.assertFalse(firstCall.isDone());
					Assertions.assertEquals(1, receivedRequests.get());

					releaseFirstRequest.countDown();

					Assertions.assertTrue(firstCall.get(3, TimeUnit.SECONDS));
					Assertions.assertTrue(client.info());
					Assertions.assertEquals(2, receivedRequests.get());
				}
			});
		} finally {
			releaseFirstRequest.countDown();
			callerExecutor.shutdownNow();
			server.stop(0);
			serverExecutor.shutdownNow();
		}
	}

	@Test
	void readTimeoutFailsPromptlyAndClientRecovers() throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);

		ExecutorService serverExecutor = Executors.newFixedThreadPool(2);
		server.setExecutor(serverExecutor);

		CountDownLatch releaseFirstRequest = new CountDownLatch(1);
		AtomicInteger receivedRequests = new AtomicInteger();

		byte[] pdf = "%PDF-1.4\nlocal test PDF\n%%EOF".getBytes(StandardCharsets.UTF_8);

		File templateFile = Files.createTempFile(workingDir, "pdf-engine-template-", ".zip").toFile();

		server.createContext("/generate-pdf", exchange -> {
			try (exchange) {
				exchange.getRequestBody().readAllBytes();
				int requestNumber = receivedRequests.incrementAndGet();

				if (requestNumber == 1) {
					try {
						if (!releaseFirstRequest.await(5, TimeUnit.SECONDS)) {
							exchange.sendResponseHeaders(HttpStatus.SC_SERVICE_UNAVAILABLE, -1);
							return;
						}
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return;
					}
				}

				exchange.sendResponseHeaders(HttpStatus.SC_OK, pdf.length);

				try (OutputStream output = exchange.getResponseBody()) {
					output.write(pdf);
				}
			}
		});

		server.start();

		try (ZipFile template = new ZipFile(templateFile)) {
			String endpoint = "http://127.0.0.1:" + server.getAddress().getPort() + "/generate-pdf";

			EnvironmentVariables environment = new EnvironmentVariables()
					.set("PDF_ENGINE_NODE_GENERATE_ENDPOINT", endpoint).set("PDF_ENGINE_HTTP_MAX_TOTAL", "1")
					.set("PDF_ENGINE_HTTP_MAX_PER_ROUTE", "1").set("PDF_ENGINE_HTTP_POOL_TIMEOUT_MS", "1000")
					.set("PDF_ENGINE_HTTP_CONNECT_TIMEOUT_MS", "2000").set("PDF_ENGINE_HTTP_SOCKET_TIMEOUT_MS", "500")
					.set("PDF_ENGINE_HTTP_TTL_MS", "60000").set("PDF_ENGINE_HTTP_IDLE_TIMEOUT_MS", "60000");

			PdfEngineRequest request = new PdfEngineRequest();
			request.setWorkingDirPath(workingDir.toString());
			request.setData("{}");
			request.setTemplate(template);

			environment.execute(() -> {
				try (PdfEngineClientImpl client = new PdfEngineClientImpl(HttpClientBuilder.create())) {

					long started = System.nanoTime();

					PdfEngineResponse timedOut = client.generatePDF(request);

					long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);

					Assertions.assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, timedOut.getStatusCode());

					Assertions.assertEquals("PDFE_902", timedOut.getErrorCode());

					Assertions.assertNotNull(timedOut.getErrorMessage());

					Assertions.assertTrue(timedOut.getErrorMessage().contains("SocketTimeoutException"),
							timedOut.getErrorMessage());

					Assertions.assertTrue(elapsedMs < 2000, "Read timeout took " + elapsedMs + " ms");

					Assertions.assertEquals(1, receivedRequests.get());

					releaseFirstRequest.countDown();

					PdfEngineResponse recovered = client.generatePDF(request);

					Assertions.assertEquals(HttpStatus.SC_OK, recovered.getStatusCode(), recovered.getErrorMessage());

					File generatedPdf = new File(recovered.getTempPdfPath());

					try {
						Assertions.assertArrayEquals(pdf, Files.readAllBytes(generatedPdf.toPath()));

						Assertions.assertEquals(2, receivedRequests.get());
					} finally {
						Files.deleteIfExists(generatedPdf.toPath());
					}
				}
			});
		} finally {
			releaseFirstRequest.countDown();
			server.stop(0);
			serverExecutor.shutdownNow();
			Files.deleteIfExists(templateFile.toPath());
		}
	}

	@Test
	void defaultsWorkWhenHttpSettingsAreMissing() throws Exception {
		EnvironmentVariables environment = new EnvironmentVariables();

		for (String name : HTTP_SETTING_NAMES) {
			environment.set(name, (String) null);
		}

		environment.execute(() -> Assertions.assertDoesNotThrow(() -> {
			try (PdfEngineClientImpl client = new PdfEngineClientImpl(HttpClientBuilder.create())) {
				// Construction and closing must succeed using the defaults.
			}
		}));
	}

	@Test
	void rejectsInvalidNumericHttpSettings() throws Exception {
		String[] invalidValues = { "", "abc", "0", "-1" };

		for (String name : HTTP_SETTING_NAMES) {
			for (String value : invalidValues) {
				assertInvalidHttpSetting(name, value);
			}
		}
	}

	@Test
	void rejectsValuesOutsideSupportedNumericRange() throws Exception {
		String[] intSettings = { "PDF_ENGINE_HTTP_MAX_TOTAL", "PDF_ENGINE_HTTP_MAX_PER_ROUTE",
				"PDF_ENGINE_HTTP_POOL_TIMEOUT_MS", "PDF_ENGINE_HTTP_CONNECT_TIMEOUT_MS",
				"PDF_ENGINE_HTTP_SOCKET_TIMEOUT_MS", "PDF_ENGINE_HTTP_REQUEST_TIMEOUT_MS" };

		for (String name : intSettings) {
			assertInvalidHttpSetting(name, "2147483648");
		}

		assertInvalidHttpSetting("PDF_ENGINE_HTTP_TTL_MS", "9223372036854775808");

		assertInvalidHttpSetting("PDF_ENGINE_HTTP_IDLE_TIMEOUT_MS", "9223372036854775808");
	}

	@Test
	void rejectsPerRouteLimitAboveTotal() throws Exception {
		// The valid fixture has maxTotal = 4.
		assertInvalidHttpSetting("PDF_ENGINE_HTTP_MAX_PER_ROUTE", "5");
	}

	@Test
	void retryPolicyHonorsConfigurationAndProtectsSentPost() throws Exception {
		String[] configuredValues = { null, "0", "1", "2" };

		for (String configuredValue : configuredValues) {
			int retryLimit = configuredValue == null ? 1 : Integer.parseInt(configuredValue);

			validHttpEnvironment().set("PDF_ENGINE_HTTP_MAX_RETRIES", configuredValue)
					.set("PDF_ENGINE_HTTP_RETRY_BACKOFF_MS", "0").set("PDF_ENGINE_HTTP_RETRY_JITTER_MS", "0")
					.execute(() -> {
						HttpClientBuilder builder = mock(HttpClientBuilder.class);
						CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
						when(builder.build()).thenReturn(httpClient);

						try (PdfEngineClientImpl client = new PdfEngineClientImpl(builder)) {
							ArgumentCaptor<HttpRequestRetryHandler> captor = ArgumentCaptor
									.forClass(HttpRequestRetryHandler.class);
							verify(builder).setRetryHandler(captor.capture());
							verify(builder, never()).disableAutomaticRetries();
							HttpRequestRetryHandler retryHandler = captor.getValue();

							HttpClientContext context = HttpClientContext.create();
							HttpGet get = new HttpGet("http://localhost/info");
							context.setAttribute(HttpCoreContext.HTTP_REQUEST, get);
							context.setAttribute(HttpCoreContext.HTTP_REQ_SENT, true);
							NoHttpResponseException recoverable = new NoHttpResponseException("Connection closed");

							// A GET may recover, but only within the configured retry limit.
							for (int attempt = 1; attempt <= retryLimit + 1; attempt++) {
								Assertions.assertEquals(attempt <= retryLimit,
										retryHandler.retryRequest(recoverable, attempt, context),
										"retryLimit=" + retryLimit + ", executionCount=" + attempt);
							}

							// Timeouts must fail without automatically repeating the request.
							Assertions.assertFalse(retryHandler
									.retryRequest(new SocketTimeoutException("Read timed out"), 1, context));
							Assertions.assertFalse(retryHandler
									.retryRequest(new ConnectTimeoutException("Connect timed out"), 1, context));

							HttpPost post = new HttpPost("http://localhost/generate-pdf");
							context.setAttribute(HttpCoreContext.HTTP_REQUEST, post);

							// A fully sent POST must not be retried.
							context.setAttribute(HttpCoreContext.HTTP_REQ_SENT, true);
							Assertions.assertFalse(retryHandler.retryRequest(recoverable, 1, context));

							// An incompletely sent POST may recover if retries are enabled.
							context.setAttribute(HttpCoreContext.HTTP_REQ_SENT, false);
							Assertions.assertEquals(retryLimit > 0, retryHandler.retryRequest(recoverable, 1, context));

							// An explicitly aborted request must stay aborted.
							get.abort();
							context.setAttribute(HttpCoreContext.HTTP_REQUEST, get);
							context.setAttribute(HttpCoreContext.HTTP_REQ_SENT, true);
							Assertions.assertFalse(retryHandler.retryRequest(recoverable, 1, context));
						}
					});
		}

		for (String value : new String[] { "", "abc", "-1", "2147483648" }) {
			assertInvalidHttpSetting("PDF_ENGINE_HTTP_MAX_RETRIES", value);
		}
	}

	@Test
	void infoRetriesAfterBackoffAndRecovers() throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		AtomicInteger receivedRequests = new AtomicInteger();
		AtomicLong firstRequestNanos = new AtomicLong();
		AtomicLong secondRequestNanos = new AtomicLong();

		server.createContext("/info", exchange -> {
			try (exchange) {
				if (receivedRequests.incrementAndGet() == 1) {
					firstRequestNanos.set(System.nanoTime());
					// Close without an HTTP response to simulate a recoverable failure.
					return;
				}

				secondRequestNanos.set(System.nanoTime());
				byte[] body = "OK".getBytes(StandardCharsets.UTF_8);
				exchange.sendResponseHeaders(200, body.length);
				exchange.getResponseBody().write(body);
			}
		});
		server.start();

		try {
			validHttpEnvironment()
					.set("PDF_ENGINE_NODE_INFO_ENDPOINT", "http://127.0.0.1:" + server.getAddress().getPort() + "/info")
					.set("PDF_ENGINE_HTTP_MAX_RETRIES", "1").set("PDF_ENGINE_HTTP_RETRY_BACKOFF_MS", "200")
					.set("PDF_ENGINE_HTTP_RETRY_JITTER_MS", "200").execute(() -> {
						try (PdfEngineClientImpl client = new PdfEngineClientImpl(HttpClientBuilder.create())) {
							Assertions.assertTrue(client.info());
							Assertions.assertEquals(2, receivedRequests.get());

							long spacingMs = TimeUnit.NANOSECONDS
									.toMillis(secondRequestNanos.get() - firstRequestNanos.get());

							// Allow a small tolerance for timer precision.
							Assertions.assertTrue(spacingMs >= 180, "Retry arrived too early: " + spacingMs + " ms");
						}
					});
		} finally {
			server.stop(0);
		}
	}

	@Test
	void retryBackoffStopsOnInterruptionOrAbort() throws Exception {
		validHttpEnvironment().set("PDF_ENGINE_HTTP_MAX_RETRIES", "1").set("PDF_ENGINE_HTTP_RETRY_BACKOFF_MS", "2000")
				.set("PDF_ENGINE_HTTP_RETRY_JITTER_MS", "0").execute(() -> {
					HttpClientBuilder builder = mock(HttpClientBuilder.class);
					when(builder.build()).thenReturn(mock(CloseableHttpClient.class));

					try (PdfEngineClientImpl client = new PdfEngineClientImpl(builder)) {
						ArgumentCaptor<HttpRequestRetryHandler> captor = ArgumentCaptor
								.forClass(HttpRequestRetryHandler.class);
						verify(builder).setRetryHandler(captor.capture());

						for (boolean interrupt : new boolean[] { true, false }) {
							HttpGet request = new HttpGet("http://localhost/info");
							HttpClientContext context = HttpClientContext.create();
							context.setAttribute(HttpCoreContext.HTTP_REQUEST, request);
							context.setAttribute(HttpCoreContext.HTTP_REQ_SENT, true);

							AtomicReference<Boolean> result = new AtomicReference<>();
							AtomicBoolean interruptedAfterCall = new AtomicBoolean();

							Thread worker = new Thread(() -> {
								result.set(captor.getValue()
										.retryRequest(new NoHttpResponseException("Connection closed"), 1, context));
								interruptedAfterCall.set(Thread.currentThread().isInterrupted());
							}, "retry-backoff-test");
							worker.start();

							try {
								Assertions.assertTrue(awaitRetryBackoff(worker),
										"Worker did not enter the backoff wait");

								if (interrupt) {
									worker.interrupt();
								} else {
									request.abort();
								}

								worker.join(4000);
								Assertions.assertFalse(worker.isAlive());
								Assertions.assertEquals(Boolean.FALSE, result.get());
								Assertions.assertEquals(interrupt, interruptedAfterCall.get());
							} finally {
								worker.interrupt();
								worker.join(1000);
							}
						}
					}
				});

		for (String name : new String[] { "PDF_ENGINE_HTTP_RETRY_BACKOFF_MS", "PDF_ENGINE_HTTP_RETRY_JITTER_MS" }) {
			for (String value : new String[] { "", "abc", "-1", "2147483648" }) {
				assertInvalidHttpSetting(name, value);
			}
		}
	}

	@Test
	void fullySentPostIsNotRetriedWhenResponseIsLost() throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		AtomicInteger receivedRequests = new AtomicInteger();
		AtomicInteger receivedBodyBytes = new AtomicInteger();
		File templateFile = Files.createTempFile(workingDir, "pdf-engine-template-", ".zip").toFile();

		server.createContext("/generate-pdf", exchange -> {
			try (exchange) {
				receivedRequests.incrementAndGet();
				// Receive the complete body, then close without sending a response.
				receivedBodyBytes.set(exchange.getRequestBody().readAllBytes().length);
			}
		});
		server.start();

		try (ZipFile template = new ZipFile(templateFile)) {
			PdfEngineRequest request = new PdfEngineRequest();
			request.setWorkingDirPath(workingDir.toString());
			request.setData("{}");
			request.setTemplate(template);

			validHttpEnvironment()
					.set("PDF_ENGINE_NODE_GENERATE_ENDPOINT",
							"http://127.0.0.1:" + server.getAddress().getPort() + "/generate-pdf")
					.set("PDF_ENGINE_HTTP_MAX_RETRIES", "1").set("PDF_ENGINE_HTTP_RETRY_BACKOFF_MS", "200")
					.set("PDF_ENGINE_HTTP_RETRY_JITTER_MS", "200").execute(() -> {
						try (PdfEngineClientImpl client = new PdfEngineClientImpl(HttpClientBuilder.create())) {
							PdfEngineResponse response = client.generatePDF(request);

							Assertions.assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatusCode());
							Assertions.assertEquals("PDFE_902", response.getErrorCode());
							Assertions.assertNotNull(response.getErrorMessage());
							Assertions.assertTrue(response.getErrorMessage().contains("NoHttpResponseException"),
									response.getErrorMessage());
							Assertions.assertTrue(receivedBodyBytes.get() > 0);
							Assertions.assertEquals(1, receivedRequests.get(),
									"A fully sent POST must not be submitted again");
						}
					});
		} finally {
			server.stop(0);
			Files.deleteIfExists(templateFile.toPath());
		}
	}

	@Test
	void deadlineStopsSlowPdfDownloadAndClientRecovers() throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		ExecutorService serverExecutor = Executors.newFixedThreadPool(2);
		ScheduledExecutorService responseScheduler = Executors.newSingleThreadScheduledExecutor();
		server.setExecutor(serverExecutor);
		AtomicInteger receivedRequests = new AtomicInteger();
		byte[] pdf = "%PDF-1.4\nlocal test PDF\n%%EOF".getBytes(StandardCharsets.UTF_8);
		File templateFile = Files.createTempFile(workingDir, "pdf-engine-template-", ".zip").toFile();

		server.createContext("/generate-pdf", exchange -> {
			try (exchange) {
				exchange.getRequestBody().readAllBytes();
				int number = receivedRequests.incrementAndGet();
				exchange.sendResponseHeaders(HttpStatus.SC_OK, pdf.length);
				try (OutputStream output = exchange.getResponseBody()) {
					if (number == 1) {
						BlockingQueue<Byte> chunks = new LinkedBlockingQueue<>();
						AtomicInteger nextByte = new AtomicInteger();

						// Model a server that produces a new PDF byte every 75 ms.
						ScheduledFuture<?> producer = responseScheduler.scheduleWithFixedDelay(() -> {
							int index = nextByte.getAndIncrement();
							if (index < pdf.length) {
								chunks.add(pdf[index]);
							}
						}, 0, 75, TimeUnit.MILLISECONDS);

						try {
							for (int i = 0; i < pdf.length; i++) {
								// Wait for an actual chunk; all socket writes stay on this handler thread.
								Byte value = chunks.poll(5, TimeUnit.SECONDS);
								if (value == null) {
									throw new IOException("Scheduled PDF chunk was not produced");
								}
								output.write(value);
								output.flush();
							}
						} finally {
							producer.cancel(false);
						}
					} else {
						output.write(pdf);
					}
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} catch (IOException e) {
				// Closing the connection at the deadline can interrupt server-side writes.
			}
		});
		server.start();

		try (ZipFile template = new ZipFile(templateFile)) {
			PdfEngineRequest request = new PdfEngineRequest();
			request.setWorkingDirPath(workingDir.toString());
			request.setData("{}");
			request.setTemplate(template);

			validHttpEnvironment()
					.set("PDF_ENGINE_NODE_GENERATE_ENDPOINT",
							"http://127.0.0.1:" + server.getAddress().getPort() + "/generate-pdf")
					.set("PDF_ENGINE_HTTP_MAX_TOTAL", "1").set("PDF_ENGINE_HTTP_MAX_PER_ROUTE", "1")
					.set("PDF_ENGINE_HTTP_SOCKET_TIMEOUT_MS", "400").set("PDF_ENGINE_HTTP_REQUEST_TIMEOUT_MS", "700")
					.set("PDF_ENGINE_HTTP_MAX_RETRIES", "1").execute(() -> {
						try (PdfEngineClientImpl client = new PdfEngineClientImpl(HttpClientBuilder.create())) {
							Set<String> filesBefore = temporaryPdfFiles(workingDir);
							long started = System.nanoTime();
							PdfEngineResponse timedOut = client.generatePDF(request);
							long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);

							Assertions.assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, timedOut.getStatusCode());
							Assertions.assertEquals("PDFE_902", timedOut.getErrorCode());
							Assertions.assertTrue(timedOut.getErrorMessage().contains("HTTP request exceeded 700 ms"),
									timedOut.getErrorMessage());
							Assertions.assertTrue(elapsedMs >= 600 && elapsedMs < 2000,
									"Deadline took " + elapsedMs + " ms");
							Assertions.assertNull(timedOut.getTempPdfPath());
							Assertions.assertNull(timedOut.getTempDirectoryPath());
							Assertions.assertEquals(filesBefore, temporaryPdfFiles(workingDir),
									"A failed download must not leave a temporary PDF");
							Assertions.assertEquals(1, receivedRequests.get());

							PdfEngineResponse recovered = client.generatePDF(request);
							Assertions.assertEquals(HttpStatus.SC_OK, recovered.getStatusCode(),
									recovered.getErrorMessage());
							File generatedPdf = new File(recovered.getTempPdfPath());
							try {
								Assertions.assertArrayEquals(pdf, Files.readAllBytes(generatedPdf.toPath()));
								Assertions.assertEquals(2, receivedRequests.get());
							} finally {
								Files.deleteIfExists(generatedPdf.toPath());
							}
						}
					});
		} finally {
			server.stop(0);
			serverExecutor.shutdownNow();
			responseScheduler.shutdownNow();
			Files.deleteIfExists(templateFile.toPath());
		}
	}

	@Test
	void deadlineStopsRetryBackoffWithoutSendingSecondRequest() throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		AtomicInteger receivedRequests = new AtomicInteger();
		server.createContext("/info", exchange -> {
			try (exchange) {
				receivedRequests.incrementAndGet();
				// Closing without a response produces an eligible GET failure.
			}
		});
		server.start();

		try {
			validHttpEnvironment()
					.set("PDF_ENGINE_NODE_INFO_ENDPOINT", "http://127.0.0.1:" + server.getAddress().getPort() + "/info")
					.set("PDF_ENGINE_HTTP_REQUEST_TIMEOUT_MS", "400").set("PDF_ENGINE_HTTP_MAX_RETRIES", "1")
					.set("PDF_ENGINE_HTTP_RETRY_BACKOFF_MS", "5000").set("PDF_ENGINE_HTTP_RETRY_JITTER_MS", "0")
					.execute(() -> {
						try (PdfEngineClientImpl client = new PdfEngineClientImpl(HttpClientBuilder.create())) {
							long started = System.nanoTime();
							Assertions.assertFalse(client.info());
							long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
							Assertions.assertTrue(elapsedMs >= 300 && elapsedMs < 2000,
									"Backoff deadline took " + elapsedMs + " ms");
							Assertions.assertEquals(1, receivedRequests.get(),
									"Expiry during backoff must prevent a second request");
						}
					});
		} finally {
			server.stop(0);
		}
	}

	@Test
	void deadlineIsSharedAcrossRetryAttempts() throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		ExecutorService serverExecutor = Executors.newFixedThreadPool(2);
		ScheduledExecutorService responseScheduler = Executors.newSingleThreadScheduledExecutor();
		server.setExecutor(serverExecutor);
		AtomicInteger receivedRequests = new AtomicInteger();
		CountDownLatch releaseSecondRequest = new CountDownLatch(1);
		server.createContext("/info", exchange -> {
			try (exchange) {
				int number = receivedRequests.incrementAndGet();
				if (number == 1) {
					CountDownLatch failFirstAttempt = new CountDownLatch(1);
					// Schedule the simulated transport failure after half of the two-second budget.
					ScheduledFuture<?> failure = responseScheduler.schedule(failFirstAttempt::countDown, 1000,
							TimeUnit.MILLISECONDS);
					try {
						if (!failFirstAttempt.await(5, TimeUnit.SECONDS)) {
							throw new IOException("Scheduled first-attempt failure did not occur");
						}
					} finally {
						failure.cancel(false);
					}
					// Leaving try-with-resources closes the exchange without an HTTP response.
					return;
				}
				if (number == 2 && !releaseSecondRequest.await(5, TimeUnit.SECONDS)) {
					return;
				}
				byte[] body = "OK".getBytes(StandardCharsets.UTF_8);
				exchange.sendResponseHeaders(HttpStatus.SC_OK, body.length);
				try (OutputStream output = exchange.getResponseBody()) {
					output.write(body);
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} catch (IOException e) {
				// The client may have aborted the second attempt before it is released.
			}
		});
		server.start();

		try {
			validHttpEnvironment()
					.set("PDF_ENGINE_NODE_INFO_ENDPOINT", "http://127.0.0.1:" + server.getAddress().getPort() + "/info")
					.set("PDF_ENGINE_HTTP_SOCKET_TIMEOUT_MS", "10000").set("PDF_ENGINE_HTTP_REQUEST_TIMEOUT_MS", "2000")
					.set("PDF_ENGINE_HTTP_MAX_RETRIES", "1").set("PDF_ENGINE_HTTP_RETRY_BACKOFF_MS", "100")
					.set("PDF_ENGINE_HTTP_RETRY_JITTER_MS", "0").execute(() -> {
						try (PdfEngineClientImpl client = new PdfEngineClientImpl(HttpClientBuilder.create())) {
							long started = System.nanoTime();
							Assertions.assertFalse(client.info());
							long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
							Assertions.assertEquals(2, receivedRequests.get());
							Assertions.assertTrue(elapsedMs >= 1800 && elapsedMs < 2700,
									"A retry must not restart the two-second budget: " + elapsedMs + " ms");

							releaseSecondRequest.countDown();
							Assertions.assertTrue(client.info());
							Assertions.assertEquals(3, receivedRequests.get());
						}
					});
		} finally {
			releaseSecondRequest.countDown();
			server.stop(0);
			serverExecutor.shutdownNow();
			responseScheduler.shutdownNow();
		}
	}

	@Test
	void deadlineCancelsPoolAcquisitionAndClientRecovers() throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		AtomicInteger receivedRequests = new AtomicInteger();
		server.createContext("/info", exchange -> {
			try (exchange) {
				receivedRequests.incrementAndGet();
				byte[] body = "OK".getBytes(StandardCharsets.UTF_8);
				exchange.sendResponseHeaders(HttpStatus.SC_OK, body.length);
				try (OutputStream output = exchange.getResponseBody()) {
					output.write(body);
				}
			}
		});
		server.start();

		try (PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager()) {
			// Hold the only pool slot independently of an HTTP call's own deadline.
			manager.setMaxTotal(1);
			manager.setDefaultMaxPerRoute(1);
			HttpRoute route = new HttpRoute(new HttpHost("127.0.0.1", server.getAddress().getPort(), "http"));
			HttpClientConnection heldConnection = manager.requestConnection(route, null).get(1, TimeUnit.SECONDS);
			validHttpEnvironment()
					.set("PDF_ENGINE_NODE_INFO_ENDPOINT", "http://127.0.0.1:" + server.getAddress().getPort() + "/info")
					.set("PDF_ENGINE_HTTP_MAX_TOTAL", "1").set("PDF_ENGINE_HTTP_MAX_PER_ROUTE", "1")
					.set("PDF_ENGINE_HTTP_POOL_TIMEOUT_MS", "5000").set("PDF_ENGINE_HTTP_REQUEST_TIMEOUT_MS", "400")
					.execute(() -> {
						try (PdfEngineClientImpl client = new PdfEngineClientImpl(HttpClientBuilder.create()
								.setConnectionManager(manager).setConnectionManagerShared(true))) {
							long started = System.nanoTime();
							Assertions.assertFalse(client.info());
							long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
							Assertions.assertTrue(elapsedMs >= 300 && elapsedMs < 2000,
									"Pool deadline took " + elapsedMs + " ms");
							Assertions.assertEquals(0, receivedRequests.get());

							manager.releaseConnection(heldConnection, null, 0, TimeUnit.MILLISECONDS);
							Assertions.assertTrue(client.info());
							Assertions.assertEquals(1, receivedRequests.get());
						}
					});
		} finally {
			server.stop(0);
		}
	}

	@Test
	void invalidWorkingDirectoryDoesNotSendHttpRequest() throws Exception {
		File templateFile = Files.createTempFile(workingDir, "template-", ".zip").toFile();
		Path missingDirectory = workingDir.resolve("missing");
		String[] invalidDirectories = { null, "", "   ", missingDirectory.toString(), templateFile.getAbsolutePath() };

		try (ZipFile template = new ZipFile(templateFile)) {
			validHttpEnvironment().execute(() -> {
				HttpClientBuilder builder = mock(HttpClientBuilder.class);
				CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
				when(builder.build()).thenReturn(httpClient);

				try (PdfEngineClientImpl client = new PdfEngineClientImpl(builder)) {
					for (String directory : invalidDirectories) {
						PdfEngineRequest request = new PdfEngineRequest();
						request.setWorkingDirPath(directory);
						request.setData("{}");
						request.setTemplate(template);

						PdfEngineResponse response = client.generatePDF(request);

						Assertions.assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatusCode());
						Assertions.assertEquals("PDFE_902", response.getErrorCode());
						Assertions.assertNotNull(response.getErrorMessage());
						Assertions.assertTrue(response.getErrorMessage().contains("workingDirPath"),
								response.getErrorMessage());
						Assertions.assertNull(response.getTempPdfPath());
						Assertions.assertNull(response.getTempDirectoryPath());
					}

					// Validate the directory before starting an HTTP exchange.
					verify(httpClient, never()).execute(any(HttpPost.class), any(HttpClientContext.class));
					Assertions.assertFalse(Files.exists(missingDirectory),
							"The client must not create a missing request directory");
				}
			});
		}
	}

	@Test
	void requestDirectoriesKeepConcurrentPdfsIsolated() throws Exception {
		Path directoryA = Files.createDirectory(workingDir.resolve("request-a"));
		Path directoryB = Files.createDirectory(workingDir.resolve("request-b"));
		File templateFile = Files.createTempFile(workingDir, "template-", ".zip").toFile();
		byte[] pdfA = "%PDF-1.4\nPDF for request A\n%%EOF".getBytes(StandardCharsets.UTF_8);
		byte[] pdfB = "%PDF-1.4\nPDF for request B\n%%EOF".getBytes(StandardCharsets.UTF_8);

		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		ExecutorService serverExecutor = Executors.newFixedThreadPool(2);
		ExecutorService callerExecutor = Executors.newFixedThreadPool(2);
		server.setExecutor(serverExecutor);
		CountDownLatch bothRequestsReceived = new CountDownLatch(2);
		AtomicInteger receivedRequests = new AtomicInteger();

		server.createContext("/generate-pdf", exchange -> {
			try (exchange) {
				String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
				receivedRequests.incrementAndGet();
				bothRequestsReceived.countDown();

				// Neither response starts until both requests have reached the server.
				if (!bothRequestsReceived.await(3, TimeUnit.SECONDS)) {
					exchange.sendResponseHeaders(HttpStatus.SC_SERVICE_UNAVAILABLE, -1);
					return;
				}

				byte[] pdf = requestBody.contains("request-a") ? pdfA : pdfB;
				exchange.sendResponseHeaders(HttpStatus.SC_OK, pdf.length);
				try (OutputStream output = exchange.getResponseBody()) {
					output.write(pdf);
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});
		server.start();

		try (ZipFile template = new ZipFile(templateFile)) {
			PdfEngineRequest requestA = new PdfEngineRequest();
			requestA.setWorkingDirPath(directoryA.toString());
			requestA.setData("{\"request\":\"request-a\"}");
			requestA.setTemplate(template);

			PdfEngineRequest requestB = new PdfEngineRequest();
			requestB.setWorkingDirPath(directoryB.toString());
			requestB.setData("{\"request\":\"request-b\"}");
			requestB.setTemplate(template);

			validHttpEnvironment()
					.set("PDF_ENGINE_NODE_GENERATE_ENDPOINT",
							"http://127.0.0.1:" + server.getAddress().getPort() + "/generate-pdf")
					.set("PDF_ENGINE_HTTP_SOCKET_TIMEOUT_MS", "5000").set("PDF_ENGINE_HTTP_REQUEST_TIMEOUT_MS", "8000")
					.execute(() -> {
						try (PdfEngineClientImpl client = new PdfEngineClientImpl(HttpClientBuilder.create())) {
							Future<PdfEngineResponse> callA = callerExecutor.submit(() -> client.generatePDF(requestA));
							Future<PdfEngineResponse> callB = callerExecutor.submit(() -> client.generatePDF(requestB));
							PdfEngineResponse responseA = callA.get(8, TimeUnit.SECONDS);
							PdfEngineResponse responseB = callB.get(8, TimeUnit.SECONDS);

							Assertions.assertEquals(HttpStatus.SC_OK, responseA.getStatusCode(),
									responseA.getErrorMessage());
							Assertions.assertEquals(HttpStatus.SC_OK, responseB.getStatusCode(),
									responseB.getErrorMessage());
							Assertions.assertEquals(2, receivedRequests.get());

							Path generatedPdfA = Path.of(responseA.getTempPdfPath());
							Path generatedPdfB = Path.of(responseB.getTempPdfPath());
							Assertions.assertEquals(directoryA.toAbsolutePath().normalize(), generatedPdfA.getParent());
							Assertions.assertEquals(directoryB.toAbsolutePath().normalize(), generatedPdfB.getParent());
							Assertions.assertEquals(directoryA.toAbsolutePath().normalize().toString(),
									responseA.getTempDirectoryPath());
							Assertions.assertEquals(directoryB.toAbsolutePath().normalize().toString(),
									responseB.getTempDirectoryPath());
							Assertions.assertArrayEquals(pdfA, Files.readAllBytes(generatedPdfA));
							Assertions.assertArrayEquals(pdfB, Files.readAllBytes(generatedPdfB));

							// Simulate the caller's cleanup for request A after processing its PDF.
							FileUtils.deleteDirectory(directoryA.toFile());
							Assertions.assertFalse(Files.exists(directoryA));
							Assertions.assertFalse(Files.exists(generatedPdfA));
							Assertions.assertTrue(Files.isDirectory(directoryB));
							Assertions.assertTrue(Files.exists(generatedPdfB),
									"Cleaning request A must not remove the PDF belonging to request B");
							Assertions.assertArrayEquals(pdfB, Files.readAllBytes(generatedPdfB));
						}
					});
		} finally {
			callerExecutor.shutdownNow();
			server.stop(0);
			serverExecutor.shutdownNow();
		}
	}

	@Test
	void expiredConnectionIsReplaced() throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);

		Set<Integer> sourcePorts = ConcurrentHashMap.newKeySet();
		AtomicInteger receivedRequests = new AtomicInteger();

		server.createContext("/info", exchange -> {
			try (exchange) {
				sourcePorts.add(exchange.getRemoteAddress().getPort());
				receivedRequests.incrementAndGet();

				byte[] body = "OK".getBytes(StandardCharsets.UTF_8);
				exchange.sendResponseHeaders(HttpStatus.SC_OK, body.length);

				try (OutputStream output = exchange.getResponseBody()) {
					output.write(body);
				}
			}
		});
		server.start();

		try {
			validHttpEnvironment()
					.set("PDF_ENGINE_NODE_INFO_ENDPOINT", "http://127.0.0.1:" + server.getAddress().getPort() + "/info")
					.set("PDF_ENGINE_HTTP_TTL_MS", "500").set("PDF_ENGINE_HTTP_IDLE_TIMEOUT_MS", "60000")
					.set("PDF_ENGINE_HTTP_MAX_RETRIES", "0").execute(() -> {
						try (PdfEngineClientImpl client = new PdfEngineClientImpl(HttpClientBuilder.create())) {

							Assertions.assertTrue(client.info());
							long firstResponseCompleted = System.nanoTime();
							Assertions.assertEquals(1, sourcePorts.size());

							// The connection already existed when the first response completed.
							Assertions.assertTrue(
									awaitCondition(() -> System.nanoTime()
											- firstResponseCompleted >= TimeUnit.MILLISECONDS.toNanos(700), 3000),
									"The connection must reach its configured TTL");

							Assertions.assertTrue(client.info());
							Assertions.assertEquals(2, receivedRequests.get());
							Assertions.assertEquals(2, sourcePorts.size(), "An expired connection must not be reused");
						}
					});
		} finally {
			server.stop(0);
		}
	}

	@Test
	void idleConnectionIsEvictedWithoutAnotherRequest() throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);

		Set<Integer> sourcePorts = ConcurrentHashMap.newKeySet();
		AtomicInteger receivedRequests = new AtomicInteger();

		server.createContext("/info", exchange -> {
			try (exchange) {
				sourcePorts.add(exchange.getRemoteAddress().getPort());
				receivedRequests.incrementAndGet();

				byte[] body = "OK".getBytes(StandardCharsets.UTF_8);
				exchange.sendResponseHeaders(HttpStatus.SC_OK, body.length);

				try (OutputStream output = exchange.getResponseBody()) {
					output.write(body);
				}
			}
		});
		server.start();

		try {
			validHttpEnvironment()
					.set("PDF_ENGINE_NODE_INFO_ENDPOINT", "http://127.0.0.1:" + server.getAddress().getPort() + "/info")
					.set("PDF_ENGINE_HTTP_TTL_MS", "60000").set("PDF_ENGINE_HTTP_IDLE_TIMEOUT_MS", "500")
					.set("PDF_ENGINE_HTTP_MAX_RETRIES", "0").execute(() -> {
						// A supplied manager lets the test observe background eviction directly.
						// It is not shared: the client must own and run its eviction thread.
						try (PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(
								60000, TimeUnit.MILLISECONDS);
								PdfEngineClientImpl client = new PdfEngineClientImpl(
										HttpClientBuilder.create().setConnectionManager(connectionManager))) {

							Assertions.assertTrue(client.info());
							Assertions.assertEquals(1, connectionManager.getTotalStats().getAvailable());

							Assertions.assertTrue(
									awaitCondition(() -> connectionManager.getTotalStats().getAvailable() == 0, 5000),
									"The idle connection must be evicted without a new request");

							Assertions.assertEquals(1, receivedRequests.get(),
									"Waiting for eviction must not send HTTP requests");
							Assertions.assertEquals(0, connectionManager.getTotalStats().getLeased());

							Assertions.assertTrue(client.info());
							Assertions.assertEquals(2, receivedRequests.get());
							Assertions.assertEquals(2, sourcePorts.size(),
									"The next request must create a new TCP connection");
						}
					});
		} finally {
			server.stop(0);
		}
	}

	@Test
	void connectionCleanupDoesNotInterruptActiveRequest() throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);

		ExecutorService callerExecutor = Executors.newSingleThreadExecutor();

		Set<Integer> sourcePorts = ConcurrentHashMap.newKeySet();
		AtomicInteger receivedRequests = new AtomicInteger();
		CountDownLatch firstRequestReceived = new CountDownLatch(1);
		CountDownLatch releaseFirstRequest = new CountDownLatch(1);

		server.createContext("/info", exchange -> {
			try (exchange) {
				sourcePorts.add(exchange.getRemoteAddress().getPort());

				if (receivedRequests.incrementAndGet() == 1) {
					firstRequestReceived.countDown();

					if (!releaseFirstRequest.await(10, TimeUnit.SECONDS)) {
						return;
					}
				}

				byte[] body = "OK".getBytes(StandardCharsets.UTF_8);
				exchange.sendResponseHeaders(HttpStatus.SC_OK, body.length);

				try (OutputStream output = exchange.getResponseBody()) {
					output.write(body);
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});
		server.start();

		try {
			validHttpEnvironment()
					.set("PDF_ENGINE_NODE_INFO_ENDPOINT", "http://127.0.0.1:" + server.getAddress().getPort() + "/info")
					.set("PDF_ENGINE_HTTP_TTL_MS", "500").set("PDF_ENGINE_HTTP_IDLE_TIMEOUT_MS", "200")
					.set("PDF_ENGINE_HTTP_SOCKET_TIMEOUT_MS", "10000")
					.set("PDF_ENGINE_HTTP_REQUEST_TIMEOUT_MS", "10000").set("PDF_ENGINE_HTTP_MAX_RETRIES", "0")
					.execute(() -> {
						try (PdfEngineClientImpl client = new PdfEngineClientImpl(HttpClientBuilder.create())) {

							Future<Boolean> firstCall = callerExecutor.submit(client::info);

							Assertions.assertTrue(firstRequestReceived.await(3, TimeUnit.SECONDS));

							long requestReceived = System.nanoTime();

							// Keep the request active past both TTL and the idle eviction threshold.
							Assertions.assertTrue(awaitCondition(
									() -> System.nanoTime() - requestReceived >= TimeUnit.MILLISECONDS.toNanos(700),
									3000));

							Assertions.assertFalse(firstCall.isDone(),
									"Connection cleanup must not abort a request that is still active");

							releaseFirstRequest.countDown();

							Assertions.assertTrue(firstCall.get(3, TimeUnit.SECONDS));
							Assertions.assertTrue(client.info());
							Assertions.assertEquals(2, receivedRequests.get());
							Assertions.assertEquals(2, sourcePorts.size(),
									"After completing the active request, its expired connection must be replaced");
						}
					});
		} finally {
			releaseFirstRequest.countDown();
			callerExecutor.shutdownNow();
			server.stop(0);
		}
	}

	@Test
	void connectTimeoutIsAppliedAndClientRecovers() throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);

		AtomicInteger receivedRequests = new AtomicInteger();

		byte[] pdf = "%PDF-1.4\nlocal test PDF\n%%EOF".getBytes(StandardCharsets.UTF_8);

		File templateFile = Files.createTempFile(workingDir, "pdf-engine-template-", ".zip").toFile();

		server.createContext("/generate-pdf", exchange -> {
			try (exchange) {
				exchange.getRequestBody().readAllBytes();
				receivedRequests.incrementAndGet();

				exchange.sendResponseHeaders(HttpStatus.SC_OK, pdf.length);

				try (OutputStream output = exchange.getResponseBody()) {
					output.write(pdf);
				}
			}
		});
		server.start();

		try {
			for (int configuredTimeoutMs : new int[] { 250, 500 }) {
				AtomicInteger connectionAttempts = new AtomicInteger();
				AtomicInteger observedConnectTimeout = new AtomicInteger(-1);

				ConnectionSocketFactory socketFactory = timeoutOnceSocketFactory(connectionAttempts,
						observedConnectTimeout);

				int requestsBefore = receivedRequests.get();

				validHttpEnvironment()
						.set("PDF_ENGINE_NODE_GENERATE_ENDPOINT",
								"http://127.0.0.1:" + server.getAddress().getPort() + "/generate-pdf")
						.set("PDF_ENGINE_HTTP_MAX_TOTAL", "1").set("PDF_ENGINE_HTTP_MAX_PER_ROUTE", "1")
						.set("PDF_ENGINE_HTTP_CONNECT_TIMEOUT_MS", String.valueOf(configuredTimeoutMs))
						.set("PDF_ENGINE_HTTP_MAX_RETRIES", "2").set("PDF_ENGINE_HTTP_RETRY_BACKOFF_MS", "0")
						.set("PDF_ENGINE_HTTP_RETRY_JITTER_MS", "0").execute(() -> {
							try (ZipFile template = new ZipFile(templateFile);
									PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(
											RegistryBuilder.<ConnectionSocketFactory>create()
													.register("http", socketFactory).build());
									PdfEngineClientImpl client = new PdfEngineClientImpl(
											HttpClientBuilder.create().setConnectionManager(connectionManager))) {

								// A supplied manager owns its pool limits; keep one slot to prove recovery.
								connectionManager.setMaxTotal(1);
								connectionManager.setDefaultMaxPerRoute(1);

								PdfEngineRequest request = new PdfEngineRequest();
								request.setWorkingDirPath(workingDir.toString());
								request.setData("{}");
								request.setTemplate(template);

								Set<String> filesBefore = temporaryPdfFiles(workingDir);

								PdfEngineResponse failed = client.generatePDF(request);

								Assertions.assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, failed.getStatusCode());
								Assertions.assertEquals("PDFE_902", failed.getErrorCode());
								Assertions.assertTrue(failed.getErrorMessage().contains("Simulated connect timeout"),
										failed.getErrorMessage());

								Assertions.assertEquals(configuredTimeoutMs, observedConnectTimeout.get(),
										"The configured timeout must reach the socket connection phase");
								Assertions.assertEquals(1, connectionAttempts.get(),
										"Connect timeouts must not be retried even when retries are enabled");
								Assertions.assertEquals(requestsBefore, receivedRequests.get(),
										"The failed connection must not send a POST to the server");

								Assertions.assertEquals(0, connectionManager.getTotalStats().getLeased());
								Assertions.assertEquals(0, connectionManager.getTotalStats().getPending());

								Assertions.assertNull(failed.getTempPdfPath());
								Assertions.assertNull(failed.getTempDirectoryPath());
								Assertions.assertEquals(filesBefore, temporaryPdfFiles(workingDir));

								PdfEngineResponse recovered = client.generatePDF(request);

								Assertions.assertEquals(HttpStatus.SC_OK, recovered.getStatusCode(),
										recovered.getErrorMessage());
								Assertions.assertEquals(configuredTimeoutMs, observedConnectTimeout.get());
								Assertions.assertEquals(2, connectionAttempts.get());
								Assertions.assertEquals(requestsBefore + 1, receivedRequests.get());

								Path generatedPdf = Path.of(recovered.getTempPdfPath());

								Assertions.assertArrayEquals(pdf, Files.readAllBytes(generatedPdf));

								Files.deleteIfExists(generatedPdf);
							}
						});
			}
		} finally {
			server.stop(0);
			Files.deleteIfExists(templateFile.toPath());
		}
	}

	/**
	 * Simulates one connect timeout; subsequent attempts use actual TCP
	 * connections.
	 */
	private static ConnectionSocketFactory timeoutOnceSocketFactory(AtomicInteger connectionAttempts,
			AtomicInteger observedConnectTimeout) {

		return new PlainConnectionSocketFactory() {
			@Override
			public Socket connectSocket(int connectTimeout, Socket socket, HttpHost host,
					InetSocketAddress remoteAddress, InetSocketAddress localAddress, HttpContext context)
					throws IOException {

				observedConnectTimeout.set(connectTimeout);

				if (connectionAttempts.incrementAndGet() == 1) {
					// The socket is borrowed from HttpClient and has not been connected yet.
					if (socket != null) {
						socket.close();
					}

					throw new ConnectTimeoutException("Simulated connect timeout");
				}

				return super.connectSocket(connectTimeout, socket, host, remoteAddress, localAddress, context);
			}
		};
	}

	/**
	 * Observes a condition until it becomes true or the bounded wait expires.
	 */
	private static boolean awaitCondition(BooleanSupplier condition, long timeoutMs) throws InterruptedException {

		CountDownLatch conditionReached = new CountDownLatch(1);
		ScheduledExecutorService observer = Executors.newSingleThreadScheduledExecutor();

		try {
			observer.scheduleWithFixedDelay(() -> {
				if (condition.getAsBoolean()) {
					conditionReached.countDown();
				}
			}, 0, 10, TimeUnit.MILLISECONDS);

			return conditionReached.await(timeoutMs, TimeUnit.MILLISECONDS);
		} finally {
			observer.shutdownNow();
		}
	}

	/**
	 * Waits for the worker to enter the timed backoff before interrupting or
	 * aborting it. The latch returns as soon as that condition is observed, within
	 * a one-second limit.
	 */
	private static boolean awaitRetryBackoff(Thread worker) throws InterruptedException {
		CountDownLatch backoffStarted = new CountDownLatch(1);
		ScheduledExecutorService observer = Executors.newSingleThreadScheduledExecutor();
		try {
			observer.scheduleWithFixedDelay(() -> {
				if (worker.getState() == Thread.State.TIMED_WAITING) {
					backoffStarted.countDown();
				}
			}, 0, 10, TimeUnit.MILLISECONDS);
			return backoffStarted.await(1, TimeUnit.SECONDS);
		} finally {
			observer.shutdownNow();
		}
	}

	private static EnvironmentVariables validHttpEnvironment() {
		return new EnvironmentVariables().set("PDF_ENGINE_HTTP_MAX_TOTAL", "4")
				.set("PDF_ENGINE_HTTP_MAX_PER_ROUTE", "2").set("PDF_ENGINE_HTTP_POOL_TIMEOUT_MS", "1000")
				.set("PDF_ENGINE_HTTP_CONNECT_TIMEOUT_MS", "2000").set("PDF_ENGINE_HTTP_SOCKET_TIMEOUT_MS", "2000")
				.set("PDF_ENGINE_HTTP_REQUEST_TIMEOUT_MS", "5000").set("PDF_ENGINE_HTTP_TTL_MS", "60000")
				.set("PDF_ENGINE_HTTP_IDLE_TIMEOUT_MS", "60000");
	}

	private static void assertInvalidHttpSetting(String name, String value) throws Exception {

		EnvironmentVariables environment = validHttpEnvironment().set(name, value);

		environment.execute(() -> {
			HttpClientBuilder builder = mock(HttpClientBuilder.class);

			IllegalArgumentException error = Assertions.assertThrows(IllegalArgumentException.class,
					() -> new PdfEngineClientImpl(builder), name + "=" + value);

			Assertions.assertTrue(error.getMessage().contains(name), error.getMessage());

			verify(builder, never()).build();
		});
	}

	private static Set<String> temporaryPdfFiles(Path directory) throws IOException {
		if (!Files.exists(directory)) {
			return Set.of();
		}
		try (java.util.stream.Stream<Path> files = Files.list(directory)) {
			return files.filter(path -> path.getFileName().toString().endsWith(".pdf"))
					.map(path -> path.getFileName().toString()).collect(java.util.stream.Collectors.toSet());
		}
	}
}