
package it.gov.pagopa.pdf.engine.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.pdf.engine.exception.RequestBodyParseException;
import it.gov.pagopa.pdf.engine.exception.UnexpectedRequestBodyFieldException;
import it.gov.pagopa.pdf.engine.model.AppErrorCodeEnum;
import it.gov.pagopa.pdf.engine.model.GeneratePDFInput;
import lombok.SneakyThrows;
import org.apache.commons.fileupload.FileUploadBase.FileUploadIOException;
import org.apache.commons.fileupload.MultipartStream;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ParseRequestBodyServiceImplTest {
    private static final String HEADER_TEMPLATE = "some string; fieldName=\"%s\"; other string";

    private static final String CONTENT_TYPE_HEADER_VALUE = "multipart/form-data";
    private static final String CONTENT_TYPE_HEADER = "content-type";

    private ObjectMapper objectMapperMock;
    private ParseRequestBodyServiceImpl sut;

    private Path workingPath;

    @BeforeEach
    void setUp() throws IOException {
        objectMapperMock = mock(ObjectMapper.class);
        sut = spy(new ParseRequestBodyServiceImpl(objectMapperMock));
        workingPath = Files.createTempDirectory("testDir");
    }

    @AfterEach
    void tearDown() throws IOException {
        FileUtils.deleteDirectory(workingPath.toFile());
    }

    @Test
    @SneakyThrows
    void retrieveInputDataSuccess() {
        MultipartStream multipartStreamMock = mock(MultipartStream.class);

        doReturn(multipartStreamMock).when(sut).getMultipartStream(any(), anyString());
        doReturn(true).when(multipartStreamMock).skipPreamble();
        doReturn(
                String.format(HEADER_TEMPLATE, "applySignature"),
                String.format(HEADER_TEMPLATE, "generateZipped"),
                String.format(HEADER_TEMPLATE, "title"),
                String.format(HEADER_TEMPLATE, "data")
        ).when(multipartStreamMock).readHeaders();
        doReturn(Collections.singletonMap("ke1", "value1")).when(objectMapperMock).readValue(anyString(), any(TypeReference.class));
        doReturn(true, true, true, false).when(multipartStreamMock).readBoundary();

        GeneratePDFInput result = sut.retrieveInputData(new byte[2], Collections.singletonMap(CONTENT_TYPE_HEADER, CONTENT_TYPE_HEADER_VALUE), workingPath);

        assertNotNull(result);
        assertNotNull(result.getData());
    }

    @Test
    @SneakyThrows
    void retrieveInputDataWithGeneratorTypeSuccess() {
        MultipartStream multipartStreamMock = mock(MultipartStream.class);

        doReturn(multipartStreamMock).when(sut).getMultipartStream(any(), anyString());
        doReturn(true).when(multipartStreamMock).skipPreamble();
        doReturn(
                String.format(HEADER_TEMPLATE, "applySignature"),
                String.format(HEADER_TEMPLATE, "generateZipped"),
                String.format(HEADER_TEMPLATE, "data"),
                String.format(HEADER_TEMPLATE, "generatorType")
        ).when(multipartStreamMock).readHeaders();
        doReturn(Collections.singletonMap("ke1", "value1")).when(objectMapperMock).readValue(anyString(), any(TypeReference.class));
        doReturn(true, true, false).when(multipartStreamMock).readBoundary();

        GeneratePDFInput result = sut.retrieveInputData(new byte[2], Collections.singletonMap(CONTENT_TYPE_HEADER, CONTENT_TYPE_HEADER_VALUE), workingPath);

        assertNotNull(result);
        assertNotNull(result.getData());
    }

    @Test
    @SneakyThrows
    void retrieveInputDataFailGetContentTypeHeaderIsNull() {
        RequestBodyParseException e = assertThrows(
                RequestBodyParseException.class,
                () -> sut.retrieveInputData(new byte[2], Collections.emptyMap(), workingPath)
        );

        Assertions.assertEquals(AppErrorCodeEnum.PDFE_711, e.getErrorCode());
    }

    @Test
    @SneakyThrows
    void retrieveInputDataFailGetContentTypeHeaderIsNotMultipart() {
        RequestBodyParseException e = assertThrows(
                RequestBodyParseException.class,
                () -> sut.retrieveInputData(new byte[2], Collections.singletonMap(CONTENT_TYPE_HEADER, "application/json"), workingPath)
        );

        Assertions.assertEquals(AppErrorCodeEnum.PDFE_712, e.getErrorCode());
    }

    @Test
    @SneakyThrows
    void retrieveInputDataFailSkipPreambleThrowsIOException() {
        MultipartStream multipartStreamMock = mock(MultipartStream.class);

        doReturn(multipartStreamMock).when(sut).getMultipartStream(any(), anyString());
        doThrow(IOException.class).when(multipartStreamMock).skipPreamble();

        RequestBodyParseException e = assertThrows(
                RequestBodyParseException.class,
                () -> sut.retrieveInputData(new byte[2], Collections.singletonMap(CONTENT_TYPE_HEADER, CONTENT_TYPE_HEADER_VALUE), workingPath)
        );

        Assertions.assertEquals(AppErrorCodeEnum.PDFE_700, e.getErrorCode());
    }

    @Test
    @SneakyThrows
    void retrieveInputDataFailReadHeaderThrowsFileUploadIOException() {
        MultipartStream multipartStreamMock = mock(MultipartStream.class);

        doReturn(multipartStreamMock).when(sut).getMultipartStream(any(), anyString());
        doReturn(true).when(multipartStreamMock).skipPreamble();
        doThrow(FileUploadIOException.class).when(multipartStreamMock).readHeaders();

        RequestBodyParseException e = assertThrows(
                RequestBodyParseException.class,
                () -> sut.retrieveInputData(new byte[2], Collections.singletonMap(CONTENT_TYPE_HEADER, CONTENT_TYPE_HEADER_VALUE), workingPath)
        );

        Assertions.assertEquals(AppErrorCodeEnum.PDFE_701, e.getErrorCode());
    }

    @Test
    @SneakyThrows
    void retrieveInputDataFailReadHeaderThrowsMalformedStreamException() {
        MultipartStream multipartStreamMock = mock(MultipartStream.class);

        doReturn(multipartStreamMock).when(sut).getMultipartStream(any(), anyString());
        doReturn(true).when(multipartStreamMock).skipPreamble();
        doThrow(MultipartStream.MalformedStreamException.class).when(multipartStreamMock).readHeaders();

        RequestBodyParseException e = assertThrows(
                RequestBodyParseException.class,
                () -> sut.retrieveInputData(new byte[2], Collections.singletonMap(CONTENT_TYPE_HEADER, CONTENT_TYPE_HEADER_VALUE), workingPath)
        );

        Assertions.assertEquals(AppErrorCodeEnum.PDFE_702, e.getErrorCode());
    }

    @Test
    @SneakyThrows
    void retrieveInputDataFailReadTemplateThrowsFileNotFoundException() {
        MultipartStream multipartStreamMock = mock(MultipartStream.class);

        doReturn(multipartStreamMock).when(sut).getMultipartStream(any(), anyString());
        doReturn(true).when(multipartStreamMock).skipPreamble();
        doReturn(String.format(HEADER_TEMPLATE, "template")).when(multipartStreamMock).readHeaders();
        doThrow(FileNotFoundException.class).when(multipartStreamMock).readBodyData(any());

        RequestBodyParseException e = assertThrows(
                RequestBodyParseException.class,
                () -> sut.retrieveInputData(new byte[2], Collections.singletonMap(CONTENT_TYPE_HEADER, CONTENT_TYPE_HEADER_VALUE), workingPath)
        );

        Assertions.assertEquals(AppErrorCodeEnum.PDFE_703, e.getErrorCode());
    }

    @Test
    @SneakyThrows
    void retrieveInputDataFailReadTemplateThrowsIOException() {
        MultipartStream multipartStreamMock = mock(MultipartStream.class);

        doReturn(multipartStreamMock).when(sut).getMultipartStream(any(), anyString());
        doReturn(true).when(multipartStreamMock).skipPreamble();
        doReturn(String.format(HEADER_TEMPLATE, "template")).when(multipartStreamMock).readHeaders();
        doThrow(IOException.class).when(multipartStreamMock).readBodyData(any());

        RequestBodyParseException e = assertThrows(
                RequestBodyParseException.class,
                () -> sut.retrieveInputData(new byte[2], Collections.singletonMap(CONTENT_TYPE_HEADER, CONTENT_TYPE_HEADER_VALUE), workingPath)
        );

        Assertions.assertEquals(AppErrorCodeEnum.PDFE_704, e.getErrorCode());
    }

    @Test
    @SneakyThrows
    void retrieveInputDataFailReadInputDataThrowsIOException() {
        MultipartStream multipartStreamMock = mock(MultipartStream.class);

        doReturn(multipartStreamMock).when(sut).getMultipartStream(any(), anyString());
        doReturn(true).when(multipartStreamMock).skipPreamble();
        doReturn(String.format(HEADER_TEMPLATE, "data")).when(multipartStreamMock).readHeaders();
        doThrow(IOException.class).when(multipartStreamMock).readBodyData(any());

        RequestBodyParseException e = assertThrows(
                RequestBodyParseException.class,
                () -> sut.retrieveInputData(new byte[2], Collections.singletonMap(CONTENT_TYPE_HEADER, CONTENT_TYPE_HEADER_VALUE), workingPath)
        );

        Assertions.assertEquals(AppErrorCodeEnum.PDFE_706, e.getErrorCode());
    }

    @Test
    @SneakyThrows
    void retrieveInputDataFailMapInputDataThrowsJsonProcessingException() {
        MultipartStream multipartStreamMock = mock(MultipartStream.class);

        doReturn(multipartStreamMock).when(sut).getMultipartStream(any(), anyString());
        doReturn(true).when(multipartStreamMock).skipPreamble();
        doReturn(String.format(HEADER_TEMPLATE, "data")).when(multipartStreamMock).readHeaders();
        doThrow(JsonProcessingException.class).when(objectMapperMock).readValue(anyString(), any(TypeReference.class));

        RequestBodyParseException e = assertThrows(
                RequestBodyParseException.class,
                () -> sut.retrieveInputData(new byte[2], Collections.singletonMap(CONTENT_TYPE_HEADER, CONTENT_TYPE_HEADER_VALUE), workingPath)
        );

        Assertions.assertEquals(AppErrorCodeEnum.PDFE_707, e.getErrorCode());
    }

    @Test
    @SneakyThrows
    void retrieveInputDataFailReadApplySignatureThrowsIOException() {
        MultipartStream multipartStreamMock = mock(MultipartStream.class);

        doReturn(multipartStreamMock).when(sut).getMultipartStream(any(), anyString());
        doReturn(true).when(multipartStreamMock).skipPreamble();
        doReturn(String.format(HEADER_TEMPLATE, "applySignature")).when(multipartStreamMock).readHeaders();
        doThrow(IOException.class).when(multipartStreamMock).readBodyData(any());

        RequestBodyParseException e = assertThrows(
                RequestBodyParseException.class,
                () -> sut.retrieveInputData(new byte[2], Collections.singletonMap(CONTENT_TYPE_HEADER, CONTENT_TYPE_HEADER_VALUE), workingPath)
        );

        Assertions.assertEquals(AppErrorCodeEnum.PDFE_708, e.getErrorCode());
    }

    @Test
    @SneakyThrows
    void retrieveInputDataFailInvalidFieldThrowsUnexpectedRequestBodyFieldException() {
        MultipartStream multipartStreamMock = mock(MultipartStream.class);

        doReturn(multipartStreamMock).when(sut).getMultipartStream(any(), anyString());
        doReturn(true).when(multipartStreamMock).skipPreamble();
        doReturn(String.format(HEADER_TEMPLATE, "invalidField")).when(multipartStreamMock).readHeaders();

        UnexpectedRequestBodyFieldException e = assertThrows(
                UnexpectedRequestBodyFieldException.class,
                () -> sut.retrieveInputData(new byte[2], Collections.singletonMap(CONTENT_TYPE_HEADER, CONTENT_TYPE_HEADER_VALUE), workingPath)
        );

        Assertions.assertEquals(AppErrorCodeEnum.PDFE_896, e.getErrorCode());
    }

    @Test
    @SneakyThrows
    void retrieveInputDataFailReadBoundaryThrowsFileUploadIOException() {
        MultipartStream multipartStreamMock = mock(MultipartStream.class);

        doReturn(multipartStreamMock).when(sut).getMultipartStream(any(), anyString());
        doReturn(true).when(multipartStreamMock).skipPreamble();
        doReturn(String.format(HEADER_TEMPLATE, "template")).when(multipartStreamMock).readHeaders();
        doThrow(FileUploadIOException.class).when(multipartStreamMock).readBoundary();

        RequestBodyParseException e = assertThrows(
                RequestBodyParseException.class,
                () -> sut.retrieveInputData(new byte[2], Collections.singletonMap(CONTENT_TYPE_HEADER, CONTENT_TYPE_HEADER_VALUE), workingPath)
        );

        Assertions.assertEquals(AppErrorCodeEnum.PDFE_709, e.getErrorCode());
    }

    @Test
    @SneakyThrows
    void retrieveInputDataFailReadBoundaryThrowsMalformedStreamException() {
        MultipartStream multipartStreamMock = mock(MultipartStream.class);

        doReturn(multipartStreamMock).when(sut).getMultipartStream(any(), anyString());
        doReturn(true).when(multipartStreamMock).skipPreamble();
        doReturn(String.format(HEADER_TEMPLATE, "applySignature")).when(multipartStreamMock).readHeaders();
        doThrow(MultipartStream.MalformedStreamException.class).when(multipartStreamMock).readBoundary();

        RequestBodyParseException e = assertThrows(
                RequestBodyParseException.class,
                () -> sut.retrieveInputData(new byte[2], Collections.singletonMap(CONTENT_TYPE_HEADER, CONTENT_TYPE_HEADER_VALUE), workingPath)
        );

        Assertions.assertEquals(AppErrorCodeEnum.PDFE_710, e.getErrorCode());
    }

    @Test
    @SneakyThrows
    void retrieveInputDataFailReadTitleThrowsIOException() {
        MultipartStream multipartStreamMock = mock(MultipartStream.class);

        doReturn(multipartStreamMock).when(sut).getMultipartStream(any(), anyString());
        doReturn(true).when(multipartStreamMock).skipPreamble();
        doReturn(String.format(HEADER_TEMPLATE, "title")).when(multipartStreamMock).readHeaders();
        doThrow(IOException.class).when(multipartStreamMock).readBodyData(any());

        RequestBodyParseException e = assertThrows(
                RequestBodyParseException.class,
                () -> sut.retrieveInputData(new byte[2], Collections.singletonMap(CONTENT_TYPE_HEADER, CONTENT_TYPE_HEADER_VALUE), workingPath)
        );

        Assertions.assertEquals(AppErrorCodeEnum.PDFE_715, e.getErrorCode());
    }

}