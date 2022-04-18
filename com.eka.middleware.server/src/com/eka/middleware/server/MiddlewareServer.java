package com.eka.middleware.server;

import java.io.File;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;

import com.eka.middleware.auth.ResourceAuthenticator;
import com.eka.middleware.auth.UserProfileManager;
import com.eka.middleware.service.ServiceUtils;
import com.eka.middleware.template.SystemException;

import io.undertow.Undertow;
import io.undertow.Undertow.Builder;
import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.AuthenticationMode;
import io.undertow.security.handlers.AuthenticationCallHandler;
import io.undertow.security.handlers.AuthenticationConstraintHandler;
import io.undertow.security.handlers.AuthenticationMechanismsHandler;
import io.undertow.security.handlers.SecurityInitialHandler;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.impl.BasicAuthenticationMechanism;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.BlockingHandler;

public class MiddlewareServer {
	public static Logger LOGGER = LogManager.getLogger(MiddlewareServer.class);
	private static String configFolderPath;
	private static String local_IP="localhost";
	private static Map<String, Long> lastModifiedMap=new ConcurrentHashMap<String,Long>();
	
	public static void main(final String[] args) throws SystemException {
		try {
		initConfig(args);
		String ports[] = ServiceUtils.getServerProperty("middleware.server.http.ports").split(",");
		final UserProfileManager identityManager = new UserProfileManager();
		Builder builder = Undertow.builder();
		for (String port : ports) {
			builder.addHttpListener(Integer.parseInt(port), local_IP).setHandler(new BlockingHandler
					(new HttpHandler() {
					//new HttpHandler() {
				
				public void handleRequest(final HttpServerExchange exchange) throws Exception {
					//synchronized (exchange) {
						if(ResourceAuthenticator.isPublic(exchange))
							ThreadManager.processRequest(exchange);
						else {
							HttpHandler hh=addSecurity(this, identityManager);
							hh.handleRequest(exchange);
						}
					//}
				}
			})
			);
		}
		Undertow server = builder.build();
		server.start();
		}catch(Exception e) {
			e.printStackTrace();
			throw new SystemException("EKA_MWS_1004", e);
		}
	}

	public static final void initConfig(String[] args) throws Exception {
		InetAddress localhost = InetAddress.getLocalHost();
		local_IP=localhost.getHostAddress().trim();
		if (args == null || args.length == 0 || args[0].trim().length() == 0) {
			configFolderPath=MiddlewareServer.class.getResource("/").toString()+"resources/config/";
			configureLog4j();
			LOGGER.info("Please provide the path config folder(format:- D:\\server\\config\\)");
		}else { // */
			configFolderPath = args[0];
			File file=new File(configFolderPath);
			configFolderPath=file.getAbsolutePath();
			configureLog4j();
		}
		String ip=ServiceUtils.getServerProperty("middleware.server.remote_ip");
		if(ip!=null) {
			local_IP=ip;
		}
		//System.out.println(configFolderPath);
	}
	
	private static void configureLog4j() throws Exception {
		LoggerContext context = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
		URL url = new File(getConfigFolderPath()+"log4j2.xml").toURL();
		// this will force a reconfiguration
		context.setConfigLocation(url.toURI());
	}

	public static String getConfigFolderPath() {
		return configFolderPath+"/";
	}
	
	private static HttpHandler addSecurity(final HttpHandler toWrap, final IdentityManager identityManager) {
        HttpHandler handler = toWrap;
        handler = new AuthenticationCallHandler(handler);
        handler = new AuthenticationConstraintHandler(handler);
        final List<AuthenticationMechanism> mechanisms = Collections.<AuthenticationMechanism>singletonList(new BasicAuthenticationMechanism("My Realm2"));
        handler = new AuthenticationMechanismsHandler(handler, mechanisms);
        handler = new SecurityInitialHandler(AuthenticationMode.PRO_ACTIVE, identityManager, handler);
        return handler;
    }
	
	public static final byte[] readConfigurationFile(String fileName) throws SystemException {
		try {
			Long lastModified=lastModifiedMap.get(fileName);
			String profileFilePath = MiddlewareServer.getConfigFolderPath() + fileName;
			//URL url = new URL(profileFilePath);
			File file = new File(profileFilePath);
			if(lastModified==null || lastModified<file.lastModified()) {
				byte[] bytes=null;
				synchronized (lastModifiedMap) {
					lastModified=lastModifiedMap.get(fileName);
					if(lastModified==null || lastModified<file.lastModified()) {
						bytes=ServiceUtils.readAllBytes(file);
						lastModifiedMap.put(fileName, file.lastModified());
					}
				}
				return bytes;
			}
		} catch (Exception e) {
			throw new SystemException("EKA_MWS_1000", e);
		}
		return null;
	}
	
	public static final void writeConfigurationFile(String fileName,byte data[]) throws SystemException {
		try {
			String profileFilePath = MiddlewareServer.getConfigFolderPath() + fileName;
			//URL url = new URL(profileFilePath);
			File file = new File(profileFilePath);
			FileOutputStream fos=new FileOutputStream(file);
			fos.write(data);
			fos.flush();
			fos.close();
		} catch (Exception e) {
			throw new SystemException("EKA_MWS_1007", e);
		}
	}
}
