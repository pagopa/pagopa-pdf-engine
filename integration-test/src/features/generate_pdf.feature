Feature: Generate a PDF document

  Scenario: An Http POST request is sent to the PDF engine
    Given a a zip file, that contains a valid HTML template
    And a Map of key-values
    When an Http POST request is sent to the PDF Engine with a zip file and the key-values
    Then response has a 200 Http status