package com.ytt.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / OpenAPI 接口文档配置
 * 访问地址：/api/swagger-ui.html
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI yttOpenAPI() {
        final String securitySchemeName = "Bearer";
        return new OpenAPI()
                .info(new Info()
                        .title("研题通·考研408刷题训练系统 API")
                        .description("前后端分离架构 RESTful 接口文档，统一响应格式 {code, message, data}")
                        .version("1.0.0")
                        .contact(new Contact().name("ytt")))
                // JWT 认证配置：文档页面可直接填写 Token 调试
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components().addSecuritySchemes(securitySchemeName,
                        new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
