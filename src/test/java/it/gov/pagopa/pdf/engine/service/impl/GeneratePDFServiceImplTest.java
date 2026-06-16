package it.gov.pagopa.pdf.engine.service.impl;

import it.gov.pagopa.pdf.engine.HttpTriggerGeneratePDFFunction;
import it.gov.pagopa.pdf.engine.client.impl.PdfEngineClientImpl;
import it.gov.pagopa.pdf.engine.exception.GeneratePDFException;
import it.gov.pagopa.pdf.engine.model.AppErrorCodeEnum;
import it.gov.pagopa.pdf.engine.model.GeneratePDFInput;
import it.gov.pagopa.pdf.engine.model.PdfEngineResponse;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeneratePDFServiceImplTest {

    @Mock
    private PdfEngineClientImpl pdfEngineClientMock;

    @InjectMocks
    private GeneratePDFServiceImpl sut;

    private Path workingPath;

    @BeforeEach
    void setUp() throws IOException {
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
        pdfEngineResponse.setTempPdfPath(validPdfResourcePath());

        when(pdfEngineClientMock.generatePDF(Mockito.any())).thenReturn(pdfEngineResponse);

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
        pdfEngineResponse.setTempPdfPath(validPdfResourcePath());

        when(pdfEngineClientMock.generatePDF(Mockito.any())).thenReturn(pdfEngineResponse);

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

        when(pdfEngineClientMock.generatePDF(Mockito.any())).thenReturn(pdfEngineResponse);

        Logger logger = LoggerFactory.getLogger(HttpTriggerGeneratePDFFunction.class);

        GeneratePDFException e = assertThrows(GeneratePDFException.class, () -> sut.generatePDF(pdfInput, workingPath, logger));

        Assertions.assertEquals(AppErrorCodeEnum.PDFE_902, e.getErrorCode());
    }

    private String validPdfResourcePath() throws java.net.URISyntaxException {
        return Paths.get(
                Objects.requireNonNull(
                        this.getClass().getClassLoader().getResource("valid_pdf.pdf")
                ).toURI()
        ).toFile().getAbsolutePath();
    }
}