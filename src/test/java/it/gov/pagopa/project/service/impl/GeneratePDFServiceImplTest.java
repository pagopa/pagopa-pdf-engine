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
        pdfInput.setTemplate(
                IOUtils.toString(
                        Objects.requireNonNull(this.getClass().getResourceAsStream("/valid_template.html")),
                        StandardCharsets.UTF_8)
        );
        pdfInput.setData(Collections.singletonMap("a", "b"));
        pdfInput.setApplySignature(false);

        ByteArrayOutputStream output = sut.generatePDF(pdfInput);

        assertNotNull(output);
    }

    @Test
    @SneakyThrows
    void generatePDFCompileTemplateException() {
        GeneratePDFInput pdfInput = new GeneratePDFInput();
        pdfInput.setTemplate("template");

        doThrow(IOException.class).when(handlebarsMock).compileInline(anyString());

        CompileTemplateException e = assertThrows(CompileTemplateException.class, () -> sut.generatePDF(pdfInput));

        assertEquals(AppErrorCodeEnum.PDFE_901, e.getErrorCode());
    }

    @Test
    @SneakyThrows
    void generatePDFFillTemplateException() {
        GeneratePDFInput pdfInput = new GeneratePDFInput();
        pdfInput.setTemplate("template");
        pdfInput.setData(Collections.EMPTY_MAP);

        Template templateMock = mock(Template.class);

        doReturn(templateMock).when(handlebarsMock).compileInline(anyString());
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