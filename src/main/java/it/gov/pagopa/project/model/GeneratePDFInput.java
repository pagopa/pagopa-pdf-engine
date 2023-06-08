package it.gov.pagopa.project.model;

import lombok.Data;

import java.util.Map;

/**
 * Model class for PDF Engine input
 */
@Data
public class GeneratePDFInput {

    private String template;
    private Map<String, Object> data;
    private boolean applySignature;

}
