package com.kumuluz.ee.swagger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kumuluz.ee.common.Extension;
import com.kumuluz.ee.common.config.EeConfig;
import com.kumuluz.ee.common.dependencies.EeComponentDependency;
import com.kumuluz.ee.common.dependencies.EeComponentType;
import com.kumuluz.ee.common.dependencies.EeExtensionDef;
import com.kumuluz.ee.common.wrapper.KumuluzServerWrapper;
import com.kumuluz.ee.jetty.JettyServletServer;
import com.kumuluz.ee.swagger.models.SwaggerConfiguration;
import com.kumuluz.ee.swagger.servlets.ApplicationServletListener;
import io.swagger.jaxrs.config.BeanConfig;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.servlet.ServletContainer;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

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

            LOG.info("Initializing Swagger extension.");

            JettyServletServer server = (JettyServletServer) kumuluzServerWrapper.getServer();

            ApplicationServletListener listener = new ApplicationServletListener();
            server.registerListener(listener);

            InputStream is = getClass().getClassLoader().getResourceAsStream("swagger-configuration.json");
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);

            List<SwaggerConfiguration> swaggerConfigurations = null;
            try {
                swaggerConfigurations = mapper.readValue(is, new TypeReference<List<SwaggerConfiguration>>() {
                });
            } catch (IOException e) {
                LOG.warning("Unable to load swagger configuration. Swagger definition will not be served.");
            }

            for (SwaggerConfiguration config : swaggerConfigurations) {
                BeanConfig beanConfig = new BeanConfig();

                if (config != null) {
                    Map<String, String> parameters = new HashMap<>();
                    parameters.put("jersey.config.server.provider.classnames", "io.swagger.jaxrs.listing.ApiListingResource,io.swagger.jaxrs" +
                            ".listing.SwaggerSerializers");

                    beanConfig.setSchemes(new String[]{"http"});
                    beanConfig.setHost(config.getSwagger().getHost());
                    beanConfig.setBasePath(config.getSwagger().getBasePath());


                    /*if (config.getApplicationClass() != null) {
                        ServletHolder[] registeredServlets = server.getRegisteredServlets();
                        ServletHolder sh = Arrays.stream(registeredServlets).filter(servletHolder -> {
                            try {
                                return servletHolder.getServlet().getServletConfig().getInitParameter("javax.ws.rs.Application").equals
                                        (config.getApplicationClass());
                            } catch (ServletException e) {
                                return false;
                            }
                        }).findFirst().get();

                        try {
                            beanConfig.setResourcePackage(sh.getServlet().getServletConfig().getInitParameter("jersey.config.server" +
                                    ".provider" +
                                    ".packages"));
                        } catch (ServletException e) {
                            LOG.info("Packages not set.");
                        }
                    } else {
                        beanConfig.setResourcePackage(config.getResourcePackagesAsString());
                    }*/

                    if(config.getApplicationClass().equals("com.kumuluz.ee.samples.swagger.v1.CustomerApplication")){
                        beanConfig.setResourcePackage("com.kumuluz.ee.samples.swagger.v1");
                        beanConfig.setScannerId("1");
                        beanConfig.setConfigId("1");
                        parameters.put("swagger.scanner.id", "1");
                        parameters.put("swagger.config.id", "1");
                    }else {
                        beanConfig.setResourcePackage("com.kumuluz.ee.samples.swagger.v2");
                        beanConfig.setScannerId("2");
                        beanConfig.setConfigId("2");
                        parameters.put("swagger.scanner.id", "2");
                        parameters.put("swagger.config.id", "2");
                    }

                    beanConfig.setScan(true);

                    String baseApiPath = StringUtils.strip(beanConfig.getBasePath(), "/");

                    server.registerServlet(ServletContainer.class, "/api-specs/" + baseApiPath + "/*", parameters, 1);
                }

            }

            LOG.info("Swagger extension initialized.");
        }
    }
}
