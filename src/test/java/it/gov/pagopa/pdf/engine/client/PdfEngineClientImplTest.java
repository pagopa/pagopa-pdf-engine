package it.gov.pagopa.pdf.engine.client;

import it.gov.pagopa.pdf.engine.exception.GeneratePDFException;
import it.gov.pagopa.pdf.engine.model.PdfEngineErrorMessage;
import it.gov.pagopa.pdf.engine.model.PdfEngineErrorResponse;
import it.gov.pagopa.pdf.engine.util.ObjectMapperUtils;
import jakarta.ws.rs.core.Response;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static it.gov.pagopa.pdf.engine.model.AppErrorCodeEnum.PDFE_709;
import static it.gov.pagopa.pdf.engine.model.AppErrorCodeEnum.PDFE_902;

class PdfEngineClientImplTest {

    @Test
    public void shouldReturn902WhenUnauthorized() {
        GeneratePDFException exception = PdfEngineClient.toException(
                Response.status(HttpStatus.SC_UNAUTHORIZED).build());
        Assertions.assertEquals(PDFE_902,exception.getErrorCode());
    }

    @Test
    public void shouldReturnUnknownWhenGeneralError() {
        GeneratePDFException exception = PdfEngineClient.toException(
                Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
        Assertions.assertEquals(PDFE_902,exception.getErrorCode());
    }

    @Test
    public void shouldReturnUnknownWhenEmptyError() {
        PdfEngineErrorResponse pdfEngineErrorResponse = new PdfEngineErrorResponse();
        pdfEngineErrorResponse.setAppStatusCode(PDFE_709.getErrorCode());
        pdfEngineErrorResponse.setErrors(Collections.singletonList(new PdfEngineErrorMessage()));
        GeneratePDFException exception = PdfEngineClient.toException(
                Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("").build());
        Assertions.assertEquals(PDFE_902,exception.getErrorCode());
    }

    @Test
    public void shouldReturnCodeError() {
        PdfEngineErrorResponse pdfEngineErrorResponse = new PdfEngineErrorResponse();
        pdfEngineErrorResponse.setAppStatusCode(PDFE_709.getErrorCode());
        pdfEngineErrorResponse.setErrors(Collections.singletonList(new PdfEngineErrorMessage()));
        GeneratePDFException exception = PdfEngineClient.toException(
                Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ObjectMapperUtils.writeValueAsString(pdfEngineErrorResponse)).build());
        Assertions.assertEquals(PDFE_709,exception.getErrorCode());
    }

}
