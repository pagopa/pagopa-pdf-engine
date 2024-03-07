package it.gov.pagopa.pdf.engine.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import net.lingala.zip4j.ZipFile;

/**
 * Model class for PDF engine request
 */
@Getter
@Setter
@NoArgsConstructor
public class PdfEngineRequest {

  String data;

  String workingDirPath;

  String title;

  ZipFile template;


}
