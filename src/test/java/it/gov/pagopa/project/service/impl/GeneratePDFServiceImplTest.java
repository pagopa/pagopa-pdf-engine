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

package it.gov.pagopa.project.service.impl;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.helper.ConditionalHelpers;
import it.gov.pagopa.project.exception.CompileTemplateException;
import it.gov.pagopa.project.exception.FillTemplateException;
import it.gov.pagopa.project.model.AppErrorCodeEnum;
import it.gov.pagopa.project.model.GeneratePDFInput;
import it.gov.pagopa.project.service.GeneratePDFService;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GeneratePDFServiceImplTest {

    private Handlebars handlebarsMock;

    private GeneratePDFService sut;

    @BeforeEach
    void setUp() {

        handlebarsMock = spy(buildHandlebars());
        sut = spy(new GeneratePDFServiceImpl(handlebarsMock));
    }

    @Test
    @SneakyThrows
    void generatePDFWithSuccess() {
        GeneratePDFInput pdfInput = new GeneratePDFInput();
        pdfInput.setData(Collections.singletonMap("a", "b"));
        pdfInput.setApplySignature(false);

        Template template = handlebarsMock.compileInline(
                IOUtils.toString(
                        Objects.requireNonNull(this.getClass().getResourceAsStream("/valid_template.html")),
                        StandardCharsets.UTF_8));

        doReturn(template).when(handlebarsMock).compile(anyString());

        ByteArrayOutputStream output = sut.generatePDF(pdfInput);

        assertNotNull(output);
    }

    @Test
    @SneakyThrows
    void generatePDFCompileTemplateException() {
        GeneratePDFInput pdfInput = new GeneratePDFInput();

        doThrow(IOException.class).when(handlebarsMock).compile(anyString());

        CompileTemplateException e = assertThrows(CompileTemplateException.class, () -> sut.generatePDF(pdfInput));

        assertEquals(AppErrorCodeEnum.PDFE_901, e.getErrorCode());
    }

    @Test
    @SneakyThrows
    void generatePDFFillTemplateException() {
        Template templateMock = mock(Template.class);

        GeneratePDFInput pdfInput = new GeneratePDFInput();
        pdfInput.setData(Collections.EMPTY_MAP);

        doReturn(templateMock).when(handlebarsMock).compile(anyString());
        doThrow(IOException.class).when(templateMock).apply(anyMap());

        FillTemplateException e = assertThrows(FillTemplateException.class, () -> sut.generatePDF(pdfInput));

        assertEquals(AppErrorCodeEnum.PDFE_900, e.getErrorCode());
    }

    private Handlebars buildHandlebars() {
        return new Handlebars()
                .registerHelper("eq", ConditionalHelpers.eq)
                .registerHelper("not", ConditionalHelpers.not);
    }
}