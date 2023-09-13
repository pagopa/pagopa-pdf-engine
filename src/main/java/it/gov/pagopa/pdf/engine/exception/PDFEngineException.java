
package it.gov.pagopa.pdf.engine.exception;

import it.gov.pagopa.pdf.engine.model.AppErrorCodeEnum;

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
