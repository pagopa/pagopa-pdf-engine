package it.gov.pagopa.project.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.HttpRequestMessage;
import it.gov.pagopa.project.exception.RequestBodyParseException;
import it.gov.pagopa.project.exception.UnexpectedRequestBodyFieldException;
import it.gov.pagopa.project.model.GeneratePDFInput;
import it.gov.pagopa.project.service.ParseRequestBodyService;
import net.lingala.zip4j.ZipFile;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.MultipartStream;

import java.io.*;
import java.util.Map;
import java.util.Optional;

import static it.gov.pagopa.project.model.AppErrorCodeEnum.*;

public class ParseRequestBodyServiceImpl implements ParseRequestBodyService {

    private static final String CONTENT_TYPE_HEADER = "content-type";
    private static final int MULTIPART_STREAM_BUFFER_SIZE = 1024;

    private final String writeFileBasePath = System.getenv("WRITE_FILE_BASE_PATH");
    private final String unzippedFilesFolder = System.getenv("UNZIPPED_FILES_FOLDER");
    private final String zipFileName = System.getenv("ZIP_FILE_NAME");

    private final ObjectMapper objectMapper;

    public ParseRequestBodyServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public GeneratePDFInput retrieveInputData(HttpRequestMessage<Optional<byte[]>> request) throws UnexpectedRequestBodyFieldException, RequestBodyParseException {
        byte[] body = request.getBody().get();
        String contentType = request.getHeaders().get(CONTENT_TYPE_HEADER);
        GeneratePDFInput generatePDFInput = new GeneratePDFInput();

        MultipartStream multipartStream = getMultipartStream(body, contentType);
        boolean nextPart = isNextPart(multipartStream);

        while (nextPart) {
            String header = readHeader(multipartStream);
            String fieldName = header.split(";")[1].split("=")[1].split("\"")[1];
            switch (fieldName) {
                case "template":
                    unzipTemplateFolderAndWriteToFileSystem(multipartStream);
                    break;
                case "data":
                    generatePDFInput.setData(getDocumentInputData(multipartStream));
                    break;
                case "applySignature":
                    generatePDFInput.setApplySignature(getApplySignatureField(multipartStream));
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
        } catch (FileUploadBase.FileUploadIOException e) {
            throw new RequestBodyParseException(PDFE_709, PDFE_709.getErrorMessage(), e);
        } catch (MultipartStream.MalformedStreamException e) {
            throw new RequestBodyParseException(PDFE_710, PDFE_710.getErrorMessage(), e);
        }
    }

    private boolean getApplySignatureField(MultipartStream multipartStream) throws RequestBodyParseException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            multipartStream.readBodyData(outputStream);
        } catch (IOException e) {
            throw new RequestBodyParseException(PDFE_708, PDFE_708.getErrorMessage(), e);
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
        } catch (FileUploadBase.FileUploadIOException e) {
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

    private void unzipTemplateFolderAndWriteToFileSystem(MultipartStream multipartStream) throws RequestBodyParseException {
        try (FileOutputStream fos = new FileOutputStream(writeFileBasePath + zipFileName);) {
            multipartStream.readBodyData(fos);
        } catch (FileNotFoundException e) {
            throw new RequestBodyParseException(PDFE_703, PDFE_703.getErrorMessage(), e);
        } catch (IOException e) {
            throw new RequestBodyParseException(PDFE_704, PDFE_704.getErrorMessage(), e);
        }
        try (ZipFile zipFile = new ZipFile(writeFileBasePath + zipFileName))
        {
            zipFile.extractAll(writeFileBasePath + unzippedFilesFolder);
        } catch (IOException e) {
            throw new RequestBodyParseException(PDFE_705, PDFE_705.getErrorMessage(), e);
        }
    }

    private MultipartStream getMultipartStream(byte[] body, String contentType) {
        InputStream inputStream = new ByteArrayInputStream(body);
        // Get boundary from content-type header
        String boundary = contentType.split(";")[1].split("=")[1];
        // Using MultipartStream to parse body input stream
        return new MultipartStream(inputStream, boundary.getBytes(), MULTIPART_STREAM_BUFFER_SIZE, null);
    }
}
