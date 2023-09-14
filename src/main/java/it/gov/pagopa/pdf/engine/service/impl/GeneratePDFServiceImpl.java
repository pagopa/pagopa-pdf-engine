
package it.gov.pagopa.pdf.engine.service.impl;

import com.spire.pdf.conversion.PdfStandardsConverter;
import it.gov.pagopa.pdf.engine.client.impl.PdfEngineClientImpl;
import it.gov.pagopa.pdf.engine.exception.GeneratePDFException;
import it.gov.pagopa.pdf.engine.model.AppErrorCodeEnum;
import it.gov.pagopa.pdf.engine.model.GeneratePDFInput;
import it.gov.pagopa.pdf.engine.model.PdfEngineRequest;
import it.gov.pagopa.pdf.engine.model.PdfEngineResponse;
import it.gov.pagopa.pdf.engine.service.GeneratePDFService;
import it.gov.pagopa.pdf.engine.util.ObjectMapperUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static it.gov.pagopa.pdf.engine.model.AppErrorCodeEnum.*;
import static it.gov.pagopa.pdf.engine.util.Constants.ZIP_FILE_NAME;

public class GeneratePDFServiceImpl implements GeneratePDFService {

    @Override
    public BufferedInputStream generatePDF(GeneratePDFInput generatePDFInput, Path workingDirPath)
            throws GeneratePDFException {

        File pdfTempFile = createTempFile("document", "pdf", workingDirPath, PDFE_903);

        try {

            PdfEngineClientImpl pdfEngineClient = PdfEngineClientImpl.getInstance();
            PdfEngineRequest pdfEngineRequest = new PdfEngineRequest();
            pdfEngineRequest.setWorkingDirPath(workingDirPath.toFile().getAbsolutePath());
            pdfEngineRequest.setData(ObjectMapperUtils.writeValueAsString(generatePDFInput.getData()));

            PdfEngineResponse response = pdfEngineClient.generatePDF(pdfEngineRequest);
            if (response.getStatusCode() != 200 || response.getTempPdfPath() == null) {
                throw new GeneratePDFException(AppErrorCodeEnum.valueOf(
                        response.getErrorCode()),response.getErrorMessage());
            }

            String fileToReturn = response.getTempPdfPath();
            PdfStandardsConverter converter = new PdfStandardsConverter(fileToReturn);
            converter.toPdfA2A(pdfTempFile.getParent() + "/ToPdfA2A.pdf");
            fileToReturn = pdfTempFile.getParent() + "/ToPdfA2A.pdf";

            if (generatePDFInput.isGenerateZipped()) {
                return zipPDFDocument(new File(fileToReturn), workingDirPath);
            }
            return new BufferedInputStream(new FileInputStream(fileToReturn));

        } catch (GeneratePDFException e) {
            throw e;
        } catch (IOException e) {
            throw new GeneratePDFException(PDFE_902, "An error occurred on generating the pdf", e);
        } catch (Exception e) {
            throw new RuntimeException(e);
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

    private File createTempFile(String fileName, String fileExtension, Path workingDirPath, AppErrorCodeEnum error)
            throws GeneratePDFException {
        try {
            return Files.createTempFile(workingDirPath, fileName, fileExtension).toFile();
        } catch (IOException e) {
            throw new GeneratePDFException(error, error.getErrorMessage(), e);
        }
    }
}
