package it.gov.pagopa.pdf.engine.model;

import jakarta.ws.rs.Path;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.BufferedInputStream;

/**
 * Model class for PDF Engine client's response
 */
@Getter
@Setter
@NoArgsConstructor
public class PdfEngineResponse {

    private BufferedInputStream bufferedInputStream;
    private java.nio.file.Path workDirPath;
    private boolean generateZipped;

}
