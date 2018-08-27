package com.kumuluz.ee.swagger;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kumuluz.ee.common.Extension;
import com.kumuluz.ee.common.config.EeConfig;
import com.kumuluz.ee.common.dependencies.EeComponentDependency;
import com.kumuluz.ee.common.dependencies.EeComponentType;
import com.kumuluz.ee.common.dependencies.EeExtensionDef;
import com.kumuluz.ee.common.wrapper.KumuluzServerWrapper;
import com.kumuluz.ee.configuration.utils.ConfigurationUtil;
import com.kumuluz.ee.jetty.JettyServletServer;
import com.kumuluz.ee.swagger.models.SwaggerConfiguration;
import com.kumuluz.ee.swagger.servlets.ApiListingServlet;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.models.Scheme;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * SwaggerExtension class.
 *
 * @author Zvone Gazvoda
 * @since 1.0.0
 */
@EeExtensionDef(name = "Swagger", group = "SWAGGER")
@EeComponentDependency(EeComponentType.JAX_RS)
public class SwaggerExtension implements Extension {

    private static final Logger LOG = Logger.getLogger(SwaggerExtension.class.getName());

    @Override
    public void load() {
    }

    @Override
    public void init(KumuluzServerWrapper kumuluzServerWrapper, EeConfig eeConfig) {
        if (kumuluzServerWrapper.getServer() instanceof JettyServletServer) {

            ConfigurationUtil configurationUtil = ConfigurationUtil.getInstance();

            if (configurationUtil.getBoolean("kumuluzee.swagger.enabled").orElse(true)) {

                LOG.info("Initializing Swagger extension.");

                JettyServletServer server = (JettyServletServer) kumuluzServerWrapper.getServer();

                List<Application> applications = new ArrayList<>();
                ServiceLoader.load(Application.class).forEach(applications::add);

                ObjectMapper mapper = new ObjectMapper();
                mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);

                if (applications.size() == 1) {
                    Application application = applications.get(0);

                    Class<?> applicationClass = application.getClass();
                    if (targetClassIsProxied(applicationClass)) {
                        applicationClass = applicationClass.getSuperclass();
                    }

                    String applicationPath = "";
                    ApplicationPath applicationPathAnnotation = applicationClass.getAnnotation(ApplicationPath.class);

                    if (applicationPathAnnotation != null) {
                        applicationPath = applicationPathAnnotation.value();
                    }

                    applicationPath = StringUtils.strip(applicationPath, "/");

                    InputStream is = null;
                    if (applicationPath.equals("")) {
                        is = getClass().getClassLoader().getResourceAsStream("api-specs/swagger-configuration.json");
                    } else {
                        is = getClass().getClassLoader().getResourceAsStream("api-specs/" + applicationPath +
                                "/swagger-configuration.json");
                    }

                    SwaggerConfiguration swaggerConfiguration = null;
                    try {
                        swaggerConfiguration = mapper.readValue(is, SwaggerConfiguration.class);
                    } catch (IOException e) {
                        LOG.warning("Unable to load swagger configuration. Swagger specification will not be served.");
                        return;
                    }

                    BeanConfig beanConfig = new BeanConfig();

                    Map<String, String> parameters = new HashMap<>();

                    URL baseUrl = null;
                    try {
                        baseUrl = new URL(eeConfig.getServer().getBaseUrl());

                        beanConfig.setSchemes(new String[]{baseUrl.getProtocol()});
                        beanConfig.setHost(baseUrl.getHost());
                        beanConfig.setBasePath(baseUrl.getPath());
                        
                    } catch (MalformedURLException e) {
                        LOG.warning("kumuluzee.server.base-url not set. Using default values.");
                    }

                    if (beanConfig.getSchemes() == null || beanConfig.getSchemes().length == 0) {
                        beanConfig.setSchemes(swaggerConfiguration.getSwagger().getSchemes().stream().map(Scheme::toValue).toArray
                                (String[]::new));
                    }
                    if (beanConfig.getHost() != null) {
                        beanConfig.setHost(swaggerConfiguration.getSwagger().getHost());
                    }
                    if (beanConfig.getBasePath() != null) {
                        beanConfig.setBasePath(swaggerConfiguration.getSwagger().getBasePath());
                    }

                    if (applications.size() == 1) {
                        beanConfig.setResourcePackage(swaggerConfiguration.getResourcePackagesAsString());
                    } else {

                        Set<Class<?>> resources = application.getClasses();
                        Set<String> resourcePackages = resources.stream().map(r -> r.getPackage().getName()).collect(Collectors
                                .toSet());

                        String packages = StringUtils.join(resourcePackages, ",");

                        beanConfig.setResourcePackage(packages);
                    }

                    beanConfig.getSwagger().setInfo(swaggerConfiguration.getSwagger().getInfo());
                    beanConfig.setScannerId(applicationPath);
                    beanConfig.setPrettyPrint(true);
                    beanConfig.setConfigId(applicationPath);
                    parameters.put("swagger.scanner.id", applicationPath);
                    parameters.put("swagger.config.id", applicationPath);
                    beanConfig.setScan(true);

                    if (applicationPath.equals("")) {
                        server.registerServlet(ApiListingServlet.class, "/api-specs/*", parameters, 1);
                    } else {
                        server.registerServlet(ApiListingServlet.class, "/api-specs/" + applicationPath + "/*", parameters, 1);
                    }

                    LOG.info("Swagger extension initialized.");

                } else {
                    LOG.warning("Multiple JAX-RS applications not supported. Swagger definitions will not be served.");
                }
            }
        }
    }

    private boolean targetClassIsProxied(Class targetClass) {
        return targetClass.getCanonicalName().contains("$Proxy");
    }
}
