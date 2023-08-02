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
import com.github.jknack.handlebars.io.FileTemplateLoader;
import com.ironsoftware.ironpdf.License;
import com.ironsoftware.ironpdf.PdfDocument;
import com.ironsoftware.ironpdf.Settings;
import com.ironsoftware.ironpdf.render.ChromePdfRenderOptions;
import com.ironsoftware.ironpdf.render.PaperSize;
import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.kernel.pdf.PdfAConformanceLevel;
import com.itextpdf.kernel.pdf.PdfOutputIntent;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.font.FontProvider;
import com.itextpdf.pdfa.PdfADocument;
import it.gov.pagopa.pdf.engine.exception.CompileTemplateException;
import it.gov.pagopa.pdf.engine.exception.GeneratePDFException;
import it.gov.pagopa.pdf.engine.exception.FillTemplateException;
import it.gov.pagopa.pdf.engine.model.AppErrorCodeEnum;
import it.gov.pagopa.pdf.engine.model.GeneratePDFInput;
import it.gov.pagopa.pdf.engine.service.GeneratePDFService;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static it.gov.pagopa.pdf.engine.model.AppErrorCodeEnum.*;
import static it.gov.pagopa.pdf.engine.util.Constants.UNZIPPED_FILES_FOLDER;
import static it.gov.pagopa.pdf.engine.util.Constants.ZIP_FILE_NAME;

public class GeneratePDFServiceImpl implements GeneratePDFService {

    private final String htmlTemplateFileName = System.getenv().getOrDefault("HTML_TEMPLATE_FILE_NAME", "template");

    private final Handlebars handlebars;

    public GeneratePDFServiceImpl(Handlebars handlebars) {
        this.handlebars = handlebars;
    }

    @Override
    public BufferedInputStream generatePDF(GeneratePDFInput generatePDFInput, Path workingDirPath)
            throws CompileTemplateException, FillTemplateException, GeneratePDFException {
        handlebars.with(new FileTemplateLoader(workingDirPath + UNZIPPED_FILES_FOLDER, ".html"));

        Template template = getTemplate();
        String filledTemplate = fillTemplate(generatePDFInput.getData(), template);
        File pdfTempFile = createTempFile("document", "pdf", workingDirPath, PDFE_903);
        try {

            License.setLicenseKey("KEY");
            Settings.setLogPath(Paths.get("C:/tmp/IronPdfEngine.log"));
            ChromePdfRenderOptions renderOptions = new ChromePdfRenderOptions();
            renderOptions.setPaperSize(PaperSize.A4);
            renderOptions.setRenderDelay(500);

            PdfDocument myPdf = PdfDocument.renderZipAsPdf(Paths.get(workingDirPath + ZIP_FILE_NAME), "template.html", new ChromePdfRenderOptions());
            myPdf.saveAs(pdfTempFile.getPath());

//            pdf.setTagged();
//            Document document = HtmlConverter.convertToDocument(filledTemplate, pdf, buildConverterProperties(workingDirPath));
//            document.close();

            if (generatePDFInput.isGenerateZipped()) {
                return zipPDFDocument(pdfTempFile, workingDirPath);
            }
            return new BufferedInputStream(new FileInputStream(pdfTempFile));

        } catch (IOException e) {
            throw new GeneratePDFException(PDFE_902, "An error occurred on generating the pdf", e);
        }
    }

    private BufferedInputStream zipPDFDocument(File pdfTempFile, Path workingDirPath) throws GeneratePDFException {
        File zippedTempFile = createTempFile("zippedDocument", "zip", workingDirPath, PDFE_904);
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

    private ConverterProperties buildConverterProperties(Path workingDirPath) {
        FontProvider fontProvider = new FontProvider();
        fontProvider.addSystemFonts();
        return new ConverterProperties()
                .setBaseUri(workingDirPath + UNZIPPED_FILES_FOLDER)
                .setFontProvider(fontProvider);
    }

    private File createTempFile(String fileName, String fileExtension, Path workingDirPath, AppErrorCodeEnum error) throws GeneratePDFException {
        try {
            return Files.createTempFile(workingDirPath, fileName, fileExtension).toFile();
        } catch (IOException e) {
            throw new GeneratePDFException(error, error.getErrorMessage(), e);
        }
    }
}
