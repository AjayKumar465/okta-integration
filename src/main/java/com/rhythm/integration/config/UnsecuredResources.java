package com.rhythm.integration.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix="unsecured")
public class UnsecuredResources {

	@Setter @Getter
	 private List<String> resources = new ArrayList<>();
}
