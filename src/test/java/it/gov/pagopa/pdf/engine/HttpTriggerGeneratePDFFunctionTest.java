/*
Copyright (C)

This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public
License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
details.

You should have received a copy of the GNU Affero General Public License along with this program.
If not, see https://www.gnu.org/licenses/.
*/

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
import it.gov.pagopa.pdf.engine.service.GeneratePDFService;
import it.gov.pagopa.pdf.engine.service.ParseRequestBodyService;
import it.gov.pagopa.pdf.engine.util.HttpResponseMessageMock;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

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

class HttpTriggerGeneratePDFFunctionTest {

    private HttpTriggerGeneratePDFFunction function;

    private GeneratePDFService generatePDFServiceMock;
    private ParseRequestBodyService parseRequestBodyServiceMock;
    private ExecutionContext executionContextMock;

    @BeforeEach
    void setUp() {
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
        generatePDFInput.setTemplateSavedOnFileSystem(true);

        BufferedInputStream inputStreamMock = mock(BufferedInputStream.class);

        doReturn(Logger.getGlobal()).when(executionContextMock).getLogger();
        doReturn(Optional.of(new byte[2])).when(request).getBody();
        doReturn(generatePDFInput).when(parseRequestBodyServiceMock).retrieveInputData(any(), anyMap(), any());
        doReturn(inputStreamMock).when(generatePDFServiceMock).generatePDF(any(), any());
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
        generatePDFInput.setTemplateSavedOnFileSystem(true);
        generatePDFInput.setGenerateZipped(true);

        BufferedInputStream inputStreamMock = mock(BufferedInputStream.class);

        doReturn(Logger.getGlobal()).when(executionContextMock).getLogger();
        doReturn(Optional.of(new byte[2])).when(request).getBody();
        doReturn(generatePDFInput).when(parseRequestBodyServiceMock).retrieveInputData(any(), anyMap(), any());
        doReturn(inputStreamMock).when(generatePDFServiceMock).generatePDF(any(), any());
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

    @Test
    @SneakyThrows
    void runFailOnInvalidTemplate() {
        // Setup
        @SuppressWarnings("unchecked")
        final HttpRequestMessage<Optional<byte[]>> request = mock(HttpRequestMessage.class);

        GeneratePDFInput generatePDFInput = new GeneratePDFInput();
        generatePDFInput.setTemplateSavedOnFileSystem(false);

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
    }

    @Test
    @SneakyThrows
    void runFailOnInvalidData() {
        // Setup
        @SuppressWarnings("unchecked")
        final HttpRequestMessage<Optional<byte[]>> request = mock(HttpRequestMessage.class);

        GeneratePDFInput generatePDFInput = new GeneratePDFInput();
        generatePDFInput.setTemplateSavedOnFileSystem(true);

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
        generatePDFInput.setTemplateSavedOnFileSystem(true);

        doReturn(Logger.getGlobal()).when(executionContextMock).getLogger();
        doReturn(Optional.of(new byte[2])).when(request).getBody();
        doReturn(generatePDFInput).when(parseRequestBodyServiceMock).retrieveInputData(any(), anyMap(), any());
        Mockito.doThrow(new CompileTemplateException(AppErrorCodeEnum.PDFE_901, "")).when(generatePDFServiceMock).generatePDF(any(), any());
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
        generatePDFInput.setTemplateSavedOnFileSystem(true);

        BufferedInputStream inputStreamMock = mock(BufferedInputStream.class);

        doReturn(Logger.getGlobal()).when(executionContextMock).getLogger();
        doReturn(Optional.of(new byte[2])).when(request).getBody();
        doReturn(generatePDFInput).when(parseRequestBodyServiceMock).retrieveInputData(any(), anyMap(), any());
        doReturn(inputStreamMock).when(generatePDFServiceMock).generatePDF(any(), any());
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
