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

package it.gov.pagopa.project.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import it.gov.pagopa.project.exception.RequestBodyParseException;
import it.gov.pagopa.project.exception.UnexpectedRequestBodyFieldException;
import it.gov.pagopa.project.model.AppErrorCodeEnum;
import it.gov.pagopa.project.model.GeneratePDFInput;
import it.gov.pagopa.project.service.ParseRequestBodyService;
import net.lingala.zip4j.ZipFile;
import org.apache.commons.fileupload.FileUploadBase.FileUploadIOException;
import org.apache.commons.fileupload.MultipartStream;

import java.io.*;
import java.nio.file.Path;
import java.util.Map;

import static it.gov.pagopa.project.model.AppErrorCodeEnum.*;
import static it.gov.pagopa.project.util.Constants.UNZIPPED_FILES_FOLDER;
import static it.gov.pagopa.project.util.Constants.ZIP_FILE_NAME;

public class ParseRequestBodyServiceImpl implements ParseRequestBodyService {

    private static final String CONTENT_TYPE_HEADER = "content-type";
    private static final int MULTIPART_STREAM_BUFFER_SIZE = 1024;

    private final ObjectMapper objectMapper;

    public ParseRequestBodyServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public GeneratePDFInput retrieveInputData(byte[] requestBody, Map<String, String> requestHeaders,  Path workingDirPath) throws UnexpectedRequestBodyFieldException, RequestBodyParseException {
        String contentType = requestHeaders.get(CONTENT_TYPE_HEADER);
        GeneratePDFInput generatePDFInput = new GeneratePDFInput();

        MultipartStream multipartStream = getMultipartStream(requestBody, contentType);
        boolean nextPart = isNextPart(multipartStream);

        while (nextPart) {
            String header = readHeader(multipartStream);
            String fieldName = header.split(";")[1].split("=")[1].split("\"")[1];
            switch (fieldName) {
                case "template":
                    generatePDFInput.setTemplateSavedOnFileSystem(unzipTemplateFolderAndWriteToFileSystem(multipartStream, workingDirPath));
                    break;
                case "data":
                    generatePDFInput.setData(getDocumentInputData(multipartStream));
                    break;
                case "applySignature":
                    generatePDFInput.setApplySignature(getBooleanField(multipartStream, PDFE_708));
                    break;
                case "generateZipped":
                    generatePDFInput.setGenerateZipped(getBooleanField(multipartStream, PDFE_713));
                    break;
                default: throw new UnexpectedRequestBodyFieldException(PDFE_896, "Unexpected field " + fieldName);
            }

            nextPart = hasNextPart(multipartStream);
        }

        return generatePDFInput;
    }

    private boolean hasNextPart(MultipartStream multipartStream) throws RequestBodyParseException {
        try {
            return multipartStream.readBoundary();
        } catch (FileUploadIOException e) {
            throw new RequestBodyParseException(PDFE_709, PDFE_709.getErrorMessage(), e);
        } catch (MultipartStream.MalformedStreamException e) {
            throw new RequestBodyParseException(PDFE_710, PDFE_710.getErrorMessage(), e);
        }
    }

    private boolean getBooleanField(MultipartStream multipartStream, AppErrorCodeEnum errorCode) throws RequestBodyParseException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            multipartStream.readBodyData(outputStream);
        } catch (IOException e) {
            throw new RequestBodyParseException(errorCode, errorCode.getErrorMessage(), e);
        }
        return Boolean.parseBoolean(outputStream.toString());
    }


    private Map<String,Object> getDocumentInputData(MultipartStream multipartStream) throws RequestBodyParseException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            multipartStream.readBodyData(os);
        } catch (IOException e) {
            throw new RequestBodyParseException(PDFE_706, PDFE_706.getErrorMessage(), e);
        }
        TypeReference<Map<String,Object>> typeRef = new TypeReference<>() {};
        try {
            return this.objectMapper.readValue(os.toString(), typeRef);
        } catch (JsonProcessingException e) {
            throw new RequestBodyParseException(PDFE_707, PDFE_707.getErrorMessage(), e);
        }
    }

    private String readHeader(MultipartStream multipartStream) throws RequestBodyParseException {
        try {
            return multipartStream.readHeaders();
        } catch (FileUploadIOException e) {
            throw new RequestBodyParseException(PDFE_701, PDFE_701.getErrorMessage(), e);
        } catch (MultipartStream.MalformedStreamException e) {
            throw new RequestBodyParseException(PDFE_702, PDFE_701.getErrorMessage(), e);
        }
    }

    private boolean isNextPart(MultipartStream multipartStream) throws RequestBodyParseException {
        try {
            return multipartStream.skipPreamble();
        } catch (IOException e) {
            throw new RequestBodyParseException(PDFE_700, PDFE_700.getErrorMessage(), e);
        }
    }

    private boolean unzipTemplateFolderAndWriteToFileSystem(MultipartStream multipartStream, Path workingDirPath) throws RequestBodyParseException {
        try (FileOutputStream fos = new FileOutputStream(workingDirPath + ZIP_FILE_NAME)) {
            multipartStream.readBodyData(fos);
        } catch (FileNotFoundException e) {
            throw new RequestBodyParseException(PDFE_703, PDFE_703.getErrorMessage(), e);
        } catch (IOException e) {
            throw new RequestBodyParseException(PDFE_704, PDFE_704.getErrorMessage(), e);
        }
        try (ZipFile zipFile = new ZipFile(workingDirPath + ZIP_FILE_NAME))
        {
            zipFile.extractAll(workingDirPath + UNZIPPED_FILES_FOLDER);
        } catch (IOException e) {
            throw new RequestBodyParseException(PDFE_705, PDFE_705.getErrorMessage(), e);
        }
        return true;
    }

    @VisibleForTesting
    public MultipartStream getMultipartStream(byte[] body, String contentType) throws RequestBodyParseException {
        InputStream inputStream = new ByteArrayInputStream(body);
        if (contentType == null) {
            throw new RequestBodyParseException(PDFE_711, PDFE_711.getErrorMessage());
        }
        String[] splitContentType = contentType.split(";");
        if (!splitContentType[0].equals("multipart/form-data")) {
            throw new RequestBodyParseException(PDFE_712, PDFE_712.getErrorMessage());
        }
        // Get boundary from content-type header
        String boundary = splitContentType[1].split("=")[1];
        // Using MultipartStream to parse body input stream
        return new MultipartStream(inputStream, boundary.getBytes(), MULTIPART_STREAM_BUFFER_SIZE, null);
    }
}
