
package it.gov.pagopa.pdf.engine.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

import static org.jboss.resteasy.reactive.RestResponse.StatusCode.BAD_REQUEST;
import static org.jboss.resteasy.reactive.RestResponse.StatusCode.INTERNAL_SERVER_ERROR;

/**
 * Model class for HTTP error response
 */
@Getter
@Setter
public class ErrorResponse {

    private String errorId;
    private int httpStatusCode;
    private String httpStatusDescription;
    private AppErrorCodeEnum appErrorCode;
    private List<ErrorMessage> errors;

    public ErrorResponse(int httpStatusCode, AppErrorCodeEnum appErrorCode, List<ErrorMessage> errors) {
        this.errorId = UUID.randomUUID().toString();
        this.httpStatusCode = httpStatusCode;
        this.appErrorCode = appErrorCode;
        this.errors = errors;

        switch (this.httpStatusCode) {
            case BAD_REQUEST:
                this.httpStatusDescription = "Bad Request";
                break;
            case INTERNAL_SERVER_ERROR:
                this.httpStatusDescription = "Internal Server Error";
                break;
            default: throw new IllegalStateException("Http error code not supported " + httpStatusCode);
        }
    }
}
