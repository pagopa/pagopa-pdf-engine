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

import it.gov.pagopa.project.exception.RequestBodyParseException;
import it.gov.pagopa.project.exception.UnexpectedRequestBodyFieldException;
import it.gov.pagopa.project.model.GeneratePDFInput;

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
     * @return {@link GeneratePDFInput} that contains the input data and other necessary information
     * @throws UnexpectedRequestBodyFieldException thrown in case an unexpected field is found when parsing the request body
     * @throws RequestBodyParseException thrown for error when parsing the request body
     */
    GeneratePDFInput retrieveInputData(byte[] requestBody, Map<String, String> requestHeaders) throws UnexpectedRequestBodyFieldException, RequestBodyParseException;
}
