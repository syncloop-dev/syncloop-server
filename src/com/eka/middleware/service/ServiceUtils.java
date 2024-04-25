package com.eka.middleware.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.sql.Connection;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
//import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.json.Json;
import javax.json.JsonObject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.graalvm.polyglot.Value;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.undertow.account.Pac4jAccount;
import org.springframework.util.AntPathMatcher;

import com.beust.jcommander.internal.Lists;
import com.eka.middleware.adapter.SQL;
import com.eka.middleware.auth.AuthAccount;
import com.eka.middleware.auth.Security;
import com.eka.middleware.auth.UserProfileManager;
import com.eka.middleware.auth.db.entity.Groups;
import com.eka.middleware.auth.db.entity.Users;
import com.eka.middleware.auth.db.repository.GroupsRepository;
import com.eka.middleware.auth.db.repository.TenantRepository;
import com.eka.middleware.auth.db.repository.UsersRepository;
import com.eka.middleware.flow.FlowResolver;
import com.eka.middleware.heap.CacheManager;
import com.eka.middleware.heap.HashMap;
import com.eka.middleware.pooling.ScriptEngineContextManager;
import com.eka.middleware.server.ServiceManager;
import com.eka.middleware.template.MultiPart;
import com.eka.middleware.template.SnippetException;
import com.eka.middleware.template.SystemException;
import com.eka.middleware.template.Tenant;
import com.eka.middleware.template.UriTemplate;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.common.collect.Maps;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;

import io.undertow.io.Receiver.FullBytesCallback;
import io.undertow.security.api.SecurityContext;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import io.undertow.server.handlers.CookieImpl;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.server.handlers.form.FormParserFactory;
import io.undertow.server.handlers.form.FormParserFactory.Builder;
import io.undertow.server.session.Session;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.Sessions;
import io.undertow.util.StatusCodes;

public class ServiceUtils {
	// private static final Properties serverProperties = new Properties();
	// private static final Properties urlMappings = new Properties();

	private static final Map<String, Properties> aliasMap = new ConcurrentHashMap<String, Properties>();

	private static final Map<String, UriTemplate> parameterResolverMap = new ConcurrentHashMap<String, UriTemplate>();
	private static final ObjectMapper om = new ObjectMapper();
	public static final XmlMapper xmlMapper = new XmlMapper();
	public static final YAMLMapper yamlMapper = new YAMLMapper();
	public static Logger LOGGER = LogManager.getLogger(ServiceUtils.class);
	private static final String OS = System.getProperty("os.name").toLowerCase();

	public static boolean isWindows() {
		return (OS.indexOf("win") >= 0);
	}

	public static List<String> searchEndpoints(final String keyword, Tenant tenant) {
		Properties urlMappings = getUrlAliasMapping(tenant);
		Set endpoints = urlMappings.keySet();
		final List<String> endpointList = new ArrayList<>();
		endpoints.forEach((k) -> {
			String key = k + "";
			String value = urlMappings.getProperty(key, "");
			if (keyword == null || keyword.trim().length() == 0 || value.toLowerCase().contains(keyword.toLowerCase())
					|| key.toLowerCase().trim().contains(keyword.toLowerCase()))
				endpointList.add(key + " : " + value);
		});
		return endpointList;
	}

	public static boolean isMac() {
		return (OS.indexOf("mac") >= 0);
	}

	public static boolean isUnix() {
		return (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0);
	}

	public static boolean isSolaris() {
		return (OS.indexOf("sunos") >= 0);
	}

	public static String getSeparator() throws Exception {
		if (isWindows()) {
			return ";";
		} else if (isMac()) {
			return ":";
		} else if (isUnix()) {
			return ":";
		} else if (isSolaris()) {
			return ":";
		} else {
			throw new Exception("Un supported OS");
		}
	}

	public static final String generateUUID(String msg) {
		UUID uuid = UUID.nameUUIDFromBytes(msg.getBytes());
		return uuid.toString();
	}

	public static final String toJson(Map<String, Object> map) throws Exception {
		try {
			om.setSerializationInclusion(JsonInclude.Include.NON_NULL);
			om.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
			String json = om.writeValueAsString(map);
			return json;
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception(e);
		}
	}
	
    public static Object convertPolyglotValue(Value value) {
        if (value.isNumber()) {
            if (value.fitsInInt()) {
                return value.asInt();
            } else if (value.fitsInLong()) {
                return value.asLong();
            } else {
                return value.asDouble();
            }
        } else if (value.isBoolean()) {
            return value.asBoolean();
        } else if (value.isString()) {
            return value.asString();
        } else if (value.hasArrayElements()) {
            return convertArray(value);
        } else if (value.hasMembers()) {
            return convertObject(value);
        }
        return null;
    }

    private static Map<String, Object> convertObject(Value value) {
        Map<String, Object> map = new HashMap<>();
        for (String key : value.getMemberKeys()) {
            map.put(key, convertPolyglotValue(value.getMember(key)));
        }
        return map;
    }

    private static List<Object> convertArray(Value value) {
        List<Object> list = new ArrayList<>();
        for (int i = 0; i < value.getArraySize(); i++) {
            list.add(convertPolyglotValue(value.getArrayElement(i)));
        }
        return list;
    }

	public static final String toJson(Object obj) {
		om.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		om.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
		String json = null;
		try {
			json = om.writeValueAsString(obj);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
		return json;
	}

	public static final String toPrettyJson(Map<String, Object> map) throws Exception {
		try {
			om.setSerializationInclusion(JsonInclude.Include.NON_NULL);
			om.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
			String json = om.writerWithDefaultPrettyPrinter().writeValueAsString(map);
			return json;
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception(e);
		}
	}

	public static final String objectToJson(Object object) throws Exception {
		om.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		om.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
		String json = om.writeValueAsString(object);
		return json;
	}

	public static final byte[] readAllBytes(File file) throws Exception {
		if (!file.exists())
			return null;
		FileInputStream fis = null;
		ByteArrayOutputStream baos = null;
		try {
			fis = new FileInputStream(file);
			baos = new ByteArrayOutputStream();
			IOUtils.copy(fis, baos);
			baos.flush();
			byte[] data = baos.toByteArray();
			baos.close();
			return data;
		} catch (Exception e) {
			throw e;
		} finally {
			fis.close();
			baos.close();
		}
	}

	public static final Map<String, Object> jsonToMap(String json) {
		try {
			om.disable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE);
			Map<String, Object> map = om.readValue(json, Map.class);
			return map;
		} catch (Exception e) {
			printException("Could not convert json to map", e);
		}
		return null;
	}

	public static final String toYaml(Map<String, Object> map) throws Exception {
		yamlMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
		String xml = yamlMapper.writeValueAsString(map);
		return xml;
	}

	public static final Map<String, Object> yamlToMap(String json) {
		try {
			yamlMapper.disable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE);
			Map<String, Object> map = yamlMapper.readValue(json, Map.class);
			return map;
		} catch (JsonProcessingException e) {
			printException("Could not convert xml to map", e);
		}
		return null;
	}

	public static final String xmlToString(Object o, String rootName) throws Exception {
		Map<String, Object> root = Maps.newHashMap();
		XmlMapper xmlMapper = new XmlMapper();
		if (StringUtils.isBlank(rootName)) {
			rootName = "";
		}

		root.put(rootName, o);
		String xml = xmlMapper.writeValueAsString(root);
		xml = xml.replaceAll("<HashMap>", "").replaceAll("</HashMap>", "");
		xml = xml.replaceAll("<>", "").replaceAll("</>", "");
		return xml;
	}

	public static final String toXml(Map<String, Object> map, String rootName) throws Exception {
		xmlMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
		xmlMapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);
		// xmlMapper.setDefaultUseWrapper(false);
		// xmlMapper.xmlBuilder().defaultUseWrapper(false)
		String xml = xmlMapper.writer().withRootName(rootName).writeValueAsString(map);
		return xml;
	}

	public static final Map<String, Object> xmlToMap(String json) {
		try {
			xmlMapper.disable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE);
			Map<String, Object> map = xmlMapper.readValue(String.format("<root>%s</root>", json), Map.class);
			return map;
		} catch (JsonProcessingException e) {
			printException("Could not convert xml to map", e);

		}
		return null;
	}

	public static final Map<String, Object> xmlToMapParser(String xml) {
		try {
			xmlMapper.disable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE);
			Map<String, Object> map = xmlMapper.readValue(String.format("<root>%s</root>", xml), Map.class);
			Map<String, Object> updatedMap = addAttributePrefix(map, xml);
			return updatedMap;
		} catch (JsonProcessingException e) {
			printException("Could not convert xml to map", e);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static Map<String, Object> addAttributePrefix(Map<String, Object> map, String xml) {
		Map<String, Object> updatedMap = new HashMap<>();
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if (value instanceof Map) {
				value = addAttributePrefix((Map<String, Object>) value, xml);
			}
			String newKey = key;
			List<String> attributeKeys = extractAttributeKeys(xml);
			for (String attributeKey : attributeKeys) {
				if (key.equalsIgnoreCase(attributeKey)) {
					newKey = "@" + key;
					break;
				}
			}
			updatedMap.put(newKey, value);
		}
		return updatedMap;
	}
	public static List<String> extractAttributeKeys(String xml) {
		List<String> attributeKeys = new ArrayList<>();
		Pattern pattern = Pattern.compile("\\s(\\w+)=\"");
		Matcher matcher = pattern.matcher(xml);
		while (matcher.find()) {
			String attributeKey = matcher.group(1);
			attributeKeys.add(attributeKey);
		}
		return attributeKeys;
	}
	public static Object getExceptionMap(Exception e) {
		Map<String, Object> lastErrorDump = new HashMap<>();
		if (e == null)
			e = new SystemException("EKA_MWS_1007",
					new Exception("Exception thrown is null but it's not clear if it's null pointer exception"));
		if (e instanceof SnippetException) {
			lastErrorDump.put("lastErrorDump", (SnippetException) e);
		} else {
			lastErrorDump.put("lastErrorDump", e);
		}

		String json = null;
		try {
			json = ServiceUtils.toJson(lastErrorDump);
			lastErrorDump = ServiceUtils.jsonToMap(json);
		} catch (Exception e2) {
			// TODO: handle exception
			lastErrorDump.clear();
			lastErrorDump.put("lastErrorDump", "");
		}
		return lastErrorDump.get("lastErrorDump");
	}

	public static final void execute(String fqn, DataPipeline dataPipeLine) throws SnippetException {
		if (!fqn.endsWith(".main"))
			fqn += ".main";
		if (fqn.startsWith("packages")) {
			ServiceManager.invokeJavaMethod(fqn, dataPipeLine);
		} else {
			executeEmbeddedService(dataPipeLine, CacheManager.getEmbeddedService(
					fqn.replaceAll("embedded.", "").replaceAll(".main", ""), dataPipeLine.rp.getTenant()));
		}
	}

	public static final void executeEmbeddedService(DataPipeline dataPipeline, String apiServiceJson)
			throws SnippetException {
		try {
			InputStream is = new ByteArrayInputStream(apiServiceJson.getBytes(StandardCharsets.UTF_8));
			JsonObject mainflowJsonObject = Json.createReader(is).readObject();
			FlowResolver.execute(dataPipeline, mainflowJsonObject);
		} catch (Throwable e) {
			dataPipeline.clear();
			dataPipeline.put("error", e.getMessage());
			dataPipeline.put("status", "Service error");
			throw new SnippetException(dataPipeline, "Failed to execute " + dataPipeline.getCurrentResource(),
					new Exception(e));
		}
	}

	public static final boolean isPublicFolder(String path) {
		// String str="/files/gui/middleware/pub/";
		String array[] = path.split("/pub/");
		if (array.length >= 2) {
			String preFixPath = array[0];
			String pArray[] = preFixPath.split("/files/gui/");
			if (pArray.length >= 2) {
				String packageName = pArray[1];
				if (!packageName.contains("/"))
					return true;
			}
		}
		return false;
	}

	public static final void compileJavaCode(String fqn, DataPipeline dataPipeLine) throws SnippetException {
		try {
			ScriptEngineContextManager.clear();
			ServiceManager.compileJava(fqn, dataPipeLine);
		} catch (Throwable e) {
			throw new SnippetException(dataPipeLine, "Error during compilation", new Exception(e));
		}
	}

	public static final void printException(String msg, Exception e) {
		String logLine = getLogLine(e, msg);
		LOGGER.error(logLine);
	}

	public static final void printException(Tenant tenant, String msg, Exception e) {
		String logLine = getLogLine(e, msg);
		tenant.logError(null, logLine);
		LOGGER.error(logLine);
	}

	public static final void printException(DataPipeline dp, String msg, Exception e) {
		String logLine = getLogLine(e, msg);
		dp.log(logLine, Level.ERROR);
		LOGGER.error(logLine);

	}

	private static String getLogLine(Exception e, String msg) {
		StringBuilder sb = new StringBuilder();
		StackTraceElement[] stackTrace = null;// e.getStackTrace();
		sb.append(msg);
		sb.append("\n");
		if (e != null) {
			sb.append(e.getMessage());
			stackTrace = e.getStackTrace();
		} else
			sb.append("Custom error");
		sb.append("\n");
		if (stackTrace == null)
			stackTrace = Thread.currentThread().getStackTrace();
		for (StackTraceElement stackTraceElement : stackTrace) {
			sb.append(stackTraceElement.toString());
			sb.append("\n");
		}
		return sb.toString();
	}

	public static final String getServerProperty(String key) {
		String val = null;
		try {
			Properties props = PropertyManager.getServerProperties("server.properties");
			val = props.getProperty(key);
		} catch (SystemException e) {
			e.printStackTrace();
		}

		return val;
	}

	private static Properties getUrlAliasMapping(Tenant tenant) {
		Properties urlMappings = aliasMap.get(tenant.getName());
		if (urlMappings == null) {
			urlMappings = new Properties();
			aliasMap.put(tenant.getName(), urlMappings);
		}
		return urlMappings;
	}

	public static byte[] compress(byte data[]) {
		Deflater def = new Deflater();
		def.setInput(data);
		def.finish();
		byte compString[] = new byte[data.length];
		int compSize = def.deflate(compString);
		String str = new String(compString, StandardCharsets.UTF_8).trim();
		def.end();
		return str.getBytes();
	}

	public static byte[] deCompress(byte data[]) throws DataFormatException {
		Inflater inf = new Inflater();
		inf.setInput(data);
		byte orgString[] = new byte[data.length];
		int orgSize = inf.inflate(orgString);
		String str = new String(orgString, StandardCharsets.UTF_8).trim();
		inf.end();
		return str.getBytes();
	}

	public static final String getPathService(String requestPath, Map<String, Object> payload, Tenant tenant) {

		if (requestPath.startsWith("GET/packages"))
			return requestPath.split("/")[1];
		try {
			String URLAliasFilePath = PropertyManager.getPackagePath(tenant) + "URLAliasMapping.properties";
			if (requestPath.contains("//")) {
				LOGGER.log(Level.INFO, requestPath);
				tenant.logInfo(null, requestPath);
				return null;
			}
			if (requestPath.endsWith("/")) {
				requestPath += "$$^%@#";
				requestPath = requestPath.replace("/$$^%@#", "");
			}
			final Properties urlMappings = getUrlAliasMapping(tenant);
			Map<String, String> pathParams = new HashMap<String, String>();
			boolean reload = PropertyManager.hasTenantFileChanged(URLAliasFilePath);
			if (reload) {
				Properties props = PropertyManager.getProperties(URLAliasFilePath);
				if (props == null)
					props = new Properties();
				props.load(new FileInputStream(new File(URLAliasFilePath)));
				Set<Object> keys = props.keySet();
				// Add keys
				for (Object keyStr : keys) {
					String key = keyStr.toString();
					urlMappings.put(key, props.get(key));
					UriTemplate parameterResolver = new UriTemplate(key);
					parameterResolverMap.put(tenant.getName() + "-" + key, parameterResolver);
				}
				// Remove Keys
				keys = urlMappings.keySet();
				for (Object keyStr : keys) {
					String key = keyStr.toString();
					if (props.get(key) == null) {
						urlMappings.remove(key);
						parameterResolverMap.remove(tenant.getName() + "-" + key);
					}
				}
			}
			String serviceName = urlMappings.getProperty(requestPath + "/*");
			if (serviceName == null)
				serviceName = urlMappings.getProperty(requestPath);
			if (serviceName == null && payload != null) {
				Set<Object> keys = urlMappings.keySet();
				for (Object keyStr : keys) {
					UriTemplate parameterResolver = parameterResolverMap.get(tenant.getName() + "-" + keyStr);
					if (parameterResolver == null) {
						continue;
					}
					// if (parameterResolver.match(requestPath)) {
					pathParams = parameterResolver.matcher(requestPath);
					if (pathParams != null && pathParams.size() > 0) {
						serviceName = urlMappings.getProperty(keyStr.toString());
						payload.put("pathParameters", pathParams);
						break;
					}
					// }
				}
			}
			return serviceName;

		} catch (Exception e) {
			ServiceUtils.printException(tenant, "Failed during evaluating resource path", e);
		}
		return null;
	}

	public static final Map<String, Object> extractHeaders(HttpServerExchange exchange) {
		Map<String, Object> map = new HashMap<String, Object>();
		HeaderMap hm = exchange.getRequestHeaders();
		Collection<HttpString> hts = hm.getHeaderNames();
		for (HttpString httpString : hts) {
			map.put(httpString.toString(), hm.get(httpString).getFirst());
		}
		return map;
	}

	public static final String getFormattedLogLine(String id, String resource, String info) {
		StringBuilder log = new StringBuilder();
		log.append(id);
		log.append("    ");
		log.append(resource);
		log.append("    ");
		log.append(info);
		return log.toString();
	}

	public static final Map<String, Object> getFormattedLogMap(String id, String resource, String info) {
		Map<String, Object> map = new HashMap<>();
		map.put("id", id);
		map.put("resource", resource);
		map.put("log", info);
		return map;
	}

	public static final MultiPart getMultiPart(DataPipeline dataPipeLine) throws SnippetException {
		String sessionId = dataPipeLine.getSessionId();
		RuntimePipeline rp = RuntimePipeline.getPipeline(sessionId);
		final HttpServerExchange exchange = rp.getExchange();

		Builder builder = FormParserFactory.builder();

		final FormDataParser formDataParser = builder.build().createParser(exchange);
		if (formDataParser != null) {

//			try {
//				ExecutorService threadpool = Executors.newCachedThreadPool();
//				@SuppressWarnings("unchecked")
//				Future<Long> futureTask = (Future<Long>) threadpool.submit(() -> {
			try {
				// BlockingHttpExchange bht = exchange.startBlocking();
				FormData formData = null;// new FormData(0);
				try {
					formData = formDataParser.parseBlocking();
				} catch (Exception e) {
					e.printStackTrace();
					throw new SnippetException(dataPipeLine,
							"Could not fetch formData for multipart request.\n" + e.getMessage(), e);
				}
				Map<String, Object> formDataMap = new HashMap<String, Object>();
				List<InputStream> files = new ArrayList<InputStream>();
				List<String> filesList = new ArrayList<>();
				for (String key : formData) {
					for (FormData.FormValue formValue : formData.get(key)) {
						if (formValue.isFileItem()) {
							if (key != null && key.trim().length() > 0) {
								formDataMap.put(key + "_fileName", formValue.getFileName());
								formDataMap.put(key, formValue.getFileItem().getInputStream());
								filesList.add(key);
							}
							InputStream is = formValue.getFileItem().getInputStream();
							files.add(is);
							formDataMap.put(formValue.getFileName(), is);
						} else
							formDataMap.put(key, formValue.getValue());
					}
				}
				formDataMap.put("uploadedFiles", files);
				formDataMap.put("keys", filesList);
				MultiPart mp = new MultiPart(formDataMap);
				rp.payload.put("*multiPartRequest", mp);
			} catch (Exception e) {
				ServiceUtils.printException(rp.getTenant(), rp.getSessionID() + " Could not stream file thread.", e);
			}
//				});
//
//				while (!futureTask.isDone()) {
//					Thread.sleep(100);
//				}
//
//				threadpool.shutdown();
//
//			} catch (Exception e) {
//				e.printStackTrace();
//				throw new SnippetException(dataPipeLine, e.getMessage(), e);
//			}

		}
		MultiPart mp = null;
		Map<String, Object> payload = rp.payload;
		if (payload.get("*multiPartRequest") != null) {
			mp = (MultiPart) payload.get("*multiPartRequest");
			payload.remove("*multiPartRequest");
		}
		return mp;
	}

	public static final String getURLAlias(String fqn, Tenant tenant) throws Exception {
		Properties urlMappings = getUrlAliasMapping(tenant);
		Set<Object> sset = urlMappings.keySet();
		for (Object setKey : sset) {
			if (fqn.equalsIgnoreCase(urlMappings.get(setKey).toString()))
				return setKey.toString();
		}
		return null;
	}

	public static final String registerURLAlias(String fqn, String alias, DataPipeline dp) throws Exception {

		String aliasTenantName = dp.rp.getTenant().getName();
		String existingFQN = getPathService(alias, null, dp.rp.getTenant());

		if (!isAliasValid(alias)) {
			return "Failed to save. The provided alias is incorrect.";
		}

		AntPathMatcher antPathMatcher = new AntPathMatcher();
		if (antPathMatcher.match(alias.replace("GET", "").replace("POST", "").replace("PUT", " ").replace("PATCH", " "),
				"/restrictUrl")) {
			return "Invalid alias. Alias cannot start /{}";
		}

		Properties urlMappings = getUrlAliasMapping(dp.rp.getTenant());

		for (Object key : urlMappings.keySet()) {
			String mappedAlias = key.toString();
			if (antPathMatcher.match(mappedAlias, alias)) {
				return "Failed to save. It already exists.";
			}
		}

		String msg = "Saved";
		if (existingFQN == null || existingFQN.equalsIgnoreCase(fqn)) {
			Set<Object> sset = urlMappings.keySet();
			for (Object setKey : sset) {
				if (fqn.equalsIgnoreCase(urlMappings.get(setKey).toString()))
					urlMappings.remove(setKey);
			}
			urlMappings.setProperty(alias, fqn);
			FileOutputStream fos = new FileOutputStream(
					new File(PropertyManager.getPackagePath(dp.rp.getTenant()) + "URLAliasMapping.properties"));
			urlMappings.store(fos, "");
			fos.flush();
			fos.close();
		} else {
			msg = "Failed to save. The alias conflicts with the existing FQN (" + existingFQN + ").";
			return msg;
		}

		return msg;
	}

	public static final String deleteURLAlias(String alias, DataPipeline dp) throws Exception {
		String aliasTenantName = dp.rp.getTenant().getName();
		String existingFQN = getPathService(alias, null, dp.rp.getTenant());

		Properties urlMappings = getUrlAliasMapping(dp.rp.getTenant());

		boolean aliasFound = false;
		for (Object key : urlMappings.keySet()) {
			String mappedAlias = key.toString();
			if (mappedAlias.equals(alias)) {
				urlMappings.remove(key);
				aliasFound = true;
				break;
			}
		}

		if (!aliasFound) {
			return "Alias not found. Nothing to delete.";
		}

		FileOutputStream fos = new FileOutputStream(
				new File(PropertyManager.getPackagePath(dp.rp.getTenant()) + "URLAliasMapping.properties"));
		urlMappings.store(fos, "");
		fos.flush();
		fos.close();

		return "Alias deleted successfully.";
	}

	private static boolean isAliasValid(String alias) {
		if (alias.contains("?")) {
			return false; // Alias contains a query parameter
		}

		// Exclude aliases with invalid characters
		String invalidCharacters = ".*[^a-zA-Z0-9_.\\-/{}]+.*";
		if (alias.matches(invalidCharacters)) {
			return false; // Alias contains special characters
		}
		if (alias.matches(".*\\{.\\}[a-zA-Z].*")) { // Alias contains dynamic pattern such as Y in {X}Y
			return false;
		}
		if (alias.matches(".*\\{[a-zA-Z0-9]+\\}[a-zA-Z0-9]+.*")) {
			return false; // The pattern matches "{X}Y" with alphanumeric X and Y.
		}
		if (alias.contains("./") || alias.contains("/.") || alias.contains("/./") || alias.contains("/{}")
				|| alias.contains("{}")) {
			return false; // Restrict If The alias contains one of the specified patterns.
		}

		if (!areBracesBalanced(alias)) {
			return false;
		}

		return true;
	}

	public static boolean areBracesBalanced(String input) {
		int braceCounter = 0;

		for (char c : input.toCharArray()) {
			if (c == '{') {
				braceCounter++;
			} else if (c == '}') {
				braceCounter--;
			}

			if (braceCounter < 0) {
				return false;
			}
		}

		return braceCounter == 0;
	}

	private static final void streamResponseFile(final RuntimePipeline rp, final MultiPart mp) throws SnippetException {

		try {
			handleFileResponse(rp.getExchange(), mp);
		} catch (Exception e) {
			e.printStackTrace();
			// TODO Auto-generated catch block
			throw new SnippetException(rp.dataPipeLine, "Exception while streaming file.\n" + e.getMessage(), e);

		} /*
			 * // Thread newThread = new Thread(() -> {
			 *
			 * ExecutorService threadpool = Executors.newCachedThreadPool();
			 *
			 * @SuppressWarnings("unchecked") Future<Long> futureTask = (Future<Long>)
			 * threadpool.submit(() -> { try { handleFileResponse(rp.getExchange(), mp); }
			 * catch (Exception e) { // TODO Auto-generated catch block
			 * ServiceUtils.printException(rp.getSessionID() +
			 * " Could not stream file thread.", e); } });
			 *
			 * while (!futureTask.isDone()) { //
			 * System.out.println("FutureTask is not finished yet..."); Thread.sleep(100); }
			 *
			 * threadpool.shutdown();
			 *
			 */
	}

	private static void handleFileResponse(HttpServerExchange exchange, MultiPart mp) throws Exception {
		OutputStream os = null;
		InputStream inputStream = null;
		try {
			File tempFile = mp.file;

			if (tempFile == null)
				inputStream = mp.is;
			else
				inputStream = new FileInputStream(tempFile);
//			exchange.startBlocking();
			Set<String> headers = mp.headers.keySet();

			for (String key : headers) {
				exchange.getResponseHeaders().put(HttpString.tryFromString(key), mp.headers.get(key).toString());
			}
			os = exchange.getOutputStream();
			if (tempFile != null)
				IOUtils.copy(inputStream, os);
			else
				IOUtils.copyLarge(inputStream, os);
			os.flush();
			os.close();
			inputStream.close();
			exchange.endExchange();

		} finally {
			if (os != null)
				os.close();
			if (inputStream != null)
				inputStream.close();
		}
	}

	public static final void startStreaming(final RuntimePipeline rp, final MultiPart mp) throws SnippetException {
		if (mp != null) {
			if (mp.type.equals("file"))
				streamResponseFile(rp, mp);
			else if (mp.type.equals("body"))
				streamBodyResponse(rp, mp);
		}
	}

	private static final void streamBodyResponse(final RuntimePipeline rp, final MultiPart mp) throws SnippetException {
		try {
			handleBodyResponse(rp.getExchange(), mp);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			ServiceUtils.printException(rp.getTenant(), rp.getSessionID() + " Could not stream body thread.", e);
		}
//		try {
//			ExecutorService threadpool = Executors.newCachedThreadPool();
//			@SuppressWarnings("unchecked")
//			Future<Long> futureTask = (Future<Long>) threadpool.submit(() -> {
//				
//			});
//
//			while (!futureTask.isDone()) {
//				// System.out.println("FutureTask is not finished yet...");
//				Thread.sleep(100);
//			}
//
//			threadpool.shutdown();
//
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			throw new SnippetException(rp.dataPipeLine, "Exception while streaming body.\n" + e.getMessage(), e);
//
//		}
	}

	private static void handleBodyResponse(HttpServerExchange exchange, MultiPart mp) throws Exception {

		InputStream targetStream = null;
		OutputStream os = null;
		try {
			if (mp.is == null)
				targetStream = new ByteArrayInputStream(mp.body);
			else
				targetStream = mp.is;
//		exchange.startBlocking();
			Set<String> headers = mp.headers.keySet();

			for (String key : headers) {
				exchange.getResponseHeaders().put(HttpString.tryFromString(key), mp.headers.get(key).toString());
			}
			os = exchange.getOutputStream();
			// System.out.println("Available bytes:" + targetStream.available());
//		byte bytes[]=targetStream.readNBytes(200);
//		System.out.println(new String(bytes));
			IOUtils.copy(targetStream, os);
			os.flush();
			os.close();
			// os=null;
			targetStream.close();
			// targetStream=null;
			exchange.endExchange();

		} catch (Throwable e) {
			e.printStackTrace();
		} finally {
			if (os != null)
				os.close();
			if (targetStream != null)
				targetStream.close();
		}
	}

	public static byte[] getBody(DataPipeline dataPipeLine) throws SnippetException {
		try {
			final RuntimePipeline rp = RuntimePipeline.getPipeline(dataPipeLine.getSessionId());
			final Map<String, Object> payload = rp.payload;
			try {
				rp.getExchange().getRequestReceiver().setMaxBufferSize(1024);
				rp.getExchange().getRequestReceiver().receiveFullBytes(new FullBytesCallback() {
					@Override
					public void handle(HttpServerExchange exchange, byte[] body) {
						payload.put("@body", body);
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
				ServiceUtils.printException(rp.getTenant(), rp.getSessionID() + " Could not stream body thread.", e);
			}

			if (payload.get("@body") != null) {
				byte body[] = (byte[]) payload.get("@body");
				payload.remove("@body");
				return body;
			}

		} catch (Exception e) {
			e.printStackTrace();
			throw new SnippetException(dataPipeLine, e.getMessage(), e);
		}
		return null;
	}

	public static final InputStream getBodyAsStream(final DataPipeline dataPipeLine) throws SnippetException {
		try {
			String sessionId = dataPipeLine.getSessionId();
			RuntimePipeline rp = RuntimePipeline.getPipeline(sessionId);
			final HttpServerExchange exchange = rp.getExchange();
			rp.payload.put("@bodyStream", exchange.getInputStream());
			if (rp.payload.get("@bodyStream") != null) {
				InputStream is = (InputStream) rp.payload.get("@bodyStream");
				rp.payload.remove("@bodyStream");
				return is;
			}
		} catch (Exception e) {
			throw new SnippetException(dataPipeLine, e.getMessage(), e);
		}
		return null;
	}

	public static void copyDirectory(String sourceDirectoryLocation, String destinationDirectoryLocation)
			throws IOException {
		LOGGER.info("Copying files from : " + sourceDirectoryLocation.toString());
		LOGGER.info("Copying files to : " + destinationDirectoryLocation.toString());

		FileUtils.copyDirectory(new File(sourceDirectoryLocation), new File(destinationDirectoryLocation));
		LOGGER.info("Tenant instance created: " + destinationDirectoryLocation.toString());
	}

	public static String setupRequestPath(HttpServerExchange exchange) {

		Cookie cookie = exchange.getRequestCookie("tenant");
		String tenantName = null;
		try {
			AuthAccount acc = UserProfileManager.getUserProfileManager()
					.getAccount(ServiceUtils.getCurrentLoggedInUserProfile(exchange));
			tenantName = (String) acc.getAuthProfile().get("tenant");
		} catch (Exception ignore) {

		}
		String token = null;
		if (cookie != null && tenantName == null) {
			tenantName = ServiceUtils.getTenantName(cookie);
			token = ServiceUtils.getToken(cookie);
		}

		String rqp = exchange.getRequestPath();
		String rsrcTokens[] = null;
		if (rqp != null)
			rsrcTokens = ("b" + rqp).split("/");

		if (rsrcTokens != null) {
			if (rsrcTokens.length > 2 && rsrcTokens[1].equalsIgnoreCase("tenant")) {
				String tName = rsrcTokens[2];
				if (tenantName == null)
					tenantName = tName;
				if (!tName.equals(tenantName))
					exchange.setRequestPath(rqp.replace("/tenant/" + tName, "/tenant/" + tenantName));
				ServiceUtils.setupCookie(exchange, tenantName, token);
			}
			if (tenantName == null) {
				Cookie cukie = ServiceUtils.setupCookie(exchange, tenantName, token);
				tenantName = getTenantName(cukie);
				exchange.setRequestPath("/tenant/" + tenantName + rqp);
				if (!ServiceUtils.isApiCall(exchange))
					exchange.setResponseCookie(cukie);
			}
		}
		return tenantName;
	}

	public static void manipulateHeaders(HttpServerExchange exchange) {

	}

	public static Cookie setupCookie(HttpServerExchange exchange, String tenantName, String token) {
		Cookie cookie = exchange.getRequestCookie("tenant");
		if (cookie == null || cookie.isDiscard()) {
			if (tenantName == null)
				tenantName = "default";
			cookie = new CookieImpl("tenant", tenantName);
			cookie.setPath("/");
			if (!ServiceUtils.isApiCall(exchange))
				exchange.setResponseCookie(cookie);
		}
		if (tenantName == null)
			tenantName = getTenantName(cookie);
		else if (getToken(cookie) == null)
			cookie.setValue(tenantName);
		// cookie.setSecure(true);
		if (token != null) {
			cookie.setValue(tenantName + " " + token);
		}
		cookie.setPath("/");
		// if(!ServiceUtils.isApiCall(exchange))
		exchange.setResponseCookie(cookie);
		// exchange.
//		((Set<Cookie>)((DelegatingIterable<Cookie>)responseCookies()).getDelegate()).add(cookie);
//		exchange.responseCookies().forEach(null);
//		exchange.remove

		return cookie;
	}

	public static void clearSession(HttpServerExchange exchange) {
		Session session = Sessions.getSession(exchange);
		if (session == null)
			return;
		HashSet<String> names = new HashSet<>(session.getAttributeNames());
		for (String attribute : names) {
			session.removeAttribute(attribute);
		}
		session.invalidate(exchange);
	}

	public static String getToken(Cookie cookie) {
		String value = cookie.getValue();
		String tokenize[] = value.split(" ");
		String token = null;

		if (tokenize.length >= 2)
			token = tokenize[1];

		return token;
	}

	public static String getTenantName(Cookie cookie) {
		String value = cookie.getValue();
		String tokenize[] = value.split(" ");
		String tenantName = tokenize[0];
		return tenantName;
	}

	public static String initNewTenant(String name, AuthAccount account, String password) {
		try {
			if (TenantRepository.exists(name)) {
				String msg = ("Tenant already exists or null. Tenant Name : " + name);
				LOGGER.error(msg);
				return msg;
			}
			List<String> groups = null;
			if (account.getAuthProfile() != null)
				groups = (List<String>) account.getAuthProfile().get("groups");
			if (groups == null) {
				groups = new ArrayList<String>();
				LOGGER.warn(
						"Claim name 'groups' and 'tenant' are required in jwt access_token to allow users to use same tenant otherwise new user will have an option to create his own tenant. Tenant:"
								+ name);
			}
			account.getAuthProfile().put("tenant", name);
			String src = PropertyManager.getPackagePath(null) + "released" + File.separator + "core";
			String srcZip = PropertyManager.getPackagePath(null) + "released" + File.separator
					+ "core/syncloop-distribution-latest.zip";
			String dest = PropertyManager.getPackagePathByTenantName(name);

			groups.add(AuthAccount.STATIC_ADMIN_GROUP);
			groups.add(AuthAccount.STATIC_DEVELOPER_GROUP);
			account.getAuthProfile().put("groups", groups);
			account.getAuthProfile().put("tenant", name);
			Users user = UserProfileManager.addUser(account, password);

			LOGGER.info("New user(" + account.getUserId() + ") added for the tenant " + name + " successfully.");
			// }

			Runnable runnable = new Runnable() {
				@Override
				public void run() {
					try {
//						copyDirectory(src, dest);
						ServiceUtils.unzipBuildFile(srcZip, dest);
						Security.generateKeyPair(name);
						Security.setupTenantSecurity(name);
						Tenant.getTenant(name).logInfo(null, "New user(" + account.getUserId()
								+ ") added for the tenant " + name + " successfully.");
						LOGGER.info("Starting newly created tenant(" + name + ")......................");
						Tenant.getTenant(name).logInfo(null,
								"Starting newly created tenant(" + name + ")......................");
						startTenantServices(name);
						int tenantId = user.getTenant();// UserProfileManager.newTenant(name);
						LOGGER.info("New tenant with name " + name + " created successfully.");
						Tenant.getTenant(name).logInfo(null,
								"New tenant with name " + name + " created and started successfully.");

						List<Groups> groupList = Lists.newArrayList();
						Groups group = new Groups();
						group.setGroupName(AuthAccount.STATIC_ADMIN_GROUP);
						group.setTenantId(tenantId);
						group.setDeleted(0);
						group.setCreated_date(new Timestamp(new Date().getTime()));
						group.setModified_date(new Timestamp(new Date().getTime()));
						groupList.add(group);
						GroupsRepository.addGroup(group);

						group = new Groups();
						group.setGroupName(AuthAccount.STATIC_DEVELOPER_GROUP);
						group.setTenantId(tenantId);
						group.setDeleted(0);
						group.setCreated_date(new Timestamp(new Date().getTime()));
						group.setModified_date(new Timestamp(new Date().getTime()));
						groupList.add(group);
						GroupsRepository.addGroup(group);

						try (Connection conn = SQL.getProfileConnection(false)) {
							UsersRepository.addGroupsForUser(conn, user.getId(), groupList);
						}

					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			};

			// new Thread(runnable).start();
			runnable.run();

		} catch (Exception e) {
			printException("Failed to create tenant with name " + name, e);
			return e.getMessage();
		}
		return "Done";
	}

	public static void startTenantServices(String tenant) throws SnippetException {
		String uuid = UUID.randomUUID().toString();
		final RuntimePipeline rp = RuntimePipeline.create(Tenant.getTenant(tenant), uuid, uuid, null,
				"packages.middleware.pub.server.core.service.main",
				"/packages.middleware.pub.server.core.service.main");
		LOGGER.trace("RP created for " + tenant);
		LOGGER.trace("Executing startup servies for " + tenant);
		rp.dataPipeLine.applyAsync("packages.middleware.pub.server.core.service");
	}

	public static final URL[] getClassesURLs(String path) throws Exception {
		List<URL> urls = new ArrayList<URL>();
		String paths[] = path.split(getSeparator());

		for (String location : paths) {
			File file = new File(location);
			LOGGER.info("Class file path: " + file.getAbsolutePath());
			if (file.getName().contains("ekamw"))
				LOGGER.info(file.getAbsolutePath());
			if (file.isFile())
				urls.add(file.toURL());
		}

		if (urls.size() > 0)
			return urls.toArray(new URL[urls.size()]);

		return null;
	}

	public static UserProfile getCurrentLoggedInUserProfile(HttpServerExchange exchange) throws SnippetException {
		final SecurityContext context = exchange.getSecurityContext();
		if (context != null)
			return ((Pac4jAccount) context.getAuthenticatedAccount()).getProfile();
		return null;
	}

	public static AuthAccount getCurrentLoggedInAuthAccount(HttpServerExchange exchange) throws SnippetException {
		if (getCurrentLoggedInUserProfile(exchange) == null)
			return null;
		return UserProfileManager.getUserProfileManager().getAccount(getCurrentLoggedInUserProfile(exchange));
	}

	public static SecretKeySpec getKey(final String myKey) {
		SecretKeySpec secretKey = null;
		byte[] key;
		MessageDigest sha = null;
		try {
			key = myKey.getBytes("UTF-8");
			sha = MessageDigest.getInstance("SHA-1");
			key = sha.digest(key);
			key = Arrays.copyOf(key, 16);
			secretKey = new SecretKeySpec(key, "AES");
		} catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
			printException("Could not create secretKey.", e);
		}
		return secretKey;
	}

	public static String encrypt(final String strToEncrypt, final String tenantName) {
		try {
			// setKey(secret);
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			RSAPublicKey rsaPublicKey = (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(Tenant.getTenant(tenantName).getPublicKey().getEncoded()));

			Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
			cipher.init(Cipher.ENCRYPT_MODE, rsaPublicKey);
			return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes("UTF-8")));
		} catch (Exception e) {
			printException(Tenant.getTenant(tenantName), "Error while encrypting: ", e);
		}
		return null;
	}

	public static String decrypt(final String strToDecrypt, final String tenantName) throws Exception {
		try {
			// setKey(secret);
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			RSAPrivateKey rsaPrivateKey = (RSAPrivateKey) keyFactory.generatePrivate(new PKCS8EncodedKeySpec(Tenant.getTenant(tenantName).getPrivateKey().getEncoded()));

			Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
			cipher.init(Cipher.DECRYPT_MODE, rsaPrivateKey);
			return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));
		} catch (Exception e) {
			printException(Tenant.getTenant(tenantName), "Error while decrypting: ", e);
			throw e;
		}
	}

	public static Date addHoursToDate(Date date, int hours) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.HOUR_OF_DAY, hours);
		return calendar.getTime();
	}

	public static final String[] getJarPaths(String path, String packagePath) throws Exception {
		URL urls[] = null;
		String paths[] = null;
		LOGGER.debug("JAR PATH: " + packagePath + path);
		File file = new File(packagePath + path);
		if (file.isDirectory()) {
			File files[] = file.listFiles();
			if (files.length > 0) {
				urls = new URL[files.length];
				paths = new String[files.length];
				int indx = 0;
				for (File fileItem : files) {
					if (fileItem.getName().endsWith(".jar")) {

						urls[indx] = fileItem.toURL();
						paths[indx] = fileItem.getAbsolutePath();
						indx++;
					}
				}
				if (indx > 0) {
					LOGGER.debug("JAR PATH(" + indx + "): " + packagePath + path);
					return paths;
				}
			}
		}
		return null;
	}

	public static void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
		if (fileToZip.isHidden()) {
			return;
		}
		if (fileToZip.isDirectory()) {
			if (fileName.endsWith("/")) {
				zipOut.putNextEntry(new ZipEntry(fileName));
				zipOut.closeEntry();
			} else {
				zipOut.putNextEntry(new ZipEntry(fileName + "/"));
				zipOut.closeEntry();
			}
			File[] children = fileToZip.listFiles();
			for (File childFile : children) {
				zipFile(childFile, fileName + "/" + childFile.getName(), zipOut);
			}
			return;
		}
		FileInputStream fis = new FileInputStream(fileToZip);
		ZipEntry zipEntry = new ZipEntry(fileName);
		zipOut.putNextEntry(zipEntry);
		byte[] bytes = new byte[1024];
		int length;
		while ((length = fis.read(bytes)) >= 0) {
			zipOut.write(bytes, 0, length);
		}
		fis.close();
	}

	public static OIDCProviderMetadata fetchMetadata(String discoveryUrl, JWSAlgorithm jwsAlgo) throws Exception {
		URI issuerURI = new URI(discoveryUrl);
		URL providerConfigurationURL = issuerURI.toURL();
		InputStream stream = providerConfigurationURL.openStream();
		// Read all data from URL
		String providerInfo = null;
		try (java.util.Scanner s = new java.util.Scanner(stream)) {
			providerInfo = s.useDelimiter("\\A").hasNext() ? s.next() : "";
		}
		OIDCProviderMetadata oidcPM = OIDCProviderMetadata.parse(providerInfo);
		List<JWSAlgorithm> jwsArr = oidcPM.getIDTokenJWSAlgs();
		if (jwsArr == null)
			jwsArr = new ArrayList<>();
		jwsArr.add(jwsAlgo);
		oidcPM.setIDTokenJWSAlgs(jwsArr);
		return oidcPM;
	}

	public static String replaceAllIgnoreRegx(String source, String search, String replace) {

		return StringUtils.join(source.split(Pattern.quote(search)), replace);
	}

	/**
	 * @param exchange
	 * @param path
	 * @throws SnippetException
	 */
	public static void redirectRequest(HttpServerExchange exchange, String path) {
		if (exchange == null)
			return;
		exchange.getResponseHeaders().clear();
		exchange.setStatusCode(StatusCodes.FOUND);
		exchange.getResponseHeaders().put(Headers.LOCATION, path);
	}

	/**
	 * @param exchange
	 * @return
	 */
	public static boolean isApiCall(HttpServerExchange exchange) {

		if (exchange.getRequestURL().endsWith(".html") || exchange.getRequestURL().endsWith(".js")
				|| exchange.getRequestURL().endsWith(".css")) {
			return false;
		}

		return true;
	}

	public static void logInfo(String tenantName, String serviceName, String message) {
		Tenant.getTenant(tenantName).logError(serviceName, message);
	}

	/**
	 * @param map
	 * @param key
	 * @return
	 */
	public static Object getCaseInsensitiveKey(Map<String, Object> map, String key) {
		return (null == map.get(key)) ? map.get(key.toLowerCase()) : map.get(key);
	}

	public static String normalizeUri(String s) {
		String r = StringUtils.stripAccents(s);
		r = r.replace(" ", "_");
		r = r.replaceAll("[^\\.A-Za-z0-9_]", "");
		return r;
	}

	public static String normalizeApiPath(String s) {
		s = StringUtils.stripEnd(s, "/");
		String path = s;// s.replaceAll("\\{", "").replaceAll("\\}", "");

		String[] paths = StringUtils.split(s, "/");
		StringBuilder pathBuilder = new StringBuilder();
		for (int i = 0; i < paths.length; i++) {
			if (paths.length != 1 && (i == (paths.length - 1) && !(paths[i].indexOf("}") > 0))) {
				break;
			}
			pathBuilder.append("/").append(paths[i].replaceAll("\\{", "").replaceAll("\\}", "").replace(".", "_"));
		}

		return pathBuilder.toString().replaceAll(Pattern.quote("."), "_").replaceAll("-", "");
	}

	public static String normalizeApiPathName(String method, String s) {
		s = StringUtils.stripEnd(s, "/");
		String path = s.replaceAll("\\{", "").replaceAll("\\}", "");

		String[] paths = StringUtils.split(path, "/");

		path = paths[paths.length - 1];
		return method + (path.substring(0, 1).toUpperCase() + path.substring(1).toLowerCase());
	}

	public static String toServiceSlug(String str) {
		if (StringUtils.isBlank(str)) {
			return "";
		}
		return str.toLowerCase().replaceAll("[^a-z0-9\\-]+", "_").replaceAll("^-|-$", "");
	}

	public static void loadCustomJar(DataPipeline dp, String packageName, String jarFileName) throws Exception {
		String packagePath = PropertyManager.getPackagePath(dp.rp.getTenant());
		String path = packagePath + "/packages/" + packageName + "/dependency/jars/" + jarFileName;
		CustomClassLoader ccl = RTCompile.classLoaderMap.get(dp.rp.getTenant().getName());
		final JarFile jar = new JarFile(new File(path));
		List<String> classNames = ccl.loadClassFromJar(jar);
	}

	public static void loadCustomJar(DataPipeline dp, String path) throws Exception {
		CustomClassLoader ccl = RTCompile.classLoaderMap.get(dp.rp.getTenant().getName());
		final JarFile jar = new JarFile(new File(path));
		List<String> classNames = ccl.loadClassFromJar(jar);
	}

	public static void unzipBuildFile(String srcZip, String destinationBasePath) throws IOException {
		byte[] buffer = new byte[1024];
		ZipInputStream zis = new ZipInputStream(new FileInputStream(srcZip));
		ZipEntry zipEntry = zis.getNextEntry();
		while (zipEntry != null) {
			File newFile = newFile(new File(destinationBasePath), zipEntry);
			if (zipEntry.isDirectory()) {
				if (!newFile.isDirectory() && !newFile.mkdirs()) {
					throw new IOException("Failed to create directory " + newFile);
				}
			} else {
				// fix for Windows-created archives
				File parent = newFile.getParentFile();
				if (!parent.isDirectory() && !parent.mkdirs()) {
					throw new IOException("Failed to create directory " + parent);
				}

				// write file content
				FileOutputStream fos = new FileOutputStream(newFile);
				int len;
				while ((len = zis.read(buffer)) > 0) {
					fos.write(buffer, 0, len);
				}
				fos.close();
			}
			zipEntry = zis.getNextEntry();
		}
		zis.closeEntry();
		zis.close();
	}

	/**
	 * @param destinationDir
	 * @param zipEntry
	 * @return
	 * @throws IOException
	 */
	private static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
		File destFile = new File(destinationDir, zipEntry.getName());

		String destDirPath = destinationDir.getCanonicalPath();
		String destFilePath = destFile.getCanonicalPath();

		// if (!destFilePath.startsWith("/" + destDirPath + File.separator)) {
		// System.out.println(destFilePath);
		// throw new IOException("Entry is outside of the target dir: " +
		// zipEntry.getName());
		// }

		return destFile;
	}

	public static void beforeServiceExecution(DataPipeline dp, String fqn, Map<String, Object> passThroughData)
			throws Exception {
		String logRequest = null;
		String logResponse = null;
		String requestJson = "";
		String responseJson = "";
		Date dateTimeStmp = null;
		Set<String> keys = passThroughData.keySet();
		dp.put("*currentResourceFQN", fqn);
		dp.appLogMul("RESOURCE_NAME", fqn);
		String gqlEnabled = (String) dp.getMyProperties().get("GraphQL");
		String GraphQLDBC = (String) dp.getMyProperties().get("GraphQL.DBC");
		String GraphQLSchema = (String) dp.getMyProperties().get("GraphQL.Schema");
		JsonObject mainflowJsonObject = (JsonObject) passThroughData.get("mainflowJsonObject");

		Long timeout = (Long) passThroughData.get("timeout");
		Integer resetServiceInMS = (Integer) passThroughData.get("resetServiceInMS");
		String gql = null;
		Object rootObject = null;
		Map<String, Object> gqlData = new HashMap<>();

		if (resetServiceInMS != null) {

			String flowRef = (String) passThroughData.get("flowRef");
			mainflowJsonObject = configureServiceStartup(dp, timeout, mainflowJsonObject, resetServiceInMS, flowRef);
			passThroughData.put("mainflowJsonObject", mainflowJsonObject);

			if (Boolean.valueOf(gqlEnabled)) {
				gql = dp.getAsString("*payload/query");
				// Setting up the graphQL introspection request

				if (StringUtils.isNotBlank(gql)) {
					dp.drop("*payload");
					gql = gql.replaceAll("\\\\s+", " ").replaceAll("\\\\r|\\\\n", "").trim();
					gqlData.put("gQuery", gql);
					gqlData.put("fqn", fqn);
					if (StringUtils.isNotBlank(GraphQLSchema))
						gqlData.put("schema", GraphQLSchema);
					if (StringUtils.isNotBlank(GraphQLDBC))
						gqlData.put("connection", GraphQLDBC);
					String[] gqlTokens = gql.split(" ");
					String rootName = gqlTokens[1];

					gqlData.put("rootName", rootName);

					if (rootName.toLowerCase().equals("introspectionquery")) {
						// dp.put("*gqlData", gqlData);
						dp.map("*gqlData", gqlData);
						dp.apply("packages.middleware.pub.graphQL.rest.flow.applyGraphQL");
						dp.clearServicePayload();
						rootObject = dp.get("*multiPart");
						if (rootObject != null)
							dp.put("*multiPart", rootObject);
						else {
							dp.put(rootName, dp.get("executionResult/errors"));
						}
						return;
					}
					gqlTokens = gql.split(Pattern.quote("{"));
					if (gqlTokens.length > 1 && gqlTokens[1] != null)
						rootName = gqlTokens[1].trim();
					gqlData.put("rootName", rootName);
				}
			}
		}
		String stopRecursiveLogging = dp.getString("stopRecursiveLogging");
		if (stopRecursiveLogging == null && !fqn.equalsIgnoreCase("packages.middleware.pub.service.auditLogging")) {
			logRequest = dp.getMyConfig("logRequest");
			logResponse = dp.getMyConfig("logResponse");
			requestJson = "";
			responseJson = "";
			if ("true".equalsIgnoreCase(logRequest))
				requestJson = dp.toJson();
			dateTimeStmp = new Date();
		}
		passThroughData.put("stopRecursiveLogging", stopRecursiveLogging);
		passThroughData.put("logRequest", logRequest);
		passThroughData.put("logResponse", logResponse);
		passThroughData.put("requestJson", requestJson);
		passThroughData.put("responseJson", responseJson);
		passThroughData.put("dateTimeStmp", dateTimeStmp);

		// Calling the service
		if (resetServiceInMS != null)
			FlowResolver.execute(dp, mainflowJsonObject);

		if (StringUtils.isNotBlank(gql) && StringUtils.isBlank(GraphQLDBC)) {
			// dp.put("*gqlData", gqlData);
			dp.map("*gqlData", gqlData);
			Map<String, Object> data = new HashMap<>();
			String rootName = (String) gqlData.get("rootName");
			rootObject = dp.get(rootName);
			data.put(rootName, rootObject);
			gqlData.put("data", data);
			dp.apply("packages.middleware.pub.graphQL.rest.flow.applyGraphQL");
			// dp.drop("*gqlData");
			rootObject = dp.getValueByPointer("executionResult/rootObject");
			if (rootObject != null) {
				dp.put(rootName, rootObject);
			}
			Object errors = dp.getValueByPointer("executionResult/errors");
			if (errors != null) {
				dp.put(rootName, errors);
			}
			dp.drop("executionResult");
			dp.drop("*gqlData");
			dp.clearServicePayload();
		}
	}

	public static void afterServiceExecution(DataPipeline dp, String fqn, Map<String, Object> passThroughData)
			throws Exception {
		Long nanoSec = (Long) passThroughData.get("nanoSec");
		String logRequest = (String) passThroughData.get("logRequest");
		String logResponse = (String) passThroughData.get("logResponse");
		String requestJson = (String) passThroughData.get("requestJson");
		String responseJson = (String) passThroughData.get("responseJson");
		Date dateTimeStmp = (Date) passThroughData.get("dateTimeStmp");
		Long startTime = (Long) passThroughData.get("startTime");
		String stopRecursiveLogging = (String) passThroughData.get("stopRecursiveLogging");
		if (stopRecursiveLogging == null && !fqn.equalsIgnoreCase("packages.middleware.pub.service.auditLogging")) {
			if ("true".equalsIgnoreCase(logResponse))
				responseJson = dp.toJson();
			long endTime = System.currentTimeMillis();
			Map<String, String> auditLog = new HashMap();
			auditLog.put("correlationId", dp.getCorrelationId());
			auditLog.put("sessionId", dp.getSessionId());
			auditLog.put("dateTimeStmp", dateTimeStmp + "");
			auditLog.put("duration", (endTime - startTime) + "");
			if (null == dp.getString("error")) {
				auditLog.put("error", "");
			} else {
				auditLog.put("error", dp.getString("error"));
			}

			auditLog.put("fqn", fqn);
			auditLog.put("request", requestJson);
			auditLog.put("response", responseJson);
			auditLog.put("nanoInstance", nanoSec + "");

			try {
				auditLog.put("hostName", InetAddress.getLocalHost().getHostName());
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
			String nodeName = dp.getGlobalConfig("nodeName");
			auditLog.put("nodeName", nodeName);

			auditLog.put("remoteAddr", dp.getRemoteIpAddr());
			auditLog.put("userId", dp.getCurrentUserProfile().getId());
			auditLog.put("urlPath", dp.getUrlPath());

			Map<String, Object> asyncInputDoc = new HashMap();
			asyncInputDoc.put("auditLog", auditLog);
			asyncInputDoc.put("stopRecursiveLogging", "true");
			dp.put("asyncInputDoc", asyncInputDoc);
			dp.applyAsync("packages.middleware.pub.service.auditLogging");
			dp.drop("asyncInputDoc");
			dp.drop("asyncOutputDoc");
			if (dp.rp.isExchangeInitialized())
				dp.rp.getExchange().getResponseHeaders().put(new HttpString("CORRELATION-ID"), dp.getCorrelationId());
		}
	}

	private static JsonObject configureServiceStartup(DataPipeline dataPipeline, Long timeout,
			JsonObject mainflowJsonObject, Integer resetServiceInMS, String flowRef) throws Exception {
		Map<String, Object> chache = CacheManager.getCacheAsMap(dataPipeline.rp.getTenant());// -----reset fix
		Boolean resetEnabled = (Boolean) chache.get("ekamw.promote.runtime.service.reload");// -----reset fix
		if (timeout < System.currentTimeMillis() && (resetEnabled == null || resetEnabled == true))// -----reset fix
			timeout = 0l;// -----reset fix
		String location = PropertyManager.getPackagePath(dataPipeline.rp.getTenant());
		flowRef = location + flowRef;
		if (mainflowJsonObject == null || timeout == 0l) { // -----reset fix
			timeout = System.currentTimeMillis() + resetServiceInMS; // -----reset fix
			mainflowJsonObject = Json.createReader(new FileInputStream(new File(flowRef))).readObject();
		}
		return mainflowJsonObject;
	}

	public static Map<String, Object> decodeJWT(String jwtToken) {
		// Splitting the JWT Token into parts
		String[] parts = jwtToken.split("\\.");
		if (parts.length < 2) {
			return new HashMap<>(); // Not enough parts for a valid JWT
		}

		// Decoding the payload
		String payload = parts[1];
		byte[] decodedBytes = Base64.getUrlDecoder().decode(payload);
		String decodedString = new String(decodedBytes);

		// Converting JSON string to Map
		ObjectMapper objectMapper = new ObjectMapper();
		Map<String, Object> tokenData = new HashMap<>();
		try {
			tokenData = objectMapper.readValue(decodedString, Map.class);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return tokenData;
	}

	public static boolean isValid(String token) throws SystemException {
		Map<String, Object> jwtData = ServiceUtils.decodeJWT(token);
		if (jwtData == null)
			return false;
		String id = (String) jwtData.get("username");
		/*
		 * Map<String, Object> usersMap = null; try { usersMap = (Map<String, Object>)
		 * UserProfileManager.getUsers(); } catch (SystemException e) {
		 * ServiceUtils.printException("Could not load users list: " + id, e); return
		 * false; }
		 */
		Map<String, Object> user = UsersRepository.getUserById(id);
		if (user != null && user.getOrDefault("salt", "").equals(jwtData.getOrDefault("salt", "")))
			return true;
		return false;
	}

	public static void saveServerProperties(DataPipeline dataPipeline) throws Exception {
		if (!dataPipeline.rp.getTenant().getName().equalsIgnoreCase("default")) {
			throw new Exception("Server properties are not allowed to save for other tenants");
		}
		Map<String, Object> properties = dataPipeline.getAsMap("properties");

		if (properties == null || properties.isEmpty()) {
			return;
		}

		Properties props = new Properties();
		String filePath = PropertyManager.getConfigFolderPath() + "server.properties";
		FileInputStream inputStream = new FileInputStream(filePath);
		props.load(new FileInputStream(filePath));
		inputStream.close();

		for (Map.Entry<String, Object> entry : properties.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue().toString();

			if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(value)) {
				props.setProperty(key, value);
			}
		}

		FileOutputStream outputStream = new FileOutputStream(filePath);
		props.store(outputStream, "");
		outputStream.flush();
		outputStream.close();
	}
}
