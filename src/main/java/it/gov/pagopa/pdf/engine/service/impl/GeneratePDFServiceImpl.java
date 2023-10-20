
package it.gov.pagopa.pdf.engine.service.impl;

import com.spire.pdf.conversion.PdfStandardsConverter;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import it.gov.pagopa.pdf.engine.client.PdfEngineClient;
import it.gov.pagopa.pdf.engine.exception.GeneratePDFException;
import it.gov.pagopa.pdf.engine.model.AppErrorCodeEnum;
import it.gov.pagopa.pdf.engine.model.GeneratePDFInput;
import it.gov.pagopa.pdf.engine.model.PdfEngineRequest;
import it.gov.pagopa.pdf.engine.model.PdfEngineResponse;
import it.gov.pagopa.pdf.engine.service.GeneratePDFService;
import it.gov.pagopa.pdf.engine.util.ObjectMapperUtils;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.commons.io.IOUtils;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static it.gov.pagopa.pdf.engine.model.AppErrorCodeEnum.*;

@ApplicationScoped
public class GeneratePDFServiceImpl implements GeneratePDFService {

    @RestClient
    PdfEngineClient pdfEngineClient;

    @Override
    public Uni<PdfEngineResponse> generatePDF(GeneratePDFInput generatePDFInput, Path workingDirPath, Logger logger)
            throws GeneratePDFException {

        PdfEngineRequest pdfEngineRequest = new PdfEngineRequest();
        pdfEngineRequest.setData(ObjectMapperUtils.writeValueAsString(generatePDFInput.getData()));
        pdfEngineRequest.setTemplate(generatePDFInput.getTemplateZip());

        logger.info("PdfEngineClient called at {}", LocalDateTime.now());
        return pdfEngineClient.generatePDF(pdfEngineRequest)
                .onFailure().invoke(throwable -> {
                    throw new GeneratePDFException(PDFE_902, String.format("Exception thrown during pdf generation process: %s", throwable));
                })
                .onItem().transformToUni(inputStream -> {
                    final AtomicReference<String>[] fileToReturn =
                            new AtomicReference[]{new AtomicReference<String>()};
            try {
                File targetFile = File.createTempFile("tempFile", ".pdf", workingDirPath.toFile());
                return Uni.createFrom().completionStage(Vertx.vertx().fileSystem().writeFile(
                        targetFile.getAbsolutePath(), Buffer.buffer(inputStream)).compose(
                        unused -> {
                        try {
                            fileToReturn[0].set(targetFile.getAbsolutePath());
                            logger.debug("Starting pdf conversion at {}", LocalDateTime.now());
                            PdfStandardsConverter converter = new PdfStandardsConverter(fileToReturn[0].get());
                            converter.toPdfA2A(targetFile.getParent() + "/ToPdfA2A.pdf");
                            fileToReturn[0].set(targetFile.getParent() + "/ToPdfA2A.pdf");
                            logger.debug("Completed pdf conversion at {}", LocalDateTime.now());

                            PdfEngineResponse pdfEngineResponse = new PdfEngineResponse();
                            pdfEngineResponse.setWorkDirPath(workingDirPath);

                            pdfEngineResponse.setBufferedInputStream(generatePDFInput.isGenerateZipped() ?
                                    zipPDFDocument(new File(fileToReturn[0].get()), workingDirPath) :
                                    new BufferedInputStream(new FileInputStream(fileToReturn[0].get())));
                            return Future.succeededFuture(pdfEngineResponse);
                        } catch (IOException e) {
                            throw new GeneratePDFException(PDFE_904,
                                    PDFE_904.getErrorMessage(), e);
                        }
                }).toCompletionStage());
            } catch (IOException e) {
                throw new GeneratePDFException(AppErrorCodeEnum.PDFE_904, AppErrorCodeEnum.PDFE_904.getErrorMessage(), e);
            }
        });

    }

    private BufferedInputStream zipPDFDocument(File pdfTempFile, Path workingDirPath) throws GeneratePDFException {
        File zippedTempFile = createTempFile(workingDirPath);
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

    private File createTempFile(Path workingDirPath)
            throws GeneratePDFException {
        try {
            return Files.createTempFile(workingDirPath, "zippedDocument", "zip").toFile();
        } catch (IOException e) {
            throw new GeneratePDFException(AppErrorCodeEnum.PDFE_904, AppErrorCodeEnum.PDFE_904.getErrorMessage(), e);
        }
    }
}
