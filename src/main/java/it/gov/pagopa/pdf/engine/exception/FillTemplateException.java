
package it.gov.pagopa.pdf.engine.exception;

import it.gov.pagopa.pdf.engine.model.AppErrorCodeEnum;

/**
 * Thrown in case of problems when filling the HTML template
 */
public class FillTemplateException extends PDFEngineException {

    /**
     * Constructs new exception with provided error code and message
     *
     * @param errorCode Error code
     * @param message Detail message
     */
    public FillTemplateException(AppErrorCodeEnum errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * Constructs new exception with provided error code, message and cause
     *
     * @param errorCode Error code
     * @param message Detail message
     * @param cause Exception causing the constructed one
     */
    public FillTemplateException(AppErrorCodeEnum errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

}
