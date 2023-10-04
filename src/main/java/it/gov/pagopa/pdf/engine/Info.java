package it.gov.pagopa.pdf.engine;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import it.gov.pagopa.pdf.engine.model.AppInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

/**
 * Azure Functions with Azure Http trigger.
 */
public class Info {

	private final Logger logger = LoggerFactory.getLogger(Info.class);

	/**
	 * This function will be invoked when a Http Trigger occurs, it will check if the underlying service does provide
	 * a response
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
				.body(getInfo())
				.build();
	}

	/**
	 * Method to produce a json containing the App Infos
	 * @return the application's info
	 */
	public AppInfo getInfo() {
		String version = null;
		String name = null;
		try (InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("application.properties")) {
			Properties properties = new Properties();
			if (inputStream != null) {
				properties.load(inputStream);
				version = properties.getProperty("version", null);
				name = properties.getProperty("name", null);
			}
		} catch (Exception e) {
			logger.error("Impossible to retrieve information from pom.properties file.", e);
		}
		return AppInfo.builder().version(version).environment("azure-fn").name(name).build();

	}
}
