package it.gov.pagopa.project.service;

import com.microsoft.azure.functions.HttpRequestMessage;
import it.gov.pagopa.project.exception.RequestBodyParseException;
import it.gov.pagopa.project.exception.UnexpectedRequestBodyFieldException;
import it.gov.pagopa.project.model.GeneratePDFInput;

import java.util.Optional;

/**
 * Interface of the service to be used to extract the input from the provided request body
 */
public interface ParseRequestBodyService {

    /**
     * Retrieve the input for the PDF generation by parsing the request body
     *
     * @param request to be parsed
     * @return {@link GeneratePDFInput} that contains the input data and other necessary information
     * @throws UnexpectedRequestBodyFieldException thrown in case an unexpected field is found when parsing the request body
     * @throws RequestBodyParseException thrown for error when parsing the request body
     */
    GeneratePDFInput retrieveInputData(HttpRequestMessage<Optional<byte[]>> request) throws UnexpectedRequestBodyFieldException, RequestBodyParseException;
}
