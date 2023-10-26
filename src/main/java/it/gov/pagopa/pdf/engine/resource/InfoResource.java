package it.gov.pagopa.pdf.engine.resource;

import it.gov.pagopa.pdf.engine.client.PdfEngineClient;
import it.gov.pagopa.pdf.engine.model.InfoResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Resource class that expose the API to retrieve info about the service */
@Path("/info")
@Tag(name = "Info", description = "Info operations")
public class InfoResource {

  @RestClient
  PdfEngineClient pdfEngineClient;

  private final Logger logger = LoggerFactory.getLogger(InfoResource.class);

  @ConfigProperty(name = "app.name", defaultValue = "app")
  String name;

  @ConfigProperty(name = "app.version", defaultValue = "0.0.0")
  String version;

  @ConfigProperty(name = "app.environment", defaultValue = "local")
  String environment;

  @Operation(summary = "Get info of Pagopa PDF Engine")
  @APIResponses(
      value = {
        @APIResponse(ref = "#/components/responses/InternalServerError"),
        @APIResponse(
            responseCode = "200",
            description = "Success",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = InfoResponse.class)))
      })
  @Produces(MediaType.APPLICATION_JSON)
  @GET
  public Response info() {
    logger.info("Info environment: [{}] - name: [{}] - version: [{}]", environment, name, version);
    InfoResponse response = InfoResponse.builder()
            .name(name)
            .version(version)
            .environment(environment)
            .description("PagoPA PDF Engine")
            .build();
    try {
      pdfEngineClient.info();
      return Response.ok().
              entity(response).build();
    } catch (Exception e) {
      return Response.status(Response.Status.SERVICE_UNAVAILABLE)
              .entity(response).build();
    }

  }
}
