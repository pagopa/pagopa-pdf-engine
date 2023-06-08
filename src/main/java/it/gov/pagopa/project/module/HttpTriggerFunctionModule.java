package it.gov.pagopa.project.module;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.helper.ConditionalHelpers;
import dagger.Module;
import dagger.Provides;
import it.gov.pagopa.project.service.GeneratePDFService;
import it.gov.pagopa.project.service.impl.GeneratePDFServiceImpl;

/**
 * Class that makes dependencies available to the container
 */
@Module
public class HttpTriggerFunctionModule {

    /**
     * Constructs a {@link GeneratePDFService} instance
     *
     * @return a {@link GeneratePDFService} instance
     */
    @Provides
    public GeneratePDFService provideGeneratePDFService(){
        return new GeneratePDFServiceImpl(buildHandlebars());
    }

    private Handlebars buildHandlebars() {
        return new Handlebars()
                .registerHelper("eq", ConditionalHelpers.eq)
                .registerHelper("not", ConditionalHelpers.not);
    }
}
