package it.gov.pagopa.pdf.engine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import net.lingala.zip4j.ZipFile;
import org.jboss.resteasy.reactive.RestForm;

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
    ZipFile template;


}
