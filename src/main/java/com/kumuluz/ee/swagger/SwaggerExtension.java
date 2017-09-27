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
import io.swagger.jaxrs.config.BeanConfig;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.servlet.ServletContainer;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
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

            for (int i = 0; i < swaggerConfigurations.size(); i++) {
                SwaggerConfiguration config = swaggerConfigurations.get(i);

                BeanConfig beanConfig = new BeanConfig();

                if (config != null) {
                    Map<String, String> parameters = new HashMap<>();
                    parameters.put("jersey.config.server.provider.classnames", "io.swagger.jaxrs.listing.ApiListingResource,io.swagger" +
                            ".jaxrs" +
                            ".listing.SwaggerSerializers");

                    beanConfig.setSchemes(new String[]{"http"});
                    beanConfig.setHost(config.getSwagger().getHost());
                    beanConfig.setBasePath(config.getSwagger().getBasePath());
                    beanConfig.setResourcePackage(config.getResourcePackagesAsString());
                    beanConfig.setScannerId(String.valueOf(i));
                    beanConfig.setConfigId(String.valueOf(i));
                    parameters.put("swagger.scanner.id", String.valueOf(i));
                    parameters.put("swagger.config.id", String.valueOf(i));

                    beanConfig.setScan(true);

                    String baseApiPath = StringUtils.strip(beanConfig.getBasePath(), "/");

                    server.registerServlet(ServletContainer.class, "/api-specs/" + baseApiPath + "/*", parameters, 1);
                }

            }

            LOG.info("Swagger extension initialized.");
        }
    }
}
