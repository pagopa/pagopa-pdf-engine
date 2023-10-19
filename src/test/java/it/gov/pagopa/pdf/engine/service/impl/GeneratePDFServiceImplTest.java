
package it.gov.pagopa.pdf.engine.service.impl;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.smallrye.mutiny.Uni;
import it.gov.pagopa.pdf.engine.client.PdfEngineClient;
import it.gov.pagopa.pdf.engine.exception.GeneratePDFException;
import it.gov.pagopa.pdf.engine.model.AppErrorCodeEnum;
import it.gov.pagopa.pdf.engine.model.GeneratePDFInput;
import it.gov.pagopa.pdf.engine.model.PdfEngineResponse;
import it.gov.pagopa.pdf.engine.service.GeneratePDFService;
import jakarta.inject.Inject;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.spy;

@QuarkusTest
class GeneratePDFServiceImplTest {


    @InjectMock
    @RestClient
    private PdfEngineClient pdfEngineClient;

    @Inject
    private GeneratePDFService sut;

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
        pdfInput.setData("{\"a\"=\"b\"}");
        pdfInput.setApplySignature(false);

        try (InputStream input = Objects.requireNonNull(this.getClass().getClassLoader()
                .getResource("valid_pdf.pdf")).openStream()) {

            when(pdfEngineClient.generatePDF(Mockito.any())).thenReturn(Uni.createFrom().item(input));

            Logger logger = LoggerFactory.getLogger(GeneratePDFService.class);

            Uni<PdfEngineResponse> uni = sut.generatePDF(pdfInput, workingPath, logger);

            assertNotNull(uni);
            assertNotNull(uni.await().indefinitely());

        }

    }
    @Test
    @SneakyThrows
    void generatePDFCallException() {
        GeneratePDFInput pdfInput = new GeneratePDFInput();

        when(pdfEngineClient.generatePDF(Mockito.any())).thenThrow(new GeneratePDFException(AppErrorCodeEnum.PDFE_902,"Error"));

        Logger logger = LoggerFactory.getLogger(GeneratePDFInput.class);

        GeneratePDFException e = assertThrows(GeneratePDFException.class,
                () -> sut.generatePDF(pdfInput, workingPath, logger));

        Assertions.assertEquals(AppErrorCodeEnum.PDFE_902, e.getErrorCode());
    }


    @Test
    @SneakyThrows
    void generatePDFZippedWithSuccess() {
        GeneratePDFInput pdfInput = new GeneratePDFInput();
        pdfInput.setData("{\"a\"=\"b\"}");
        pdfInput.setApplySignature(false);
        pdfInput.setGenerateZipped(true);


        try (InputStream input = Objects.requireNonNull(this.getClass().getClassLoader()
                .getResource("valid_pdf.pdf")).openStream()) {

            when(pdfEngineClient.generatePDF(Mockito.any())).thenReturn(Uni.createFrom().item(input));

            Logger logger = LoggerFactory.getLogger(GeneratePDFService.class);

            Uni<PdfEngineResponse> uni = sut.generatePDF(pdfInput, workingPath, logger);

            assertNotNull(uni);
            assertNotNull(uni.await().indefinitely());

        }

    }
//
//    @Test
//    @SneakyThrows
//    void generatePDFCallException() {
//        GeneratePDFInput pdfInput = new GeneratePDFInput();
//
//        PdfEngineResponse pdfEngineResponse = new PdfEngineResponse();
//        pdfEngineResponse.setStatusCode(400);
//        pdfEngineResponse.setErrorCode(AppErrorCodeEnum.PDFE_902.getErrorCode());
//
//        when(pdfEngineClient.generatePDF(Mockito.any())).thenReturn(pdfEngineResponse);
//
//        Logger logger = LoggerFactory.getLogger(GeneratePDFInput.class);
//
//        GeneratePDFException e = assertThrows(GeneratePDFException.class, () -> sut.generatePDF(pdfInput, workingPath, logger));
//
//        Assertions.assertEquals(AppErrorCodeEnum.PDFE_902, e.getErrorCode());
//    }

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