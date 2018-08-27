package com.kumuluz.ee.swagger.models;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.HashSet;
import java.util.Set;

/**
 * SwaggerConfiguration class.
 *
 * @author Zvone Gazvoda
 * @since 1.0.0
 */
public class SwaggerConfiguration {
    private Set<String> resourcePackages;
    private Swagger swagger;
    private String applicationClass;
    private String swaggerDefinitionClass;

    public Set<String> getResourcePackages() {
        if (resourcePackages == null) {
            resourcePackages = new HashSet<>();
        }
        return resourcePackages;
    }

    public void setResourcePackages(Set<String> resourcePackages) {
        this.resourcePackages = resourcePackages;
    }

    public Swagger getSwagger() {
        return swagger;
    }

    public void setSwagger(Swagger swagger) {
        this.swagger = swagger;
    }

    @JsonIgnore
    public String getResourcePackagesAsString() {
        String packages = "";

        for (String resourcePackage : this.getResourcePackages()) {
            packages += "," + resourcePackage;
        }

        return packages.substring(1);
    }

    public String getApplicationClass() {
        return applicationClass;
    }

    public void setApplicationClass(String applicationClass) {
        this.applicationClass = applicationClass;
    }

    public String getSwaggerDefinitionClass() {
        return swaggerDefinitionClass;
    }

    public void setSwaggerDefinitionClass(String swaggerDefinitionClass) {
        this.swaggerDefinitionClass = swaggerDefinitionClass;
    }
}
