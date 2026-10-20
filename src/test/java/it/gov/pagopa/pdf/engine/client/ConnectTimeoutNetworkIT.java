package it.gov.pagopa.pdf.engine.client;

import com.sun.net.httpserver.HttpServer;
import it.gov.pagopa.pdf.engine.client.impl.PdfEngineClientImpl;
import it.gov.pagopa.pdf.engine.model.PdfEngineRequest;
import it.gov.pagopa.pdf.engine.model.PdfEngineResponse;
import net.lingala.zip4j.ZipFile;
import org.apache.http.HttpStatus;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Opt-in network tests for the isolated Docker laboratory. Uses the actual HTTP
 * client and Linux TCP stack; no socket factory is mocked.
 */
@EnabledIfEnvironmentVariable(named = "PDF_ENGINE_NETWORK_TESTS", matches = "1")
class ConnectTimeoutNetworkIT {

	private static final Logger LOGGER = LoggerFactory.getLogger(ConnectTimeoutNetworkIT.class);
	private static final byte[] TEST_PDF = "%PDF-1.4\nnetwork test PDF\n%%EOF".getBytes(StandardCharsets.UTF_8);

	@TempDir
	Path workingDir;

	@Test
	void connectTimeoutStopsBlockedTcpAndClientRecovers() throws Exception {
		AtomicInteger receivedRequests = new AtomicInteger();
		HttpServer server = startPdfServer(receivedRequests);

		try (ZipFile template = new ZipFile(Files.createTempFile(workingDir, "template-", ".zip").toFile())) {
			PdfEngineRequest request = createRequest(template);

			for (int connectTimeoutMs : new int[] { 250, 500 }) {
				httpEnvironment(server, connectTimeoutMs, 5000).execute(() -> {
					try (PdfEngineClientImpl client = new PdfEngineClientImpl(HttpClientBuilder.create())) {
						int requestsBefore = receivedRequests.get();
						TimedResponse timed = generateWithBlockedTcp(server.getAddress().getPort(), client, request);

						assertFailedBeforeSending(timed.response(), requestsBefore, receivedRequests);
						Assertions.assertTrue(timed.response().getErrorMessage().contains("ConnectTimeoutException"),
								timed.response().getErrorMessage());
						Assertions.assertTrue(
								timed.elapsedMs() >= connectTimeoutMs - 50L
										&& timed.elapsedMs() < connectTimeoutMs + 1000L,
								"Connect timeout=" + connectTimeoutMs + " ms; elapsed=" + timed.elapsedMs() + " ms");

						LOGGER.info("Connect timeout={} ms; generatePDF duration={} ms", connectTimeoutMs,
								timed.elapsedMs());
						assertRecovery(client, request, requestsBefore + 1, receivedRequests);
					}
				});
			}
		} finally {
			server.stop(0);
		}
	}

	@Test
	void requestDeadlineStopsBlockedTcpBeforeConnectTimeout() throws Exception {
		AtomicInteger receivedRequests = new AtomicInteger();
		HttpServer server = startPdfServer(receivedRequests);

		try (ZipFile template = new ZipFile(Files.createTempFile(workingDir, "template-", ".zip").toFile())) {
			PdfEngineRequest request = createRequest(template);

			httpEnvironment(server, 10000, 700).execute(() -> {
				try (PdfEngineClientImpl client = new PdfEngineClientImpl(HttpClientBuilder.create())) {
					TimedResponse timed = generateWithBlockedTcp(server.getAddress().getPort(), client, request);

					assertFailedBeforeSending(timed.response(), 0, receivedRequests);
					Assertions.assertTrue(timed.response().getErrorMessage().contains("HTTP request exceeded 700 ms"),
							timed.response().getErrorMessage());
					Assertions.assertTrue(timed.elapsedMs() >= 600 && timed.elapsedMs() < 2000,
							"The request budget must abort the TCP connection: " + timed.elapsedMs() + " ms");

					LOGGER.info("Request deadline=700 ms; connect timeout=10000 ms; generatePDF duration={} ms",
							timed.elapsedMs());
					assertRecovery(client, request, 1, receivedRequests);
				}
			});
		} finally {
			server.stop(0);
		}
	}

	private static HttpServer startPdfServer(AtomicInteger receivedRequests) throws IOException {
		HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/generate-pdf", exchange -> {
			try (exchange) {
				exchange.getRequestBody().readAllBytes();
				receivedRequests.incrementAndGet();
				exchange.sendResponseHeaders(HttpStatus.SC_OK, TEST_PDF.length);
				try (OutputStream output = exchange.getResponseBody()) {
					output.write(TEST_PDF);
				}
			}
		});
		server.start();
		return server;
	}

	private PdfEngineRequest createRequest(ZipFile template) {
		PdfEngineRequest request = new PdfEngineRequest();
		request.setWorkingDirPath(workingDir.toString());
		request.setData("{}");
		request.setTemplate(template);
		return request;
	}

	private static EnvironmentVariables httpEnvironment(HttpServer server, int connectTimeoutMs, int requestTimeoutMs) {
		return new EnvironmentVariables()
				.set("PDF_ENGINE_NODE_GENERATE_ENDPOINT",
						"http://127.0.0.1:" + server.getAddress().getPort() + "/generate-pdf")
				.set("PDF_ENGINE_HTTP_MAX_TOTAL", "1").set("PDF_ENGINE_HTTP_MAX_PER_ROUTE", "1")
				.set("PDF_ENGINE_HTTP_POOL_TIMEOUT_MS", "1000")
				.set("PDF_ENGINE_HTTP_CONNECT_TIMEOUT_MS", String.valueOf(connectTimeoutMs))
				.set("PDF_ENGINE_HTTP_SOCKET_TIMEOUT_MS", "10000")
				.set("PDF_ENGINE_HTTP_REQUEST_TIMEOUT_MS", String.valueOf(requestTimeoutMs))
				.set("PDF_ENGINE_HTTP_TTL_MS", "60000").set("PDF_ENGINE_HTTP_IDLE_TIMEOUT_MS", "60000")
				.set("PDF_ENGINE_HTTP_MAX_RETRIES", "2").set("PDF_ENGINE_HTTP_RETRY_BACKOFF_MS", "0")
				.set("PDF_ENGINE_HTTP_RETRY_JITTER_MS", "0");
	}

	private static TimedResponse generateWithBlockedTcp(int port, PdfEngineClientImpl client, PdfEngineRequest request)
			throws IOException, InterruptedException {
		changeSynRule("-A", port);
		try {
			// Measure generatePDF, including request preparation; firewall setup/cleanup is
			// outside this interval.
			long started = System.nanoTime();
			PdfEngineResponse response = client.generatePDF(request);
			long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
			return new TimedResponse(response, elapsedMs);
		} finally {
			changeSynRule("-D", port);
		}
	}

	private static void changeSynRule(String operation, int port) throws IOException, InterruptedException {
		// This rule affects only loopback SYN packets for this test server's port,
		// inside the container.
		List<String> command = List.of("iptables", "-w", "2", operation, "INPUT", "-i", "lo", "-p", "tcp", "--dport",
				String.valueOf(port), "--syn", "-j", "DROP");
		Process process = new ProcessBuilder(command).redirectErrorStream(true)
				.redirectInput(ProcessBuilder.Redirect.from(new File("/dev/null"))).start();

		try (InputStream output = process.getInputStream()) {
			if (!process.waitFor(5, TimeUnit.SECONDS)) {
				process.destroyForcibly();
				throw new IOException("Timed out executing the laboratory iptables command");
			}
			String diagnostic = new String(output.readAllBytes(), StandardCharsets.UTF_8);
			if (process.exitValue() != 0) {
				throw new IOException("Cannot configure the laboratory TCP block: " + diagnostic);
			}
		} finally {
			process.destroy();
		}
	}

	private static void assertFailedBeforeSending(PdfEngineResponse response, int requestsBefore,
			AtomicInteger receivedRequests) {
		Assertions.assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatusCode());
		Assertions.assertEquals("PDFE_902", response.getErrorCode());
		Assertions.assertEquals(requestsBefore, receivedRequests.get(),
				"A blocked TCP handshake must not deliver a POST to the server");
		Assertions.assertNull(response.getTempPdfPath());
		Assertions.assertNull(response.getTempDirectoryPath());
	}

	private static void assertRecovery(PdfEngineClientImpl client, PdfEngineRequest request, int expectedRequests,
			AtomicInteger receivedRequests) throws IOException {
		PdfEngineResponse recovered = client.generatePDF(request);
		Assertions.assertEquals(HttpStatus.SC_OK, recovered.getStatusCode(), recovered.getErrorMessage());
		Assertions.assertEquals(expectedRequests, receivedRequests.get());
		Path generatedPdf = Path.of(recovered.getTempPdfPath());
		Assertions.assertArrayEquals(TEST_PDF, Files.readAllBytes(generatedPdf));
		Files.deleteIfExists(generatedPdf);
	}

	private record TimedResponse(PdfEngineResponse response, long elapsedMs) {
	}
}