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
import java.util.Map;

import static it.gov.pagopa.project.model.AppErrorCodeEnum.*;

public class GeneratePDFServiceImpl implements GeneratePDFService {

    private final String writeFileBasePath = System.getenv().getOrDefault("WRITE_FILE_BASE_PATH", "C:/tmp");
    private final String unzippedFilesFolder = System.getenv().getOrDefault("UNZIPPED_FILES_FOLDER", "/unzipped");
    private final String htmlTemplateFileName = System.getenv().getOrDefault("HTML_TEMPLATE_FILE_NAME", "template");

    private final Handlebars handlebars;

    public GeneratePDFServiceImpl(Handlebars handlebars) {
        this.handlebars = handlebars;
    }

    @Override
    public ByteArrayOutputStream generatePDF(GeneratePDFInput generatePDFInput) throws CompileTemplateException, FillTemplateException, GeneratePDFException {
        Template template = getTemplate();
        String filledTemplate = fillTemplate(generatePDFInput.getData(), template);

        try (
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                PdfWriter pdfWriter = new PdfWriter(os);
                PdfADocument pdf = getPdfADocument(pdfWriter)
        ) {
            pdf.setTagged();
            Document document = HtmlConverter.convertToDocument(filledTemplate, pdf, buildConverterProperties());
            document.close();
            return os;
        } catch (IOException e) {
            throw new GeneratePDFException(PDFE_902, "An error occurred on generating the pdf", e);
        }
    }

    private String fillTemplate(Map<String, Object> inputData, Template template) throws FillTemplateException {
        try {
            return template.apply(inputData);
        } catch (IOException e) {
            throw new FillTemplateException(PDFE_900, "Error filling the template with the provided data", e);
        }
    }

    private Template getTemplate() throws CompileTemplateException {
        try {
            return this.handlebars.compile(htmlTemplateFileName);
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

    private ConverterProperties buildConverterProperties() {
        FontProvider fontProvider = new FontProvider();
        fontProvider.addSystemFonts();
        return new ConverterProperties()
                .setBaseUri(writeFileBasePath + unzippedFilesFolder)
                .setFontProvider(fontProvider);
    }
}
