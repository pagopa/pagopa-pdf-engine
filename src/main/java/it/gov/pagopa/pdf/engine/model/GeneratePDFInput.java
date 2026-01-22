
package it.gov.pagopa.pdf.engine.model;

import lombok.Data;

import java.util.Map;

import lombok.ToString;
import net.lingala.zip4j.ZipFile;

/**
 * Model class for PDF Engine input
 */
@Data
@ToString
public class GeneratePDFInput {

    private Map<String, Object> data;
    private boolean applySignature;
    private boolean generateZipped;
    @ToString.Exclude
    private ZipFile templateZip;
    private String title;

    private GeneratorType generatorType = GeneratorType.ITEXT;

}
