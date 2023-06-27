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
import it.gov.pagopa.project.model.AppErrorCodeEnum;
import it.gov.pagopa.project.model.GeneratePDFInput;
import it.gov.pagopa.project.service.GeneratePDFService;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.file.Files;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static it.gov.pagopa.project.model.AppErrorCodeEnum.*;

public class GeneratePDFServiceImpl implements GeneratePDFService {

    public static final String WORKING_DIR = "/workingDir";

    private final String writeFileBasePath = System.getenv().getOrDefault("WRITE_FILE_BASE_PATH", "/tmp");
    private final String unzippedFilesFolder = System.getenv().getOrDefault("UNZIPPED_FILES_FOLDER", "/unzipped");
    private final String htmlTemplateFileName = System.getenv().getOrDefault("HTML_TEMPLATE_FILE_NAME", "template");

    private final Handlebars handlebars;

    public GeneratePDFServiceImpl(Handlebars handlebars) {
        this.handlebars = handlebars;
    }

    @Override
    public BufferedInputStream generatePDF(GeneratePDFInput generatePDFInput)
            throws CompileTemplateException, FillTemplateException, GeneratePDFException {
        Template template = getTemplate();
        String filledTemplate = fillTemplate(generatePDFInput.getData(), template);
        File pdfTempFile = createTempFile("document", "pdf", PDFE_903);
        try (
                FileOutputStream os = new FileOutputStream(pdfTempFile);
                PdfWriter pdfWriter = new PdfWriter(os);
                PdfADocument pdf = getPdfADocument(pdfWriter)
        ) {
            pdf.setTagged();
            Document document = HtmlConverter.convertToDocument(filledTemplate, pdf, buildConverterProperties());
            document.close();

            if (generatePDFInput.isGenerateZipped()) {
                return zipPDFDocument(pdfTempFile);
            }
            return new BufferedInputStream(new FileInputStream(pdfTempFile));

        } catch (IOException e) {
            throw new GeneratePDFException(PDFE_902, "An error occurred on generating the pdf", e);
        }
    }

    private BufferedInputStream zipPDFDocument(File pdfTempFile) throws GeneratePDFException {
        File zippedTempFile = createTempFile("zippedDocument", "zip", PDFE_904);
        try (
                ZipOutputStream zipOutputStream = new ZipOutputStream(
                        new BufferedOutputStream(
                                new FileOutputStream(zippedTempFile)))
        ) {
            ZipEntry zipEntry = new ZipEntry("document.pdf");
            zipOutputStream.putNextEntry(zipEntry);

            try (BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(pdfTempFile))) {
                IOUtils.copy(bufferedInputStream, zipOutputStream);
                return new BufferedInputStream(new FileInputStream(zippedTempFile));
            }
        } catch (FileNotFoundException e) {
            throw new GeneratePDFException(PDFE_905, "An error occurred when zipping the PDF document", e);
        } catch (IOException e) {
            throw new GeneratePDFException(PDFE_906, "An error occurred when zipping the PDF document", e);
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

    private File createTempFile(String fileName, String fileExtension, AppErrorCodeEnum error) throws GeneratePDFException {
        File directory = new File(writeFileBasePath + WORKING_DIR);
        try {
            if (!directory.exists()) {
                Files.createDirectory(directory.toPath());
            }
            return File.createTempFile(fileName, fileExtension, directory);
        } catch (IOException e) {
            throw new GeneratePDFException(error, error.getErrorMessage(), e);
        }
    }
}
