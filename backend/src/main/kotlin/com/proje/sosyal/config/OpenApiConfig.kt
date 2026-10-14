package com.proje.sosyal.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * OpenAPI belgesi. Swagger UI ve /v3/api-docs yalnızca dev profilinde açıktır
 * (bkz. application.yml -> springdoc.*.enabled).
 */
@Configuration(proxyBeanMethods = false)
class OpenApiConfig {
    @Bean
    fun openApi(): OpenAPI =
        OpenAPI()
            .info(
                Info()
                    .title("Sosyal Platform API")
                    .version("v1")
                    .description("Android uygulaması ve web sitesi tarafından ortak kullanılan REST API."),
            )
            .components(
                Components().addSecuritySchemes(
                    BEARER_SCHEME,
                    SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"),
                ),
            )

    companion object {
        const val BEARER_SCHEME = "bearerAuth"
    }
}
