package com.rhythm.integration.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

//import com.google.common.base.Predicates;

import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SwaggerConfig {
	
    @Bean
    public Docket customDocket(){
    	 return new Docket(DocumentationType.SWAGGER_2)
    	            .forCodeGeneration(Boolean.TRUE)
    	            .select()
    	            .apis(RequestHandlerSelectors.basePackage("com.rhythm"))
    	            .paths(PathSelectors.any())
    	           // .paths(Predicates.not(PathSelectors.regex("/logout.*")))
    	            .build()
    	            .apiInfo(apiInfo());
    }
    
    

    @SuppressWarnings("deprecation")
	private ApiInfo apiInfo() {
        ApiInfo apiInfo = new ApiInfo(
                "PersonSearchApplication",
                "An application to search Person from a Person repository by personId",
                "PersonSearchApplication v1",
                "Terms of service",
                "hendisantika@gmail.com",
                "License of API",
                "https://swagger.io/docs/");
        return apiInfo;
    }
}