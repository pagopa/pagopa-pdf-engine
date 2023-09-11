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

package it.gov.pagopa.pdf.engine.model;

import lombok.Getter;

/**
 * Enumeration for application error codes and messages
 */
@Getter
public enum AppErrorCodeEnum {

    PDFE_700("PDFE_700", "I/O error when finding the beginning of the multipart stream"),
    PDFE_701("PDFE_701", "Error reading request headers"),
    PDFE_702("PDFE_702", "I/O error reading request headers"),
    PDFE_703("PDFE_703", "Error accessing the destination for the zip file, file does not exist, cannot be created or cannot be opened"),
    PDFE_704("PDFE_704", "I/O error when reading zip file from request and writing it to the destination"),
    PDFE_705("PDFE_705", "Error unzipping the file"),
    PDFE_706("PDFE_706", "I/O error when reading PDF document input data from request and writing it to the output stream"),
    PDFE_707("PDFE_707", "Error parsing PDF document input data from output stream"),
    PDFE_708("PDFE_708", "I/O error when reading apply signature boolean from request and writing it to the output stream"),
    PDFE_709("PDFE_709", "Error when switching to the next multipart stream token, byte exceeded the size limits"),
    PDFE_710("PDFE_710", "Error when switching to the next multipart stream token, stream end unexpectedly"),
    PDFE_711("PDFE_711", "Content-type header is null"),
    PDFE_712("PDFE_712", "Content-type header is not multipart/form-data"),
    PDFE_713("PDFE_713", "I/O error when reading generate zipped boolean from request and writing it to the output stream"),

    PDFE_714("PDFE_708", "I/O error when reading generate type from request, and writing it to the output stream"),

    PDFE_896("PDFE_896", "Unexpected field in request body"),
    PDFE_897("PDFE_897", "Invalid HTML template, template not provided"),
    PDFE_898("PDFE_898", "Invalid document data"),
    PDFE_899("PDFE_899", "Invalid payload"),

    PDFE_900("PDFE_900", "Error filling the HTML template"),
    PDFE_901("PDFE_901", "Error compiling the HTML template"),
    PDFE_902("PDFE_902", "Error generating the PDF document"),
    PDFE_903("PDFE_903", "Error creating temp file for PDF document"),
    PDFE_904("PDFE_904", "Error creating temp file for zipped PDF document"),
    PDFE_905("PDFE_905", "Error accessing the temporary zip file, file does not exist, cannot be created or cannot be opened"),
    PDFE_906("PDFE_906", "I/O error when zipping the PDF document"),
    PDFE_907("PDFE_907", "I/O error on handling the PDF generation result"),
    PDFE_908("PDFE_908", "I/O error on creating the working directory");



    private final String errorCode;
    private final String errorMessage;

    AppErrorCodeEnum(String errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
}
