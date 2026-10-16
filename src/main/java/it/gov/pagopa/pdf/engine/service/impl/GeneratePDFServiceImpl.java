
package it.gov.pagopa.pdf.engine.service.impl;

import com.spire.pdf.conversion.PdfStandardsConverter;

import it.gov.pagopa.pdf.engine.client.PdfEngineClient;
import it.gov.pagopa.pdf.engine.client.impl.PdfEngineClientImpl;
import it.gov.pagopa.pdf.engine.exception.GeneratePDFException;
import it.gov.pagopa.pdf.engine.model.AppErrorCodeEnum;
import it.gov.pagopa.pdf.engine.model.GeneratePDFInput;
import it.gov.pagopa.pdf.engine.model.PdfEngineRequest;
import it.gov.pagopa.pdf.engine.model.PdfEngineResponse;
import it.gov.pagopa.pdf.engine.service.GeneratePDFService;
import it.gov.pagopa.pdf.engine.util.ObjectMapperUtils;
import org.apache.commons.io.IOUtils;

import org.slf4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static it.gov.pagopa.pdf.engine.model.AppErrorCodeEnum.*;
public class GeneratePDFServiceImpl implements GeneratePDFService {

	private final PdfEngineClient pdfEngineClient;

	/**
	 * Uses the shared application client when the service is created by the
	 * Function.
	 */
	public GeneratePDFServiceImpl() {
		this(PdfEngineClientImpl.getInstance());
	}

	/** Receives a borrowed client; its owner manages the client's lifecycle. */
	public GeneratePDFServiceImpl(PdfEngineClient pdfEngineClient) {
		this.pdfEngineClient = Objects.requireNonNull(pdfEngineClient, "pdfEngineClient must not be null");
	}

	@Override
	public BufferedInputStream generatePDF(GeneratePDFInput generatePDFInput, Path workingDirPath, Logger logger)
			throws GeneratePDFException {

		File pdfTempFile = createTempFile("document", "pdf", workingDirPath, PDFE_903);

		try {

			PdfEngineRequest pdfEngineRequest = new PdfEngineRequest();
			pdfEngineRequest.setTitle(
					generatePDFInput.getTitle() != null ? generatePDFInput.getTitle() : "Documento PDF PagoPA");
			pdfEngineRequest.setWorkingDirPath(workingDirPath.toFile().getAbsolutePath());
			pdfEngineRequest.setData(ObjectMapperUtils.writeValueAsString(generatePDFInput.getData()));
			pdfEngineRequest.setTemplate(generatePDFInput.getTemplateZip());

			logger.info("generatePDF - PdfEngineClient called at {}", LocalDateTime.now());
			PdfEngineResponse response = pdfEngineClient.generatePDF(pdfEngineRequest);
			if (response.getStatusCode() != 200 || response.getTempPdfPath() == null) {
				throw new GeneratePDFException(resolveErrorCode(response.getErrorCode()), response.getErrorMessage());
			}

			String originalPdfPath = response.getTempPdfPath();
			String pdfA2aPath = pdfTempFile.getParent() + "/ToPdfA2A.pdf";

			convertToPdfA2A(originalPdfPath, pdfA2aPath);
			logger.info("generatePDF - Completed PDF/A-2a conversion at {}", LocalDateTime.now());

			if (generatePDFInput.isGenerateZipped()) {
				return zipPDFDocument(new File(pdfA2aPath), workingDirPath);
			}
			return new BufferedInputStream(new FileInputStream(pdfA2aPath));

		} catch (GeneratePDFException e) {
			throw e;
		} catch (IOException e) {
			throw new GeneratePDFException(PDFE_902, "An error occurred on generating the pdf", e);
		} catch (Exception e) {
			throw new GeneratePDFException(PDFE_902, "Unexpected error while generating the PDF document", e);
		}
	}

	/**
	 * Converts the PDF and preserves conversion failures as PDFE_909 with their
	 * original cause.
	 */
	private void convertToPdfA2A(String originalPdfPath, String pdfA2aPath) throws GeneratePDFException {
		try {
			PdfStandardsConverter converter = new PdfStandardsConverter(originalPdfPath);
			converter.toPdfA2A(pdfA2aPath);
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new GeneratePDFException(PDFE_909,
					"Failed to convert PDF to PDF/A-2a format due to internal library error", e);
		} catch (Exception e) {
			throw new GeneratePDFException(PDFE_909, "Failed to convert PDF to PDF/A-2a format", e);
		}
	}

	private BufferedInputStream zipPDFDocument(File pdfTempFile, Path workingDirPath) throws GeneratePDFException {
		File zippedTempFile = createTempFile("zippedDocument", "zip", workingDirPath, PDFE_904);
		try (ZipOutputStream zipOutputStream = new ZipOutputStream(
				new BufferedOutputStream(new FileOutputStream(zippedTempFile)))) {
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

	/** Maps missing or unknown client codes to the standard generation error. */
	private static AppErrorCodeEnum resolveErrorCode(String errorCode) {
		if (errorCode == null || errorCode.isBlank()) {
			return PDFE_902;
		}

		try {
			return AppErrorCodeEnum.valueOf(errorCode);
		} catch (IllegalArgumentException e) {
			return PDFE_902;
		}
	}
}
