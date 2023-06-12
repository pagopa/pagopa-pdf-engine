package it.gov.pagopa.project.model;

import lombok.Getter;

/**
 * Enumeration for application error codes and messages
 */
@Getter
public enum AppErrorCodeEnum {

    PDFE_700("PDFE_700", "I/O error when finding the beginning of the multipart stream"),
    PDFE_701("PDFE_701", "Error reading request headers"),
    PDFE_702("PDFE_702", "I/O error reading request headers"),
    PDFE_703("PDFE_703", "Error accessing the destination for the zip file, file does not exist, cannot be created or cannot be opened"),
    PDFE_704("PDFE_704", "I/O error when reading zip file from request and writing it to the destination"),
    PDFE_705("PDFE_705", "Error unzipping the file"),
    PDFE_706("PDFE_706", "I/O error when reading PDF document input data from request and writing it to the output stream"),
    PDFE_707("PDFE_707", "Error parsing PDF document input data from output stream"),
    PDFE_708("PDFE_708", "I/O error when reading apply signature boolean from request and writing it to the output stream"),
    PDFE_709("PDFE_709", "Error when switching to the next multipart stream token, byte exceeded the size limits"),
    PDFE_710("PDFE_710", "Error when switching to the next multipart stream token, stream end unexpectedly"),

    PDFE_896("PDFE_896", "Unexpected field in request body"),
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
