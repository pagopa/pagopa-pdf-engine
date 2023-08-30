package it.gov.pagopa.pdf.engine.client;


import it.gov.pagopa.pdf.engine.model.PdfEngineRequest;
import it.gov.pagopa.pdf.engine.model.PdfEngineResponse;

public interface PdfEngineClient {

    PdfEngineResponse generatePDF(PdfEngineRequest pdfEngineRequest);
}
