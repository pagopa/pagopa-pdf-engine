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

package it.gov.pagopa.project.exception;

import it.gov.pagopa.project.model.AppErrorCodeEnum;

import java.util.Objects;

/**
 * Base exception for PDF Engine exceptions
 */
public class PDFEngineException extends Exception {

    /** Error code of this exception */
    private final AppErrorCodeEnum errorCode;

    /**
     * Constructs new exception with provided error code and message
     *
     * @param errorCode Error code
     * @param message Detail message
     */
    public PDFEngineException(AppErrorCodeEnum errorCode, String message) {
        super(message);
        this.errorCode = Objects.requireNonNull(errorCode);
    }

    /**
     * Constructs new exception with provided error code, message and cause
     *
     * @param errorCode Error code
     * @param message Detail message
     * @param cause Exception causing the constructed one
     */
    public PDFEngineException(AppErrorCodeEnum errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = Objects.requireNonNull(errorCode);
    }

    /**
     * Returns error code
     *
     * @return Error code of this exception
     */
    public AppErrorCodeEnum getErrorCode() {
        return errorCode;
    }
}
