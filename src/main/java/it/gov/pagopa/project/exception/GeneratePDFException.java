package it.gov.pagopa.project.exception;

import it.gov.pagopa.project.model.AppErrorCodeEnum;

import java.util.Objects;

/**
 * Thrown in case of problems when generating the PDFA/2a document
 */
public class GeneratePDFException extends Exception {

    /** Error code of this exception */
    private final AppErrorCodeEnum errorCode;

    /**
     * Constructs new exception with provided error code and message
     *
     * @param errorCode Error code
     * @param message Detail message
     */
    public GeneratePDFException(AppErrorCodeEnum errorCode, String message) {
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
    public GeneratePDFException(AppErrorCodeEnum errorCode, String message, Throwable cause) {
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
