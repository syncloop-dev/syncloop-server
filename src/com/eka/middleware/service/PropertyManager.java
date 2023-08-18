package com.eka.middleware.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;

import com.eka.middleware.ext.spec.Tenant;
import com.eka.middleware.server.MiddlewareServer;
import com.eka.middleware.template.SnippetException;
import com.eka.middleware.template.SystemException;

public class PropertyManager {
	private static final Map<String, Properties> propertiesMap = new ConcurrentHashMap<String, Properties>();
	public static Logger LOGGER = LogManager.getLogger(PropertyManager.class);
	private static Map<String, Long> lastModifiedMap = new ConcurrentHashMap<String, Long>();
	private static String configFolderPath;
	private static String local_IP = "localhost";

	public static Properties getProperties(String key) {
		return propertiesMap.get(key);
	}

	public static String getPackagePath(Tenant tenant) {
		if (tenant == null)
			return ServiceUtils.getServerProperty("middleware.server.home.dir");
		String tenantName = tenant.getName();
		String tenantDir = tenantName;
		// if(tenantName.equals("dev"))
		// tenantDir="";
		return ServiceUtils.getServerProperty("middleware.server.home.dir") + "tenants/" + tenantDir + "/";
	}
	
	public static String getPackagePathByTenantName(String tenantName) {
		String tenantDir = tenantName;
		// if(tenantName.equals("dev"))
		// tenantDir="";
		return ServiceUtils.getServerProperty("middleware.server.home.dir") + "tenants/" + tenantDir + "/";
	}

	public static final Properties getProperties(DataPipeline dataPipeLine, String fileName) throws SnippetException {
		String packageName = dataPipeLine.getCurrentResourceName();
		String packagePath = PropertyManager.getPackagePath(dataPipeLine.rp.getTenant()) + "packages/";
		if (fileName.startsWith("global"))
			packageName = "global";
		else {
			fileName = fileName.replace(".main", "");
		}
		// String fileName=thisPackageName+".properties";
		String key = fileName;
		String pkg[] = packageName.split(Pattern.quote("."));
		if (pkg.length == 1)
			packageName = pkg[0];
		else
			packageName = pkg[1];
		String path = packageName + "/dependency/config/" + fileName;
		Properties props = propertiesMap.get(key);
		try {
			File uriFile = new File(packagePath + "/" + path);
			if (!uriFile.exists()) {
				return new Properties();
			}
			byte[] bytes = readConfigurationAbsFile(uriFile);
			if (bytes != null) {
				ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
				if (props == null) {
					props = new Properties();
					propertiesMap.put(path, props);
				}
				loadServerProperties(bais, props);
				bais.close();
			} else if (props == null) {
				bytes = ServiceUtils.readAllBytes(uriFile);
				ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
				props = new Properties();
				loadServerProperties(bais, props);
				propertiesMap.put(path, props);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new SnippetException(dataPipeLine, "Error while getting property file '" + fileName + "'", e);
		}
		return props;
	}

	public static Properties getGlobalProperties(String tenantName) {
		String packagePath = PropertyManager.getPackagePathByTenantName(tenantName) + "packages/";
		String packageName = "global";
		String path = packageName + "/dependency/config/global.properties";
		File uriFile = new File(packagePath + "/" + path);
		Properties props = null;
		if (!uriFile.exists())
			return new Properties();
		if (hasfileChanged(path)) {
			try {
				byte[] bytes = FileUtils.readFileToByteArray(uriFile);
				ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
				props = new Properties();
				loadServerProperties(bais, props);
				propertiesMap.put(path, props);
			} catch (Exception e) {
				ServiceUtils.printException(Tenant.getTenant(tenantName), "Failed while loading global properties for '" + tenantName + "'", e);
			}
		}
		return props;
	}

	public static void saveGlobalProperties(String tenantName, Properties props) {
		String packagePath = PropertyManager.getPackagePathByTenantName(tenantName) + "packages/";
		String packageName = "global";
		String path = packageName + "/dependency/config/global.properties";
		saveProperties(packagePath + "/" + path, props, "global properties");
	}

	public static void saveProperties(String path, Properties props, String comment) {
		File uriFile = new File(path);
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		FileWriter fw = null;
		PrintWriter printWriter = null;

		try {
			fw = new FileWriter(uriFile);
			printWriter = new PrintWriter(fw);
			printWriter.println("# " + comment);
			final PrintWriter pw = printWriter;
			props.forEach((k, v) -> pw.println(k + "=" + v));
			fw.flush();
			printWriter.flush();
			fw.close();
			printWriter.close();
		} catch (Exception e) {
			ServiceUtils.printException("Could not save properties '" + path + "'", e);
		} finally {

			try {
				if (printWriter != null)
					printWriter.close();
				if (fw != null)
					fw.close();
			} catch (Exception e1) {
				ServiceUtils.printException("Could not close file output stream '" + path + "'", e1);
			}
		}
	}

	public static Map<String, String> typeCastConvert(Properties prop) {
		Map props = prop;
		Map<String, String> map = (Map<String, String>) props;
		return new HashMap<>(map);
	}

	public static final Properties getServerProperties(String filePath) throws SystemException {
		String absoluteFilePath = getConfigFolderPath() + filePath;
		File file = new File(absoluteFilePath);
		// absoluteFilePath=file.getAbsolutePath();
		Properties props = propertiesMap.get(absoluteFilePath);
		try {
			byte[] bytes = readConfigurationAbsFile(file);
			if (bytes != null) {
				ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
				if (props == null) {
					props = new Properties();
					propertiesMap.put(absoluteFilePath, props);
				}
				loadServerProperties(bais, props);
				bais.close();
			} else if (props == null) {
				bytes = ServiceUtils.readAllBytes(new File(absoluteFilePath));
				ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
				props = new Properties();
				loadServerProperties(bais, props);
				propertiesMap.put(absoluteFilePath, props);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new SystemException("EKA_MWS_1006", e);
		}
		return props;
	}

	private static final boolean loadServerProperties(ByteArrayInputStream bais, Properties properties)
			throws SystemException {
		try {
			properties.clear();
			properties.load(bais);
		} catch (Exception e) {
			throw new SystemException("EKA_MWS_1005", e);
		}
		return true;
	}

	public static boolean hasfileChanged(String filePath) {
		return true;
		/*
		 * String absoluteFilePath = getConfigFolderPath() + filePath; File file = new
		 * File(absoluteFilePath); Long lastModified =
		 * lastModifiedMap.get(absoluteFilePath); if (lastModified == null ||
		 * lastModified < file.lastModified()) { lastModified=file.lastModified();
		 * lastModifiedMap.put(absoluteFilePath, lastModified); return true; } return
		 * true;
		 */
	}

	public static boolean hasTenantFileChanged(String filePath) {
		return true;
		/*
		 * //String absoluteFilePath = getConfigFolderPath() + filePath; File file = new
		 * File(filePath); Long lastModified = lastModifiedMap.get(filePath); if
		 * (lastModified == null || lastModified < file.lastModified()) {
		 * lastModified=file.lastModified(); lastModifiedMap.put(filePath,
		 * lastModified); return true; } return true;
		 */
	}

	public static final byte[] readConfigurationFile(String filePath) throws SystemException {
		String absoluteFilePath = getConfigFolderPath() + filePath;
		File file = new File(absoluteFilePath);
		return readConfigurationAbsFile(file);
	}
	private static Object obj=new Object();
	public static final byte[] readConfigurationAbsFile(File file) throws SystemException {
		try {
			String absoluteFilePath = file.getAbsolutePath();
			if (!file.exists()) {
				throw new FileNotFoundException(file.getAbsolutePath());
			}
			Long lastModified = lastModifiedMap.get(absoluteFilePath);
			// File file = new File(uri);
			if (lastModified == null || lastModified < file.lastModified()) {
				byte[] bytes = null;
				synchronized (obj) {
					lastModified = lastModifiedMap.get(absoluteFilePath);
					if (lastModified == null || lastModified < file.lastModified()) {
						bytes = ServiceUtils.readAllBytes(file);
						lastModifiedMap.put(absoluteFilePath, file.lastModified());
					}
				}
				return bytes;
			}
		} catch (Exception e) {
			ServiceUtils.printException("EKA_MWS_1000 : " + file.getAbsolutePath(), e);
			throw new SystemException("EKA_MWS_1000", e);
		}
		return null;
	}

	public static final void initConfig(String[] args) throws Exception {
		InetAddress localhost = InetAddress.getLocalHost();
		local_IP = localhost.getHostAddress().trim();
		if (args == null || args.length == 0 || args[0].trim().length() == 0) {
			configFolderPath = MiddlewareServer.class.getResource("/").toString() + "resources/config/";
			configureLog4j();
			LOGGER.info("Please provide the path config folder(format:- D:\\server\\config\\)");
		} else { // */
			configFolderPath = args[0];
			File file = new File(configFolderPath);
			configFolderPath = file.getAbsolutePath();
			configureLog4j();
		}
		String ip = ServiceUtils.getServerProperty("middleware.server.remote_ip");
		if (ip != null) {
			local_IP = ip;
		}
		// System.out.println(configFolderPath);
	}

	private static void configureLog4j() throws Exception {
		LoggerContext context = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
		URL url = new File(getConfigFolderPath() + "log4j2.xml").toURL();
		// this will force a reconfiguration
		context.setConfigLocation(url.toURI());
	}

	public static final void writeConfigurationFile(String fileName, byte data[]) throws SystemException {
		try {
			String fullFilePath = getConfigFolderPath() + fileName;
			// URL url = new URL(profileFilePath);
			File file = new File(fullFilePath);
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(data);
			fos.flush();
			fos.close();
		} catch (Exception e) {
			throw new SystemException("EKA_MWS_1007", e);
		}
	}

	public static final void writeConfigurationFile(File file, byte data[]) throws SystemException {
		try {
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(data);
			fos.flush();
			fos.close();
		} catch (Exception e) {
			throw new SystemException("EKA_MWS_1007", e);
		}
	}

	public static String getConfigFolderPath() {
		return configFolderPath + "/";
	}

	public static String getLocal_IP() {
		return local_IP;
	}

}
