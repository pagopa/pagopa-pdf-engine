package it.gov.pagopa.project.dihook;

import com.microsoft.azure.functions.spi.inject.FunctionInstanceInjector;
import it.gov.pagopa.project.component.DaggerHttpTriggerFunctionComponent;

/**
 * Class used for dependency injection in the Azure function
 */
public class HttpTriggerFunctionInstanceInjector implements FunctionInstanceInjector {
    @Override
    public <T> T getInstance(Class<T> aClass) {
        return (T) DaggerHttpTriggerFunctionComponent.create().buildFunction();
    }
}
