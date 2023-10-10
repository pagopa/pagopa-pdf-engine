
package it.gov.pagopa.pdf.engine.model;

import lombok.Data;

import java.util.Map;
import net.lingala.zip4j.ZipFile;

/**
 * Model class for PDF Engine input
 */
@Data
public class GeneratePDFInput {

    private String data;
    private boolean applySignature;
    private boolean generateZipped;
    private ZipFile templateZip;

}
