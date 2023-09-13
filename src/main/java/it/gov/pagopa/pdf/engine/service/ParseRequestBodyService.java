
package it.gov.pagopa.pdf.engine.service;

import it.gov.pagopa.pdf.engine.exception.RequestBodyParseException;
import it.gov.pagopa.pdf.engine.exception.UnexpectedRequestBodyFieldException;
import it.gov.pagopa.pdf.engine.model.GeneratePDFInput;

import java.nio.file.Path;
import java.util.Map;

/**
 * Interface of the service to be used to extract the input from the provided request body
 */
public interface ParseRequestBodyService {

    /**
     * Retrieve the input for the PDF generation by parsing the request body
     *
     * @param requestBody the request body
     * @param requestHeaders the request headers
     * @param workingDirPath the path to the working directory
     * @return {@link GeneratePDFInput} that contains the input data and other necessary information
     * @throws UnexpectedRequestBodyFieldException thrown in case an unexpected field is found when parsing the request body
     * @throws RequestBodyParseException thrown for error when parsing the request body
     */
    GeneratePDFInput retrieveInputData(byte[] requestBody, Map<String, String> requestHeaders, Path workingDirPath) throws UnexpectedRequestBodyFieldException, RequestBodyParseException;
}
