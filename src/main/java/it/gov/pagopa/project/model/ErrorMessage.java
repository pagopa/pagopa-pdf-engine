package it.gov.pagopa.project.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ErrorMessage {

    private String path;
    private String message;
}
