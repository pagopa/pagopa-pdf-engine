package it.gov.pagopa.pdf.engine.client;


import io.quarkus.rest.client.reactive.ClientExceptionMapper;
import io.smallrye.mutiny.Uni;
import it.gov.pagopa.pdf.engine.exception.GeneratePDFException;
import it.gov.pagopa.pdf.engine.model.AppErrorCodeEnum;
import it.gov.pagopa.pdf.engine.model.PdfEngineErrorResponse;
import it.gov.pagopa.pdf.engine.model.PdfEngineRequest;
import it.gov.pagopa.pdf.engine.util.ObjectMapperUtils;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.http.HttpStatus;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.io.InputStream;

@RegisterRestClient
@Path("/")
public interface PdfEngineClient {

    @Path("generate-pdf")
    @POST
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    Uni<byte[]> generatePDF(PdfEngineRequest pdfEngineRequest);

    @Path("info")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    void info();

    @ClientExceptionMapper
    static GeneratePDFException toException(Response response) {

        if (response != null &&
                response.getStatus() == HttpStatus.SC_UNAUTHORIZED
        ) {
            return new GeneratePDFException(AppErrorCodeEnum.PDFE_902, "Unauthorized call to PDF engine function");

        } else {
            try {
                String entityResponse = response.getEntity().toString();
                if (!entityResponse.isEmpty()) {
                    PdfEngineErrorResponse errorResponse =
                            ObjectMapperUtils.mapString(entityResponse, PdfEngineErrorResponse.class);

                    if (errorResponse != null &&
                            errorResponse.getErrors() != null &&
                            !errorResponse.getErrors().isEmpty() &&
                            errorResponse.getErrors().get(0) != null
                    ) {
                        return new GeneratePDFException(AppErrorCodeEnum.valueOf(errorResponse.getAppStatusCode()),
                                errorResponse.getErrors().get(0).getMessage());
                    }
                }
            } catch (Exception e) {
                return new GeneratePDFException(AppErrorCodeEnum.PDFE_902, "Unknown error in PDF engine function");
            }

        }

        return new GeneratePDFException(AppErrorCodeEnum.PDFE_902, "Unknown error in PDF engine function");

    }

}
