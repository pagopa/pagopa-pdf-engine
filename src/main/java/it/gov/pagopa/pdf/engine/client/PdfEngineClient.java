package it.gov.pagopa.pdf.engine.client;


import it.gov.pagopa.pdf.engine.model.PdfEngineRequest;
import it.gov.pagopa.pdf.engine.model.PdfEngineResponse;

public interface PdfEngineClient {

    /**
     * Sends the request to the PDF engine and returns the response.
     *
     * @param pdfEngineRequest input request
     * @return response containing the generated PDF or an error description
     */
    PdfEngineResponse generatePDF(PdfEngineRequest pdfEngineRequest);

    /**
     * Pings the underlying service info endpoint.
     *
     * @return {@code true} if the service answered with HTTP 200, {@code false} otherwise
     */
    boolean info();
}
