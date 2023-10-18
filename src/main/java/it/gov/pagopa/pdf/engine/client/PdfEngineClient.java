package it.gov.pagopa.pdf.engine.client;


import io.smallrye.mutiny.Uni;
import it.gov.pagopa.pdf.engine.model.PdfEngineRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.io.InputStream;

@RegisterRestClient
@Path("/")
public interface PdfEngineClient {

    @Path("generate-pdf")
    @POST
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    Uni<InputStream> generatePDF(PdfEngineRequest pdfEngineRequest);

}
