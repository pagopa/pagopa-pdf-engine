package it.gov.pagopa.pdf.engine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Model class for PDF Engine client's response
 */
@Getter
@Setter
@NoArgsConstructor
public class PdfEngineResponse {

    String tempDirectoryPath;
    String tempPdfPath;
    int statusCode;
    String errorMessage;

}
