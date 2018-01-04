package com.kumuluz.ee.swagger.servlets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.swagger.config.Scanner;
import io.swagger.config.SwaggerConfig;
import io.swagger.jaxrs.Reader;
import io.swagger.jaxrs.config.JaxrsScanner;
import io.swagger.jaxrs.config.ReaderConfigUtils;
import io.swagger.jaxrs.config.SwaggerContextService;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import io.swagger.models.Swagger;
import io.swagger.util.Json;
import io.swagger.util.Yaml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.*;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * ApiListingServlet class.
 *
 * @author Zvone Gazvoda
 * @since 1.0.0
 */
@WebServlet("/swagger.*")
public class ApiListingServlet extends HttpServlet {

    private static volatile boolean initialized = false;
    private static volatile ConcurrentMap<String, Boolean> initializedScanner = new ConcurrentHashMap();
    private static volatile ConcurrentMap<String, Boolean> initializedConfig = new ConcurrentHashMap();
    private static Logger LOGGER = LoggerFactory.getLogger(ApiListingServlet.class);

    private static synchronized Swagger scan(Application app, ServletContext context, ServletConfig sc, String basePath) {
        Swagger swagger = null;
        SwaggerContextService ctxService = (new SwaggerContextService()).withServletConfig(sc).withBasePath(basePath);
        Scanner scanner = ctxService.getScanner();
        if (scanner != null) {
            SwaggerSerializers.setPrettyPrint(scanner.getPrettyPrint());
            swagger = (new SwaggerContextService()).withServletConfig(sc).withBasePath(basePath).getSwagger();
            Set classes;
            if (scanner instanceof JaxrsScanner) {
                JaxrsScanner jaxrsScanner = (JaxrsScanner) scanner;
                classes = jaxrsScanner.classesFromContext(app, sc);
            } else {
                classes = scanner.classes();
            }

            if (classes != null) {
                Reader reader = new Reader(swagger, ReaderConfigUtils.getReaderConfig(context));
                swagger = reader.read(classes);
                if (scanner instanceof SwaggerConfig) {
                    swagger = ((SwaggerConfig) scanner).configure(swagger);
                } else {
                    SwaggerConfig swaggerConfig = ctxService.getConfig();
                    if (swaggerConfig != null) {
                        LOGGER.debug("configuring swagger with " + swaggerConfig);
                        swaggerConfig.configure(swagger);
                    } else {
                        LOGGER.debug("no configurator");
                    }
                }

                (new SwaggerContextService()).withServletConfig(sc).withBasePath(basePath).updateSwagger(swagger);
            }
        }

        if (SwaggerContextService.isScannerIdInitParamDefined(sc)) {
            initializedScanner.put(sc.getServletName() + "_" + SwaggerContextService.getScannerIdFromInitParam(sc), Boolean.valueOf(true));
        } else if (SwaggerContextService.isConfigIdInitParamDefined(sc)) {
            initializedConfig.put(sc.getServletName() + "_" + SwaggerContextService.getConfigIdFromInitParam(sc), Boolean.valueOf(true));
        } else if (SwaggerContextService.isUsePathBasedConfigInitParamDefined(sc)) {
            initializedConfig.put(sc.getServletName() + "_" + ctxService.getBasePath(), Boolean.valueOf(true));
        } else {
            initialized = true;
        }

        return swagger;
    }

    private static Map<String, List<String>> getQueryParams(MultivaluedMap<String, String> params) {
        Map<String, List<String>> output = new HashMap();
        if (params != null) {
            Iterator i$ = params.keySet().iterator();

            while (i$.hasNext()) {
                String key = (String) i$.next();
                List<String> values = (List) params.get(key);
                output.put(key, values);
            }
        }

        return output;
    }

    private static Map<String, String> getCookies(HttpHeaders headers) {
        Map<String, String> output = new HashMap();
        if (headers != null) {
            Iterator i$ = headers.getCookies().keySet().iterator();

            while (i$.hasNext()) {
                String key = (String) i$.next();
                Cookie cookie = (Cookie) headers.getCookies().get(key);
                output.put(key, cookie.getValue());
            }
        }

        return output;
    }

    private static Map<String, List<String>> getHeaders(HttpHeaders headers) {
        Map<String, List<String>> output = new HashMap();
        if (headers != null) {
            Iterator i$ = headers.getRequestHeaders().keySet().iterator();

            while (i$.hasNext()) {
                String key = (String) i$.next();
                List<String> values = (List) headers.getRequestHeaders().get(key);
                output.put(key, values);
            }
        }

        return output;
    }

    private static String getBasePath(UriInfo uriInfo) {
        return uriInfo != null ? uriInfo.getBaseUri().getPath() : "/";
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws JsonProcessingException {

        String type = "json";
        String requestedType = request.getPathInfo().split("\\.")[1];

        if (requestedType != null) {
            type = requestedType;
        }

        int index = request.getServletPath().indexOf("api-specs");
        String applicationBasePath = request.getServletPath().substring(index + 10);

        try {
            PrintWriter out = response.getWriter();

            String specification = null;
            if (type.trim().equalsIgnoreCase("json")) {
                response.setContentType("application/json");

                specification = this.getListingJsonResponse(null, this.getServletContext(), this.getServletConfig(), null,
                        applicationBasePath);
            } else if (type.trim().equalsIgnoreCase("yaml")) {
                response.setContentType("application/yaml");

                specification = this.getListingYamlResponse(null, this.getServletContext(), this.getServletConfig(), null,
                        applicationBasePath);
            }

            out.print(specification);

            response.setStatus(HttpServletResponse.SC_OK);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private Swagger process(Application app, ServletContext servletContext, ServletConfig sc, HttpHeaders headers, String basePath) {
        SwaggerContextService ctxService = (new SwaggerContextService()).withServletConfig(sc).withBasePath(basePath);
        Swagger swagger = ctxService.getSwagger();
        Class var8 = ApiListingResource.class;
        synchronized (ApiListingResource.class) {
            if (SwaggerContextService.isScannerIdInitParamDefined(sc)) {
                if (!initializedScanner.containsKey(sc.getServletName() + "_" + SwaggerContextService.getScannerIdFromInitParam(sc))) {
                    swagger = scan(app, servletContext, sc, basePath);
                }
            } else if (SwaggerContextService.isConfigIdInitParamDefined(sc)) {
                if (!initializedConfig.containsKey(sc.getServletName() + "_" + SwaggerContextService.getConfigIdFromInitParam(sc))) {
                    swagger = scan(app, servletContext, sc, basePath);
                }
            } else if (SwaggerContextService.isUsePathBasedConfigInitParamDefined(sc)) {
                if (!initializedConfig.containsKey(sc.getServletName() + "_" + ctxService.getBasePath())) {
                    swagger = scan(app, servletContext, sc, basePath);
                }
            } else if (!initialized) {
                swagger = scan(app, servletContext, sc, basePath);
            }
        }

        /*if (swagger != null) {
            SwaggerSpecFilter filterImpl = FilterFactory.getFilter();
            if (filterImpl != null) {
                SpecFilter f = new SpecFilter();
                swagger = f.filter(swagger, filterImpl, getQueryParams(uriInfo.getQueryParameters()), getCookies(headers), getHeaders
                        (headers));
            }
        }*/

        return swagger;
    }

    private String getListingYamlResponse(Application app, ServletContext servletContext, ServletConfig servletConfig, HttpHeaders
            headers, String basePath) {
        Swagger swagger = this.process(app, servletContext, servletConfig, headers, basePath);

        try {
            if (swagger != null) {
                String yaml = Yaml.mapper().writeValueAsString(swagger);
                StringBuilder b = new StringBuilder();
                String[] parts = yaml.split("\n");
                String[] arr$ = parts;
                int len$ = parts.length;

                for (int i$ = 0; i$ < len$; ++i$) {
                    String part = arr$[i$];
                    b.append(part);
                    b.append("\n");
                }

                return b.toString();
            }
        } catch (Exception var14) {
            var14.printStackTrace();
        }

        return null;
    }

    private String getListingJsonResponse(Application app, ServletContext servletContext, ServletConfig servletConfig, HttpHeaders
            headers, String basePath) throws JsonProcessingException {
        Swagger swagger = this.process(app, servletContext, servletConfig, headers, basePath);
        return Json.mapper().enable(SerializationFeature.INDENT_OUTPUT).writeValueAsString(swagger);
    }
}
