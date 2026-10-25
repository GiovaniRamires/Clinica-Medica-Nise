package br.com.cs.ClinicaMedicaNuse.corsconfig;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {

            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("Content-Type")
                        .maxAge(3600);
            }

            /**
             * Publica a pasta front-end/ em /app/, para abrir a interface em
             * http://localhost:8080/app/index.html (mesma origem da API, sem
             * depender de CORS). O caminho é relativo à pasta onde a aplicação
             * foi iniciada, que é a raiz do projeto no mvn spring-boot:run.
             */
            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                registry.addResourceHandler("/app/**")
                        .addResourceLocations("file:front-end/");
            }

            /** Atalho: a raiz abre a interface. */
            @Override
            public void addViewControllers(ViewControllerRegistry registry) {
                registry.addRedirectViewController("/", "/app/index.html");
            }
        };
    }
}
