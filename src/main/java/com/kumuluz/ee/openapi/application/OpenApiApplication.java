package com.kumuluz.ee.openapi.application;

import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import org.glassfish.jersey.server.ResourceConfig;

/**
 * Created by zvoneg on 18/09/2017.
 */
public class OpenApiApplication extends ResourceConfig {

    public void exposeSwagger(String resourcePackage) {
        packages(resourcePackage);

        register(ApiListingResource.class);
        register(SwaggerSerializers.class);

        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setResourcePackage(resourcePackage);
        beanConfig.setScan(true);
    }
}
