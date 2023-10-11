
package it.gov.pagopa.pdf.engine.service;

import it.gov.pagopa.pdf.engine.exception.RequestBodyParseException;
import it.gov.pagopa.pdf.engine.exception.UnexpectedRequestBodyFieldException;
import it.gov.pagopa.pdf.engine.model.GeneratePDFInput;
import org.jboss.resteasy.reactive.server.multipart.MultipartFormDataInput;

/**
 * Interface of the service to be used to extract the input from the provided request body
 */
public interface ParseRequestBodyService {

    /**
     * Retrieve the input for the PDF generation by parsing the request body
     *
     * @param request the request body
     * @return {@link GeneratePDFInput} that contains the input data and other necessary information
     * @throws UnexpectedRequestBodyFieldException thrown in case an unexpected field is found when parsing the request body
     * @throws RequestBodyParseException thrown for error when parsing the request body
     */
    GeneratePDFInput retrieveInputData(MultipartFormDataInput request) throws UnexpectedRequestBodyFieldException, RequestBodyParseException;
}
