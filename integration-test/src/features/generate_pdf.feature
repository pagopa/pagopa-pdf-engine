Feature: Generate a PDF document

  Scenario: Execute a GeneratePDF request
    Given a zip file, that contains a valid HTML template
    And a Map of key-values
    When an Http POST request is sent to the PDF Engine with a zip file and the key-values
    Then response has a 200 Http status
    
  Scenario: Execute a GeneratePDF request without inputData
    Given a zip file, that contains a valid HTML template
    When an Http POST request is sent to the PDF Engine with a zip file and without key-values
    Then response has a 400 Http status
    And application error code is "PDFE_898"
    
  Scenario: Execute a GeneratePDF request without template
    Given a Map of key-values
    When an Http POST request is sent to the PDF Engine with key-values and without a zip file
    Then response has a 400 Http status
    And application error code is "PDFE_897"

  Scenario: Execute a GeneratePDF request with Playwright
    Given a zip file, that contains a valid HTML template to use in Playwright
    And a Map of key-values
    When an Http POST request is sent to the PDF Engine with a zip file and the key-values and with Playwright Engine
    Then response has a 200 Http status