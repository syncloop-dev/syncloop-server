package com.eka.middleware.auth.pac4j;

import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pac4j.core.authorization.authorizer.RequireAnyRoleAuthorizer;
import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.client.direct.AnonymousClient;
import org.pac4j.core.config.Config;
import org.pac4j.core.config.ConfigFactory;
import org.pac4j.http.client.direct.DirectBasicAuthClient;
import org.pac4j.http.client.direct.HeaderClient;
import org.pac4j.http.client.indirect.FormClient;
import org.pac4j.http.client.indirect.IndirectBasicAuthClient;
import org.pac4j.http.credentials.authenticator.test.SimpleTestUsernamePasswordAuthenticator;
import org.pac4j.jwt.config.signature.SecretSignatureConfiguration;
import org.pac4j.jwt.credentials.authenticator.JwtAuthenticator;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;

import com.eka.middleware.auth.manager.BasicAuthenticator;
import com.eka.middleware.ext.spec.Tenant;
import com.eka.middleware.service.PropertyManager;
import com.eka.middleware.service.ServiceUtils;
import com.eka.middleware.template.SystemException;
import com.nimbusds.jose.JWSAlgorithm;

public class AuthConfigFactory implements ConfigFactory {
	public static Logger LOGGER = LogManager.getLogger(AuthConfigFactory.class);
	//private static final String JWT_MASALA=ServiceUtils.generateUUID(System.nanoTime()+"");
	private static Config newConfig(Client clnt) {
		final Config config = new Config(clnt);
		config.addAuthorizer("admin", new RequireAnyRoleAuthorizer("ROLE_ADMIN"));
		config.addAuthorizer("custom", new CustomAuthorizer());
		return config;
	}

	private static Config newConfig(Clients clnts) {
		final Config config = new Config(clnts);
		config.addAuthorizer("admin", new RequireAnyRoleAuthorizer("ROLE_ADMIN"));
		config.addAuthorizer("custom", new CustomAuthorizer());
		return config;
	}

	public Config build(Object... parameters) {
		final AnonymousClient anonymousClient = new AnonymousClient();
		//final Clients clients = new Clients("http://localhost:8080/callback", anonymousClient);
		return newConfig(anonymousClient);
	}

	private static Config DirectBasicAuthClientConfig;

	public static Config getBasicDirectAuthConfig() {
		if (DirectBasicAuthClientConfig == null) {
			final DirectBasicAuthClient client = new DirectBasicAuthClient(new BasicAuthenticator());
			DirectBasicAuthClientConfig = newConfig(client);
		}
		return DirectBasicAuthClientConfig;
	}
	
	private static Config LogouConf;

	public static Config getLogoutConfig() {
		if (LogouConf == null) {
			LogouConf = new Config();
		}
		return LogouConf;
	}
	
	private static Config indirectBasicAuthClientConfig;
	
	public static Config getBasicAuthConfig() {
		if (indirectBasicAuthClientConfig == null) {
			final IndirectBasicAuthClient client = new IndirectBasicAuthClient(new BasicAuthenticator());
			client.setCallbackUrl("/login");
			indirectBasicAuthClientConfig = newConfig(client);
		}
		return indirectBasicAuthClientConfig;
	}
	
	private static final Map<String, Config> formClientAuthClientConfigMap=new ConcurrentHashMap<>();
	
	public static Config getFormClientAuthConfig(String loginURL,Tenant tenant, String authenticationPath) {
		Config formClientAuthClientConfig=formClientAuthClientConfigMap.get(tenant.getID());
		if (formClientAuthClientConfig == null) {
			final FormClient client = new FormClient("/tenant/"+tenant.getName()+loginURL,new BasicAuthenticator());
			client.setCallbackUrl("/tenant/"+tenant.getName()+authenticationPath);
			//client.`
			formClientAuthClientConfig = newConfig(client);
			formClientAuthClientConfigMap.put(tenant.getID(), formClientAuthClientConfig);
		}
		return formClientAuthClientConfig;
	}

	//private static Config JWTAuthClientConfig;

	public static Config getJWTAuthClientConfig(Tenant tenant) {
		Config JWTAuthClientConfig=(Config) tenant.getProperties().get("JWTAuthClientConfig");
		if (JWTAuthClientConfig == null) {
			final HeaderClient client = new HeaderClient("Authorization",
					new JwtAuthenticator((SecretSignatureConfiguration)tenant.getProperties().get("secConf")));
			JWTAuthClientConfig = newConfig(client);
		}
		tenant.getProperties().put("JWTAuthClientConfig", JWTAuthClientConfig);
		return JWTAuthClientConfig;
	}

	private static Config OIDCAuthClientConfig;

	public static Config getOIDCAuthClientConfig() throws SystemException {
		boolean reload = PropertyManager.hasfileChanged("auth/oidc.properties");
		OidcConfiguration oidcConfiguration = null;
		if (OIDCAuthClientConfig == null || reload)
			oidcConfiguration = new OidcConfiguration();
		if (oidcConfiguration != null)
			synchronized (oidcConfiguration) {
				if (reload) {
					reload = false;
					Properties props = PropertyManager.getServerProperties("auth/oidc.properties");
					oidcConfiguration.setClientId(props.getProperty("clientId"));
					oidcConfiguration.setSecret(props.getProperty("secret"));
					oidcConfiguration.setDiscoveryURI(props.getProperty("discoveryURI"));
					oidcConfiguration.setUseNonce(true);
					String preferedJwsAlgo = props.getProperty("preferredJwsAlgorithm");
					if (preferedJwsAlgo != null)
						try {
							JWSAlgorithm jwsAlgo = new JWSAlgorithm(preferedJwsAlgo);
							oidcConfiguration.setPreferredJwsAlgorithm(jwsAlgo);
							oidcConfiguration.setProviderMetadata(
									ServiceUtils.fetchMetadata(props.getProperty("discoveryURI"), jwsAlgo));
						} catch (Exception e) {
							System.err.println("Failed to validate Bearer token: " + e.getMessage());
							e.printStackTrace();
						}

					Set<Object> keys = props.keySet();
					for (Object keyStr : keys) {
						String key = keyStr.toString();
						if (key.startsWith("customParam")) {
							String customKey = key.replace("customParam.", "");
							String val = props.getProperty(key);
							oidcConfiguration.addCustomParam(customKey, val);
						}
					}
					final OidcClient oidcClient = new OidcClient(oidcConfiguration);
					oidcClient.setAuthorizationGenerator((ctx, session, profile) -> {
						profile.addRole("ROLE_ADMIN");
						if(profile.getAttribute("email")==null)
							profile.addAttribute("email", profile.getId());
						return Optional.of(profile);

					});
					final Clients clients = new Clients("http://localhost:8080/callback", oidcClient);// ,headerClient);
					OIDCAuthClientConfig = newConfig(clients);
				}
			}
		return OIDCAuthClientConfig;
	}
	public static Config getOIDCAuthClientConfig(Map<String,Object> props,String tenantName) throws SystemException {
		OidcConfiguration oidcConfiguration = null;
		oidcConfiguration = new OidcConfiguration();
		oidcConfiguration.setClientId((String)props.get("clientId"));
		oidcConfiguration.setSecret((String)props.get("secret"));
		oidcConfiguration.setDiscoveryURI((String)props.get("discoveryURI"));
		oidcConfiguration.setUseNonce(true);
		if(props.get("scope")!=null)
			oidcConfiguration.setScope((String)props.get("scope"));
		String preferedJwsAlgo = (String)props.get("preferredJwsAlgorithm");
		if (preferedJwsAlgo != null)
			try {
				JWSAlgorithm jwsAlgo = new JWSAlgorithm(preferedJwsAlgo);
				oidcConfiguration.setPreferredJwsAlgorithm(jwsAlgo);
				oidcConfiguration
						.setProviderMetadata(ServiceUtils.fetchMetadata((String)props.get("discoveryURI"), jwsAlgo));
			} catch (Exception e) {
				System.err.println("Failed to validate Bearer token: " + e.getMessage());
				e.printStackTrace();
			}

		Set<String> keys = props.keySet();
		for (String keyStr : keys) {
			String key = keyStr.toString();
			if (key.startsWith("customParam")) {
				String customKey = key.replace("customParam.", "");
				String val = (String)props.get(key);
				oidcConfiguration.addCustomParam(customKey, val);
			}
		}
		
		final OidcClient oidcClient = new OidcClient(oidcConfiguration);
		oidcClient.setAuthorizationGenerator((ctx, session, profile) -> {
			profile.addRole("ROLE_ADMIN");
			return Optional.of(profile);

		});

		String basePath = ServiceUtils.getServerProperty("middleware.host.name");
		if (StringUtils.isBlank(basePath)) {
			basePath=(String)props.get("basePath");
		} else {
			basePath = StringUtils.stripEnd(basePath, "/");
		}

		String loginHandlerAPI=(String)props.get("loginHandlerAPI");
		//final Clients clients = new Clients();//oidcClient);// ,headerClient);
		String reDirectURI=basePath+"/callback/tenant/"+tenantName+loginHandlerAPI;
		oidcClient.setCallbackUrl(reDirectURI);
		LOGGER.info("Re-direct URI:----------------------");
		LOGGER.info(reDirectURI+"?client_name=OidcClient");
		//clients.setClients(oidcClient);
		Config OIDCAuthClientConfig = newConfig(oidcClient);
		
		return OIDCAuthClientConfig;
	}

	private static Config AnonymousConfig;

	public static Config getAnonymousClientConfig() {
		if (AnonymousConfig == null) {
			final AnonymousClient anonymousClient = new AnonymousClient();
			AnonymousConfig = newConfig(anonymousClient);
		}
		return AnonymousConfig;
	}

	private static Config IndirectBasicAuthClientConfig;

	public static Config getIndirectBasicAuthClientConfig() {
		if (IndirectBasicAuthClientConfig == null) {
			final IndirectBasicAuthClient indirectBasicAuthClient = new IndirectBasicAuthClient(
					new SimpleTestUsernamePasswordAuthenticator());
			IndirectBasicAuthClientConfig = newConfig(indirectBasicAuthClient);
		}
		return IndirectBasicAuthClientConfig;
	}

	// final IndirectBasicAuthClient indirectBasicAuthClient = new
	// IndirectBasicAuthClient(new SimpleTestUsernamePasswordAuthenticator());

	/*
	 * private static Config OIDCAuthClientConfig;
	 * 
	 * public static Config getOIDCAuthClientConfig() {
	 * if(OIDCAuthClientConfig==null) { final OIDCAuthClientConfig =
	 * newConfig(client); } return OIDCAuthClientConfig; }
	 */

}
