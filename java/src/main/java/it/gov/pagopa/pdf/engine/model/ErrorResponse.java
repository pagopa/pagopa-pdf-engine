/*
Copyright (C)

This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public
License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
details.

You should have received a copy of the GNU Affero General Public License along with this program.
If not, see https://www.gnu.org/licenses/.
*/

package it.gov.pagopa.pdf.engine.model;

import com.microsoft.azure.functions.HttpStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/**
 * Model class for HTTP error response
 */
@Getter
@Setter
public class ErrorResponse {

    private String errorId;
    private HttpStatus httpStatusCode;
    private String httpStatusDescription;
    private AppErrorCodeEnum appErrorCode;
    private List<ErrorMessage> errors;

    public ErrorResponse(HttpStatus httpStatusCode, AppErrorCodeEnum appErrorCode, List<ErrorMessage> errors) {
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
