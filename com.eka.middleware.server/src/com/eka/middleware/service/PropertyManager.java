package com.eka.middleware.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URI;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.eka.middleware.server.ServiceManager;
import com.eka.middleware.template.SnippetException;
import com.eka.middleware.template.SystemException;

public class PropertyManager {
private static final Map<String, Properties> propertiesMap=new ConcurrentHashMap<String, Properties>();
public static Logger LOGGER = LogManager.getLogger(PropertyManager.class);
private static Map<String, Long> lastModifiedMap=new ConcurrentHashMap<String,Long>();
public static final Properties getProperties(DataPipeline dataPipeLine,String fileName) throws SnippetException {
	String packageName=dataPipeLine.getCurrentResource();
	if(fileName.startsWith("global"))
		packageName="global";
	else {
		fileName=fileName.replace(".main", "");
	}
	//String fileName=thisPackageName+".properties";
	String key=fileName;
	String pkg[]=packageName.split(Pattern.quote("."));
	if(pkg.length==1)
		packageName=pkg[0];
	else
		packageName=pkg[1];
	String path=packageName+"/dependency/config/" + fileName;
	Properties props=propertiesMap.get(key);
	try {
		File uriFile = new File(ServiceManager.packagePath+"packages/" + path);
		byte[] bytes=readConfigurationFile(packageName, uriFile.toURI());
		if(bytes!=null) {
			ByteArrayInputStream bais=new ByteArrayInputStream(bytes);
			if(props==null) {
				props=new Properties();
				propertiesMap.put(key, props);
			}
			ServiceUtils.loadServerProperties(bais, props);
			bais.close();
		}else if(props==null) {
			bytes=ServiceUtils.readAllBytes(uriFile);
			ByteArrayInputStream bais=new ByteArrayInputStream(bytes);
			props=new Properties();
			ServiceUtils.loadServerProperties(bais, props);
			propertiesMap.put(key, props);
		}
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		throw new SnippetException(dataPipeLine, "Error while getting property file '"+fileName+"'", e);
	}
	return props;
}

public static final byte[] readConfigurationFile(String packageName,URI uri) throws SystemException {
	try {
		Long lastModified=lastModifiedMap.get(packageName);
		File file = new File(uri);
		if(lastModified==null || lastModified<file.lastModified()) {
			byte[] bytes=null;
			synchronized (lastModifiedMap) {
				lastModified=lastModifiedMap.get(packageName);
				if(lastModified==null || lastModified<file.lastModified()) {
					bytes=ServiceUtils.readAllBytes(file);
					lastModifiedMap.put(packageName, file.lastModified());
				}
			}
			return bytes;
		}
	} catch (Exception e) {
		ServiceUtils.printException("EKA_MWS_1000 : "+packageName+" : "+uri, e);
		throw new SystemException("EKA_MWS_1000", e);
	}
	return null;
}


}
