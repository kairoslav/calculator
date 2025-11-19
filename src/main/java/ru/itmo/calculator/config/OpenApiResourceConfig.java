package ru.itmo.calculator.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class OpenApiResourceConfig implements WebMvcConfigurer {

    private static final String OPENAPI_RESOURCE_PATH = "classpath:/openapi/";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/openapi/**").addResourceLocations(OPENAPI_RESOURCE_PATH);
    }
}
