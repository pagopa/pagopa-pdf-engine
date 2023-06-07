package it.gov.pagopa.project.model;

import lombok.Data;

import java.util.Map;

@Data
public class GeneratePDFInput {

    private String template;
    private Map<String, Object> data;
    private boolean applySignature;

}
