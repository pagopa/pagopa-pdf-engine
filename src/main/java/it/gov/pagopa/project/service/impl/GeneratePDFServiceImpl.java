package it.gov.pagopa.project.service.impl;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.kernel.pdf.PdfAConformanceLevel;
import com.itextpdf.kernel.pdf.PdfOutputIntent;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.font.FontProvider;
import com.itextpdf.pdfa.PdfADocument;
import it.gov.pagopa.project.exception.CompileTemplateException;
import it.gov.pagopa.project.exception.FillTemplateException;
import it.gov.pagopa.project.exception.GeneratePDFException;
import it.gov.pagopa.project.model.GeneratePDFInput;
import it.gov.pagopa.project.service.GeneratePDFService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static it.gov.pagopa.project.model.AppErrorCodeEnum.*;

public class GeneratePDFServiceImpl implements GeneratePDFService {

    private final Handlebars handlebars;

    public GeneratePDFServiceImpl(Handlebars handlebars) {
        this.handlebars = handlebars;
    }

    @Override
    public ByteArrayOutputStream generatePDF(GeneratePDFInput generatePDFInput) throws CompileTemplateException, FillTemplateException, GeneratePDFException {
        Template template = getTemplate(generatePDFInput, handlebars);
        String filledTemplate = fillTemplate(generatePDFInput, template);

        try (
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                PdfWriter pdfWriter = new PdfWriter(os);
                PdfADocument pdf = getPdfADocument(pdfWriter)
        ) {
            pdf.setTagged();
            FontProvider fontProvider = new FontProvider();
            fontProvider.addSystemFonts();
            Document document = HtmlConverter.convertToDocument(filledTemplate, pdf, new ConverterProperties().setFontProvider(fontProvider));
            document.close();
            return os;
        } catch (IOException e) {
            throw new GeneratePDFException(PDFE_902, "An error occurred on generating the pdf", e);
        }
    }

    private String fillTemplate(GeneratePDFInput generatePDFInput, Template template) throws FillTemplateException {
        try {
            return template.apply(generatePDFInput.getData());
        } catch (IOException e) {
            throw new FillTemplateException(PDFE_900, "Error filling the template with the provided data", e);
        }
    }

    private Template getTemplate(GeneratePDFInput generatePDFInput, Handlebars handlebars) throws CompileTemplateException {
        try {
            return handlebars.compileInline(generatePDFInput.getTemplate());
        } catch (IOException e) {
            throw new CompileTemplateException(PDFE_901, "Error compiling the provided template", e);

        }
    }

    private PdfADocument getPdfADocument(PdfWriter pdfWriter) {
        return new PdfADocument(
                pdfWriter,
                PdfAConformanceLevel.PDF_A_2A,
                new PdfOutputIntent(
                        "Custom",
                        "",
                        "https://www.color.org",
                        "sRGB IEC61966-2.1",
                        this.getClass().getResourceAsStream("/sRGB_CS_profile.icm")
                ));
    }
}
