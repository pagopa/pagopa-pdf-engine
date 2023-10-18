
package it.gov.pagopa.pdf.engine.service.impl;

import it.gov.pagopa.pdf.engine.model.GeneratePDFInput;
import lombok.SneakyThrows;
import org.jboss.resteasy.reactive.server.core.multipart.FormData;
import org.jboss.resteasy.reactive.server.multipart.MultipartFormDataInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.spy;

class ParseRequestBodyServiceImplTest {

    private ParseRequestBodyServiceImpl sut;

    @BeforeEach
    void setUp() throws IOException {
        sut = spy(new ParseRequestBodyServiceImpl());
    }

    @Test
    @SneakyThrows
    void validContentShouldReturnExactCopy() {
        String data = "{\"test1\":\"data\"}";
        File file = new File("template");
        MultipartFormDataInput multipartFormDataInput = () -> {
            FormData formData = new FormData(2);
            formData.add("data",data);
            formData.add("template", file.toPath(),
                    "template.zip", null);
            return formData.toMultipartFormDataInput().getValues();
        };
        GeneratePDFInput result = sut.retrieveInputData(multipartFormDataInput);
        assertNotNull(result);
        assertNotNull(result.getData());
        assertEquals(data, result.getData());
        assertEquals(file, result.getTemplateZip());
    }

//    @Test
//    @SneakyThrows
//    void retrieveInputDataWithGeneratorTypeSuccess() {
//        MultipartStream multipartStreamMock = mock(MultipartStream.class);
//
//        doReturn(multipartStreamMock).when(sut).getMultipartStream(any(), anyString());
//        doReturn(true).when(multipartStreamMock).skipPreamble();
//        doReturn(
//                String.format(HEADER_TEMPLATE, "applySignature"),
//                String.format(HEADER_TEMPLATE, "generateZipped"),
//                String.format(HEADER_TEMPLATE, "data"),
//                String.format(HEADER_TEMPLATE, "generatorType")
//        ).when(multipartStreamMock).readHeaders();
//        doReturn(Collections.singletonMap("ke1", "value1")).when(objectMapperMock).readValue(anyString(), any(TypeReference.class));
//        doReturn(true, true, false).when(multipartStreamMock).readBoundary();
//
//        GeneratePDFInput result = sut.retrieveInputData(new byte[2], Collections.singletonMap(CONTENT_TYPE_HEADER, CONTENT_TYPE_HEADER_VALUE), workingPath);
//
//        assertNotNull(result);
//        assertNotNull(result.getData());
//    }
//
//    @Test
//    @SneakyThrows
//    void retrieveInputDataFailGetContentTypeHeaderIsNull() {
//        RequestBodyParseException e = assertThrows(
//                RequestBodyParseException.class,
//                () -> sut.retrieveInputData(new byte[2], Collections.emptyMap(), workingPath)
//        );
//
//        Assertions.assertEquals(AppErrorCodeEnum.PDFE_711, e.getErrorCode());
//    }
//
//    @Test
//    @SneakyThrows
//    void retrieveInputDataFailGetContentTypeHeaderIsNotMultipart() {
//        RequestBodyParseException e = assertThrows(
//                RequestBodyParseException.class,
//                () -> sut.retrieveInputData(new byte[2], Collections.singletonMap(CONTENT_TYPE_HEADER, "application/json"), workingPath)
//        );
//
//        Assertions.assertEquals(AppErrorCodeEnum.PDFE_712, e.getErrorCode());
//    }
//
//    @Test
//    @SneakyThrows
//    void retrieveInputDataFailSkipPreambleThrowsIOException() {
//        MultipartStream multipartStreamMock = mock(MultipartStream.class);
//
//        doReturn(multipartStreamMock).when(sut).getMultipartStream(any(), anyString());
//        doThrow(IOException.class).when(multipartStreamMock).skipPreamble();
//
//        RequestBodyParseException e = assertThrows(
//                RequestBodyParseException.class,
//                () -> sut.retrieveInputData(new byte[2], Collections.singletonMap(CONTENT_TYPE_HEADER, CONTENT_TYPE_HEADER_VALUE), workingPath)
//        );
//
//        Assertions.assertEquals(AppErrorCodeEnum.PDFE_700, e.getErrorCode());
//    }
//
//    @Test
//    @SneakyThrows
//    void retrieveInputDataFailReadHeaderThrowsFileUploadIOException() {
//        MultipartStream multipartStreamMock = mock(MultipartStream.class);
//
//        doReturn(multipartStreamMock).when(sut).getMultipartStream(any(), anyString());
//        doReturn(true).when(multipartStreamMock).skipPreamble();
//        doThrow(FileUploadIOException.class).when(multipartStreamMock).readHeaders();
//
//        RequestBodyParseException e = assertThrows(
//                RequestBodyParseException.class,
//                () -> sut.retrieveInputData(new byte[2], Collections.singletonMap(CONTENT_TYPE_HEADER, CONTENT_TYPE_HEADER_VALUE), workingPath)
//        );
//
//        Assertions.assertEquals(AppErrorCodeEnum.PDFE_701, e.getErrorCode());
//    }
//
//    @Test
//    @SneakyThrows
//    void retrieveInputDataFailReadHeaderThrowsMalformedStreamException() {
//        MultipartStream multipartStreamMock = mock(MultipartStream.class);
//
//        doReturn(multipartStreamMock).when(sut).getMultipartStream(any(), anyString());
//        doReturn(true).when(multipartStreamMock).skipPreamble();
//        doThrow(MultipartStream.MalformedStreamException.class).when(multipartStreamMock).readHeaders();
//
//        RequestBodyParseException e = assertThrows(
//                RequestBodyParseException.class,
//                () -> sut.retrieveInputData(new byte[2], Collections.singletonMap(CONTENT_TYPE_HEADER, CONTENT_TYPE_HEADER_VALUE), workingPath)
//        );
//
//        Assertions.assertEquals(AppErrorCodeEnum.PDFE_702, e.getErrorCode());
//    }
//
//    @Test
//    @SneakyThrows
//    void retrieveInputDataFailReadTemplateThrowsFileNotFoundException() {
//        MultipartStream multipartStreamMock = mock(MultipartStream.class);
//
//        doReturn(multipartStreamMock).when(sut).getMultipartStream(any(), anyString());
//        doReturn(true).when(multipartStreamMock).skipPreamble();
//        doReturn(String.format(HEADER_TEMPLATE, "template")).when(multipartStreamMock).readHeaders();
//        doThrow(FileNotFoundException.class).when(multipartStreamMock).readBodyData(any());
//
//        RequestBodyParseException e = assertThrows(
//                RequestBodyParseException.class,
//                () -> sut.retrieveInputData(new byte[2], Collections.singletonMap(CONTENT_TYPE_HEADER, CONTENT_TYPE_HEADER_VALUE), workingPath)
//        );
//
//        Assertions.assertEquals(AppErrorCodeEnum.PDFE_703, e.getErrorCode());
//    }
//
//    @Test
//    @SneakyThrows
//    void retrieveInputDataFailReadTemplateThrowsIOException() {
//        MultipartStream multipartStreamMock = mock(MultipartStream.class);
//
//        doReturn(multipartStreamMock).when(sut).getMultipartStream(any(), anyString());
//        doReturn(true).when(multipartStreamMock).skipPreamble();
//        doReturn(String.format(HEADER_TEMPLATE, "template")).when(multipartStreamMock).readHeaders();
//        doThrow(IOException.class).when(multipartStreamMock).readBodyData(any());
//
//        RequestBodyParseException e = assertThrows(
//                RequestBodyParseException.class,
//                () -> sut.retrieveInputData(new byte[2], Collections.singletonMap(CONTENT_TYPE_HEADER, CONTENT_TYPE_HEADER_VALUE), workingPath)
//        );
//
//        Assertions.assertEquals(AppErrorCodeEnum.PDFE_704, e.getErrorCode());
//    }
//
//    @Test
//    @SneakyThrows
//    void retrieveInputDataFailReadInputDataThrowsIOException() {
//        MultipartStream multipartStreamMock = mock(MultipartStream.class);
//
//        doReturn(multipartStreamMock).when(sut).getMultipartStream(any(), anyString());
//        doReturn(true).when(multipartStreamMock).skipPreamble();
//        doReturn(String.format(HEADER_TEMPLATE, "data")).when(multipartStreamMock).readHeaders();
//        doThrow(IOException.class).when(multipartStreamMock).readBodyData(any());
//
//        RequestBodyParseException e = assertThrows(
//                RequestBodyParseException.class,
//                () -> sut.retrieveInputData(new byte[2], Collections.singletonMap(CONTENT_TYPE_HEADER, CONTENT_TYPE_HEADER_VALUE), workingPath)
//        );
//
//        Assertions.assertEquals(AppErrorCodeEnum.PDFE_706, e.getErrorCode());
//    }
//
//    @Test
//    @SneakyThrows
//    void retrieveInputDataFailMapInputDataThrowsJsonProcessingException() {
//        MultipartStream multipartStreamMock = mock(MultipartStream.class);
//
//        doReturn(multipartStreamMock).when(sut).getMultipartStream(any(), anyString());
//        doReturn(true).when(multipartStreamMock).skipPreamble();
//        doReturn(String.format(HEADER_TEMPLATE, "data")).when(multipartStreamMock).readHeaders();
//        doThrow(JsonProcessingException.class).when(objectMapperMock).readValue(anyString(), any(TypeReference.class));
//
//        RequestBodyParseException e = assertThrows(
//                RequestBodyParseException.class,
//                () -> sut.retrieveInputData(new byte[2], Collections.singletonMap(CONTENT_TYPE_HEADER, CONTENT_TYPE_HEADER_VALUE), workingPath)
//        );
//
//        Assertions.assertEquals(AppErrorCodeEnum.PDFE_707, e.getErrorCode());
//    }
//
//    @Test
//    @SneakyThrows
//    void retrieveInputDataFailReadApplySignatureThrowsIOException() {
//        MultipartStream multipartStreamMock = mock(MultipartStream.class);
//
//        doReturn(multipartStreamMock).when(sut).getMultipartStream(any(), anyString());
//        doReturn(true).when(multipartStreamMock).skipPreamble();
//        doReturn(String.format(HEADER_TEMPLATE, "applySignature")).when(multipartStreamMock).readHeaders();
//        doThrow(IOException.class).when(multipartStreamMock).readBodyData(any());
//
//        RequestBodyParseException e = assertThrows(
//                RequestBodyParseException.class,
//                () -> sut.retrieveInputData(new byte[2], Collections.singletonMap(CONTENT_TYPE_HEADER, CONTENT_TYPE_HEADER_VALUE), workingPath)
//        );
//
//        Assertions.assertEquals(AppErrorCodeEnum.PDFE_708, e.getErrorCode());
//    }
//
//    @Test
//    @SneakyThrows
//    void retrieveInputDataFailInvalidFieldThrowsUnexpectedRequestBodyFieldException() {
//        MultipartStream multipartStreamMock = mock(MultipartStream.class);
//
//        doReturn(multipartStreamMock).when(sut).getMultipartStream(any(), anyString());
//        doReturn(true).when(multipartStreamMock).skipPreamble();
//        doReturn(String.format(HEADER_TEMPLATE, "invalidField")).when(multipartStreamMock).readHeaders();
//
//        UnexpectedRequestBodyFieldException e = assertThrows(
//                UnexpectedRequestBodyFieldException.class,
//                () -> sut.retrieveInputData(new byte[2], Collections.singletonMap(CONTENT_TYPE_HEADER, CONTENT_TYPE_HEADER_VALUE), workingPath)
//        );
//
//        Assertions.assertEquals(AppErrorCodeEnum.PDFE_896, e.getErrorCode());
//    }
//
//    @Test
//    @SneakyThrows
//    void retrieveInputDataFailReadBoundaryThrowsFileUploadIOException() {
//        MultipartStream multipartStreamMock = mock(MultipartStream.class);
//
//        doReturn(multipartStreamMock).when(sut).getMultipartStream(any(), anyString());
//        doReturn(true).when(multipartStreamMock).skipPreamble();
//        doReturn(String.format(HEADER_TEMPLATE, "template")).when(multipartStreamMock).readHeaders();
//        doThrow(FileUploadIOException.class).when(multipartStreamMock).readBoundary();
//
//        RequestBodyParseException e = assertThrows(
//                RequestBodyParseException.class,
//                () -> sut.retrieveInputData(new byte[2], Collections.singletonMap(CONTENT_TYPE_HEADER, CONTENT_TYPE_HEADER_VALUE), workingPath)
//        );
//
//        Assertions.assertEquals(AppErrorCodeEnum.PDFE_709, e.getErrorCode());
//    }
//
//    @Test
//    @SneakyThrows
//    void retrieveInputDataFailReadBoundaryThrowsMalformedStreamException() {
//        MultipartStream multipartStreamMock = mock(MultipartStream.class);
//
//        doReturn(multipartStreamMock).when(sut).getMultipartStream(any(), anyString());
//        doReturn(true).when(multipartStreamMock).skipPreamble();
//        doReturn(String.format(HEADER_TEMPLATE, "applySignature")).when(multipartStreamMock).readHeaders();
//        doThrow(MultipartStream.MalformedStreamException.class).when(multipartStreamMock).readBoundary();
//
//        RequestBodyParseException e = assertThrows(
//                RequestBodyParseException.class,
//                () -> sut.retrieveInputData(new byte[2], Collections.singletonMap(CONTENT_TYPE_HEADER, CONTENT_TYPE_HEADER_VALUE), workingPath)
//        );
//
//        Assertions.assertEquals(AppErrorCodeEnum.PDFE_710, e.getErrorCode());
//    }
}