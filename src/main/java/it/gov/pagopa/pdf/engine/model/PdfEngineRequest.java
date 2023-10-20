package it.gov.pagopa.pdf.engine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.jboss.resteasy.reactive.RestForm;

import java.io.File;

/**
 * Model class for PDF engine request
 */
@Getter
@Setter
@NoArgsConstructor
public class PdfEngineRequest {

    @RestForm
    String data;

    @RestForm
    File template;


}
