
package it.gov.pagopa.pdf.engine.service;


import it.gov.pagopa.pdf.engine.exception.CompileTemplateException;
import it.gov.pagopa.pdf.engine.exception.FillTemplateException;
import it.gov.pagopa.pdf.engine.exception.GeneratePDFException;
import it.gov.pagopa.pdf.engine.model.GeneratePDFInput;

import org.slf4j.Logger;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Interface of the service to be used to generate a PDFA/2a document based on the information provided through the {@link GeneratePDFInput}
 */
public interface GeneratePDFService {

    /**
     * Generate a PDF document using the provided HTML template and data
     *
     * @param generatePDFInput the input containing the document data
     * @param workingDirPath the path to the working directory
     * @return a {@link ByteArrayOutputStream} containing the PDFA/2a document
     * @throws CompileTemplateException thrown for error when compiling the template
     * @throws FillTemplateException    thrown for error when filling the template with the provided data
     * @throws GeneratePDFException     thrown for error when generating the PDFA/2a document
     */
    BufferedInputStream generatePDF(GeneratePDFInput generatePDFInput, Path workingDirPath, Logger logger)
            throws CompileTemplateException, FillTemplateException, GeneratePDFException, IOException;
}
