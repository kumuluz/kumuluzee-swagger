package com.kumuluz.ee.swagger.processor;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.kumuluz.ee.swagger.models.Swagger;
import com.kumuluz.ee.swagger.models.SwaggerConfiguration;
import com.kumuluz.ee.swagger.utils.AnnotationProcessorUtil;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.models.Contact;
import io.swagger.models.License;
import io.swagger.models.Scheme;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Types;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Path;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * JaxRsSwaggerAnnotationProcessor annotation processor class.
 *
 * @author Zvone Gazvoda
 * @since 1.0.0
 */
public class JaxRsSwaggerAnnotationProcessor extends AbstractProcessor {
    private static final Logger LOG = Logger.getLogger(JaxRsSwaggerAnnotationProcessor.class.getName());

    private Set<String> applicationElementNames = new HashSet<>();
    private Set<String> resourceElementNames = new HashSet<>();
    private Set<String> swaggerElementNames = new HashSet<>();

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
        Set<? extends Element> swaggerElements;
        Set<? extends Element> jaxRSElements;
        Set<? extends Element> resourceElements;

        try {
            Class.forName("javax.ws.rs.core.Application");
        } catch (ClassNotFoundException e) {
            LOG.info("javax.ws.rs.core.Application not found, skipping JAX-RS Swagger annotation processing");
            return false;
        }

        resourceElements = roundEnv.getElementsAnnotatedWith(Path.class);
        resourceElements.forEach(e -> getElementPackage(resourceElementNames, e));

        swaggerElements = roundEnv.getElementsAnnotatedWith(SwaggerDefinition.class);
        swaggerElements.forEach(e -> getElementName(swaggerElementNames, e, processingEnv.getTypeUtils()));

        jaxRSElements = roundEnv.getElementsAnnotatedWith(ApplicationPath.class);
        jaxRSElements.forEach(e -> getElementName(applicationElementNames, e, processingEnv
                .getTypeUtils()));

        SwaggerConfiguration config = new SwaggerConfiguration();

        if (jaxRSElements.size() == 1) {
            Swagger swagger = new Swagger();
            io.swagger.models.Info info = new io.swagger.models.Info();

            for (Element element : swaggerElements) {

                SwaggerDefinition swaggerDefinitionAnnotation = element.getAnnotation(SwaggerDefinition.class);

                if (swaggerDefinitionAnnotation != null) {

                    config.setSwaggerDefinitionClass(element.toString());

                    info.setTitle(swaggerDefinitionAnnotation.info().title());
                    info.setVersion(swaggerDefinitionAnnotation.info().version());

                    Contact contact = null;
                    if (!swaggerDefinitionAnnotation.info().contact().email().equals("")) {
                        contact = new Contact();
                        contact.setEmail(swaggerDefinitionAnnotation.info().contact().email());
                    }
                    if (!swaggerDefinitionAnnotation.info().contact().name().equals("")) {
                        if (contact == null) contact = new Contact();
                        contact.setName(swaggerDefinitionAnnotation.info().contact().name());
                    }
                    if (!swaggerDefinitionAnnotation.info().contact().url().equals("")) {
                        if (contact == null) contact = new Contact();
                        contact.setUrl(swaggerDefinitionAnnotation.info().contact().url());
                    }
                    info.setContact(contact);

                    if (!swaggerDefinitionAnnotation.info().description().equals("")) {
                        info.setDescription(swaggerDefinitionAnnotation.info().description());
                    }

                    License license = null;
                    if (!swaggerDefinitionAnnotation.info().license().name().equals("")) {
                        license = new License();
                        license.setName(swaggerDefinitionAnnotation.info().license().name());
                    }
                    if (!swaggerDefinitionAnnotation.info().license().url().equals("")) {
                        if (license == null) license = new License();
                        license.setUrl(swaggerDefinitionAnnotation.info().license().url());
                    }

                    info.setLicense(license);

                    if (!swaggerDefinitionAnnotation.info().termsOfService().equals("")) {
                        info.setTermsOfService(swaggerDefinitionAnnotation.info().termsOfService());
                    }

                    swagger.setInfo(info);

                    List<Scheme> schemes = Arrays.stream(swaggerDefinitionAnnotation.schemes()).map(s -> {
                        Scheme scheme = null;

                        switch (s.ordinal()) {
                            case 1:
                                scheme = Scheme.HTTP;
                                break;
                            case 2:
                                scheme = Scheme.HTTPS;
                                break;
                            case 3:
                                scheme = Scheme.WS;
                                break;
                            case 4:
                                scheme = Scheme.WSS;
                                break;
                            default:
                                scheme = Scheme.HTTP;
                                break;
                        }

                        return scheme;
                    }).collect(Collectors.toList());

                    if (schemes.size() == 0) {
                        schemes.add(Scheme.HTTP);
                    }

                    swagger.setSchemes(schemes);
                    swagger.setHost(swaggerDefinitionAnnotation.host());

                    config.setSwagger(swagger);

                    if (applicationElementNames.size() == 1) {
                        Set<String> swaggerElementPackages = new HashSet<>();

                        for (String ele : swaggerElementNames) {
                            swaggerElementPackages.add(ele.substring(0, ele.lastIndexOf('.')));
                        }

                        swaggerElementPackages.addAll(resourceElementNames);

                        config.getResourcePackages().addAll(swaggerElementPackages);
                    }
                }
            }

            for (Element element : jaxRSElements) {

                ApplicationPath applicationPathAnnotation = element.getAnnotation(ApplicationPath.class);

                if (applicationPathAnnotation != null) {
                    config.setApplicationClass(element.toString());
                    swagger.setBasePath(applicationPathAnnotation.value());
                }

                if (swagger.getBasePath() == null || swagger.getBasePath().equals("")) {
                    LOG.warning("Unable to obtain API Base path. Provide @ApplicationPath.");
                }
            }

            try {
                ObjectMapper mapper = new ObjectMapper();
                mapper.enable(SerializationFeature.INDENT_OUTPUT);
                mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
                String jsonOAC = mapper.writeValueAsString(config);

                String path = swagger.getBasePath();

                path = StringUtils.strip(path, "/");

                if ("".equals(path)) {
                    AnnotationProcessorUtil.writeFile(jsonOAC, "api-specs/swagger-configuration.json", filer);
                } else {
                    AnnotationProcessorUtil.writeFile(jsonOAC, "api-specs/" + path + "/swagger-configuration.json", filer);
                }
            } catch (IOException e) {
                LOG.warning(e.getMessage());
            }

            try {
                AnnotationProcessorUtil.writeFileSet(applicationElementNames, "META-INF/services/javax.ws.rs.core.Application", filer);
            } catch (IOException e) {
                LOG.warning(e.getMessage());
            }
        }

        return false;
    }

    private void getElementPackage(Set<String> jaxRsElementNames, Element e) {

        ElementKind elementKind = e.getKind();

        if (elementKind.equals(ElementKind.CLASS)) {
            jaxRsElementNames.add(e.toString().substring(0, e.toString().lastIndexOf(".")));
        }
    }

    private void getElementName(Set<String> elementNames, Element e, Types types) {

        ElementKind elementKind = e.getKind();

        if (elementKind.equals(ElementKind.CLASS)) {
            elementNames.add(e.toString());
        }
    }
}
