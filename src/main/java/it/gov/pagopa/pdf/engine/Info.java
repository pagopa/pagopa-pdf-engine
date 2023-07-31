package it.gov.pagopa.pdf.engine;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import it.gov.pagopa.project.model.AppInfo;

import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Azure Functions with Azure Http trigger.
 */
public class Info {

	/**
	 * This function will be invoked when a Http Trigger occurs
	 * @return {@link HttpResponseMessage} with {@link HttpStatus#OK}
	 */
	@FunctionName("Info")
	public HttpResponseMessage run (
			@HttpTrigger(name = "InfoTrigger",
			methods = {HttpMethod.GET},
			route = "info",
			authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
			final ExecutionContext context) {

		return request.createResponseBuilder(HttpStatus.OK)
                .body(getInfo(context.getLogger(), "/META-INF/maven/it.gov.pagopa.project/pdf-engine/pom.properties"))
				.build();
	}
	public synchronized AppInfo getInfo(Logger logger, String path) {
		String version = null;
		String name = null;
		try {
			Properties properties = new Properties();
			InputStream inputStream = getClass().getResourceAsStream(path);
			if (inputStream != null) {
				properties.load(inputStream);
				version = properties.getProperty("version", null);
				name = properties.getProperty("artifactId", null);
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Impossible to retrieve information from pom.properties file.", e);
		}
		return AppInfo.builder().version(version).environment("azure-fn").name(name).build();
	}
}
