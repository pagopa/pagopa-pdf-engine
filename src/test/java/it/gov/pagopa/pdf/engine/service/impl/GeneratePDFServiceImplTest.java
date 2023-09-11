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

package it.gov.pagopa.pdf.engine.service.impl;

import it.gov.pagopa.pdf.engine.client.impl.PdfEngineClientImpl;
import it.gov.pagopa.pdf.engine.exception.GeneratePDFException;
import it.gov.pagopa.pdf.engine.model.AppErrorCodeEnum;
import it.gov.pagopa.pdf.engine.model.GeneratePDFInput;
import it.gov.pagopa.pdf.engine.model.PdfEngineRequest;
import it.gov.pagopa.pdf.engine.model.PdfEngineResponse;
import it.gov.pagopa.pdf.engine.service.GeneratePDFService;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

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

class GeneratePDFServiceImplTest {

    private GeneratePDFService sut;

    private Path workingPath;

    @BeforeEach
    void setUp() throws IOException, GeneratePDFException {
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

        BufferedInputStream output = sut.generatePDF(pdfInput, workingPath);

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

        PdfEngineResponse pdfEngineResponse = new PdfEngineResponse();
        pdfEngineResponse.setStatusCode(200);
        pdfEngineResponse.setTempPdfPath(Objects.requireNonNull(this.getClass().getClassLoader()
                .getResource("valid_pdf.pdf")).toURI().normalize().getPath().replaceFirst("\\\\",""));

        PdfEngineClientImpl pdfEngineClient = mock(PdfEngineClientImpl.class);
        when(pdfEngineClient.generatePDF(Mockito.any())).thenReturn(pdfEngineResponse);
        GeneratePDFServiceImplTest.setMock(PdfEngineClientImpl.class, pdfEngineClient);

        BufferedInputStream output = sut.generatePDF(pdfInput, workingPath);

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

        GeneratePDFException e = assertThrows(GeneratePDFException.class, () -> sut.generatePDF(pdfInput, workingPath));

        Assertions.assertEquals(AppErrorCodeEnum.PDFE_902, e.getErrorCode());
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