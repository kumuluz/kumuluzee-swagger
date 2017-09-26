package com.kumuluz.ee.swagger.processor;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.kumuluz.ee.swagger.models.SwaggerConfiguration;
import com.kumuluz.ee.swagger.utils.AnnotationProcessorUtil;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.models.Contact;
import io.swagger.models.License;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Path;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by zvoneg on 26/09/2017.
 */
public class JaxRsSwaggerAnnotationProcessor extends AbstractProcessor {
    private static final Logger LOG = Logger.getLogger(JaxRsSwaggerAnnotationProcessor.class.getName());

    private Set<String> applicationElementNames = new HashSet<>();

    private Filer filer;

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton("*");
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        filer = processingEnv.getFiler();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> elements;

        try {
            Class.forName("javax.ws.rs.core.Application");
        } catch (ClassNotFoundException e) {
            LOG.info("javax.ws.rs.core.Application not found, skipping JAX-RS CORS annotation processing");
            return false;
        }

        elements = roundEnv.getElementsAnnotatedWith(SwaggerDefinition.class);

        com.kumuluz.ee.swagger.models.Swagger swagger = new com.kumuluz.ee.swagger.models.Swagger();

        Element[] elems = elements.toArray(new Element[elements.size()]);
        io.swagger.models.Info info = new io.swagger.models.Info();

        if (elements.size() > 0) {
            List<SwaggerConfiguration> configs = new ArrayList<>();

            for (int i = 0; i < elems.length; i++) {

                SwaggerConfiguration swaggerConfiguration = new SwaggerConfiguration();

                if (elements.size() == 1) {
                    elements = roundEnv.getElementsAnnotatedWith(Path.class);
                    elements.forEach(e -> getElementName(applicationElementNames, e));

                    swaggerConfiguration.setResourcePackages(applicationElementNames);
                } else {
                    swaggerConfiguration.setApplicationClass(elems[i].toString());
                }

                SwaggerDefinition swaggerDefinitionAnnotation = elems[i].getAnnotation(SwaggerDefinition.class);

                info.setTitle(swaggerDefinitionAnnotation.info().title());
                info.setVersion(swaggerDefinitionAnnotation.info().version());

                Contact contact = new Contact();
                contact.setEmail(swaggerDefinitionAnnotation.info().contact().email());
                contact.setName(swaggerDefinitionAnnotation.info().contact().name());
                contact.setUrl(swaggerDefinitionAnnotation.info().contact().url());
                info.setContact(contact);

                info.setDescription(swaggerDefinitionAnnotation.info().description());

                License license = new License();
                license.setName(swaggerDefinitionAnnotation.info().license().name());
                license.setUrl(swaggerDefinitionAnnotation.info().license().url());

                info.setLicense(license);
                info.setTermsOfService(swaggerDefinitionAnnotation.info().termsOfService());

                swagger.setInfo(info);

                ApplicationPath applicationPathAnnotation = elems[0].getAnnotation(ApplicationPath.class);
                if (applicationPathAnnotation != null) {
                    swagger.setBasePath(applicationPathAnnotation.value());
                } else {
                    swagger.setBasePath(swaggerDefinitionAnnotation.basePath());
                }

                swagger.setHost(swaggerDefinitionAnnotation.host());

                swaggerConfiguration.setSwagger(swagger);

                configs.add(swaggerConfiguration);
            }

            try {
                ObjectMapper mapper = new ObjectMapper();
                mapper.enable(SerializationFeature.INDENT_OUTPUT);
                mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
                String jsonOAC = mapper.writeValueAsString(configs);

                AnnotationProcessorUtil.writeFile(jsonOAC, "swagger-configuration.json", filer);
            } catch (IOException e) {
                LOG.warning(e.getMessage());
            }

        }

        return false;
    }

    private void getElementName(Set<String> jaxRsElementNames, Element e) {

        ElementKind elementKind = e.getKind();

        if (elementKind.equals(ElementKind.CLASS)) {
            jaxRsElementNames.add(e.toString().substring(0, e.toString().lastIndexOf(".")));
        }
    }
}
