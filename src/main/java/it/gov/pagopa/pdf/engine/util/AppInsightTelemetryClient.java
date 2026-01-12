package it.gov.pagopa.pdf.engine.util;

import com.microsoft.applicationinsights.TelemetryClient;

import java.util.Map;

public class AppInsightTelemetryClient {

    private final String connectionString = System.getenv("APPLICATIONINSIGHTS_CONNECTION_STRING");

    private final TelemetryClient telemetryClient;

    public AppInsightTelemetryClient() {
        this.telemetryClient = new TelemetryClient();
    }


    public void createCustomEvent(String title, String details) {
        Map<String, String> props =
                Map.of(
                        "type",
                        "PDF_ENGINE_JAVA_LOG",
                        "title",
                        title,
                        "details",
                        details);
        this.telemetryClient.trackEvent("PDF_ENGINE_JAVA", props, null);
    }

    public void createCustomEventError(String title, Exception e) {
        Map<String, String> props =
                Map.of(
                        "type",
                        "PDF_ENGINE_JAVA_LOG",
                        "title",
                        title,
                        "details",
                        e.getMessage(),
                        "cause",
                        e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
        this.telemetryClient.trackEvent("PDF_ENGINE_JAVA_ALERT", props, null);
    }

    public void createCustomEventError(String title) {
        Map<String, String> props =
                Map.of(
                        "type",
                        "PDF_ENGINE_JAVA_LOG",
                        "title",
                        title);
        this.telemetryClient.trackEvent("PDF_ENGINE_JAVA_ALERT", props, null);
    }


}
