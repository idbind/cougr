/*******************************************************************************
 * Copyright 2016 The MITRE Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.mitre.cougr.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mitre.jose.keystore.JWKSetKeyStore;
import org.mitre.jwt.signer.service.impl.DefaultJWTSigningAndValidationService;
import org.mitre.jwt.signer.service.impl.JWKSetCacheService;
import org.mitre.oauth2.model.RegisteredClient;
import org.mitre.oauth2.model.ClientDetailsEntity.AuthMethod;
import org.mitre.openid.connect.client.OIDCAuthenticationFilter;
import org.mitre.openid.connect.client.service.impl.DynamicServerConfigurationService;
import org.mitre.openid.connect.client.service.impl.PlainAuthRequestUrlBuilder;
import org.mitre.openid.connect.client.service.impl.StaticAuthRequestOptionsService;
import org.mitre.openid.connect.client.service.impl.StaticClientConfigurationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.servlet.configuration.EnableWebMvcSecurity;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

import com.google.common.collect.Sets;
import com.nimbusds.jose.JWSAlgorithm;
import org.mitre.openid.connect.client.OIDCAuthenticationProvider;
import org.mitre.openid.connect.client.service.impl.StaticSingleIssuerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

@Configuration
@EnableWebMvcSecurity
@Order(4) 
// The EnableResourceServer annotation of OAuthProtectedResourceConfiguration uses a WebSecurityConfigurer with hard-coded Order of 3.
public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter{
	private final static Logger LOG = LoggerFactory.getLogger(WebSecurityConfiguration.class);

	@Value( "${authentication.entryPoint}" )
	private String authEntryPoint;	
		
	@Value( "${issuerService.loginUrl}" )
	private String issuerLoginUrl;
	
	@Value( "${staticClient.id}" )
	private String staticId;
	@Value( "${staticClient.secret}" )
	private String staticSecret;
	@Value( "${staticClient.name}" )
	private String staticName;
	@Value( "#{'${staticClient.scope}'.split(',')}" )
	private List<String> staticScope;
	@Value( "#{'${staticClient.redirectUris}'.split(',')}" )
	private List<String> staticRedirects;
	@Value( "${staticClient.jwksUri}" )
	private String staticJwks;
	@Value( "${staticClient.introspection}" )
	private String staticInstrospection;
	
	@Value( "${client.uri}" )
	private String clienturi;

	
	@Value("${signerService.defaultAlgorithm}")
	private String defaultAlgorithm;
	
	@Value("${signerService.defaultId}")
	private String defaultSignerId;
	
	
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.authorizeRequests().antMatchers("/css/**").permitAll()
			.and()
				.formLogin().loginPage("/login").permitAll().and().logout().permitAll()
			.and()
				.authorizeRequests().anyRequest().authenticated()
			.and()
				.exceptionHandling().authenticationEntryPoint(authenticationEntryPoint())
			.and()
				.addFilterBefore(openIdConnectAuthenticationFilter(), AbstractPreAuthenticatedProcessingFilter.class)
			.addFilterAfter(new CsrfHeaderFilter(), CsrfFilter.class)
			.csrf().csrfTokenRepository(csrfTokenRepository());
	}
	

	private CsrfTokenRepository csrfTokenRepository() {
	  HttpSessionCsrfTokenRepository repository = new HttpSessionCsrfTokenRepository();
	  repository.setHeaderName("X-XSRF-TOKEN");
	  return repository;
	}
	
	
	@Override
    public void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(openIdConnectAuthenticationProvider());
    }
	
	
	@Bean
	public OIDCAuthenticationProvider openIdConnectAuthenticationProvider() {
		OIDCAuthenticationProvider authenticationProvider = new OIDCAuthenticationProvider();
		return authenticationProvider;
	}

	
	
	@Bean
    public LoginUrlAuthenticationEntryPoint authenticationEntryPoint() {
    	return new LoginUrlAuthenticationEntryPoint(authEntryPoint);
    }
	
	@Bean
	public OIDCAuthenticationFilter openIdConnectAuthenticationFilter() throws Exception {
		OIDCAuthenticationFilter filter = new OIDCAuthenticationFilter();
		
		filter.setAuthenticationManager(authenticationManager());
		
		filter.setIssuerService(staticSingleIssuerService());
		filter.setClientConfigurationService(staticClientConfigurationService());
		filter.setServerConfigurationService(dynamicServerConfigurationService());
		filter.setAuthRequestOptionsService(staticAuthRequestOptionsService());
		filter.setAuthRequestUrlBuilder(plainAuthRequestUrlBuilder());
		
		return filter;
	}
	
	
	
	@Bean
	public StaticSingleIssuerService staticSingleIssuerService() {
		StaticSingleIssuerService issuerService = new StaticSingleIssuerService();
		issuerService.setIssuer(issuerLoginUrl);
		return issuerService;
	}
	
	@Bean
	@Primary
	public DynamicServerConfigurationService dynamicServerConfigurationService() {
		return new DynamicServerConfigurationService();
	}
	
	@Bean
	public StaticClientConfigurationService staticClientConfigurationService() {
		StaticClientConfigurationService clientConfigurationService = new StaticClientConfigurationService();
		
		RegisteredClient client = new RegisteredClient();
		client.setClientId(staticId);
		client.setClientSecret(staticSecret);
		client.setClientName(staticName);
		client.setScope(Sets.newHashSet(staticScope));
		client.setTokenEndpointAuthMethod(AuthMethod.SECRET_BASIC);
		client.setRedirectUris(Sets.newHashSet(staticRedirects));
		client.setRequestObjectSigningAlg(JWSAlgorithm.RS256);
		client.setJwksUri(staticJwks);
		client.setAllowIntrospection(Boolean.parseBoolean(staticInstrospection));
		
		Map<String, RegisteredClient> clients = new HashMap<String, RegisteredClient>();
		clients.put(clienturi, client);
		clientConfigurationService.setClients(clients);
		
		/*
		 * Registered Client Service. Uncomment this to save dynamically registered clients out to a 
		 * file on disk (indicated by the filename property) or replace this with another implementation 
		 * of RegisteredClientService. This defaults to an in-memory implementation of RegisteredClientService 
		 * which will forget and re-register all clients on restart.
		 */
		// clientConfigurationService.setRegisteredClientService(new JsonFileRegisteredClientService("/tmp/simple-web-app-clients.json"));
		
		return clientConfigurationService;
	}
	
	
	@Bean
	public StaticAuthRequestOptionsService staticAuthRequestOptionsService() {
		return new StaticAuthRequestOptionsService();
	}
	
	@Bean
	public PlainAuthRequestUrlBuilder plainAuthRequestUrlBuilder() {
		return new PlainAuthRequestUrlBuilder();
	}
	
	@Bean
	public JWKSetCacheService jwkSetCacheService() {
		return new JWKSetCacheService();
	}
	
	@Bean
	public DefaultJWTSigningAndValidationService defaultSignerService() throws Exception {
		JWKSetKeyStore defaultKeyStore = new JWKSetKeyStore();
		defaultKeyStore.setLocation(new ClassPathResource("keystore.jwks"));
		
		DefaultJWTSigningAndValidationService signerService = new DefaultJWTSigningAndValidationService(defaultKeyStore);
		signerService.setDefaultSignerKeyId(defaultSignerId);
		signerService.setDefaultSigningAlgorithmName(defaultAlgorithm);
		
		return signerService;
	}

	
}
