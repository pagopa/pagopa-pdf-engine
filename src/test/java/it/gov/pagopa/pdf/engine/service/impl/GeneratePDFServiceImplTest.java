
package it.gov.pagopa.pdf.engine.service.impl;

import it.gov.pagopa.pdf.engine.HttpTriggerGeneratePDFFunction;
import it.gov.pagopa.pdf.engine.client.impl.PdfEngineClientImpl;
import it.gov.pagopa.pdf.engine.exception.GeneratePDFException;
import it.gov.pagopa.pdf.engine.model.AppErrorCodeEnum;
import it.gov.pagopa.pdf.engine.model.GeneratePDFInput;
import it.gov.pagopa.pdf.engine.model.PdfEngineResponse;
import it.gov.pagopa.pdf.engine.service.GeneratePDFService;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import org.mockito.MockedConstruction;

class GeneratePDFServiceImplTest {

    private GeneratePDFService sut;

    private Path workingPath;

    @BeforeEach
    void setUp() throws IOException {
        sut = spy(new GeneratePDFServiceImpl());
        workingPath = Files.createTempDirectory("testDir");
    }

    @AfterEach
    void tearDown() throws IOException {
        FileUtils.deleteDirectory(workingPath.toFile());
    }

    @Test
    @SneakyThrows
    void generatePDFNotZippedWithSuccess() {
        GeneratePDFInput pdfInput = new GeneratePDFInput();
        pdfInput.setData(Collections.singletonMap("a", "b"));
        pdfInput.setApplySignature(false);

        PdfEngineResponse pdfEngineResponse = new PdfEngineResponse();
        pdfEngineResponse.setStatusCode(200);
        pdfEngineResponse.setTempPdfPath(Objects.requireNonNull(this.getClass().getClassLoader()
                .getResource("valid_pdf.pdf")).getPath());

        PdfEngineClientImpl pdfEngineClient = mock(PdfEngineClientImpl.class);
        when(pdfEngineClient.generatePDF(Mockito.any())).thenReturn(pdfEngineResponse);
        GeneratePDFServiceImplTest.setMock(PdfEngineClientImpl.class, pdfEngineClient);

        Logger logger = LoggerFactory.getLogger(HttpTriggerGeneratePDFFunction.class);

        BufferedInputStream output = sut.generatePDF(pdfInput, workingPath, logger);

        assertNotNull(output);
        output.close();
    }


    @Test
    @SneakyThrows
    void generatePDFZippedWithSuccess() {
        GeneratePDFInput pdfInput = new GeneratePDFInput();
        pdfInput.setData(Collections.singletonMap("a", "b"));
        pdfInput.setApplySignature(false);
        pdfInput.setGenerateZipped(true);
        pdfInput.setTitle("title");

        PdfEngineResponse pdfEngineResponse = new PdfEngineResponse();
        pdfEngineResponse.setStatusCode(200);
        pdfEngineResponse.setTempPdfPath(Objects.requireNonNull(this.getClass().getClassLoader()
                .getResource("valid_pdf.pdf")).toURI().normalize().getPath().replaceFirst("\\\\",""));

        PdfEngineClientImpl pdfEngineClient = mock(PdfEngineClientImpl.class);
        when(pdfEngineClient.generatePDF(Mockito.any())).thenReturn(pdfEngineResponse);
        GeneratePDFServiceImplTest.setMock(PdfEngineClientImpl.class, pdfEngineClient);

        Logger logger = LoggerFactory.getLogger(HttpTriggerGeneratePDFFunction.class);

        BufferedInputStream output = sut.generatePDF(pdfInput, workingPath, logger);

        assertNotNull(output);
        output.close();
    }

    @Test
    @SneakyThrows
    void generatePDFCallException() {
        GeneratePDFInput pdfInput = new GeneratePDFInput();

        PdfEngineResponse pdfEngineResponse = new PdfEngineResponse();
        pdfEngineResponse.setStatusCode(400);
        pdfEngineResponse.setErrorCode(AppErrorCodeEnum.PDFE_902.getErrorCode());

        PdfEngineClientImpl pdfEngineClient = mock(PdfEngineClientImpl.class);
        when(pdfEngineClient.generatePDF(Mockito.any())).thenReturn(pdfEngineResponse);
        GeneratePDFServiceImplTest.setMock(PdfEngineClientImpl.class, pdfEngineClient);

        Logger logger = LoggerFactory.getLogger(HttpTriggerGeneratePDFFunction.class);

        GeneratePDFException e = assertThrows(GeneratePDFException.class, () -> sut.generatePDF(pdfInput, workingPath, logger));

        Assertions.assertEquals(AppErrorCodeEnum.PDFE_902, e.getErrorCode());
    }

    @Test
    @SneakyThrows
    void generatePDFArrayIndexOutOfBoundsExceptionFallbackToPdfA1b() {
        GeneratePDFInput pdfInput = new GeneratePDFInput();
        pdfInput.setData(Collections.singletonMap("a", "b"));
        pdfInput.setApplySignature(false);

        PdfEngineResponse pdfEngineResponse = new PdfEngineResponse();
        pdfEngineResponse.setStatusCode(200);
        String originalPdfPath = Objects.requireNonNull(this.getClass().getClassLoader()
                .getResource("valid_pdf.pdf")).getPath();
        pdfEngineResponse.setTempPdfPath(originalPdfPath);

        PdfEngineClientImpl pdfEngineClient = mock(PdfEngineClientImpl.class);
        when(pdfEngineClient.generatePDF(Mockito.any())).thenReturn(pdfEngineResponse);
        GeneratePDFServiceImplTest.setMock(PdfEngineClientImpl.class, pdfEngineClient);

        Logger logger = LoggerFactory.getLogger(HttpTriggerGeneratePDFFunction.class);

        try (MockedConstruction<com.spire.pdf.conversion.PdfStandardsConverter> mockedConstruction =
                mockConstruction(com.spire.pdf.conversion.PdfStandardsConverter.class,
                (mock, context) -> {
                    doThrow(new ArrayIndexOutOfBoundsException("Index 76253 out of bounds."))
                            .when(mock).toPdfA2A(Mockito.anyString());
                    doAnswer(invocation -> {
                        String pdfA1bPath = invocation.getArgument(0);
                        Files.copy(new java.io.File(originalPdfPath).toPath(),
                                new java.io.File(pdfA1bPath).toPath());
                        return null;
                    }).when(mock).toPdfA1B(Mockito.anyString());
                })) {

            BufferedInputStream output = sut.generatePDF(pdfInput, workingPath, logger);

            assertNotNull(output);
            output.close();
        }
    }

    @Test
    @SneakyThrows
    void generatePDFGenericExceptionFallbackToPdfA1b() {
        GeneratePDFInput pdfInput = new GeneratePDFInput();
        pdfInput.setData(Collections.singletonMap("a", "b"));
        pdfInput.setApplySignature(false);

        PdfEngineResponse pdfEngineResponse = new PdfEngineResponse();
        pdfEngineResponse.setStatusCode(200);
        String originalPdfPath = Objects.requireNonNull(this.getClass().getClassLoader()
                .getResource("valid_pdf.pdf")).getPath();
        pdfEngineResponse.setTempPdfPath(originalPdfPath);

        PdfEngineClientImpl pdfEngineClient = mock(PdfEngineClientImpl.class);
        when(pdfEngineClient.generatePDF(Mockito.any())).thenReturn(pdfEngineResponse);
        GeneratePDFServiceImplTest.setMock(PdfEngineClientImpl.class, pdfEngineClient);

        Logger logger = LoggerFactory.getLogger(HttpTriggerGeneratePDFFunction.class);

        try (MockedConstruction<com.spire.pdf.conversion.PdfStandardsConverter> mockedConstruction =
                mockConstruction(com.spire.pdf.conversion.PdfStandardsConverter.class,
                (mock, context) -> {
                    doThrow(new RuntimeException("Conversion error"))
                            .when(mock).toPdfA2A(Mockito.anyString());
                    doAnswer(invocation -> {
                        String pdfA1bPath = invocation.getArgument(0);
                        Files.copy(new java.io.File(originalPdfPath).toPath(),
                                new java.io.File(pdfA1bPath).toPath());
                        return null;
                    }).when(mock).toPdfA1B(Mockito.anyString());
                })) {

            BufferedInputStream output = sut.generatePDF(pdfInput, workingPath, logger);

            assertNotNull(output);
            output.close();
        }
    }

    @Test
    @SneakyThrows
    void generatePDFBothConversionsFailReturnsOriginalPdf() {
        GeneratePDFInput pdfInput = new GeneratePDFInput();
        pdfInput.setData(Collections.singletonMap("a", "b"));
        pdfInput.setApplySignature(false);

        PdfEngineResponse pdfEngineResponse = new PdfEngineResponse();
        pdfEngineResponse.setStatusCode(200);
        pdfEngineResponse.setTempPdfPath(Objects.requireNonNull(this.getClass().getClassLoader()
                .getResource("valid_pdf.pdf")).getPath());

        PdfEngineClientImpl pdfEngineClient = mock(PdfEngineClientImpl.class);
        when(pdfEngineClient.generatePDF(Mockito.any())).thenReturn(pdfEngineResponse);
        GeneratePDFServiceImplTest.setMock(PdfEngineClientImpl.class, pdfEngineClient);

        Logger logger = LoggerFactory.getLogger(HttpTriggerGeneratePDFFunction.class);

        try (MockedConstruction<com.spire.pdf.conversion.PdfStandardsConverter> mockedConstruction =
                mockConstruction(com.spire.pdf.conversion.PdfStandardsConverter.class,
                (mock, context) -> {
                    doThrow(new RuntimeException("PDF/A-2a conversion failed"))
                            .when(mock).toPdfA2A(Mockito.anyString());
                    doThrow(new RuntimeException("PDF/A-1b conversion failed"))
                            .when(mock).toPdfA1B(Mockito.anyString());
                })) {

            BufferedInputStream output = sut.generatePDF(pdfInput, workingPath, logger);

            assertNotNull(output);
            output.close();
        }
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