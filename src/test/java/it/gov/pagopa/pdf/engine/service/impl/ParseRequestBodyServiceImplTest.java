
package it.gov.pagopa.pdf.engine.service.impl;

import it.gov.pagopa.pdf.engine.exception.GeneratePDFException;
import it.gov.pagopa.pdf.engine.exception.RequestBodyParseException;
import it.gov.pagopa.pdf.engine.model.GeneratePDFInput;
import lombok.SneakyThrows;
import org.jboss.resteasy.reactive.server.core.multipart.FormData;
import org.jboss.resteasy.reactive.server.multipart.MultipartFormDataInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.spy;

class ParseRequestBodyServiceImplTest {

    private ParseRequestBodyServiceImpl sut;

    @BeforeEach
    void setUp() throws IOException {
        sut = spy(new ParseRequestBodyServiceImpl());
    }

    @Test
    @SneakyThrows
    void validContentShouldReturnExactCopy() {
        String data = "{\"test1\":\"data\"}";
        File file = new File("template");
        MultipartFormDataInput multipartFormDataInput = () -> {
            FormData formData = new FormData(2);
            formData.add("data",data);
            formData.add("template", file.toPath(),
                    "template.zip", null);
            return formData.toMultipartFormDataInput().getValues();
        };
        GeneratePDFInput result = sut.retrieveInputData(multipartFormDataInput);
        assertNotNull(result);
        assertNotNull(result.getData());
        assertEquals(data, result.getData());
        assertEquals(file, result.getTemplateZip());
    }

    @Test
    @SneakyThrows
    void missingDataContentShouldReturnException() {
        File file = new File("template");
        MultipartFormDataInput multipartFormDataInput = () -> {
            FormData formData = new FormData(2);
            formData.add("data",null);
            formData.add("template", file.toPath(),
                    "template.zip", null);
            return formData.toMultipartFormDataInput().getValues();
        };
        assertThrows(RequestBodyParseException.class,
                () -> sut.retrieveInputData(multipartFormDataInput));

    }

    @SneakyThrows
    @Test
    void missingTemplateContentShouldReturnException() {
        String data = "{\"test1\":\"data\"}";
        MultipartFormDataInput multipartFormDataInput = () -> {
            FormData formData = new FormData(2);
            formData.add("data",data);
            formData.add("template",null);
            return formData.toMultipartFormDataInput().getValues();
        };
        assertThrows(RequestBodyParseException.class,
                () -> sut.retrieveInputData(multipartFormDataInput));

    }

}