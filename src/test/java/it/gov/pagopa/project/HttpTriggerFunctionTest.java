package it.gov.pagopa.project;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import it.gov.pagopa.project.exception.CompileTemplateException;
import it.gov.pagopa.project.exception.PDFEngineException;
import it.gov.pagopa.project.model.AppErrorCodeEnum;
import it.gov.pagopa.project.model.ErrorResponse;
import it.gov.pagopa.project.model.GeneratePDFInput;
import it.gov.pagopa.project.service.GeneratePDFService;
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
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HttpTriggerFunctionTest {

    private HttpTriggerFunction function;

    @Mock
    private GeneratePDFService generatePDFServiceMock;

    @Mock
    ExecutionContext context;

    @BeforeEach
    void setUp() {
        function = spy(new HttpTriggerFunction(generatePDFServiceMock));
    }

    @Test
    @SneakyThrows
    void runOk() {
        // Setup
        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        @SuppressWarnings("unchecked")
        final HttpRequestMessage<GeneratePDFInput> request = mock(HttpRequestMessage.class);

        GeneratePDFInput inputBody = new GeneratePDFInput();
        inputBody.setTemplate("adsfasdf");
        inputBody.setData(Collections.singletonMap("a", "b"));
        inputBody.setApplySignature(false);

        doReturn(inputBody).when(request).getBody();
        doReturn(new ByteArrayOutputStream()).when(generatePDFServiceMock).generatePDF(any());

        doAnswer((Answer<HttpResponseMessage.Builder>) invocation -> {
            HttpStatus status = (HttpStatus) invocation.getArguments()[0];
            return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
        }).when(request).createResponseBuilder(any(HttpStatus.class));

        // Invoke
        final HttpResponseMessage response = function.run(request, context);

        // Verify
        assertEquals(HttpStatus.OK, response.getStatus());
        assertEquals( "application/pdf", response.getHeader("content-type"));
        assertEquals( "attachment; ", response.getHeader("content-disposition"));
    }

    @Test
    void runFailOnInvalidInput() {
        // Setup
        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        @SuppressWarnings("unchecked")
        final HttpRequestMessage<GeneratePDFInput> request = mock(HttpRequestMessage.class);

        doReturn(null).when(request).getBody();

        doAnswer((Answer<HttpResponseMessage.Builder>) invocation -> {
            HttpStatus status = (HttpStatus) invocation.getArguments()[0];
            return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
        }).when(request).createResponseBuilder(any(HttpStatus.class));

        // Invoke
        final HttpResponseMessage response = function.run(request, context);

        // Verify
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatus());
        Object body = response.getBody();
        assertNotNull(body);
        assertTrue(body instanceof ErrorResponse);
        assertEquals(AppErrorCodeEnum.PDFE_899, ((ErrorResponse) body).getAppErrorCode());
    }

    @Test
    void runFailOnNullTemplate() {
        // Setup
        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        @SuppressWarnings("unchecked")
        final HttpRequestMessage<GeneratePDFInput> request = mock(HttpRequestMessage.class);

        GeneratePDFInput inputBody = new GeneratePDFInput();
        doReturn(inputBody).when(request).getBody();

        doAnswer((Answer<HttpResponseMessage.Builder>) invocation -> {
            HttpStatus status = (HttpStatus) invocation.getArguments()[0];
            return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
        }).when(request).createResponseBuilder(any(HttpStatus.class));

        // Invoke
        final HttpResponseMessage response = function.run(request, context);

        // Verify
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatus());
        Object body = response.getBody();
        assertNotNull(body);
        assertTrue(body instanceof ErrorResponse);
        assertEquals(AppErrorCodeEnum.PDFE_897, ((ErrorResponse) body).getAppErrorCode());
    }

    @Test
    void runFailOnEmptyTemplate() {
        // Setup
        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        @SuppressWarnings("unchecked")
        final HttpRequestMessage<GeneratePDFInput> request = mock(HttpRequestMessage.class);

        GeneratePDFInput inputBody = new GeneratePDFInput();
        inputBody.setTemplate("");
        doReturn(inputBody).when(request).getBody();

        doAnswer((Answer<HttpResponseMessage.Builder>) invocation -> {
            HttpStatus status = (HttpStatus) invocation.getArguments()[0];
            return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
        }).when(request).createResponseBuilder(any(HttpStatus.class));

        // Invoke
        final HttpResponseMessage response = function.run(request, context);

        // Verify
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatus());
        Object body = response.getBody();
        assertNotNull(body);
        assertTrue(body instanceof ErrorResponse);
        assertEquals(AppErrorCodeEnum.PDFE_897, ((ErrorResponse) body).getAppErrorCode());
    }

    @Test
    void runFailOnInvalidData() {
        // Setup
        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        @SuppressWarnings("unchecked")
        final HttpRequestMessage<GeneratePDFInput> request = mock(HttpRequestMessage.class);

        GeneratePDFInput inputBody = new GeneratePDFInput();
        inputBody.setTemplate("adsfasdf");
        doReturn(inputBody).when(request).getBody();

        doAnswer((Answer<HttpResponseMessage.Builder>) invocation -> {
            HttpStatus status = (HttpStatus) invocation.getArguments()[0];
            return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
        }).when(request).createResponseBuilder(any(HttpStatus.class));

        // Invoke
        final HttpResponseMessage response = function.run(request, context);

        // Verify
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatus());
        Object body = response.getBody();
        assertNotNull(body);
        assertTrue(body instanceof ErrorResponse);
        assertEquals(AppErrorCodeEnum.PDFE_898, ((ErrorResponse) body).getAppErrorCode());
    }

    @Test
    @SneakyThrows
    void runFailOnGeneratePDFForPDFEngineException() {
        // Setup
        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        @SuppressWarnings("unchecked")
        final HttpRequestMessage<GeneratePDFInput> request = mock(HttpRequestMessage.class);

        GeneratePDFInput inputBody = new GeneratePDFInput();
        inputBody.setTemplate("adsfasdf");
        inputBody.setData(Collections.singletonMap("a", "b"));
        inputBody.setApplySignature(false);

        doReturn(inputBody).when(request).getBody();
        doThrow(new CompileTemplateException(AppErrorCodeEnum.PDFE_901, "")).when(generatePDFServiceMock).generatePDF(any());

        doAnswer((Answer<HttpResponseMessage.Builder>) invocation -> {
            HttpStatus status = (HttpStatus) invocation.getArguments()[0];
            return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
        }).when(request).createResponseBuilder(any(HttpStatus.class));

        // Invoke
        final HttpResponseMessage response = function.run(request, context);

        // Verify
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatus());
        Object body = response.getBody();
        assertNotNull(body);
        assertTrue(body instanceof ErrorResponse);
        assertEquals(AppErrorCodeEnum.PDFE_901, ((ErrorResponse) body).getAppErrorCode());
    }
}
