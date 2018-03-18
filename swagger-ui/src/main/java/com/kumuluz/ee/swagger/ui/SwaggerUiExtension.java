package com.kumuluz.ee.swagger.ui;

import com.kumuluz.ee.common.Extension;
import com.kumuluz.ee.common.config.EeConfig;
import com.kumuluz.ee.common.dependencies.EeComponentDependency;
import com.kumuluz.ee.common.dependencies.EeComponentType;
import com.kumuluz.ee.common.dependencies.EeExtensionDef;
import com.kumuluz.ee.common.utils.ResourceUtils;
import com.kumuluz.ee.common.wrapper.KumuluzServerWrapper;
import com.kumuluz.ee.configuration.utils.ConfigurationUtil;
import com.kumuluz.ee.jetty.JettyServletServer;
import com.kumuluz.ee.swagger.ui.filters.SwaggerUIFilter;
import io.swagger.annotations.SwaggerDefinition;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.servlet.DefaultServlet;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.net.URL;
import java.util.*;
import java.util.logging.Logger;

/**
 * SwaggerUiExtension class - hook-up servlets to expose api specifications.
 *
 * @author Zvone Gazvoda
 * @since 1.0.0
 */
@EeExtensionDef(name = "Swagger-UI", group = "SWAGGER_UI")
@EeComponentDependency(EeComponentType.JAX_RS)
public class SwaggerUiExtension implements Extension {

    private static final Logger LOG = Logger.getLogger(SwaggerUiExtension.class.getName());

    @Override
    public void load() {

    }

    @Override
    public void init(KumuluzServerWrapper kumuluzServerWrapper, EeConfig eeConfig) {

        ConfigurationUtil configurationUtil = ConfigurationUtil.getInstance();

        if (kumuluzServerWrapper.getServer() instanceof JettyServletServer) {

            LOG.info("Initializing Swagger UI extension.");

            JettyServletServer server = (JettyServletServer) kumuluzServerWrapper.getServer();

            try {
                Class.forName("com.kumuluz.ee.swagger.SwaggerExtension");
            } catch (ClassNotFoundException e) {
                LOG.severe("Unable to find Swagger extension, Swagger UI will not be initialized: " + e.getMessage());
                return;
            }

            List<Application> applications = new ArrayList<>();
            ServiceLoader.load(Application.class).forEach(applications::add);

            if (applications.size() == 1) {
                Application application = applications.get(0);

                Class<?> applicationClass = application.getClass();
                if (targetClassIsProxied(applicationClass)) {
                    applicationClass = applicationClass.getSuperclass();
                }

                String applicationPath = "";
                ApplicationPath applicationPathAnnotation = applicationClass.getAnnotation(ApplicationPath.class);
                SwaggerDefinition swaggerAnnotation = applicationClass.getAnnotation(SwaggerDefinition.class);

                String serverUrl = "localhost";
                Integer port = null;

                if (eeConfig.getServer().getHttp() != null) {
                    port = eeConfig.getServer().getHttp().getPort();
                    serverUrl = "http://" + serverUrl;
                } else {
                    port = eeConfig.getServer().getHttps().getPort();
                    serverUrl = "https://" + serverUrl;
                }

                serverUrl += (port != null ? ":" + port.toString() : "");

                if (swaggerAnnotation != null) {
                    if (!swaggerAnnotation.host().equals("")) {
                        serverUrl = swaggerAnnotation.host();
                    }

                    List<SwaggerDefinition.Scheme> schemas = Arrays.asList(swaggerAnnotation.schemes());

                    if (schemas.contains(SwaggerDefinition.Scheme.DEFAULT) || schemas.contains(SwaggerDefinition.Scheme.HTTP)) {
                        serverUrl = "http://" + serverUrl;
                    } else if (schemas.contains(SwaggerDefinition.Scheme.HTTPS)) {
                        serverUrl = "https://" + serverUrl;
                    }
                }

                if (applicationPathAnnotation != null) {
                    applicationPath = applicationPathAnnotation.value();
                } else {
                    if (swaggerAnnotation != null) {
                        applicationPath = swaggerAnnotation.basePath();
                    }
                }

                applicationPath = StringUtils.strip(applicationPath, "/");

                Map<String, String> swaggerUiParams = new HashMap<>();
                URL webApp = ResourceUtils.class.getClassLoader().getResource("swagger-ui");

                if (webApp != null && configurationUtil.getBoolean("kumuluzee.swagger.ui.enabled").orElse(true) && configurationUtil
                        .getBoolean("kumuluzee.swagger.spec.enabled").orElse(true)) {

                    swaggerUiParams.put("resourceBase", webApp.toString());
                    server.registerServlet(DefaultServlet.class, "/api-specs/ui/*", swaggerUiParams, 1);

                    Map<String, String> swaggerUiFilterParams = new HashMap<>();

                    swaggerUiFilterParams.put("url", serverUrl + "/api-specs/" + applicationPath + "/swagger.json");
                    server.registerFilter(SwaggerUIFilter.class, "/api-specs/ui/*", swaggerUiFilterParams);
                } else {
                    LOG.warning("Unable to find Swagger-UI artifacts or Swagger UI is disabled.");
                }
            }
        }
    }

    private boolean targetClassIsProxied(Class targetClass) {
        return targetClass.getCanonicalName().contains("$Proxy");
    }
}
