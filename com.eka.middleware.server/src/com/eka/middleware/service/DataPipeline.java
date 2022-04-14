package com.eka.middleware.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.eka.middleware.auth.AuthAccount;
import com.eka.middleware.server.ServiceManager;
import com.eka.middleware.template.MultiPart;
import com.eka.middleware.template.SnippetException;

public class DataPipeline {
	private static Logger LOGGER = LogManager.getLogger(DataPipeline.class);
	private final RuntimePipeline rp;
	private final String resource;
	private MultiPart mp = null;
	private final Map<String, Map<String, Object>> payloadStack = new HashMap<String, Map<String, Object>>();
	private final Map<String, Boolean> hasDroppedPrevious = new HashMap<String, Boolean>();
	private final List<String> resourceStack = new ArrayList<String>();
	private String currentResource = null;
	private final String urlPath;

	public DataPipeline(RuntimePipeline runtimePipeline, String resource, String urlPath) {
		rp = runtimePipeline;
		this.resource = resource;
		this.urlPath = urlPath;
		payloadStack.put(resource, rp.payload);
		currentResource = resource;
		resourceStack.add(resource);
	}

	public Object get(String key) {
		String currentResource = getCurrentResource();
		Map<String, Object> map = payloadStack.get(currentResource);
		if (map != null) {
			Object value = map.get(key);
			if (value != null)
				return value;
		}

		if (hasDroppedPrevious.get(currentResource) != null && hasDroppedPrevious.get(currentResource)
				|| (hasDroppedPrevious.get(currentResource + "-" + key) != null
						&& hasDroppedPrevious.get(currentResource + "-" + key))) {
			return null;
		}

		int length = resourceStack.size();
		if (length > 1) {
			boolean bit = false;
			for (int i = length - 1; i >= 0; i--) {
				String resource = resourceStack.get(i);
				if (resource.equals(currentResource) || bit) {
					bit = true;
					Map<String, Object> map2 = payloadStack.get(resource);
					if (map2 != null) {
						Object value = map2.get(key);
						if (value != null)
							return value;
					}
				}
				if (hasDroppedPrevious.get(resource + "-" + key) != null
						&& hasDroppedPrevious.get(resource + "-" + key))
					break;
			}
		}
		return null;
	}

	public String getString(String key) {

		Object value = get(key);
		if (value != null) {
			try {
				ArrayDeque<String> dVal = (ArrayDeque<String>) value;
				if (dVal.size() > 0)
					return dVal.getFirst();
			} catch (Exception e) {
				return value.toString();
			}
		}

		/*
		 * String currentResource = getCurrentResource(); Map<String, Object> map =
		 * payloadStack.get(currentResource); if (map != null) { Object value =
		 * map.get(key); if (value != null) { try { ArrayDeque<String>
		 * dVal=(ArrayDeque<String>)value; if(dVal.size()>0) return dVal.getFirst();
		 * }catch(Exception e) { return value.toString(); } } }
		 * 
		 * if (hasDroppedPrevious.get(currentResource) != null &&
		 * hasDroppedPrevious.get(currentResource) ||
		 * (hasDroppedPrevious.get(currentResource + "-" + key) != null &&
		 * hasDroppedPrevious.get(currentResource + "-" + key))) { return null; }
		 * 
		 * int length = resourceStack.size(); if (length > 1) { boolean bit = false; for
		 * (int i = length - 1; i >= 0; i--) { String resource = resourceStack.get(i);
		 * if (resource.equals(currentResource) || bit) { bit = true; Map<String,
		 * Object> map2 = payloadStack.get(resource); if (map2 != null) { Object value =
		 * map2.get(key); if (value != null) { try { ArrayDeque<String>
		 * dVal=(ArrayDeque<String>)value; if(dVal.size()>0) return dVal.getFirst();
		 * }catch(Exception e) { return value.toString(); } } } } if
		 * (hasDroppedPrevious.get(resource + "-" + key) != null &&
		 * hasDroppedPrevious.get(resource + "-" + key)) break; } }
		 */
		return null;
	}

	public Integer getInteger(String key) throws SnippetException {
		Object value = get(key);
		if (value != null) {
			try {
				ArrayDeque<Integer> dVal = (ArrayDeque<Integer>) value;
				if (dVal.size() > 0)
					return dVal.getFirst();
			} catch (Exception e) {
				try {
					return (Integer) value;
				} catch (Exception ee) {
					throw new SnippetException(this, ee.getMessage(), ee);
				}
			}
		}
		return null;
	}

	public void refresh() {
		int length = resourceStack.size();
		String currentResource = getCurrentResource();
		if (length > 1) {
			for (int i = length - 1; i >= 0; i--) {
				String resource = resourceStack.get(i);
				if (resource.equals(currentResource)) {
					String prevResource = resourceStack.get(i + 1);
					Map<String, Object> mapCur = payloadStack.get(currentResource);
					Map<String, Object> mapPrev = payloadStack.get(prevResource);
					if (mapPrev != null) {
						//mapCur.putAll(mapPrev);
						mapPrev.forEach((k,v) -> {if(v!=null)mapCur.put(k, v);});
						payloadStack.remove(prevResource);
					}
					resourceStack.remove(i + 1);
					payloadStack.remove(prevResource);
					Set<String> keys = hasDroppedPrevious.keySet();

					for (Iterator<String> iterator = keys.iterator(); iterator.hasNext();) {
						String key = iterator.next();
						if (key.startsWith(prevResource)) {
							iterator.remove();
						}
					}

					return;
				}
			}
		}
	}

	public void drop(String key) {
		String currentResource = getCurrentResource();
		Map<String, Object> map = payloadStack.get(currentResource);
		if (map != null)
			map.remove(key);
		hasDroppedPrevious.put(currentResource + "-" + key, true);
	}

	public void setResponseStatus(int statusCode) throws SnippetException {
		rp.getExchange().setStatusCode(statusCode);
	}

	public void put(String key, Object value) {
		String currentResource = getCurrentResource();
		Map<String, Object> map = payloadStack.get(currentResource);
		if (map == null) {
			map = new HashMap<String, Object>();
			payloadStack.put(currentResource, map);
			//resourceStack.add(currentResource);
		}
		map.put(key, value);
		hasDroppedPrevious.put(resource + "-" + key, false);
	}

	public void putAll(Map<String, Object> value) {
		String currentResource = getCurrentResource();
		Map<String, Object> map = payloadStack.get(currentResource);
		if (map == null) {
			map = new HashMap<String, Object>();
			payloadStack.put(currentResource, map);
			//resourceStack.add(currentResource);
		}
		map.putAll(value);// (key, value);
		for (String key : map.keySet()) {
			hasDroppedPrevious.put(resource + "-" + key, false);
		}
	}

	public String getSessionId() {
		return rp.getSessionID();
	}

	public String getCorrelationId() {
		return rp.getCorrelationId();
	}

	public String getResource() {
		return resource;
	}

	public Map<String, Object> getPathParameters() {
		return (Map<String, Object>) rp.payload.get("pathParameters");
	}

	public Map<String, Object> getQueryParameters() {
		return (Map<String, Object>) rp.payload.get("parameters");
	}

	public Map<String, Object> getHeaders() {
		return (Map<String, Object>) rp.payload.get("requestHeaders");
	}

	public Map getAsMap(String pointer) {
		Map<String, Object> myMap = (Map<String, Object>) getValueByPointer(pointer);
		return myMap;
	}

	public List getAsList(String pointer) {
		Object list=getValueByPointer(pointer);
		if(list==null || list.getClass()!=ArrayList.class)
			return null;
		List<Object> arrayList = (List<Object>) list;
		return arrayList;
	}

	public Integer getAsInteger(String pointer) {
		Integer val = (Integer) getValueByPointer(pointer);
		return val;
	}
	
	public Boolean getAsBoolean(String pointer) {
		Boolean val = (Boolean) getValueByPointer(pointer);
		return val;
	}

	public String getAsString(String pointer) {
		String val = (String) getValueByPointer(pointer);
		return val;
	}
	
	

	public Object getValueByPointer(String pointer) {
		Object obj = null;
		pointer = "//" + pointer;
		pointer = pointer.replace("///", "").replace("//", "");
		Map<String, Object> map = null;
		List<Object> arrayList = null;
		boolean isNumeric = false;
		String[] tokenize = pointer.split("/");
		for (String key : tokenize) {
			isNumeric = NumberUtils.isCreatable(key);
			if (obj != null && !isNumeric)
				map = (Map<String, Object>) obj;
			else if (isNumeric)
				arrayList = (List<Object>) obj;
			if (map != null)
				obj = map.get(key);
			else if (arrayList != null) {
				int index=Integer.parseInt(key);
				if(arrayList.size()>index)
					obj = arrayList.get(index);
				else
					obj=null;
			}
			else
				obj = get(key);
			
			map = null;
			arrayList = null;
			if (obj == null)
				return null;
		}
		try {
			ArrayDeque dVal = (ArrayDeque) obj;
			if (dVal.size() > 0)
				return dVal.getFirst();
		} catch (Exception e) {
			return obj;
		}
		return null;
	}

	public void setValueByPointer(String pointer, Object value, String outTypePath) {
		Object obj = null;
		String path="";
		Object preObj=this;
		pointer = "//" + pointer;
		pointer = pointer.replace("///", "").replace("//", "").replace("#", "");
		boolean isNumeric = false;
		String[] pointerTokens = pointer.split("/");
		String[] typeTokens=outTypePath.split("/");
		int tokenCount = pointerTokens.length;
		int typeIndex=0;
		
		String valueType=(typeTokens[typeTokens.length-1]).toLowerCase();
		switch(valueType) {
			case "integer":
				value=Integer.parseInt(value+"");
				break;
			case "number":
				value=Double.parseDouble(value+"");
				break;
			case "boolean":
				value=Boolean.parseBoolean(value+"");
				break;
		}
		
		if (tokenCount==1) {
			put(pointerTokens[0], value);
			return;
		}
		String key;
		for (int i = 0; i < tokenCount-1; i++) {
			key=pointerTokens[i];
			isNumeric = NumberUtils.isCreatable(key);
			if(i==0)
				path=key;
			else
				path+="/"+key;
			obj=getValueByPointer(path);
			if(obj==null) {
				if(preObj.getClass().toString().contains("ArrayList") && isNumeric) {
					int index=Integer.parseInt(key);
					List<Object> list=(List)preObj;
					Map<String, Object> map=new HashMap<String, Object>();
					if(list.size()>index)
						list.add(index, map);
					else
						list.add(map);
					obj=map;
					preObj=obj;
				}else if(typeTokens[typeIndex].contains("List")) {
					List<Object> list=new ArrayList<Object>();
					if(preObj.getClass().toString().contains("DataPipeline")) {
						DataPipeline dp=(DataPipeline)preObj;
						dp.put(key, list);
						preObj=list;
					}else {
						Map<String, Object> map=(Map<String, Object>)preObj;
						map.put(key, list);
						preObj=list;
					}
				}else if(typeTokens[typeIndex].endsWith("document")) {
					obj=new HashMap<String, Object>();
					if(preObj.getClass().toString().contains("DataPipeline")) {
						DataPipeline dp=(DataPipeline)preObj;
						dp.put(key, obj);
					}else
						((Map)preObj).put(key, obj);
					preObj=obj;
				}
			}else {
				preObj=obj;
			}
			if(!isNumeric)
				typeIndex++;
		}
		key=pointerTokens[tokenCount-1];
		((Map)preObj).put(key, value);
	}

	public String toJson() {
		try {

			return ServiceUtils.toJson(payloadStack.get(currentResource));
		} catch (Exception e) {
			ServiceUtils.printException("Could not convert datapipeline '" + rp.getSessionID() + "' to Json", e);
		}
		;
		return null;
	}

	public String toXml() {
		try {
			String wrapperRootName = (String) get("@wrapperRootName");
			if (wrapperRootName == null)
				wrapperRootName = "root";
			return ServiceUtils.toXml(payloadStack.get(currentResource), wrapperRootName);
		} catch (Exception e) {
			ServiceUtils.printException("Could not convert datapipeline '" + rp.getSessionID() + "' to Xml", e);
		}
		return null;
	}

	public String toYaml() {
		try {

			return ServiceUtils.toYaml(payloadStack.get(currentResource));
		} catch (Exception e) {
			ServiceUtils.printException("Could not convert datapipeline '" + rp.getSessionID() + "' to Yaml", e);
		}
		;
		return null;
	}

	public void save(String name) {
		File file = new File(ServiceManager.packagePath + "/packages/" + (resource.replace(".", "/")) + "_" + name
				+ "_dataPipeline.json");

		LOGGER.info("Saving dataPipeline: " + file.getAbsolutePath());
		String dirname = file.getAbsolutePath().replace(file.getName(), "");
		File dir = new File(dirname);
		if (!dir.exists())
			dir.mkdirs();
		try {
			if (!file.exists()) {
				file.createNewFile();
			}
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("@sessionId", getSessionId());
			map.put("@correlationId", getCorrelationId());
			map.put("@resource", resource);
			Map<String, Object> payload = payloadStack.get(currentResource);
			payload.put("@DataPipeLineMetaData", map);
			String json = toJson();
			if (json != null) {
				FileOutputStream fos = new FileOutputStream(file);
				fos.write(json.getBytes());
				fos.flush();
				fos.close();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			ServiceUtils.printException("Could not save pipeline", e);
		}
	}

	public void restore(String name) {
		try {
			File file = new File(ServiceManager.packagePath + "/packages/" + (currentResource.replace(".", "/")) + "_"
					+ name + "_dataPipeline.json");
			byte[] data = ServiceUtils.readAllBytes(file);
			String json = new String(data);
			Map<String, Object> payload = ServiceUtils.jsonToMap(json);
			if (resource.equals(currentResource))
				rp.payload.putAll(payload);
			else
				payloadStack.put(currentResource, payload);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			ServiceUtils.printException("Could not save pipeline", e);
		}
	}

	public InputStream getBodyAsStream() throws SnippetException {
		return ServiceUtils.getBodyAsStream(this);
	}

	public InputStream getFile(String paramNameOrFileName) throws SnippetException {
		Object obj = getMultiPart().formData.get(paramNameOrFileName);
		if (obj == null)
			return null;
		InputStream is = (InputStream) obj;
		return is;
	}

	public String getFileName(String paramName) throws SnippetException {
		Object obj = getMultiPart().formData.get(paramName + "_fileName");
		return (String) obj;
	}

	public List<InputStream> getFiles() throws SnippetException {
		Object obj = getMultiPart().formData.get("uploadedFiles");
		if (obj == null)
			return null;
		List<InputStream> streams = (List<InputStream>) obj;
		return streams;
	}

	public String getFileName(InputStream fileStream) throws SnippetException {
		Object obj = getMultiPart().formData.get(fileStream);
		return (String) obj;
	}

	private byte body[] = null;

	public byte[] getBody() throws SnippetException {
		if (this.rp.getExchange()==null)
			throw new SnippetException(this, "Error while getting body from datapipeline.", new Exception(
					"Multipart entry can only be fetched from top level service stack called from outside the middleware server any async or pub/sub will cause exchange to close the session"));
		if (body == null)
			body = ServiceUtils.getBody(this);
		return body;
	}

	public void setBody(byte bytes[]) throws SnippetException {
		try {
			new MultiPart(this, bytes);
		} catch (Exception e) {
			throw new SnippetException(this, "Service('" + getCurrentResource() + "') failed while setting body bytes",
					e);
		}
	}

	public void setBody(InputStream is, boolean downloadAsStreamFile) throws SnippetException {
		try {
			new MultiPart(this, is, downloadAsStreamFile);
		} catch (Exception e) {
			throw new SnippetException(this,
					"Service('" + getCurrentResource() + "') failed while setting body streaming", e);
		}
	}

	public void setBody(File file) throws SnippetException {
		try {
			new MultiPart(this, file);
		} catch (Exception e) {
			throw new SnippetException(this, "Service('" + getCurrentResource() + "') failed while setting body file",
					e);
		}
	}

	public MultiPart getMultiPart() throws SnippetException {
		if (this.rp.getExchange()==null)
			throw new SnippetException(this, "Error while getting body from datapipeline.", new Exception(
					"Multipart entry can only be fetched from top level service stack called from outside the middleware server any async or pub/sub will cause exchange to close the session"));
		if (mp == null)
			mp = ServiceUtils.getMultiPart(this);
		return mp;
	}

	public String getCurrentResource() {
		// StackTraceElement[] ste = Thread.currentThread().getStackTrace();
		// Since its a private method it will be called
		// inside it's own class function so we get the
		// stack that called the last public function that
		// called this method
		//LOGGER.info("Current stack resource: " + currentResource);
		return currentResource;// ste[3].getClassName() + "." + ste[3].getMethodName();
	}

	public void clear() {
		String currentResource = getCurrentResource();
		if (currentResource.equals(resource))
			rp.payload.clear();
		hasDroppedPrevious.put(currentResource, true);
		Map<String, Object> map = payloadStack.get(currentResource);
		if (map != null)
			map.clear();
	}

	public void apply(String fqnOfMethod) throws SnippetException {
		fqnOfMethod = fqnOfMethod.replace("/", ".");
		if (payloadStack.get(fqnOfMethod) != null) {
			Exception e = new Exception("Can not apply method.");
			e.setStackTrace(Thread.currentThread().getStackTrace());
			throw new SnippetException(this, "Recursion is not allowed for method '" + fqnOfMethod + "'", e);
		}
		String curResourceBkp = currentResource;
		currentResource = fqnOfMethod;
		resourceStack.add(currentResource);
		try {
			ServiceUtils.execute(fqnOfMethod, this);
		} catch (SnippetException e) {
			// currentResource = curResourceBkp;
			// refresh();
			throw e;
			// ServiceUtils.printException("Error caused by "+fqnOfMethod, new
			// Exception(e));
		} finally {
			currentResource = curResourceBkp;
			refresh();
		}

	}
	
	public String getMyConfig(String key) throws SnippetException {
		Properties props = PropertyManager.getProperties(this, getCurrentResource() + ".properties");
		String value = props.getProperty(key);
		if (value != null && value.trim().toUpperCase().startsWith("#GLOBAL/")) {
			String vls[] = value.split("/");
			if (vls.length == 2 && vls[1] != null && vls[1].trim().length() > 0)
				key = vls[1];
			else
				return null;
			return getGlobalConfig(key);
		}else if (value != null && value.trim().toUpperCase().startsWith("#PACKAGE/")) {
			String vls[] = value.split("/");
			if (vls.length == 2 && vls[1] != null && vls[1].trim().length() > 0)
				key = vls[1];
			else
				return null;
			return getGlobalConfig(key);
		}
		return props.getProperty(key);
	}
	
	public String getMyPackageConfig(String key) throws SnippetException {
		Properties props = PropertyManager.getProperties(this, "package.properties");
		return props.getProperty(key);
	}
	
	public Properties getMyProperties() throws SnippetException {
		Properties props = PropertyManager.getProperties(this, getCurrentResource() + ".properties");
		return props;
	}

	public void logDataPipeline(Level level) {
		if (level == null)
			level = Level.INFO;
		String dpl = ServiceUtils.getFormattedLogLine(getCorrelationId(), currentResource, toJson());
		LOGGER.log(level, dpl);
	}

	public void logException(Throwable exception) {
		new SnippetException(this, "Eexception reported by " + getCurrentResource(), new Exception(exception));
	}

	public void logDataPipeline() {
		logDataPipeline(null);
	}

	public void log(String msg) {
		log(msg, null);
	}

	public void log(String msg, Level level) {
		if (level == null)
			level = Level.INFO;
		String log = ServiceUtils.getFormattedLogLine(getCorrelationId(), currentResource, msg);
		LOGGER.log(level, log);
	}

	public String getGlobalConfig(String key) throws SnippetException {
		Properties props = PropertyManager.getProperties(this, "global.properties");
		return props.getProperty(key);
	}

	public int getStackIndex() {
		return resourceStack.size() - 1;
	}

	public String getUrlPath() {
		return urlPath;
	}

	public AuthAccount getCurrentRuntimeAccount() throws SnippetException {
		return rp.getCurrentRuntimeAccount();
	}

	public void clearUserSession() throws SnippetException {
		rp.logOut();
	}
}
