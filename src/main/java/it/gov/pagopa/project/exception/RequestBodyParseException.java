package it.gov.pagopa.project.exception;

import it.gov.pagopa.project.model.AppErrorCodeEnum;

/**
 * Thrown in case of problems when parsing request body
 */
public class RequestBodyParseException extends PDFEngineException {

    /**
     * Constructs new exception with provided error code and message
     *
     * @param errorCode Error code
     * @param message Detail message
     */
    public RequestBodyParseException(AppErrorCodeEnum errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * Constructs new exception with provided error code, message and cause
     *
     * @param errorCode Error code
     * @param message Detail message
     * @param cause Exception causing the constructed one
     */
    public RequestBodyParseException(AppErrorCodeEnum errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
