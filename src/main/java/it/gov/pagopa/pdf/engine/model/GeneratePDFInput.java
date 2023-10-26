
package it.gov.pagopa.pdf.engine.model;

import lombok.Data;

import java.io.File;
import java.nio.file.Path;

/**
 * Model class for PDF Engine input
 */
@Data
public class GeneratePDFInput {

    private String data;
    private boolean applySignature;
    private boolean generateZipped;
    private File templateZip;
    private Path workingDir;

}
