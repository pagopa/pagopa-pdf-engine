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

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.helper.ConditionalHelpers;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import it.gov.pagopa.pdf.engine.exception.CompileTemplateException;
import it.gov.pagopa.pdf.engine.exception.FillTemplateException;
import it.gov.pagopa.pdf.engine.exception.GeneratePDFException;
import it.gov.pagopa.pdf.engine.model.AppErrorCodeEnum;
import it.gov.pagopa.pdf.engine.model.GeneratePDFInput;
import it.gov.pagopa.pdf.engine.model.GeneratorType;
import it.gov.pagopa.pdf.engine.service.GeneratePDFService;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GeneratePDFServiceImplTest {

    private Handlebars handlebarsMock;

    private BrowserContext browserContextMock;

    private Page pageMock;

    private GeneratePDFService sut;

    private Path workingPath;

    @BeforeEach
    void setUp() throws IOException, GeneratePDFException {

        handlebarsMock = spy(buildHandlebars());
        browserContextMock = mock(BrowserContext.class);
        pageMock = mock(Page.class);

        sut = spy(new GeneratePDFServiceImpl(handlebarsMock));
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

        Template template = handlebarsMock.compileInline(
                IOUtils.toString(
                        Objects.requireNonNull(this.getClass().getResourceAsStream("/valid_template.html")),
                        StandardCharsets.UTF_8));

        doReturn(template).when(handlebarsMock).compile(anyString());

        BufferedInputStream output = sut.generatePDF(pdfInput, workingPath);

        assertNotNull(output);
        output.close();
    }

    @Test
    @SneakyThrows
    void generatePDFNotZippedWithSuccessWithPlaywright() {
        GeneratePDFInput pdfInput = new GeneratePDFInput();
        pdfInput.setData(Collections.singletonMap("a", "b"));
        pdfInput.setApplySignature(false);
        pdfInput.setGeneratorType(GeneratorType.PLAYWRIGHT);

        Template template = handlebarsMock.compileInline(
                IOUtils.toString(
                        Objects.requireNonNull(this.getClass().getResourceAsStream("/valid_template.html")),
                        StandardCharsets.UTF_8));

        doReturn(template).when(handlebarsMock).compile(anyString());
        when(browserContextMock.newPage()).thenReturn(pageMock);
        when(pageMock.pdf(any())).thenAnswer(invocation -> {
            File[] files = workingPath.toFile().listFiles();
            for (File file : files) {
                if (file.getName().contains("document")) {
                    Path targetPath = file.toPath();
                    Path sourcePath = new File(this.getClass().getResource("/valid_pdf.pdf").getPath()).toPath();
                    try  {
                        FileUtils.copyFile(sourcePath.toFile(), targetPath.toFile());
                    } finally {}
                }
            }
            return "".getBytes();
        });

        BufferedInputStream output = sut.generatePDF(pdfInput, workingPath);

        workingPath.toAbsolutePath();

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

        Template template = handlebarsMock.compileInline(
                IOUtils.toString(
                        Objects.requireNonNull(this.getClass().getResourceAsStream("/valid_template.html")),
                        StandardCharsets.UTF_8));

        doReturn(template).when(handlebarsMock).compile(anyString());

        BufferedInputStream output = sut.generatePDF(pdfInput, workingPath);

        assertNotNull(output);
        output.close();
    }

    @Test
    @SneakyThrows
    void generatePDFCompileTemplateException() {
        GeneratePDFInput pdfInput = new GeneratePDFInput();

        doThrow(IOException.class).when(handlebarsMock).compile(anyString());

        CompileTemplateException e = assertThrows(CompileTemplateException.class, () -> sut.generatePDF(pdfInput, workingPath));

        Assertions.assertEquals(AppErrorCodeEnum.PDFE_901, e.getErrorCode());
    }

    @Test
    @SneakyThrows
    void generatePDFFillTemplateException() {
        Template templateMock = mock(Template.class);

        GeneratePDFInput pdfInput = new GeneratePDFInput();
        pdfInput.setData(Collections.EMPTY_MAP);

        doReturn(templateMock).when(handlebarsMock).compile(anyString());
        doThrow(IOException.class).when(templateMock).apply(anyMap());

        FillTemplateException e = assertThrows(FillTemplateException.class, () -> sut.generatePDF(pdfInput, workingPath));

        Assertions.assertEquals(AppErrorCodeEnum.PDFE_900, e.getErrorCode());
    }

    private Handlebars buildHandlebars() {
        return new Handlebars()
                .registerHelper("eq", ConditionalHelpers.eq)
                .registerHelper("not", ConditionalHelpers.not);
    }
}