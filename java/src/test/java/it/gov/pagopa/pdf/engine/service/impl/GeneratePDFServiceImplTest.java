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

import it.gov.pagopa.pdf.engine.exception.CompileTemplateException;
import it.gov.pagopa.pdf.engine.exception.GeneratePDFException;
import it.gov.pagopa.pdf.engine.model.AppErrorCodeEnum;
import it.gov.pagopa.pdf.engine.model.GeneratePDFInput;
import it.gov.pagopa.pdf.engine.service.GeneratePDFService;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GeneratePDFServiceImplTest {

    private GeneratePDFService sut;

    private Path workingPath;

    @BeforeEach
    void setUp() throws IOException, GeneratePDFException {

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


        BufferedInputStream output = sut.generatePDF(pdfInput, workingPath);

        assertNotNull(output);
        output.close();
    }

    @Test
    @SneakyThrows
    void generatePDFCompileTemplateException() {
        GeneratePDFInput pdfInput = new GeneratePDFInput();

        CompileTemplateException e = assertThrows(CompileTemplateException.class, () -> sut.generatePDF(pdfInput, workingPath));

        Assertions.assertEquals(AppErrorCodeEnum.PDFE_901, e.getErrorCode());
    }


}