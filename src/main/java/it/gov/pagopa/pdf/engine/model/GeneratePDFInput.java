
package it.gov.pagopa.pdf.engine.model;

import lombok.Data;

import java.util.Map;

/**
 * Model class for PDF Engine input
 */
@Data
public class GeneratePDFInput {

    private boolean templateSavedOnFileSystem;
    private Map<String, Object> data;
    private boolean applySignature;
    private boolean generateZipped;

    private GeneratorType generatorType = GeneratorType.ITEXT;

}
