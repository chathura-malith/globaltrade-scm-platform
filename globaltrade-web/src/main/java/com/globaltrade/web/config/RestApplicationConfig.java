package com.globaltrade.web.config;

import jakarta.annotation.security.DeclareRoles;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

import java.util.HashMap;
import java.util.Map;

@ApplicationPath("/api/v1")
@DeclareRoles({"SYSTEM_ADMIN", "ADMIN", "USER", "VENDOR", "LOGISTICS_COORDINATOR", "CUSTOMS_OFFICER"})
public class RestApplicationConfig extends Application {
    @Override
    public Map<String, Object> getProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("jersey.config.beanValidation.sendErrorInResponse", false);
        return properties;
    }
}