package it.gov.pagopa.pdf.engine.client;


import io.smallrye.mutiny.Uni;
import it.gov.pagopa.pdf.engine.model.PdfEngineRequest;
import it.gov.pagopa.pdf.engine.model.PdfEngineResponse;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.io.InputStream;

@RegisterRestClient
public interface PdfEngineClient {

    @Path("/generate-pdf")
    @POST
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    Uni<InputStream> generatePDF(PdfEngineRequest pdfEngineRequest);

}
