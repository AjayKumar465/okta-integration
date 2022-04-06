package com.rhythm.integration.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.*;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class OAuthClientConfiguration {

	@Value("${spring.security.oauth2.client.provider.okta.token-uri}") String tokenUri;
	@Value("${spring.security.oauth2.client.registration.okta.client-id}") String clientId;
	@Value("${spring.security.oauth2.client.registration.okta.client-secret}") String clientSecret;
	@Value("${spring.security.oauth2.client.registration.okta.scope}") String scope;
	@Value("${spring.security.oauth2.client.registration.okta.authorization-grant-type}") String authorizationGrantType;
	
	@Bean
	ReactiveClientRegistrationRepository clientRegistrations() {
		ClientRegistration registration = oktaClientRegistration();
		return new InMemoryReactiveClientRegistrationRepository(registration);
	}

	@Bean
	ClientRegistration oktaClientRegistration() {
		return ClientRegistration.withRegistrationId("okta").tokenUri(tokenUri).clientId(clientId).clientSecret(clientSecret).scope(scope)
				.authorizationGrantType(new AuthorizationGrantType(authorizationGrantType)).build();
	}

	@Bean
	WebClient webClient(ReactiveClientRegistrationRepository clientRegistrations) {
		InMemoryReactiveOAuth2AuthorizedClientService clientService = new InMemoryReactiveOAuth2AuthorizedClientService(clientRegistrations);
		AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager authorizedClientManager = new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(clientRegistrations,
				clientService);
		ServerOAuth2AuthorizedClientExchangeFilterFunction oauth = new ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
		oauth.setDefaultClientRegistrationId("okta");
		return WebClient.builder().filter(oauth).build();

	}

	@Bean
	public ClientRegistrationRepository clientRegistrationRepository(ClientRegistration oktaClientRegistration) {
		return new InMemoryClientRegistrationRepository(oktaClientRegistration);
	}

	// Create the authorized client service
	@Bean
	public OAuth2AuthorizedClientService auth2AuthorizedClientService(ClientRegistrationRepository clientRegistrationRepository) {
		return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
	}

	// Create the authorized client manager and service manager using the
	// beans created and configured above
	@Bean
	public AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientServiceAndManager(ClientRegistrationRepository clientRegistrationRepository,
			OAuth2AuthorizedClientService authorizedClientService) {

		OAuth2AuthorizedClientProvider authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build();

		AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository,
				authorizedClientService);
		authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

		return authorizedClientManager;
	}

}