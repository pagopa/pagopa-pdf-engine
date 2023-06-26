/*
Copyright (C)

This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public
License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
details.

You should have received a copy of the GNU Affero General Public License along with this program.
If not, see https://www.gnu.org/licenses/.
*/

package it.gov.pagopa.project.service;


import it.gov.pagopa.project.exception.CompileTemplateException;
import it.gov.pagopa.project.exception.FillTemplateException;
import it.gov.pagopa.project.exception.GeneratePDFException;
import it.gov.pagopa.project.model.GeneratePDFInput;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Interface of the service to be used to generate a PDFA/2a document based on the information provided through the {@link GeneratePDFInput}
 */
public interface GeneratePDFService {

    /**
     * Generate a PDF document using the provided HTML template and data
     *
     * @param generatePDFInput the input containing the document data
     * @return a {@link ByteArrayOutputStream} containing the PDFA/2a document
     * @throws CompileTemplateException thrown for error when compiling the template
     * @throws FillTemplateException    thrown for error when filling the template with the provided data
     * @throws GeneratePDFException     thrown for error when generating the PDFA/2a document
     */
    BufferedInputStream generatePDF(GeneratePDFInput generatePDFInput) throws CompileTemplateException, FillTemplateException, GeneratePDFException;
}
