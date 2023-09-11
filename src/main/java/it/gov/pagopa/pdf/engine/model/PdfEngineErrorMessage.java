package it.gov.pagopa.pdf.engine.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

/**
 * Model class for PDF engine HTTP error messages
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PdfEngineErrorMessage {
    private String message;
}
