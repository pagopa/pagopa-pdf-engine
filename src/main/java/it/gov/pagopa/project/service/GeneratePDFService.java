package it.gov.pagopa.project.service;


import it.gov.pagopa.project.exception.CompileTemplateException;
import it.gov.pagopa.project.exception.FillTemplateException;
import it.gov.pagopa.project.exception.GeneratePDFException;
import it.gov.pagopa.project.model.GeneratePDFInput;

import java.io.ByteArrayOutputStream;

/**
 * Interface of the service to be used to generate a PDFA/2a document based on the information provided through the {@link GeneratePDFInput}
 */
public interface GeneratePDFService {

    /**
     * Generate a PDF document using the provided HTML template and data
     *
     * @param generatePDFInput the input containing the HTML template and data
     * @return a {@link ByteArrayOutputStream} containing the PDFA/2a document
     * @throws CompileTemplateException thrown for error when compiling the template
     * @throws FillTemplateException thrown for error when filling the template with the provided data
     * @throws GeneratePDFException thrown for error when generating the PDFA/2a document
     */
    ByteArrayOutputStream generatePDF(GeneratePDFInput generatePDFInput) throws CompileTemplateException, FillTemplateException, GeneratePDFException;
}
