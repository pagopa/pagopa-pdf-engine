package it.gov.pagopa.pdf.engine;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import it.gov.pagopa.pdf.engine.client.impl.PdfEngineClientImpl;
import it.gov.pagopa.pdf.engine.model.AppInfo;
import it.gov.pagopa.pdf.engine.util.HttpResponseMessageMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InfoTest {

	@Mock
    ExecutionContext executionContextMock;

    @Spy
    Info sut;

    @Test
    void runOK() {
        @SuppressWarnings("unchecked")
        HttpRequestMessage<Optional<String>> request = mock(HttpRequestMessage.class);

        PdfEngineClientImpl pdfEngineClient = mock(PdfEngineClientImpl.class);
        when(pdfEngineClient.info()).thenReturn(true);
        InfoTest.setMock(PdfEngineClientImpl.class, pdfEngineClient);

        doAnswer((Answer<HttpResponseMessage.Builder>) invocation -> {
            HttpStatus status = (HttpStatus) invocation.getArguments()[0];
            return new HttpResponseMessageMock.HttpResponseMessageBuilderMock().status(status);
        }).when(request).createResponseBuilder(any(HttpStatus.class));

        // test execution
        HttpResponseMessage response = sut.run(request, executionContextMock);

        // test assertion
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatus());
        assertNotNull(response.getBody());
        AppInfo responseBody = (AppInfo) response.getBody();
        assertNotNull(responseBody.getName());
        assertNotNull(responseBody.getVersion());
        assertNotNull(responseBody.getEnvironment());
        assertEquals("pagopa-pdf-engine", responseBody.getName());
        assertEquals("azure-fn", responseBody.getEnvironment());
    }

    private static <T> void setMock(Class<T> classToMock, T mock) {
        try {
            Field instance = classToMock.getDeclaredField("instance");
            instance.setAccessible(true);
            instance.set(instance, mock);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
