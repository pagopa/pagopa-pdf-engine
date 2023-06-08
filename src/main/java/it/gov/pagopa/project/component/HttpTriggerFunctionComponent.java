package it.gov.pagopa.project.component;

import dagger.Component;
import it.gov.pagopa.project.HttpTriggerFunction;
import it.gov.pagopa.project.module.HttpTriggerFunctionModule;

/**
 * Interface that generate the {@link HttpTriggerFunction} instances,
 * injecting dependencies provided by {@link HttpTriggerFunctionModule}
 */
@Component(modules = HttpTriggerFunctionModule.class)
public interface HttpTriggerFunctionComponent {
    HttpTriggerFunction buildFunction();
}
