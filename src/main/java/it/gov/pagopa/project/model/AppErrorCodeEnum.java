package it.gov.pagopa.project.model;

/**
 * Enumeration for application error codes
 */
public enum AppErrorCodeEnum {

    PDFE_897("PDFE_897", "Invalid HTML template"),
    PDFE_898("PDFE_898", "Invalid document data"),
    PDFE_899("PDFE_899", "Invalid payload"),

    PDFE_900("PDFE_900", "Error filling the HTML template"),
    PDFE_901("PDFE_901", "Error compiling the HTML template"),
    PDFE_902("PDFE_902", "Error generating the PDF document");

    private final String errorCode;
    private final String errorMessage;

    AppErrorCodeEnum(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
}
