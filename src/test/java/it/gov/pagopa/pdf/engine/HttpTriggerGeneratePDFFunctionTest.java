
package it.gov.pagopa.pdf.engine;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import it.gov.pagopa.pdf.engine.exception.CompileTemplateException;
import it.gov.pagopa.pdf.engine.exception.RequestBodyParseException;
import it.gov.pagopa.pdf.engine.model.AppErrorCodeEnum;
import it.gov.pagopa.pdf.engine.model.ErrorResponse;
import it.gov.pagopa.pdf.engine.model.GeneratePDFInput;
import it.gov.pagopa.pdf.engine.model.GeneratorType;
import it.gov.pagopa.pdf.engine.service.GeneratePDFService;
import it.gov.pagopa.pdf.engine.service.ParseRequestBodyService;
import it.gov.pagopa.pdf.engine.util.HttpResponseMessageMock;
import lombok.SneakyThrows;
import net.lingala.zip4j.ZipFile;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.microsoft.azure.functions.HttpStatus.BAD_REQUEST;
import static com.microsoft.azure.functions.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SystemStubsExtension.class)
class HttpTriggerGeneratePDFFunctionTest {

	// The base directory belongs to JUnit; the function owns only its request
	// subdirectory.
	@TempDir
	Path workingDirectory;

	private Path requestWorkingDirectory;
	private Path otherRequestDirectory;

	@SystemStub
	private EnvironmentVariables environmentVariables;

	private HttpTriggerGeneratePDFFunction function;

	private GeneratePDFService generatePDFServiceMock;
	private ParseRequestBodyService parseRequestBodyServiceMock;
	private ExecutionContext executionContextMock;
	private TelemetryClient telemetryClientMock;

	@BeforeEach
	void setUp() throws IOException {
		environmentVariables.set("WORKING_DIRECTORY_PATH", workingDirectory.toString());
		environmentVariables.set("APPLICATIONINSIGHTS_CONNECTION_STRING", "secret");

		otherRequestDirectory = Files.createDirectory(workingDirectory.resolve("other-request"));
		Files.writeString(otherRequestDirectory.resolve("keep.pdf"), "PDF belonging to another request");

		generatePDFServiceMock = mock(GeneratePDFService.class);
		parseRequestBodyServiceMock = mock(ParseRequestBodyService.class);
		executionContextMock = mock(ExecutionContext.class);
		telemetryClientMock = mock(TelemetryClient.class);

		function = spy(new HttpTriggerGeneratePDFFunction(generatePDFServiceMock, parseRequestBodyServiceMock));
	}

	@Test
	@SneakyThrows
	void runGenerateNotZippedOk() {
		// Setup
		@SuppressWarnings("unchecked")
		final HttpRequestMessage<Optional<byte[]>> request = mock(HttpRequestMessage.class);

		GeneratePDFInput generatePDFInput = new GeneratePDFInput();
		generatePDFInput.setData(Collections.singletonMap("a", "b"));
		generatePDFInput.setApplySignature(false);
		generatePDFInput.setTemplateZip(mock(ZipFile.class));
		generatePDFInput.setGeneratorType(GeneratorType.ITEXT);

		BufferedInputStream inputStreamMock = mock(BufferedInputStream.class);

		doReturn(Logger.getGlobal()).when(executionContextMock).getLogger();
		doReturn(Optional.of(new byte[2])).when(request).getBody();
		stubParsedInput(generatePDFInput);
		stubGeneratedStream(inputStreamMock);
		doReturn(new byte[5]).when(inputStreamMock).readAllBytes();

		createHttpMessageBuilderSub(request);

		// Invoke
		final HttpResponseMessage response = function.run(request, executionContextMock);

		// Verify
		assertEquals(HttpStatus.OK, response.getStatus());
		assertEquals("application/pdf", response.getHeader("content-type"));
		assertEquals("attachment; ", response.getHeader("content-disposition"));
		assertRequestDirectoryCleaned();
		verify(inputStreamMock).close();
	}

	@Test
	@SneakyThrows
	void runGenerateZippedOk() {
		// Setup
		@SuppressWarnings("unchecked")
		final HttpRequestMessage<Optional<byte[]>> request = mock(HttpRequestMessage.class);

		GeneratePDFInput generatePDFInput = new GeneratePDFInput();
		generatePDFInput.setData(Collections.singletonMap("a", "b"));
		generatePDFInput.setApplySignature(false);
		generatePDFInput.setTemplateZip(mock(ZipFile.class));
		generatePDFInput.setGenerateZipped(true);

		BufferedInputStream inputStreamMock = mock(BufferedInputStream.class);

		doReturn(Logger.getGlobal()).when(executionContextMock).getLogger();
		doReturn(Optional.of(new byte[2])).when(request).getBody();
		stubParsedInput(generatePDFInput);
		stubGeneratedStream(inputStreamMock);
		doReturn(new byte[5]).when(inputStreamMock).readAllBytes();

		createHttpMessageBuilderSub(request);

		// Invoke
		final HttpResponseMessage response = function.run(request, executionContextMock);

		// Verify
		assertEquals(HttpStatus.OK, response.getStatus());
		assertEquals("application/zip", response.getHeader("content-type"));
		assertEquals("attachment; ", response.getHeader("content-disposition"));
		assertRequestDirectoryCleaned();
		verify(inputStreamMock).close();
	}

	@Test
	void runFailOnInvalidInput() throws IOException {
		// Setup
		@SuppressWarnings("unchecked")
		final HttpRequestMessage<Optional<byte[]>> request = mock(HttpRequestMessage.class);

		doReturn(Logger.getGlobal()).when(executionContextMock).getLogger();
		doReturn(Optional.empty()).when(request).getBody();
		createHttpMessageBuilderSub(request);

		// Invoke
		final HttpResponseMessage response = function.run(request, executionContextMock);

		// Verify
		assertEquals(BAD_REQUEST, response.getStatus());
		Object body = response.getBody();
		assertNotNull(body);
		assertTrue(body instanceof ErrorResponse);
		Assertions.assertEquals(AppErrorCodeEnum.PDFE_899, ((ErrorResponse) body).getAppErrorCode());
		assertNoRequestDirectoryCreated();
		verifyNoInteractions(parseRequestBodyServiceMock, generatePDFServiceMock);
	}

	@Test
	void runFailOnCreateWorkingDirectory() throws IOException {
		// Setup
		@SuppressWarnings("unchecked")
		final HttpRequestMessage<Optional<byte[]>> request = mock(HttpRequestMessage.class);

		doReturn(Logger.getGlobal()).when(executionContextMock).getLogger();
		doReturn(Optional.of(new byte[2])).when(request).getBody();
		createHttpMessageBuilderSub(request);

		try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
			filesMockedStatic.when(() -> Files.createTempDirectory(any(), anyString())).thenThrow(IOException.class);

			// Invoke
			final HttpResponseMessage response = function.run(request, executionContextMock);

			// Verify
			assertEquals(INTERNAL_SERVER_ERROR, response.getStatus());
			Object body = response.getBody();
			assertNotNull(body);
			assertTrue(body instanceof ErrorResponse);
			Assertions.assertEquals(AppErrorCodeEnum.PDFE_908, ((ErrorResponse) body).getAppErrorCode());
		}

		assertNoRequestDirectoryCreated();
		verifyNoInteractions(parseRequestBodyServiceMock, generatePDFServiceMock);

	}

	@Test
	@SneakyThrows
	void runFailOnParseRequestBodyWith400() {
		// Setup
		@SuppressWarnings("unchecked")
		final HttpRequestMessage<Optional<byte[]>> request = mock(HttpRequestMessage.class);

		doReturn(Logger.getGlobal()).when(executionContextMock).getLogger();
		doReturn(Optional.of(new byte[2])).when(request).getBody();
		stubParsingFailure(new RequestBodyParseException(AppErrorCodeEnum.PDFE_700, ""));
		createHttpMessageBuilderSub(request);

		// Invoke
		final HttpResponseMessage response = function.run(request, executionContextMock);

		// Verify
		assertEquals(BAD_REQUEST, response.getStatus());
		Object body = response.getBody();
		assertNotNull(body);
		assertTrue(body instanceof ErrorResponse);
		Assertions.assertEquals(AppErrorCodeEnum.PDFE_700, ((ErrorResponse) body).getAppErrorCode());
		assertRequestDirectoryCleaned();
		verifyNoInteractions(generatePDFServiceMock);
	}

	@Test
	@SneakyThrows
	void runFailOnParseRequestBodyWith500() {
		// Setup
		@SuppressWarnings("unchecked")
		final HttpRequestMessage<Optional<byte[]>> request = mock(HttpRequestMessage.class);

		doReturn(Logger.getGlobal()).when(executionContextMock).getLogger();
		doReturn(Optional.of(new byte[2])).when(request).getBody();
		stubParsingFailure(new RequestBodyParseException(AppErrorCodeEnum.PDFE_704, ""));
		createHttpMessageBuilderSub(request);

		// Invoke
		final HttpResponseMessage response = function.run(request, executionContextMock);

		// Verify
		assertEquals(INTERNAL_SERVER_ERROR, response.getStatus());
		Object body = response.getBody();
		assertNotNull(body);
		assertTrue(body instanceof ErrorResponse);
		Assertions.assertEquals(AppErrorCodeEnum.PDFE_704, ((ErrorResponse) body).getAppErrorCode());
		assertRequestDirectoryCleaned();
		verifyNoInteractions(generatePDFServiceMock);
	}

	@Test
	@SneakyThrows
	void runFailOnInvalidTemplate() {
		// Setup
		@SuppressWarnings("unchecked")
		final HttpRequestMessage<Optional<byte[]>> request = mock(HttpRequestMessage.class);

		GeneratePDFInput generatePDFInput = new GeneratePDFInput();

		doReturn(Logger.getGlobal()).when(executionContextMock).getLogger();
		doReturn(Optional.of(new byte[2])).when(request).getBody();
		stubParsedInput(generatePDFInput);
		createHttpMessageBuilderSub(request);

		// Invoke
		final HttpResponseMessage response = function.run(request, executionContextMock);

		// Verify
		assertEquals(BAD_REQUEST, response.getStatus());
		Object body = response.getBody();
		assertNotNull(body);
		assertTrue(body instanceof ErrorResponse);
		Assertions.assertEquals(AppErrorCodeEnum.PDFE_897, ((ErrorResponse) body).getAppErrorCode());
		assertRequestDirectoryCleaned();
		verifyNoInteractions(generatePDFServiceMock);
	}

	@Test
	@SneakyThrows
	void runFailOnInvalidData() {
		// Setup
		@SuppressWarnings("unchecked")
		final HttpRequestMessage<Optional<byte[]>> request = mock(HttpRequestMessage.class);

		GeneratePDFInput generatePDFInput = new GeneratePDFInput();
		generatePDFInput.setTemplateZip(mock(ZipFile.class));

		doReturn(Logger.getGlobal()).when(executionContextMock).getLogger();
		doReturn(Optional.of(new byte[2])).when(request).getBody();
		stubParsedInput(generatePDFInput);
		createHttpMessageBuilderSub(request);

		// Invoke
		final HttpResponseMessage response = function.run(request, executionContextMock);

		// Verify
		assertEquals(BAD_REQUEST, response.getStatus());
		Object body = response.getBody();
		assertNotNull(body);
		assertTrue(body instanceof ErrorResponse);
		Assertions.assertEquals(AppErrorCodeEnum.PDFE_898, ((ErrorResponse) body).getAppErrorCode());
		assertRequestDirectoryCleaned();
		verifyNoInteractions(generatePDFServiceMock);
	}

	@Test
	@SneakyThrows
	void runFailOnGeneratePDFForPDFEngineException() {
		// Setup
		@SuppressWarnings("unchecked")
		final HttpRequestMessage<Optional<byte[]>> request = mock(HttpRequestMessage.class);

		GeneratePDFInput generatePDFInput = new GeneratePDFInput();
		generatePDFInput.setData(Collections.singletonMap("a", "b"));
		generatePDFInput.setApplySignature(false);
		generatePDFInput.setTemplateZip(mock(ZipFile.class));

		doReturn(Logger.getGlobal()).when(executionContextMock).getLogger();
		doReturn(Optional.of(new byte[2])).when(request).getBody();
		stubParsedInput(generatePDFInput);
		Mockito.doThrow(new CompileTemplateException(AppErrorCodeEnum.PDFE_901, "")).when(generatePDFServiceMock)
				.generatePDF(any(), any(), any());
		createHttpMessageBuilderSub(request);

		// Invoke
		final HttpResponseMessage response = function.run(request, executionContextMock);

		// Verify
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatus());
		Object body = response.getBody();
		assertNotNull(body);
		assertTrue(body instanceof ErrorResponse);
		assertEquals(AppErrorCodeEnum.PDFE_901, ((ErrorResponse) body).getAppErrorCode());
		assertRequestDirectoryCleaned();
	}

	@Test
	@SneakyThrows
	void runFailOnReadPDFDocumentBytesForIOException() {
		// Setup
		@SuppressWarnings("unchecked")
		final HttpRequestMessage<Optional<byte[]>> request = mock(HttpRequestMessage.class);

		GeneratePDFInput generatePDFInput = new GeneratePDFInput();
		generatePDFInput.setData(Collections.singletonMap("a", "b"));
		generatePDFInput.setApplySignature(false);
		generatePDFInput.setTemplateZip(mock(ZipFile.class));

		BufferedInputStream inputStreamMock = mock(BufferedInputStream.class);

		doReturn(Logger.getGlobal()).when(executionContextMock).getLogger();
		doReturn(Optional.of(new byte[2])).when(request).getBody();
		stubParsedInput(generatePDFInput);
		stubGeneratedStream(inputStreamMock);
		doThrow(IOException.class).when(inputStreamMock).readAllBytes();

		createHttpMessageBuilderSub(request);

		// Invoke
		final HttpResponseMessage response = function.run(request, executionContextMock);

		// Verify
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatus());
		Object body = response.getBody();
		assertNotNull(body);
		assertTrue(body instanceof ErrorResponse);
		assertEquals(AppErrorCodeEnum.PDFE_907, ((ErrorResponse) body).getAppErrorCode());
		assertRequestDirectoryCleaned();
		verify(inputStreamMock).close();
	}

	@Test
	void unexpectedParsingFailureStillCleansRequestDirectory() throws Exception {

		@SuppressWarnings("unchecked")
		HttpRequestMessage<Optional<byte[]>> request = mock(HttpRequestMessage.class);

		doReturn(Optional.of(new byte[2])).when(request).getBody();
		createHttpMessageBuilderSub(request);

		doAnswer(invocation -> {
			createRequestFiles(invocation.getArgument(2, Path.class));
			throw new IllegalStateException("Unexpected parser failure");
		}).when(parseRequestBodyServiceMock).retrieveInputData(any(), anyMap(), any());

		IllegalStateException failure = assertThrows(IllegalStateException.class,
				() -> function.run(request, executionContextMock));

		assertEquals("Unexpected parser failure", failure.getMessage());
		assertRequestDirectoryCleaned();
		verifyNoInteractions(generatePDFServiceMock);
	}

	@Test
	void successfulResponseClosesRealPdfStreamAndCleansAllRequestFiles() throws Exception {

		@SuppressWarnings("unchecked")
		HttpRequestMessage<Optional<byte[]>> request = mock(HttpRequestMessage.class);

		doReturn(Optional.of(new byte[2])).when(request).getBody();
		createHttpMessageBuilderSub(request);

		GeneratePDFInput input = new GeneratePDFInput();
		input.setData(Collections.singletonMap("a", "b"));
		input.setTemplateZip(mock(ZipFile.class));
		stubParsedInput(input);

		byte[] pdf = "%PDF-1.4\nrequest PDF\n%%EOF".getBytes(java.nio.charset.StandardCharsets.UTF_8);

		AtomicReference<BufferedInputStream> returnedStream = new AtomicReference<>();

		doAnswer(invocation -> {
			Path directory = invocation.getArgument(1, Path.class);
			assertEquals(requestWorkingDirectory, directory);

			Files.write(directory.resolve("original-node.pdf"), pdf);
			Path converted = Files.write(directory.resolve("converted-pdfa.pdf"), pdf);

			// Ownership of this stream is transferred to the function's try-with-resources.
			BufferedInputStream stream = spy(new BufferedInputStream(Files.newInputStream(converted)));

			returnedStream.set(stream);
			return stream;

		}).when(generatePDFServiceMock).generatePDF(any(), any(), any());

		HttpResponseMessage response = function.run(request, executionContextMock);

		assertEquals(HttpStatus.OK, response.getStatus());
		assertArrayEquals(pdf, (byte[]) response.getBody());
		assertEquals(String.valueOf(pdf.length), response.getHeader("content-length"));

		verify(returnedStream.get()).close();
		assertRequestDirectoryCleaned();
	}

	private void createHttpMessageBuilderSub(HttpRequestMessage<Optional<byte[]>> request) {
		doAnswer((Answer<HttpResponseMessage.Builder>) invocation -> {
			HttpStatus status = (HttpStatus) invocation.getArguments()[0];
			return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
		}).when(request).createResponseBuilder(any(HttpStatus.class));
	}

	private void stubParsedInput(GeneratePDFInput input) throws Exception {
		doAnswer(invocation -> {
			createRequestFiles(invocation.getArgument(2, Path.class));
			return input;
		}).when(parseRequestBodyServiceMock).retrieveInputData(any(), anyMap(), any());
	}

	private void stubParsingFailure(RequestBodyParseException failure) throws Exception {

		doAnswer(invocation -> {
			createRequestFiles(invocation.getArgument(2, Path.class));
			throw failure;
		}).when(parseRequestBodyServiceMock).retrieveInputData(any(), anyMap(), any());
	}

	private void createRequestFiles(Path directory) throws IOException {
		requestWorkingDirectory = directory;

		assertTrue(Files.isDirectory(directory), "The request directory must exist before parsing");

		Files.writeString(directory.resolve("template.zip"), "Partial or complete uploaded template");

		Path assets = Files.createDirectory(directory.resolve("assets"));
		Files.writeString(assets.resolve("image.bin"), "Uploaded template asset");
	}

	private void stubGeneratedStream(BufferedInputStream stream) throws Exception {

		doAnswer(invocation -> {
			Path directory = invocation.getArgument(1, Path.class);
			assertEquals(requestWorkingDirectory, directory);

			Files.writeString(directory.resolve("original-node.pdf"), "Original PDF returned by Node");
			Files.writeString(directory.resolve("converted-pdfa.pdf"), "Converted PDF");

			return stream;
		}).when(generatePDFServiceMock).generatePDF(any(), any(), any());
	}

	private void assertRequestDirectoryCleaned() throws IOException {
		assertNotNull(requestWorkingDirectory, "The parser must receive the actual request directory");

		assertFalse(Files.exists(requestWorkingDirectory),
				"The function must remove the request directory and every file inside it");

		assertOtherRequestIntact();
	}

	private void assertNoRequestDirectoryCreated() throws IOException {
		assertNull(requestWorkingDirectory);

		try (Stream<Path> entries = Files.list(workingDirectory)) {
			assertEquals(Set.of(otherRequestDirectory), entries.collect(Collectors.toSet()));
		}

		assertOtherRequestIntact();
	}

	private void assertOtherRequestIntact() throws IOException {
		assertTrue(Files.isDirectory(workingDirectory), "The shared base directory must remain available");
		assertTrue(Files.isDirectory(otherRequestDirectory));

		assertEquals("PDF belonging to another request", Files.readString(otherRequestDirectory.resolve("keep.pdf")));
	}
}
