package com.rhythm.integration.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtBearerTokenAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.OpaqueTokenAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.introspection.NimbusOpaqueTokenIntrospector;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
@ConfigurationProperties(prefix = "unsecured")
@EnableWebSecurity
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

	@Autowired
	private UnsecuredResources unsecuredResources;

	@Override
	public void configure(HttpSecurity http) throws Exception {
		http.authorizeRequests().antMatchers(unsecuredResources.getResources().toArray(new String[unsecuredResources.getResources().size()])).permitAll().antMatchers("/**/*")
				.authenticated().and().sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and().securityContext();

		http.oauth2ResourceServer().authenticationManagerResolver(customAuthenticationManager());
	}

	// Inject the `OAuth2ClientProperties` we configured in the previous step
	@Autowired
	private OAuth2ClientProperties oAuth2ClientProperties;

	AuthenticationManagerResolver<HttpServletRequest> customAuthenticationManager() {
		LinkedHashMap<RequestMatcher, AuthenticationManager> authenticationManagers = new LinkedHashMap<>();

		// USE JWT tokens (locally validated) to validate HEAD, TRACE, and OPTIONS
		// requests
		List<String> readMethod = Arrays.asList("HEAD", "TRACE", "OPTIONS");
		RequestMatcher readMethodRequestMatcher = request -> readMethod.contains(request.getMethod());
		authenticationManagers.put(readMethodRequestMatcher, jwt());

		// all other requests will use opaque tokens (remotely validated)
		RequestMatchingAuthenticationManagerResolver authenticationManagerResolver = new RequestMatchingAuthenticationManagerResolver(authenticationManagers);

		// Use opaque tokens (remotely validated) for all other requests
		authenticationManagerResolver.setDefaultAuthenticationManager(opaque());
		return authenticationManagerResolver;
	}

	// Mimic the default configuration for JWT validation.
	AuthenticationManager jwt() {
		// this is the keys endpoint for okta
		String issuer = oAuth2ClientProperties.getProvider().get("okta").getIssuerUri();
		String jwkSetUri = issuer + "/v1/keys";

		NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

		// okta recommends validating the `iss` and `aud` claims
		// see:
		// https://developer.okta.com/docs/guides/validate-access-tokens/java/overview/
		List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
		validators.add(new JwtTimestampValidator());
		// Add validation of the issuer claim
		validators.add(new JwtIssuerValidator(issuer));
		validators.add(token -> {
			Set<String> expectedAudience = new HashSet<>();
			// Add validation of the audience claim
			expectedAudience.add("api://default");
			// For new Okta orgs, the default audience is `api://default`,
			// if you have changed this from the default update this value
			return !Collections.disjoint(token.getAudience(), expectedAudience) ? OAuth2TokenValidatorResult.success()
					: OAuth2TokenValidatorResult.failure(new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST, "This aud claim is not equal to the configured audience",
							"https://tools.ietf.org/html/rfc6750#section-3.1"));
		});
		OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(validators);
		jwtDecoder.setJwtValidator(validator);

		JwtAuthenticationProvider authenticationProvider = new JwtAuthenticationProvider(jwtDecoder);
		authenticationProvider.setJwtAuthenticationConverter(new JwtBearerTokenAuthenticationConverter());
		return authenticationProvider::authenticate;
	}

	// Mimic the default configuration for opaque token validation
	AuthenticationManager opaque() {
		String issuer = oAuth2ClientProperties.getProvider().get("okta").getIssuerUri();
		String introspectionUri = issuer + "/v1/introspect";

		// The default opaque token logic
		OAuth2ClientProperties.Registration oktaRegistration = oAuth2ClientProperties.getRegistration().get("okta");
		OpaqueTokenIntrospector introspectionClient = new NimbusOpaqueTokenIntrospector(introspectionUri, oktaRegistration.getClientId(), oktaRegistration.getClientSecret());
		return new OpaqueTokenAuthenticationProvider(introspectionClient)::authenticate;
	}
}
