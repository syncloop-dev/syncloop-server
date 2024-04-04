package com.eka.middleware.auth;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.Cipher;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pac4j.core.config.Config;
import org.pac4j.undertow.handler.CallbackHandler;
import org.pac4j.undertow.handler.SecurityHandler;

import com.eka.middleware.auth.pac4j.AuthConfigFactory;
import com.eka.middleware.auth.pac4j.AuthHandlers;
import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.PropertyManager;
import com.eka.middleware.service.ServiceUtils;
import com.eka.middleware.template.Tenant;

import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;

public class Security {
	private static final PathHandler path = new PathHandler();
	public static final String defaultTenantPage = "/files/gui/middleware/pub/server/ui/tenant/newTenant.html";
	public static final String defaultLoginPage = "/files/gui/middleware/pub/server/ui/welcome/onboarding/login.html";
	public static final String defaultWelcomePage = defaultLoginPage;//"/files/gui/middleware/pub/server/ui/workspace/web/dashboard.html";//"/files/gui/middleware/pub/server/ui/welcome/index.html";
	public static final String defaultWorkspacePage = "/files/gui/middleware/pub/server/ui/workspace/web/dashboard.html";//"/files/gui/middleware/pub/server/ui/welcome/index.html";
	public static final String defaultLoginPage_NonTenant = "/files/gui/middleware/pub/server/ui/welcome/onboarding/signin.html";
	public static final String defaultLoginAction = "/packages.middleware.pub.security.BasicLoginHandler.main";
	private static final Set<String> paths = new HashSet<String>();
	private static final Set<String> publicPaths = new HashSet<String>();
	private static Map<String, List<String>> publicPrefixPathsMap = new ConcurrentHashMap();
	private static Map<String, List<String>> publicExactPathsMap = new ConcurrentHashMap();
	public final static Map<String, Config> loginExactPathsMap = new ConcurrentHashMap();
	private static Map<String, String> defaultNewUserPathMap = new ConcurrentHashMap();
	public static Logger LOGGER = LogManager.getLogger(Security.class);
	public static String PRIVATE_PROPERTY_KEY_NAME="ekamw.private.key";
	public static String PUBLIC_PROPERTY_KEY_NAME="ekamw.public.key";
	public static Set<String> getPath() {
		return paths;
	}

	public static PathHandler init() throws Exception {
		//path.addPrefixPath("/public",	SecurityHandler.build(AuthHandlers.mainHandler, AuthConfigFactory.getAnonymousClientConfig()));
		//paths.add("/public");
		//path.addPrefixPath("/public",	SecurityHandler.build(AuthHandlers.mainHandler, AuthConfigFactory.getAnonymousClientConfig()));
//		addLoginExactPath(Tenant.getTenant("default"), defaultLoginPage);
//		addLoginExactPath("/login",Tenant.getTenant("default"), 
//				CallbackHandler.build(AuthConfigFactory.getFormClientAuthConfig(defaultLoginPage,Tenant.getTenant("default")), null));

		path.addExactPath("/jwt/basic",
				SecurityHandler.build(AuthHandlers.mainHandler, AuthConfigFactory.getBasicDirectAuthConfig()));
		paths.add("/basic/jwt");
		path.addExactPath("/JWT/basic",
				SecurityHandler.build(AuthHandlers.mainHandler, AuthConfigFactory.getBasicDirectAuthConfig()));
		paths.add("/basic/JWT");
		
		paths.add("/jwt");
		path.addPrefixPath("/jwt",AuthHandlers.mainHandler);
		
		paths.add("/JWT");
		path.addPrefixPath("/JWT",AuthHandlers.mainHandler);
		
		path.addPrefixPath("/", AuthHandlers.mainHandler);
		paths.add("/");
//		path.addExactPath("/keycloak", getKeyCloakAuthenticator("keycloak.json"));
		
		path.addExactPath("/logout", AuthHandlers.logoutHandler);
		return path;
	}

	public static void generateKeyPair(String tenantName) {
		try {
			Properties props = PropertyManager.getGlobalProperties(tenantName);
			String pubKeyString = props.getProperty(PUBLIC_PROPERTY_KEY_NAME);
			String priKeyString = props.getProperty(PRIVATE_PROPERTY_KEY_NAME);
			//if (pubKeyString == null || priKeyString == null) {
				KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
				keyPairGen.initialize(2048, new SecureRandom(ServiceUtils.generateUUID(System.currentTimeMillis()+"").getBytes()));
				KeyPair pair = keyPairGen.generateKeyPair();
				PrivateKey privKey = pair.getPrivate();
				byte[] privateKey = privKey.getEncoded();
				PublicKey pubKey = pair.getPublic();
				byte[] publicKey = pubKey.getEncoded();
				props.put(PUBLIC_PROPERTY_KEY_NAME, new String(Base64.getEncoder().encode(publicKey),StandardCharsets.UTF_8));
				props.put(PRIVATE_PROPERTY_KEY_NAME, new String(Base64.getEncoder().encode(privateKey),StandardCharsets.UTF_8));
				PropertyManager.saveGlobalProperties(tenantName, props);
			//}
		} catch (Exception e) {
			ServiceUtils.printException("Failed while generating keypair for '"+tenantName+"'", e);
		}
	}
	private static PrivateKey getPrivateKey(byte[] key) throws Exception {
		//X509EncodedKeySpec privateKeySpec = new X509EncodedKeySpec(key, "RSA");
		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(key);
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		PrivateKey privateKey = keyFactory.generatePrivate(spec);
		return privateKey;
	}

	private static PublicKey getPublicKey(byte[] key) throws Exception {
		X509EncodedKeySpec spec = new X509EncodedKeySpec(key, "RSA");
//		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(key);
//		RSAPublicKeySpec spec=new RSAPublicKeySpec
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		PublicKey publicKey = keyFactory.generatePublic(spec);
		return publicKey;
	}

	public static String getSecureString(final String data,final String publicKey) throws Exception {
		Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding", "BC");
		//String publicKey = ServiceUtils.getServerProperty("middleware.server.public.key");
		cipher.init(Cipher.ENCRYPT_MODE, getPublicKey(Base64.getDecoder().decode(publicKey.getBytes(StandardCharsets.UTF_8))));
		cipher.update(data.getBytes());
		byte[] cipherText = cipher.doFinal();
		cipherText=Base64.getEncoder().encode(cipherText);
		return new String(cipherText, StandardCharsets.UTF_8);
	}

	public static String getNormalString(final String secureString, final String privateKey) throws Exception {
		Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding", "BC");
		//String privateKey = ServiceUtils.getServerProperty("middleware.server.private.key");
		cipher.init(Cipher.DECRYPT_MODE, getPrivateKey(Base64.getDecoder().decode(privateKey.getBytes(StandardCharsets.UTF_8))));
		final byte[] decipheredText = cipher.doFinal(Base64.getDecoder().decode(secureString.getBytes(StandardCharsets.UTF_8)));
		return new String(decipheredText);
	}

/*	public static SecurityInitialHandler getKeyCloakAuthenticator(String keycloakJsonFileName) throws Exception {

		UserProfileManager identityManager = UserProfileManager.create();
		String keycloakJson = PropertyManager.getConfigFolderPath() + keycloakJsonFileName;
		final AdapterConfig adapterConfig = KeycloakDeploymentBuilder
				.loadAdapterConfig(new FileInputStream(keycloakJson));

		final KeycloakDeployment keycloakDeployment = KeycloakDeploymentBuilder.build(adapterConfig);

		final AdapterDeploymentContext adapterDeploymentContext = new AdapterDeploymentContext(keycloakDeployment);

		final UndertowAuthenticationMechanism keycloakAuthMech = new UndertowAuthenticationMechanism(
				adapterDeploymentContext, new UndertowUserSessionManagement(), new NodesRegistrationManagement(), 443,
				"");

		final AuthenticationCallHandler authenticationCallHandler = new AuthenticationCallHandler(
				ThreadManager::processRequest);

		final AuthenticationConstraintHandler authenticationConstraintHandler = new AuthenticationConstraintHandler(
				authenticationCallHandler);


		final AuthenticationMechanismsHandler authenticationMechanismsHandler = new AuthenticationMechanismsHandler(
				authenticationConstraintHandler, Arrays.asList(keycloakAuthMech));


		final SecurityInitialHandler securityInitialHandler = new SecurityInitialHandler(AuthenticationMode.PRO_ACTIVE,
				identityManager, authenticationMechanismsHandler);
		return securityInitialHandler;
	}*/

	public static void setupTenantSecurity(final String tenantName) {
		//Security.generateKeyPair(tenantName);
		String tenantPath="/tenant/" + tenantName;
		addPublicPrefixPath(tenantPath + "/files/gui/middleware/pub/server/ui/welcome/",Tenant.getTenant(tenantName));
		List<String> tennatPublicExactPaths = publicExactPathsMap.get(tenantName);
		if (tennatPublicExactPaths != null && tennatPublicExactPaths.size() > 0)
			tennatPublicExactPaths.forEach(path -> Security.addPublicExactPath(path, Tenant.getTenant(tenantName)));

		List<String> tennatPublicPrefixPaths = publicPrefixPathsMap.get(tenantName);
		if (tennatPublicPrefixPaths != null && tennatPublicPrefixPaths.size() > 0)
			tennatPublicPrefixPaths.forEach(path -> Security.addPublicPrefixPath(path, Tenant.getTenant(tenantName)));
		
		String pathname = PropertyManager.getPackagePath(Tenant.getTenant(tenantName)) + "packages" + File.separator;
        File[] folders = new File(pathname).listFiles();
        for (File file : folders) {
			if(file.isDirectory()) {
				String packageName=file.getName();
				addPublicPrefixPath("/files/gui/"+packageName+"/pub/", Tenant.getTenant(tenantName));
			}
			
		}
		
		//addPublicPrefixPath("/files/gui/middleware/pub", Tenant.getTenant(tenantName));
		//addPublicPrefixPath("/files/gui/middleware/pub/server/ui/icons/", Tenant.getTenant(tenantName));
		//addPublicPrefixPath("/files/gui/middleware/pub/server/ui/assets/", Tenant.getTenant(tenantName));
		//addPublicPrefixPath("/files/gui/middleware/pub/server/ui/javascript/middleware.js", Tenant.getTenant(tenantName));
		addPublicPrefixPath("/packages.middleware.pub.security.OIDCLogin.", Tenant.getTenant(tenantName));
		addPublicPrefixPath("/public", Tenant.getTenant(tenantName));
		
		if (Boolean.parseBoolean(System.getProperty("CORE_DEPLOYMENT"))) {
			addPublicExactPath(defaultLoginPage,Tenant.getTenant(tenantName));
		} else {
			addPublicExactPath(defaultLoginPage_NonTenant,Tenant.getTenant(tenantName));
		}


		//addPublicExactPath(defaultWelcomePage,Tenant.getTenant(tenantName));
		//addPublicExactPath(defaultTenantPage,Tenant.getTenant(tenantName));
		
		path.addExactPath("/tenant/"+tenantName,AuthHandlers.defaultWelcomePageHandler("/tenant/"+tenantName+defaultLoginAction));
		//if(tenantName.equals("default"))
		path.addExactPath("/",AuthHandlers.defaultWelcomePageHandler("/tenant/"+tenantName+defaultLoginAction));

		if (Boolean.parseBoolean(System.getProperty("CORE_DEPLOYMENT"))) {
			addLoginExactPath(Tenant.getTenant(tenantName), defaultLoginPage);
		} else {
			addLoginExactPath(Tenant.getTenant(tenantName), defaultLoginPage_NonTenant);
		}
	}

	public static void addPublicPrefixPath(String resourcePrefixPath, Tenant tenant) {
		List<String> tennatPublicPaths = publicPrefixPathsMap.get(tenant.getName());
		if (tennatPublicPaths == null || tennatPublicPaths.size() == 0) {
			tennatPublicPaths = new ArrayList();
			publicPrefixPathsMap.put(tenant.getName(), tennatPublicPaths);
		}
		if (!tennatPublicPaths.contains(resourcePrefixPath)) {
			tennatPublicPaths.add(resourcePrefixPath);
			path.addPrefixPath(resourcePrefixPath,
					SecurityHandler.build(AuthHandlers.indexHandler(), AuthConfigFactory.getAnonymousClientConfig()));
			path.addPrefixPath("/tenant/" + tenant.getName() + resourcePrefixPath,
					SecurityHandler.build(AuthHandlers.indexHandler(), AuthConfigFactory.getAnonymousClientConfig()));
			LOGGER.info("Public prefix path: "+tenant.getName()+": "+resourcePrefixPath);
			tenant.logInfo(null, resourcePrefixPath);
		}
		
	}

	public static void addPublicExactPath(String resourceExactPath, Tenant tenant) {
		List<String> tennatPublicPaths = publicExactPathsMap.get(tenant.getName());
		if (tennatPublicPaths == null || tennatPublicPaths.size() == 0) {
			tennatPublicPaths = new ArrayList();
			publicExactPathsMap.put(tenant.getName(), tennatPublicPaths);
		}
		if (!tennatPublicPaths.contains(resourceExactPath)) {
			tennatPublicPaths.add(resourceExactPath);
			path.addExactPath(resourceExactPath,
					SecurityHandler.build(AuthHandlers.indexHandler(), AuthConfigFactory.getAnonymousClientConfig()));
			path.addExactPath("/tenant/" + tenant.getName() + "/" + resourceExactPath,
					SecurityHandler.build(AuthHandlers.indexHandler(), AuthConfigFactory.getAnonymousClientConfig()));
			LOGGER.info("Public exact path: "+tenant.getName()+": "+resourceExactPath);
			tenant.logInfo(null, "Public exact path: "+tenant.getName()+": "+resourceExactPath);
		}
		
	}
	
	public static void addLoginExactPath(Tenant tenant, String loginUrl) {
		Config config = loginExactPathsMap.get(tenant.getName());
		String tenantPath="/tenant/" + tenant.getName();
		String loginFormActionPath=tenantPath+"/authenticate";
		if (config == null) {
			config=AuthConfigFactory.getFormClientAuthConfig(loginUrl,tenant,"/authenticate");
			loginExactPathsMap.put(tenant.getName(), config);
		}
		HttpHandler handler = CallbackHandler.build(config);
		path.addExactPath(loginFormActionPath,handler);
	}
	
	public static boolean isPublic(String path, String tenantName) {
		List<String> tennatPublicPaths = publicPrefixPathsMap.get(tenantName);
		if (tennatPublicPaths != null) {
			for (String resourcePath : tennatPublicPaths) {
				if (path.startsWith(resourcePath))
					return true;
			}
		}
		if (publicExactPathsMap.get(tenantName) != null && publicExactPathsMap.get(tenantName).contains(path))
			return true;
		return false;
	}

	public static void addDefaultNewUserPath1(String path, String tenantName) {
		defaultNewUserPathMap.put(tenantName, path);
	}

	public static void removeDefaultNewUserPath(String path, String tenantName) {
		defaultNewUserPathMap.remove(tenantName);
	}

	public static String gerDefaultNewUserPath(String tenantName) {
		return defaultNewUserPathMap.get(tenantName);
	}

	public static String addExternalOIDCAuthorizationServer(Map<String, Object> props, String resourcePath,
			DataPipeline dp) throws Exception {
		// props.put("tenant", dp.rp.getTenant().getName());
		String loginPath = resourcePath;
		Config conf = AuthConfigFactory.getOIDCAuthClientConfig(props, dp.rp.getTenant().getName());
		String jwtPath="/tenant/" + dp.rp.getTenant().getName()+ "/jwt" + resourcePath;
		resourcePath = "/tenant/" + dp.rp.getTenant().getName() + resourcePath;
		String basePath = (String) props.get("basePath");
		String reDirectURI = basePath + "/callback" + resourcePath + "?client_name=OidcClient";
		
		paths.add(jwtPath);
		path.addExactPath(jwtPath, SecurityHandler.build(AuthHandlers.mainHandler, conf));
		LOGGER.info("JWT endpoint");
		dp.log("JWT endpoint");
		LOGGER.info(jwtPath);
		dp.log(jwtPath);
		paths.add(resourcePath);
		path.addExactPath(resourcePath, SecurityHandler.build(AuthHandlers.mainHandler, conf));
		
		paths.add("/callback" + resourcePath);
		path.addExactPath("/callback" + resourcePath, CallbackHandler.build(conf, null));
		return "Login url=" + basePath + loginPath + ", Redirect_uri=" + reDirectURI;
	}

	public static List<String> getPublicPrefixPaths(String tenantName) {
		return publicPrefixPathsMap.get(tenantName);
	}

	public static List<String> getPublicExactPaths(String tenantName) {
		return publicExactPathsMap.get(tenantName);
	}

	public static void removePath(String resourcePath) throws Exception {
		path.removeExactPath(resourcePath);
		path.removePrefixPath(resourcePath);
	}

}
