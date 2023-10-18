
package it.gov.pagopa.pdf.engine.service.impl;

import it.gov.pagopa.pdf.engine.exception.RequestBodyParseException;
import it.gov.pagopa.pdf.engine.exception.UnexpectedRequestBodyFieldException;
import it.gov.pagopa.pdf.engine.model.GeneratePDFInput;
import it.gov.pagopa.pdf.engine.service.ParseRequestBodyService;
import jakarta.enterprise.context.ApplicationScoped;
import net.lingala.zip4j.ZipFile;
import org.jboss.resteasy.reactive.server.multipart.FormValue;
import org.jboss.resteasy.reactive.server.multipart.MultipartFormDataInput;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

import static it.gov.pagopa.pdf.engine.model.AppErrorCodeEnum.*;

@ApplicationScoped
public class ParseRequestBodyServiceImpl implements ParseRequestBodyService {

    @Override
    public GeneratePDFInput retrieveInputData(MultipartFormDataInput request) throws UnexpectedRequestBodyFieldException, RequestBodyParseException {
        GeneratePDFInput generatePDFInput = new GeneratePDFInput();
        Map<String, Collection<FormValue>> map = request.getValues();
        for (var entry : map.entrySet()) {
            switch (entry.getKey()) {
                case "template" -> {
                    Collection<FormValue> values = entry.getValue();
                    if (values == null || values.isEmpty()) {
                        throw new RequestBodyParseException(PDFE_709, PDFE_709.getErrorMessage());
                    }
                    try {
                        generatePDFInput.setTemplateZip(values.stream().findFirst().get()
                                .getFileItem().getFile().toFile());
                    } catch (Exception e) {
                        throw new RequestBodyParseException(PDFE_703, PDFE_703.getErrorMessage(), e);
                    }
                    generatePDFInput.setTemplateZip(entry.getValue().stream().findFirst()
                            .get().getFileItem().getFile().toFile());
                }
                case "data" -> {
                    Collection<FormValue> values = entry.getValue();
                    if (values == null || values.isEmpty()) {
                        throw new RequestBodyParseException(PDFE_706, PDFE_706.getErrorMessage());
                    }
                    try {
                        generatePDFInput.setData(entry.getValue().stream().findFirst().get().getValue());
                    } catch (Exception e) {
                        throw new RequestBodyParseException(PDFE_707, PDFE_707.getErrorMessage(), e);
                    }
                }
            }
        }
        return generatePDFInput;
    }

}
