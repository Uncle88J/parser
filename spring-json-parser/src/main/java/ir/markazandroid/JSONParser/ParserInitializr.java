package ir.markazandroid.JSONParser;

import ir.markazandroid.JSONParser.annotations.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

/**
 * Created by Ali on 4/24/2019.
 */
public class ParserInitializr {

    private final String PARSER_INITIALIZR_TAG = "ParserInitializr: ";
    private static final Logger logger = LoggerFactory.getLogger(ParserInitializr.class);

    private Parser parser;

    public ParserInitializr(Parser parser) {
        this.parser = parser;
    }

    public void findAnnotatedClasses() {
        logger.info("Finding JSON class candidates");
        ClassPathScanningCandidateComponentProvider provider = createComponentScanner();
        for (BeanDefinition beanDef : provider.findCandidateComponents("ir.markazandroid.StocksSignal")) {
            addClass(beanDef);
        }
    }

    private ClassPathScanningCandidateComponentProvider createComponentScanner() {
        // Don't pull default filters (@Component, etc.):
        ClassPathScanningCandidateComponentProvider provider
                = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new AnnotationTypeFilter(JSON.class));
        return provider;
    }

    private void addClass(BeanDefinition beanDef) {
        try {
            Class<?> cl = Class.forName(beanDef.getBeanClassName());
            if (cl.isAnnotationPresent(JSON.class)) {
                parser.addWithSuperClasses(cl);
                logger.info("Added class= " + cl.getName());
            }
        } catch (Exception e) {
            logger.error("Got exception: " + e.getMessage(), e);
        }
    }
}
