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

package it.gov.pagopa.project;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import it.gov.pagopa.project.exception.CompileTemplateException;
import it.gov.pagopa.project.exception.RequestBodyParseException;
import it.gov.pagopa.project.model.AppErrorCodeEnum;
import it.gov.pagopa.project.model.ErrorResponse;
import it.gov.pagopa.project.model.GeneratePDFInput;
import it.gov.pagopa.project.service.GeneratePDFService;
import it.gov.pagopa.project.service.ParseRequestBodyService;
import it.gov.pagopa.project.util.HttpResponseMessageMock;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.Optional;
import java.util.logging.Logger;

import static com.microsoft.azure.functions.HttpStatus.BAD_REQUEST;
import static it.gov.pagopa.project.model.AppErrorCodeEnum.PDFE_700;
import static it.gov.pagopa.project.model.AppErrorCodeEnum.PDFE_898;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HttpTriggerFunctionTest {

    private HttpTriggerGeneratePDFFunction function;

    @Mock
    private GeneratePDFService generatePDFServiceMock;
    @Mock
    private ParseRequestBodyService parseRequestBodyServiceMock;

    @Mock
    private ExecutionContext executionContextMock;

    @BeforeEach
    void setUp() {
        function = spy(new HttpTriggerGeneratePDFFunction(generatePDFServiceMock, parseRequestBodyServiceMock));
    }

    @Test
    @SneakyThrows
    void runOk() {
        // Setup
        @SuppressWarnings("unchecked")
        final HttpRequestMessage<Optional<byte[]>> request = mock(HttpRequestMessage.class);

        GeneratePDFInput generatePDFInput = new GeneratePDFInput();
        generatePDFInput.setData(Collections.singletonMap("a", "b"));
        generatePDFInput.setApplySignature(false);

        doReturn(Logger.getGlobal()).when(executionContextMock).getLogger();
        doReturn(Optional.of(new byte[2])).when(request).getBody();
        doReturn(generatePDFInput).when(parseRequestBodyServiceMock).retrieveInputData(any(), anyMap());
        doReturn(new ByteArrayOutputStream()).when(generatePDFServiceMock).generatePDF(any());
        createHttpMessageBuilderSub(request);

        // Invoke
        final HttpResponseMessage response = function.run(request, executionContextMock);

        // Verify
        assertEquals(HttpStatus.OK, response.getStatus());
        assertEquals( "application/pdf", response.getHeader("content-type"));
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
        assertEquals(AppErrorCodeEnum.PDFE_899, ((ErrorResponse) body).getAppErrorCode());
    }

    @Test
    @SneakyThrows
    void runFailOnParseRequestBody() {
        // Setup
        @SuppressWarnings("unchecked")
        final HttpRequestMessage<Optional<byte[]>> request = mock(HttpRequestMessage.class);

        doReturn(Logger.getGlobal()).when(executionContextMock).getLogger();
        doReturn(Optional.of(new byte[2])).when(request).getBody();
        doThrow(new RequestBodyParseException(PDFE_700, "")).when(parseRequestBodyServiceMock).retrieveInputData(any(), anyMap());
        createHttpMessageBuilderSub(request);

        // Invoke
        final HttpResponseMessage response = function.run(request, executionContextMock);

        // Verify
        assertEquals(BAD_REQUEST, response.getStatus());
        Object body = response.getBody();
        assertNotNull(body);
        assertTrue(body instanceof ErrorResponse);
        assertEquals(PDFE_700, ((ErrorResponse) body).getAppErrorCode());
    }

    @Test
    @SneakyThrows
    void runFailOnInvalidData() {
        // Setup
        @SuppressWarnings("unchecked")
        final HttpRequestMessage<Optional<byte[]>> request = mock(HttpRequestMessage.class);

        GeneratePDFInput generatePDFInput = new GeneratePDFInput();

        doReturn(Logger.getGlobal()).when(executionContextMock).getLogger();
        doReturn(Optional.of(new byte[2])).when(request).getBody();
        doReturn(generatePDFInput).when(parseRequestBodyServiceMock).retrieveInputData(any(), anyMap());
        createHttpMessageBuilderSub(request);

        // Invoke
        final HttpResponseMessage response = function.run(request, executionContextMock);

        // Verify
        assertEquals(BAD_REQUEST, response.getStatus());
        Object body = response.getBody();
        assertNotNull(body);
        assertTrue(body instanceof ErrorResponse);
        assertEquals(PDFE_898, ((ErrorResponse) body).getAppErrorCode());
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

        doReturn(Logger.getGlobal()).when(executionContextMock).getLogger();
        doReturn(Optional.of(new byte[2])).when(request).getBody();
        doReturn(generatePDFInput).when(parseRequestBodyServiceMock).retrieveInputData(any(), anyMap());
        doThrow(new CompileTemplateException(AppErrorCodeEnum.PDFE_901, "")).when(generatePDFServiceMock).generatePDF(any());
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

    private void createHttpMessageBuilderSub(HttpRequestMessage<Optional<byte[]>> request) {
        doAnswer((Answer<HttpResponseMessage.Builder>) invocation -> {
            HttpStatus status = (HttpStatus) invocation.getArguments()[0];
            return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
        }).when(request).createResponseBuilder(any(HttpStatus.class));
    }
}
