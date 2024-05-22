package com.eka.middleware.server;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import com.eka.middleware.scheduling.ApplicationSchedulerFactory;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.quartz.Scheduler;
import org.xnio.CompressionType;
import org.xnio.Options;

import com.eka.middleware.auth.AuthAccount;
import com.eka.middleware.auth.Security;
import com.eka.middleware.auth.UserProfileManager;
import com.eka.middleware.distributed.offHeap.IgNode;
import com.eka.middleware.service.PropertyManager;
import com.eka.middleware.service.ServiceUtils;
import com.eka.middleware.template.SystemException;
import com.eka.middleware.template.Tenant;

import io.undertow.Undertow;
import io.undertow.Undertow.Builder;
import io.undertow.UndertowOptions;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.session.InMemorySessionManager;
import io.undertow.server.session.SessionAttachmentHandler;
import io.undertow.server.session.SessionCookieConfig;

public class MiddlewareServer {

	public static Logger LOGGER = LogManager.getLogger(MiddlewareServer.class);
	private static String local_IP = "localhost";
	// public static String JWT_MASALA="17a0dab4-991c-47b6-91c6-109e640a817f";
	private static Map<String, Long> lastModifiedMap = new ConcurrentHashMap<String, Long>();
	public static final Builder builder = Undertow.builder();
	public static Undertow server = null;

	public static final String BUILD_NAME = "v1.6.5";
	public static final boolean IS_COMMUNITY_VERSION = Boolean.parseBoolean(System.getProperty("COMMUNITY_DEPLOYMENT"));

	public static String allowRestrictedHeaders = System.getProperty("jdk.httpclient.allowRestrictedHeaders");
	public static String headless = System.getProperty("java.awt.headless");
	public static String CONTAINER_DEPLOYMENT = System.getProperty("CONTAINER_DEPLOYMENT");
	public static String CONTAINER_ON_PRIM_DEPLOYMENT = System.getProperty("CONTAINER_ON_PRIM_DEPLOYMENT");
	public static String COMMUNITY_DEPLOYMENT = System.getProperty("COMMUNITY_DEPLOYMENT");
	public static String CORE_DEPLOYMENT = System.getProperty("CORE_DEPLOYMENT");
	public static String disableEndpointIdentification = System.getProperty("com.sun.jndi.ldap.object.disableEndpointIdentification");

	public static void main(final String[] args) throws SystemException {

		if (Boolean.parseBoolean(System.getProperty("CONTAINER_DEPLOYMENT"))) {
			try {
				BootBuild.bootBuild();
			} catch (Exception e) {
				e.printStackTrace();
			}
			//Build.download("latest");
		} else {
			LOGGER.info("No Container Deployment");
		}

		try {
			PropertyManager.initConfig(args);
			IgNode.getIgnite();
		} catch (Exception e) {
			e.printStackTrace();
		}

		ApplicationShutdownHook.arg = args;

		try {
			//PropertyManager.initConfig(args);
			UserProfileManager.migrationProfiles();
			local_IP = PropertyManager.getLocal_IP();
			Scheduler scheduler = ApplicationSchedulerFactory.initScheduler(null, "default");
			String ports[] = ServiceUtils.getServerProperty("middleware.server.http.ports").split(",");
			String https = ServiceUtils.getServerProperty("middleware.server.https.ports");
			String keyStoreFilePath = ServiceUtils.getServerProperty("middleware.server.keyStore.jks");
			String keyStorePassword = ServiceUtils.getServerProperty("middleware.server.keyStore.jks.password");

			java.security.Security.addProvider(new BouncyCastleProvider()); // Initializing Security for secure properties.

			/**
			 * Create Server Instance Cluster/Group ID
			 */
			{
				File instanceUUID = new File(getConfigFolderPath() + "INSTANCE.UUID");
				File instanceGroupUUID = new File(getConfigFolderPath() + "INSTANCE-GROUP.UUID");
				if (!instanceUUID.exists()) IOUtils.write(UUID.randomUUID().toString(), new FileOutputStream(instanceUUID), StandardCharsets.UTF_8);
				if (!instanceGroupUUID.exists()) IOUtils.write(UUID.randomUUID().toString(), new FileOutputStream(instanceGroupUUID), StandardCharsets.UTF_8);
			}
			/**
			 * End
			 */

			// https="8443";
			String securePorts[] = null;
			if (https != null)
				securePorts = https.split(",");
			final UserProfileManager identityManager = UserProfileManager.create();
			LOGGER.trace("Users loaded..................");
			List<String> tenants = UserProfileManager.getTenants();
			LOGGER.trace("Tenants loaded: " + tenants);
			try {
				Tenant defaultTenant = Tenant.getTempTenant("default");
				String dirPath = PropertyManager.getPackagePath(defaultTenant) + "packages";
				File dir = new File(dirPath);

				if (!dir.exists()) {
					AuthAccount authAcc = new AuthAccount("admin");
					Map<String, Object> profile = UserProfileManager.createDefaultProfile(null, "default");
					List<String> groups = new ArrayList<String>();
					groups.add(AuthAccount.STATIC_ADMIN_GROUP);
					groups.add(AuthAccount.STATIC_DEVELOPER_GROUP);
					profile.put("groups", groups);
					profile.put("forceCreateUser", true);
					authAcc.setProfile(profile);
					//UserProfileManager.addUser(authAcc);
					ServiceUtils.initNewTenant("default", authAcc, "admin");
				} else {
					LOGGER.info("Starting default tenant......................");
					defaultTenant.logDebug(null, "Starting default tenant......................");
					Security.setupTenantSecurity("default");
					ServiceUtils.startTenantServices("default");
				}

				Thread.sleep(500);
				startServer(ports, keyStoreFilePath, keyStorePassword, securePorts);

				Thread.sleep(2000);
				for (String tenant : tenants) {
					Tenant tent = Tenant.getTenant(tenant);
					dirPath = PropertyManager.getPackagePath(tent) + "packages";
					dir = new File(dirPath);
					LOGGER.trace("Tenant directory: " + dirPath);
					// tent.logInfo("", dirPath);
					if (!dir.exists()) {
						LOGGER.trace("Tenant directory not found: " + dirPath);
						continue;
					}
					if (!"default".equalsIgnoreCase(tenant)) {
						LOGGER.info("Starting " + tenant + " tenant......................");
						tent.logDebug(null, "Starting " + tenant + " tenant......................");
						ApplicationSchedulerFactory.initScheduler(null, tenant);
						Security.setupTenantSecurity(tenant);
						//ServiceUtils.startTenantServices(tenant);
						Thread.sleep(2000);
					}

					if (!Boolean.parseBoolean(System.getProperty("CORE_DEPLOYMENT"))) {
						tent.rotateKeys();
					}
				}

				ApplicationShutdownHook.getCurrentProcess();
				ApplicationShutdownHook.prepareOutputFile();
				Runtime.getRuntime().addShutdownHook(new Thread(new ApplicationShutdownHook()));
			} catch (Exception e) {
				throw new SystemException("EKA_MWS_1008", e);
			}

		} catch (Exception e) {
			e.printStackTrace();
			throw new SystemException("EKA_MWS_1004", e);
		}
	}

	private static void startServer(String[] ports, String keyStoreFilePath, String keyStorePassword, String[] securePorts) throws Exception {
		LOGGER.trace("Creating server builder ........");
		PathHandler pathHandler = Security.init();

		for (String port : ports) {
			builder.addHttpListener(Integer.parseInt(port), local_IP).setHandler(new SessionAttachmentHandler(
					pathHandler, new InMemorySessionManager("SessionManager"), new SessionCookieConfig()));
		}
		SSLContext context = null;
		if (keyStoreFilePath != null && new File(keyStoreFilePath).exists())
			context = createSSLContext(keyStoreFilePath, keyStorePassword);

		if (context != null)
			for (String port : securePorts) {
				builder.addHttpsListener(Integer.parseInt(port), local_IP, context)
						.setHandler(new SessionAttachmentHandler(pathHandler,
								new InMemorySessionManager("SessionManager"), new SessionCookieConfig()));
			}

		configureUndertow(builder);
		server = builder.build();
		server.start();
	}

	private static void configureUndertow(Builder builder) throws IllegalArgumentException, IOException {
		String MAX_PARAMETERS = ServiceUtils.getServerProperty("middleware.server.UndertowOptions.MAX_PARAMETERS");
		String ENABLE_HTTP2 = ServiceUtils.getServerProperty("middleware.server.UndertowOptions.ENABLE_HTTP2");
		String TCP_NODELAY = ServiceUtils.getServerProperty("middleware.server.worker.Options.TCP_NODELAY");
		String REUSE_ADDRESSES = ServiceUtils.getServerProperty("middleware.server.worker.Options.REUSE_ADDRESSES");
		String WORKER_IO_THREADS = ServiceUtils.getServerProperty("middleware.server.worker.Options.WORKER_IO_THREADS");
		String WORKER_TASK_CORE_THREADS = ServiceUtils
				.getServerProperty("middleware.server.worker.Options.WORKER_TASK_CORE_THREADS");
		String WORKER_TASK_KEEPALIVE = ServiceUtils
				.getServerProperty("middleware.server.worker.Options.WORKER_TASK_KEEPALIVE");
		String WORKER_TASK_LIMIT = ServiceUtils.getServerProperty("middleware.server.worker.Options.WORKER_TASK_LIMIT");
		String WORKER_TASK_MAX_THREADS = ServiceUtils
				.getServerProperty("middleware.server.worker.Options.WORKER_TASK_MAX_THREADS");
		String COMPRESSION_LEVEL = ServiceUtils.getServerProperty("middleware.server.worker.Options.COMPRESSION_LEVEL");
		String COMPRESSION_TYPE = ServiceUtils.getServerProperty("middleware.server.worker.Options.COMPRESSION_TYPE");
		String CONNECTION_HIGH_WATER = ServiceUtils
				.getServerProperty("middleware.server.worker.Options.CONNECTION_HIGH_WATER");
		String CONNECTION_LOW_WATER = ServiceUtils
				.getServerProperty("middleware.server.worker.Options.CONNECTION_LOW_WATER");
		String MAX_INBOUND_MESSAGE_SIZE = ServiceUtils
				.getServerProperty("middleware.server.worker.Options.MAX_INBOUND_MESSAGE_SIZE");
		String MAX_OUTBOUND_MESSAGE_SIZE = ServiceUtils
				.getServerProperty("middleware.server.worker.Options.MAX_OUTBOUND_MESSAGE_SIZE");

		// The maximum number of parameters that will be parsed. This is used to protect
		// against hash vulnerabilities.
		// This applies to both query parameters, and to POST data, but is not
		// cumulative (i.e. you can potentially have max parameters * 2 total
		// parameters).
		if (MAX_PARAMETERS != null && MAX_PARAMETERS.trim().length() > 0)
			builder.setServerOption(UndertowOptions.MAX_PARAMETERS, 1000);

		// If we should attempt to use HTTP2 for HTTPS connections.
		if (ENABLE_HTTP2 != null && ENABLE_HTTP2.trim().length() > 0)
			builder.setServerOption(UndertowOptions.ENABLE_HTTP2, Boolean.parseBoolean(ENABLE_HTTP2));

		// Configure a TCP socket to disable Nagle's algorithm. The value type for this
		// option is boolean.
		if (TCP_NODELAY != null && TCP_NODELAY.trim().length() > 0)
			builder.setWorkerOption(Options.TCP_NODELAY, Boolean.parseBoolean(TCP_NODELAY));

		// Configure an IP socket to reuse addresses. The value type for this option is
		// boolean.
		if (REUSE_ADDRESSES != null && REUSE_ADDRESSES.trim().length() > 0)
			builder.setWorkerOption(Options.REUSE_ADDRESSES, Boolean.parseBoolean(REUSE_ADDRESSES));

		// Specify the number of I/O threads to create for the worker. If not specified,
		// a default will be chosen.
		if (WORKER_IO_THREADS != null && WORKER_IO_THREADS.trim().length() > 0) {
			int val=Integer.parseInt(WORKER_IO_THREADS);
			builder.setWorkerOption(Options.WORKER_IO_THREADS, val);
			builder.setWorkerThreads(val*8);
		}

		// Specify the number of "core" threads for the worker task thread pool.
		if (ENABLE_HTTP2 != null && WORKER_TASK_CORE_THREADS.trim().length() > 0)
			builder.setWorkerOption(Options.WORKER_TASK_CORE_THREADS, Integer.parseInt(WORKER_TASK_CORE_THREADS));

		// Specify the number of milliseconds to keep non-core task threads alive.
		if (WORKER_TASK_KEEPALIVE != null && WORKER_TASK_KEEPALIVE.trim().length() > 0)
			builder.setWorkerOption(Options.WORKER_TASK_KEEPALIVE, Integer.parseInt(WORKER_TASK_KEEPALIVE));

		// Specify the maximum number of worker tasks to allow before rejecting.
		if (WORKER_TASK_LIMIT != null && WORKER_TASK_LIMIT.trim().length() > 0)
			builder.setWorkerOption(Options.WORKER_TASK_LIMIT, Integer.parseInt(WORKER_TASK_LIMIT));

		// Specify the maximum number of threads for the worker task thread pool.
		if (WORKER_TASK_MAX_THREADS != null && WORKER_TASK_MAX_THREADS.trim().length() > 0)
			builder.setWorkerOption(Options.WORKER_TASK_MAX_THREADS, Integer.parseInt(WORKER_TASK_MAX_THREADS));

		// The compression level to apply for compressing streams and channels.
		if (COMPRESSION_LEVEL != null && COMPRESSION_LEVEL.trim().length() > 0)
			builder.setWorkerOption(Options.COMPRESSION_LEVEL, Integer.parseInt(COMPRESSION_LEVEL));

		// The compression type to apply for compressing streams and channels.
		if (COMPRESSION_TYPE != null && COMPRESSION_TYPE.trim().length() > 0) {
			switch (COMPRESSION_TYPE) {
			case "GZIP": {
				builder.setWorkerOption(Options.COMPRESSION_TYPE, CompressionType.GZIP);
			}
			default:
				throw new IllegalArgumentException(
						"Unexpected value for middleware.server.worker.Options.COMPRESSION_TYPE : " + COMPRESSION_TYPE);
			}
		}

		// The high water mark for a server's connections. Once this number of
		// connections have been accepted, accepts will be suspended for that server.
		if (CONNECTION_HIGH_WATER != null && CONNECTION_HIGH_WATER.trim().length() > 0)
			builder.setWorkerOption(Options.CONNECTION_HIGH_WATER, Integer.parseInt(CONNECTION_HIGH_WATER));

		// The low water mark for a server's connections. Once the number of active
		// connections have dropped below this number, accepts can be resumed for that
		// server.
		if (CONNECTION_LOW_WATER != null && CONNECTION_LOW_WATER.trim().length() > 0)
			builder.setWorkerOption(Options.CONNECTION_LOW_WATER, Integer.parseInt(CONNECTION_LOW_WATER));

		// The maximum inbound message size.
		if (MAX_INBOUND_MESSAGE_SIZE != null && MAX_INBOUND_MESSAGE_SIZE.trim().length() > 0)
			builder.setWorkerOption(Options.MAX_INBOUND_MESSAGE_SIZE, Integer.parseInt(MAX_INBOUND_MESSAGE_SIZE));

		// The maximum outbound message size.
		if (MAX_OUTBOUND_MESSAGE_SIZE != null && MAX_OUTBOUND_MESSAGE_SIZE.trim().length() > 0)
			builder.setWorkerOption(Options.MAX_OUTBOUND_MESSAGE_SIZE, Integer.parseInt(MAX_OUTBOUND_MESSAGE_SIZE));

		// Specify whether SASL mechanisms which are susceptible to passive dictionary
		// attacks are permitted.
		builder.setWorkerOption(Options.SASL_POLICY_NODICTIONARY, false);

		// Specify whether SASL mechanisms which are susceptible to simple plain passive
		// attacks are permitted.
		builder.setWorkerOption(Options.SASL_POLICY_NOPLAINTEXT, false);
		
	}

	public static SSLContext createSSLContext(String keyStoreFilePath, String password) throws Exception {
		char[] passphrase = password.toCharArray();
		KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
		FileInputStream fis = new FileInputStream(new File(keyStoreFilePath));
		ks.load(fis, passphrase);
		fis.close();

		TrustManager[] tms = new TrustManager[] {};
		KeyManager[] kms = null;
		KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		kmf.init(ks, passphrase);
		kms = kmf.getKeyManagers();
		SSLContext context = SSLContext.getInstance("TLS");
		context.init(kms, tms, new SecureRandom());
		return context;
	}

	public static String getConfigFolderPath() {
		return PropertyManager.getConfigFolderPath();
	}

	private static class BootBuild {

		/**
		 *
		 */
		private static void bootBuild() throws Exception {

			String distributionName = "eka-distribution-" + BUILD_NAME + ".zip";
			if (Boolean.parseBoolean(System.getProperty("COMMUNITY_DEPLOYMENT"))) {
				distributionName = "eka-distribution-community-" + BUILD_NAME + ".zip";
			}

			File eka = new File("./eka/version");
			if (eka.exists()) {
				LOGGER.info("Build already existed.");
			} else {
				if (new File("./" + distributionName).exists()) {
					LOGGER.info("Found Build.");
				} else {
					LOGGER.info("Build Downloading...");
					InputStream in = new URL(Build.DISTRIBUTION_REPO + distributionName)
							.openStream();
					Files.copy(in, Paths.get(distributionName), StandardCopyOption.REPLACE_EXISTING);
					LOGGER.info("Build Download Completed.");
				}

				LOGGER.info("Unzipping build...");
				ServiceUtils.unzipBuildFile("./" + distributionName, "");
			}
		}
	}
}
