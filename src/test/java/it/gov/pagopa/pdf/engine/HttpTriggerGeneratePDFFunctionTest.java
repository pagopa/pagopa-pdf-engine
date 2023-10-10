
package it.gov.pagopa.pdf.engine;

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
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Optional;
import java.util.logging.Logger;

import static com.microsoft.azure.functions.HttpStatus.BAD_REQUEST;
import static com.microsoft.azure.functions.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SystemStubsExtension.class)
class HttpTriggerGeneratePDFFunctionTest {

    @SystemStub
    private EnvironmentVariables environmentVariables;

    private HttpTriggerGeneratePDFFunction function;

    private GeneratePDFService generatePDFServiceMock;
    private ParseRequestBodyService parseRequestBodyServiceMock;
    private ExecutionContext executionContextMock;

    @BeforeEach
    void setUp() {
        environmentVariables.set("WORKING_DIRECTORY_PATH", "temp");

        generatePDFServiceMock = mock(GeneratePDFService.class);
        parseRequestBodyServiceMock = mock(ParseRequestBodyService.class);
        executionContextMock = mock(ExecutionContext.class);
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
        generatePDFInput.setTemplateZip(new ZipFile(""));
        generatePDFInput.setGeneratorType(GeneratorType.ITEXT);

        BufferedInputStream inputStreamMock = mock(BufferedInputStream.class);

        doReturn(Logger.getGlobal()).when(executionContextMock).getLogger();
        doReturn(Optional.of(new byte[2])).when(request).getBody();
        doReturn(generatePDFInput).when(parseRequestBodyServiceMock).retrieveInputData(any(), anyMap(), any());
        doReturn(inputStreamMock).when(generatePDFServiceMock).generatePDF(any(), any(), any());
        doReturn(new byte[5]).when(inputStreamMock).readAllBytes();

        createHttpMessageBuilderSub(request);

        // Invoke
        final HttpResponseMessage response = function.run(request, executionContextMock);

        // Verify
        assertEquals(HttpStatus.OK, response.getStatus());
        assertEquals( "application/pdf", response.getHeader("content-type"));
        assertEquals( "attachment; ", response.getHeader("content-disposition"));
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
        generatePDFInput.setTemplateZip(new ZipFile(""));
        generatePDFInput.setGenerateZipped(true);

        BufferedInputStream inputStreamMock = mock(BufferedInputStream.class);

        doReturn(Logger.getGlobal()).when(executionContextMock).getLogger();
        doReturn(Optional.of(new byte[2])).when(request).getBody();
        doReturn(generatePDFInput).when(parseRequestBodyServiceMock).retrieveInputData(any(), anyMap(), any());
        doReturn(inputStreamMock).when(generatePDFServiceMock).generatePDF(any(), any(), any());
        doReturn(new byte[5]).when(inputStreamMock).readAllBytes();

        createHttpMessageBuilderSub(request);

        // Invoke
        final HttpResponseMessage response = function.run(request, executionContextMock);

        // Verify
        assertEquals(HttpStatus.OK, response.getStatus());
        assertEquals( "application/zip", response.getHeader("content-type"));
        assertEquals( "attachment; ", response.getHeader("content-disposition"));
    }

    @Test
    void runFailOnInvalidInput() {
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
    }

    @Test
    void runFailOnCreateWorkingDirectory() {
        // Setup
        @SuppressWarnings("unchecked")
        final HttpRequestMessage<Optional<byte[]>> request = mock(HttpRequestMessage.class);

        doReturn(Logger.getGlobal()).when(executionContextMock).getLogger();
        doReturn(Optional.of(new byte[2])).when(request).getBody();
        createHttpMessageBuilderSub(request);

        try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
            filesMockedStatic.when(
                    () -> Files.createTempDirectory(any(), anyString())
            ).thenThrow(IOException.class);

            // Invoke
            final HttpResponseMessage response = function.run(request, executionContextMock);

            // Verify
            assertEquals(INTERNAL_SERVER_ERROR, response.getStatus());
            Object body = response.getBody();
            assertNotNull(body);
            assertTrue(body instanceof ErrorResponse);
            Assertions.assertEquals(AppErrorCodeEnum.PDFE_908, ((ErrorResponse) body).getAppErrorCode());
        }

    }

    @Test
    @SneakyThrows
    void runFailOnParseRequestBodyWith400() {
        // Setup
        @SuppressWarnings("unchecked")
        final HttpRequestMessage<Optional<byte[]>> request = mock(HttpRequestMessage.class);

        doReturn(Logger.getGlobal()).when(executionContextMock).getLogger();
        doReturn(Optional.of(new byte[2])).when(request).getBody();
        Mockito.doThrow(new RequestBodyParseException(AppErrorCodeEnum.PDFE_700, "")).when(parseRequestBodyServiceMock).retrieveInputData(any(), anyMap(), any());
        createHttpMessageBuilderSub(request);

        // Invoke
        final HttpResponseMessage response = function.run(request, executionContextMock);

        // Verify
        assertEquals(BAD_REQUEST, response.getStatus());
        Object body = response.getBody();
        assertNotNull(body);
        assertTrue(body instanceof ErrorResponse);
        Assertions.assertEquals(AppErrorCodeEnum.PDFE_700, ((ErrorResponse) body).getAppErrorCode());
    }

    @Test
    @SneakyThrows
    void runFailOnParseRequestBodyWith500() {
        // Setup
        @SuppressWarnings("unchecked")
        final HttpRequestMessage<Optional<byte[]>> request = mock(HttpRequestMessage.class);

        doReturn(Logger.getGlobal()).when(executionContextMock).getLogger();
        doReturn(Optional.of(new byte[2])).when(request).getBody();
        doThrow(new RequestBodyParseException(AppErrorCodeEnum.PDFE_704, "")).when(parseRequestBodyServiceMock).retrieveInputData(any(), anyMap(), any());
        createHttpMessageBuilderSub(request);

        // Invoke
        final HttpResponseMessage response = function.run(request, executionContextMock);

        // Verify
        assertEquals(INTERNAL_SERVER_ERROR, response.getStatus());
        Object body = response.getBody();
        assertNotNull(body);
        assertTrue(body instanceof ErrorResponse);
        Assertions.assertEquals(AppErrorCodeEnum.PDFE_704, ((ErrorResponse) body).getAppErrorCode());
    }

/*    @Test
    @SneakyThrows
    void runFailOnInvalidTemplate() {
        // Setup
        @SuppressWarnings("unchecked")
        final HttpRequestMessage<Optional<byte[]>> request = mock(HttpRequestMessage.class);

        GeneratePDFInput generatePDFInput = new GeneratePDFInput();

        doReturn(Logger.getGlobal()).when(executionContextMock).getLogger();
        doReturn(Optional.of(new byte[2])).when(request).getBody();
        doReturn(generatePDFInput).when(parseRequestBodyServiceMock).retrieveInputData(any(), anyMap(), any());
        createHttpMessageBuilderSub(request);

        // Invoke
        final HttpResponseMessage response = function.run(request, executionContextMock);

        // Verify
        assertEquals(BAD_REQUEST, response.getStatus());
        Object body = response.getBody();
        assertNotNull(body);
        assertTrue(body instanceof ErrorResponse);
        Assertions.assertEquals(AppErrorCodeEnum.PDFE_897, ((ErrorResponse) body).getAppErrorCode());
    }*/

/*    @Test
    @SneakyThrows
    void runFailOnInvalidData() {
        // Setup
        @SuppressWarnings("unchecked")
        final HttpRequestMessage<Optional<byte[]>> request = mock(HttpRequestMessage.class);

        GeneratePDFInput generatePDFInput = new GeneratePDFInput();
        generatePDFInput.setTemplateZip(new ZipFile(""));

        doReturn(Logger.getGlobal()).when(executionContextMock).getLogger();
        doReturn(Optional.of(new byte[2])).when(request).getBody();
        doReturn(generatePDFInput).when(parseRequestBodyServiceMock).retrieveInputData(any(), anyMap(), any());
        createHttpMessageBuilderSub(request);

        // Invoke
        final HttpResponseMessage response = function.run(request, executionContextMock);

        // Verify
        assertEquals(BAD_REQUEST, response.getStatus());
        Object body = response.getBody();
        assertNotNull(body);
        assertTrue(body instanceof ErrorResponse);
        Assertions.assertEquals(AppErrorCodeEnum.PDFE_898, ((ErrorResponse) body).getAppErrorCode());
    }*/

    @Test
    @SneakyThrows
    void runFailOnGeneratePDFForPDFEngineException() {
        // Setup
        @SuppressWarnings("unchecked")
        final HttpRequestMessage<Optional<byte[]>> request = mock(HttpRequestMessage.class);

        GeneratePDFInput generatePDFInput = new GeneratePDFInput();
        generatePDFInput.setData(Collections.singletonMap("a", "b"));
        generatePDFInput.setApplySignature(false);
        generatePDFInput.setTemplateZip(new ZipFile(""));

        doReturn(Logger.getGlobal()).when(executionContextMock).getLogger();
        doReturn(Optional.of(new byte[2])).when(request).getBody();
        doReturn(generatePDFInput).when(parseRequestBodyServiceMock).retrieveInputData(any(), anyMap(), any());
        Mockito.doThrow(new CompileTemplateException(AppErrorCodeEnum.PDFE_901, "")).when(generatePDFServiceMock).generatePDF(any(), any(), any());
        createHttpMessageBuilderSub(request);

        // Invoke
        final HttpResponseMessage response = function.run(request, executionContextMock);

        // Verify
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatus());
        Object body = response.getBody();
        assertNotNull(body);
        assertTrue(body instanceof ErrorResponse);
        assertEquals(AppErrorCodeEnum.PDFE_901, ((ErrorResponse) body).getAppErrorCode());
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
        generatePDFInput.setTemplateZip(new ZipFile(""));

        BufferedInputStream inputStreamMock = mock(BufferedInputStream.class);

        doReturn(Logger.getGlobal()).when(executionContextMock).getLogger();
        doReturn(Optional.of(new byte[2])).when(request).getBody();
        doReturn(generatePDFInput).when(parseRequestBodyServiceMock).retrieveInputData(any(), anyMap(), any());
        doReturn(inputStreamMock).when(generatePDFServiceMock).generatePDF(any(), any(), any());
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
    }

    private void createHttpMessageBuilderSub(HttpRequestMessage<Optional<byte[]>> request) {
        doAnswer((Answer<HttpResponseMessage.Builder>) invocation -> {
            HttpStatus status = (HttpStatus) invocation.getArguments()[0];
            return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
        }).when(request).createResponseBuilder(any(HttpStatus.class));
    }
}
