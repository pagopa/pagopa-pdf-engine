
package it.gov.pagopa.pdf.engine.service.impl;

import it.gov.pagopa.pdf.engine.client.PdfEngineClient;
import it.gov.pagopa.pdf.engine.exception.GeneratePDFException;
import it.gov.pagopa.pdf.engine.model.AppErrorCodeEnum;
import it.gov.pagopa.pdf.engine.model.GeneratePDFInput;
import it.gov.pagopa.pdf.engine.model.PdfEngineRequest;
import it.gov.pagopa.pdf.engine.model.PdfEngineResponse;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GeneratePDFServiceImplTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(GeneratePDFServiceImplTest.class);

	private GeneratePDFServiceImpl sut;
	private PdfEngineClient pdfEngineClient;

	private Path workingPath;

	@BeforeEach
	void setUp() throws IOException {
		pdfEngineClient = mock(PdfEngineClient.class);
		sut = new GeneratePDFServiceImpl(pdfEngineClient);
		workingPath = Files.createTempDirectory("testDir");
	}

	@AfterEach
	void tearDown() throws IOException {
		FileUtils.deleteDirectory(workingPath.toFile());
	}

	@Test
	void generatePDFNotZippedWithSuccess() throws IOException, URISyntaxException, GeneratePDFException {
		GeneratePDFInput pdfInput = new GeneratePDFInput();
		pdfInput.setData(Collections.singletonMap("a", "b"));
		pdfInput.setApplySignature(false);

		PdfEngineResponse pdfEngineResponse = new PdfEngineResponse();
		pdfEngineResponse.setStatusCode(200);
		pdfEngineResponse.setTempPdfPath(
				Path.of(Objects.requireNonNull(this.getClass().getClassLoader().getResource("valid_pdf.pdf")).toURI())
						.toString());

		when(pdfEngineClient.generatePDF(any(PdfEngineRequest.class))).thenReturn(pdfEngineResponse);

		try (BufferedInputStream output = sut.generatePDF(pdfInput, workingPath, LOGGER)) {
			assertNotNull(output);
		}
	}

	@Test
	void generatePDFZippedWithSuccess() throws IOException, URISyntaxException, GeneratePDFException {
		GeneratePDFInput pdfInput = new GeneratePDFInput();
		pdfInput.setData(Collections.singletonMap("a", "b"));
		pdfInput.setApplySignature(false);
		pdfInput.setGenerateZipped(true);
		pdfInput.setTitle("title");

		PdfEngineResponse pdfEngineResponse = new PdfEngineResponse();
		pdfEngineResponse.setStatusCode(200);
		pdfEngineResponse.setTempPdfPath(
				Path.of(Objects.requireNonNull(this.getClass().getClassLoader().getResource("valid_pdf.pdf")).toURI())
						.toString());

		when(pdfEngineClient.generatePDF(any(PdfEngineRequest.class))).thenReturn(pdfEngineResponse);

		try (BufferedInputStream output = sut.generatePDF(pdfInput, workingPath, LOGGER)) {
			assertNotNull(output);
		}
	}

	@Test
	void generatePDFCallException() {
		GeneratePDFException failure = generateErrorResponse(400, "PDFE_901");
		Assertions.assertEquals(AppErrorCodeEnum.PDFE_901, failure.getErrorCode());
		Assertions.assertEquals("Upstream failure", failure.getMessage());
	}

	@Test
	void generatePDFWithMissingErrorCodeUsesFallback() {
		GeneratePDFException failure = generateErrorResponse(503, null);
		Assertions.assertEquals(AppErrorCodeEnum.PDFE_902, failure.getErrorCode());
		Assertions.assertEquals("Upstream failure", failure.getMessage());
	}

	@Test
	void generatePDFWithUnknownErrorCodeUsesFallback() {
		GeneratePDFException failure = generateErrorResponse(503, "UNSUPPORTED_CODE");
		Assertions.assertEquals(AppErrorCodeEnum.PDFE_902, failure.getErrorCode());
		Assertions.assertEquals("Upstream failure", failure.getMessage());
	}

	@Test
	void generatePDFWithBlankErrorCodeUsesFallback() {
		for (String errorCode : new String[] { "", " \t " }) {
			GeneratePDFException failure = generateErrorResponse(503, errorCode);
			Assertions.assertEquals(AppErrorCodeEnum.PDFE_902, failure.getErrorCode());
		}
	}

	@Test
	void generatePDFWithMissingPdfUsesFallback() {
		GeneratePDFException failure = generateErrorResponse(200, null);
		Assertions.assertEquals(AppErrorCodeEnum.PDFE_902, failure.getErrorCode());
	}

	@Test
	void generatePDFWrapsUnexpectedClientFailureWithContext() {
		GeneratePDFInput pdfInput = new GeneratePDFInput();
		pdfInput.setData(Collections.singletonMap("a", "b"));

		IllegalStateException originalCause = new IllegalStateException("Client is unavailable");

		when(pdfEngineClient.generatePDF(any(PdfEngineRequest.class))).thenThrow(originalCause);

		GeneratePDFException failure = assertThrows(GeneratePDFException.class,
				() -> sut.generatePDF(pdfInput, workingPath, LOGGER));

		Assertions.assertEquals(AppErrorCodeEnum.PDFE_902, failure.getErrorCode());
		Assertions.assertEquals("Unexpected error while generating the PDF document", failure.getMessage());
		Assertions.assertSame(originalCause, failure.getCause());
	}

	private GeneratePDFException generateErrorResponse(int statusCode, String errorCode) {
		GeneratePDFInput pdfInput = new GeneratePDFInput();
		pdfInput.setData(Collections.singletonMap("a", "b"));

		PdfEngineResponse response = new PdfEngineResponse();
		response.setStatusCode(statusCode);
		response.setErrorCode(errorCode);
		response.setErrorMessage("Upstream failure");
		when(pdfEngineClient.generatePDF(any(PdfEngineRequest.class))).thenReturn(response);

		return assertThrows(GeneratePDFException.class, () -> sut.generatePDF(pdfInput, workingPath, LOGGER));
	}
}
