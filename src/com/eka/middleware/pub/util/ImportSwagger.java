package com.eka.middleware.pub.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import com.beust.jcommander.internal.Lists;
import com.eka.middleware.server.MiddlewareServer;
import com.eka.middleware.service.DataPipeline;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.nimbusds.jose.shaded.gson.Gson;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.eka.middleware.heap.HashMap;
import com.eka.middleware.service.ServiceUtils;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.apache.commons.lang3.StringUtils;

public class ImportSwagger {

	final static String supportedTypes = "string,integer,number,date,boolean,object,byte,document,stringList,integerList,numberList,dateList,booleanList,documentList,objectList,byteList,";

	public static void main(String[] args) throws Exception {
		String path = "D:\\domains.json";// petstore

		File openAPIFile = new File(path);
		byte openAPI[] = ServiceUtils.readAllBytes(openAPIFile);
		//Map responseLib = asClientLib("D:\\d\\JWORK\\nature9\\ekamw-distributions\\integration\\middleware\\tenants\\default\\packages\\godaddy\\clientLib", openAPI);
		//Map responseStub = asServerStub("D:\\d\\JWORK\\nature9\\ekamw-distributions\\integration\\middleware\\tenants\\default\\packages\\godaddy\\serverStub", openAPI);
		//String json = Json.pretty().writeValueAsString(responseLib);
		//System.out.println(json);
		//json = Json.pretty().writeValueAsString(responseStub);
		//System.out.println(json);
	}

	public static Map<String, String> asServerStub(String folderPath, byte[] openAPI,DataPipeline dataPipeline,Boolean isClientRequested,String packageName) throws Exception {

		File folder = new File(folderPath);
		folder.mkdirs();
		OpenAPI swagger = null;
		SwaggerConfiguration sc = new SwaggerConfiguration();
		Map<String, String> response = new HashMap();

		swagger = Json.mapper().readValue(openAPI, OpenAPI.class);
		Map<String, Object> flow = null;
		Set<Entry<String, PathItem>> pathItemMap = swagger.getPaths().entrySet();
		for (Entry<String, PathItem> entry : pathItemMap) {
			String alias = entry.getKey();
			PathItem pi = entry.getValue();
			Operation op = pi.getGet();
			String opId = ServiceUtils.normalizeUri( null == op || op.getOperationId() == null ? ServiceUtils.normalizeApiPathName("get", alias) : ServiceUtils.toServiceSlug(op.getOperationId()));
			String servicePath = folderPath + ServiceUtils.normalizeApiPath(alias)+ File.separator + opId + ".flow";

			File file = new File(servicePath);
			flow = generateServerStub(op, swagger, servicePath,isClientRequested,packageName,alias);
			if (flow != null) {
				String json = Json.pretty().writeValueAsString(flow);
				saveFlow(servicePath, json);
				generateJavaClass(file,servicePath,dataPipeline);
				response.put("GET" + alias, opId);
			}
			op = pi.getPost();
			opId = ServiceUtils.normalizeUri( null == op || op.getOperationId() == null ? ServiceUtils.normalizeApiPathName("post", alias) + alias : ServiceUtils.toServiceSlug(op.getOperationId()));
			servicePath = folderPath + ServiceUtils.normalizeApiPath(alias)+ File.separator + opId + ".flow";
			flow = generateServerStub(op, swagger, servicePath,isClientRequested,packageName,alias);
			if (flow != null) {
				String json = Json.pretty().writeValueAsString(flow);
				saveFlow(servicePath, json);
				generateJavaClass(file,servicePath,dataPipeline);
				response.put("POST" + alias, opId);
			}

			op = pi.getDelete();
			opId = ServiceUtils.normalizeUri( null == op || op.getOperationId() == null ? ServiceUtils.normalizeApiPathName("delete", alias) : ServiceUtils.toServiceSlug(op.getOperationId()));
			servicePath = folderPath + ServiceUtils.normalizeApiPath(alias)+ File.separator + opId + ".flow";
			flow = generateServerStub(op, swagger, servicePath,isClientRequested,packageName,alias);
			if (flow != null) {
				String json = Json.pretty().writeValueAsString(flow);
				saveFlow(servicePath, json);
				generateJavaClass(file,servicePath,dataPipeline);
				response.put("DELETE" + alias, opId);
			}

			op = pi.getPatch();
			opId = ServiceUtils.normalizeUri( null == op || op.getOperationId() == null ? ServiceUtils.normalizeApiPathName("patch", alias) + alias : ServiceUtils.toServiceSlug(op.getOperationId()));
			servicePath = folderPath + ServiceUtils.normalizeApiPath(alias)+ File.separator + opId + ".flow";
			flow = generateServerStub(op, swagger, servicePath,isClientRequested,packageName,alias);
			if (flow != null) {
				String json = Json.pretty().writeValueAsString(flow);
				saveFlow(servicePath, json);
				generateJavaClass(file,servicePath,dataPipeline);
				response.put("PATCH" + alias, opId);
			}

			op = pi.getPut();
			opId = ServiceUtils.normalizeUri( null == op || op.getOperationId() == null ? ServiceUtils.normalizeApiPathName("put", alias) : ServiceUtils.toServiceSlug(op.getOperationId()));
			servicePath = folderPath + ServiceUtils.normalizeApiPath(alias)+ File.separator + opId + ".flow";
			flow = generateServerStub(op, swagger, servicePath,isClientRequested,packageName,alias);
			if (flow != null) {
				String json = Json.pretty().writeValueAsString(flow);
				saveFlow(servicePath, json);
				generateJavaClass(file,servicePath,dataPipeline);
				response.put("PUT" + alias, opId);
			}
		}
		return response;
	}
	public static Map<String, String> asClientLib(String folderPath, byte[] openAPI,DataPipeline dataPipeline) throws Exception {

		File folder = new File(folderPath);
		folder.mkdirs();

		OpenAPI swagger = null;
		SwaggerConfiguration sc = new SwaggerConfiguration();
		Map<String, String> response = new HashMap();

		swagger = Json.mapper().readValue(openAPI, OpenAPI.class);
		Map<String, Object> flow = null;
		Set<Entry<String, PathItem>> pathItemMap = swagger.getPaths().entrySet();
		for (Entry<String, PathItem> entry : pathItemMap) {
			String alias = entry.getKey();
			PathItem pi = entry.getValue();
			Operation op = pi.getGet();

			flow = generateClientLib(alias, op, swagger, "GET");
			if (flow != null) {
				String opId = ServiceUtils.normalizeUri(op.getOperationId() == null ? ServiceUtils.normalizeApiPathName("get", alias) : ServiceUtils.toServiceSlug(op.getOperationId()));
				String json = Json.pretty().writeValueAsString(flow);
				String servicePath = folderPath + ServiceUtils.normalizeApiPath(alias) + File.separator + opId + ".flow";
				saveFlow(servicePath, json);
				File file = new File(servicePath);
				generateJavaClass(file,servicePath,dataPipeline);
				response.put("GET" + alias, opId);
			}
			op = pi.getPost();
			flow = generateClientLib(alias, op, swagger, "POST");
			if (flow != null) {
				String opId = ServiceUtils.normalizeUri(op.getOperationId() == null ? ServiceUtils.normalizeApiPathName("post", alias) : ServiceUtils.toServiceSlug(op.getOperationId()));
				String json = Json.pretty().writeValueAsString(flow);
				String servicePath = folderPath + ServiceUtils.normalizeApiPath(alias) + File.separator + opId + ".flow";
				saveFlow(servicePath, json);
				File file = new File(servicePath);
				generateJavaClass(file,servicePath,dataPipeline);
				response.put("POST" + alias, opId);
			}

			op = pi.getDelete();
			flow = generateClientLib(alias, op, swagger, "DELETE");
			if (flow != null) {
				String opId = ServiceUtils.normalizeUri(op.getOperationId() == null ? ServiceUtils.normalizeApiPathName("delete", alias) : ServiceUtils.toServiceSlug(op.getOperationId()));
				String json = Json.pretty().writeValueAsString(flow);
				String servicePath = folderPath + ServiceUtils.normalizeApiPath(alias) + File.separator + opId + ".flow";
				saveFlow(servicePath, json);
				File file = new File(servicePath);
				generateJavaClass(file,servicePath,dataPipeline);
				response.put("DELETE" + alias, opId);
			}

			op = pi.getPatch();
			flow = generateClientLib(alias, op, swagger, "PATCH");
			if (flow != null) {
				String opId = ServiceUtils.normalizeUri(op.getOperationId() == null ? ServiceUtils.normalizeApiPathName("patch", alias) : ServiceUtils.toServiceSlug(op.getOperationId()));
				String json = Json.pretty().writeValueAsString(flow);
				String servicePath = folderPath + ServiceUtils.normalizeApiPath(alias) + File.separator + opId + ".flow";
				saveFlow(servicePath, json);
				File file = new File(servicePath);
				generateJavaClass(file,servicePath,dataPipeline);
				response.put("PATCH" + alias, opId);
			}

			op = pi.getPut();
			flow = generateClientLib(alias, op, swagger, "PUT");
			if (flow != null) {
				String opId = ServiceUtils.normalizeUri(op.getOperationId() == null ? ServiceUtils.normalizeApiPathName("put", alias) : ServiceUtils.toServiceSlug(op.getOperationId()));
				String json = Json.pretty().writeValueAsString(flow);
				String servicePath = folderPath + ServiceUtils.normalizeApiPath(alias) + File.separator + opId + ".flow";
				System.err.println("client service path >> " + servicePath);
				saveFlow(servicePath, json);
				File file = new File(servicePath);
				generateJavaClass(file,servicePath,dataPipeline);
				response.put("PUT" + alias, opId);
			}
		}
		return response;
	}

	private static void saveFlow(String filePath, String json) {
		File file = new File(filePath);
		file.getParentFile().mkdirs();
		try (FileOutputStream fos = new FileOutputStream(file)) {
			IOUtils.write(json.getBytes(), fos);
			fos.flush();
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static Map<String, Object> createDocument(String name, List<Object> list, String description) {
		Map<String, Object> map = createParam(name, "document");
		map.put("children", list);

		if (StringUtils.isNotBlank(description)) {
			Map<String, String> data = Maps.newHashMap();
			data.put("fieldDescription", Base64.getEncoder().encodeToString(description.getBytes()));
			map.put("data", data);
		}

		return map;
	}

	public static Map<String, Object> createDocumentList(String name, List<Object> list, String description) {
		Map<String, Object> map = createParam(name, "documentList");
		map.put("children", list);

		if (StringUtils.isNotBlank(description)) {
			Map<String, String> data = Maps.newHashMap();
			data.put("fieldDescription", Base64.getEncoder().encodeToString(description.getBytes()));
			map.put("data", data);
		}

		return map;
	}

	private static Map<String, Object> createInteger(String name) {
		return createEntity(name, "integer");
	}

	private static Map<String, Object> createNumber(String name) {
		return createEntity(name, "number");
	}

	private static Map<String, Object> createBoolean(String name) {
		return createEntity(name, "boolean");
	}

	private static Map<String, Object> createString(String name) {
		return createEntity(name, "string");
	}

	private static Map<String, Object> createDate(String name) {
		return createEntity(name, "date");
	}

	private static Map<String, Object> createByte(String name) {
		return createEntity(name, "byte");
	}

	private static Map<String, Object> createObject(String name) {
		return createEntity(name, "object");
	}

	private static Map<String, Object> createIntegerList(String name) {
		return createEntity(name, "integerList");
	}

	private static Map<String, Object> createNumberList(String name) {
		return createEntity(name, "numberList");
	}

	private static Map<String, Object> createBooleanList(String name) {
		return createEntity(name, "booleanList");
	}

	private static Map<String, Object> createStringList(String name) {
		return createEntity(name, "stringList");
	}

	private static Map<String, Object> createDateList(String name) {
		return createEntity(name, "dateList");
	}

	private static Map<String, Object> createByteList(String name) {
		return createEntity(name, "byteList");
	}

	private static Map<String, Object> createObjectList(String name) {
		return createEntity(name, "objectList");
	}

	private static Map<String, Object> createParam(String name, String type) {
		return createEntity(name, type);
	}

	private static Map<String, Object> createEntity(String name, String type) {
		//TODO key sorting
		Map<String, Object> map = new HashMap();
		map.put("text", name);
		map.put("type", type);
		map.put("a_attr", new HashMap<>());
		return map;
	}

	private static void addRequestBody(List<Object> inputs, RequestBody rb, OpenAPI swagger, boolean isClient) {

		String payloadParam = "*payload";
		if (isClient) {
			payloadParam = "payload";
		}

		if (rb != null && rb.getContent() != null && rb.getContent().entrySet() != null
				&& rb.getContent().entrySet().size() > 0) {
			//int bodySize = rb.getContent().entrySet().size();

			//MediaType[] mts = new MediaType[bodySize];
			MediaType mt = ((Entry<String, MediaType>) rb.getContent().entrySet().toArray()[0]).getValue();

			Schema bodySchema = mt.getSchema();
			if (bodySchema != null) {
				List<Object> body = getBody(bodySchema, swagger);
				if (!body.isEmpty()) {
					Map<String, Object> objectMap = (Map<String, Object>) body.get(0);
					objectMap.put("text", payloadParam);
					if ("array".equals(bodySchema.getType())) {
						objectMap.put("type", "documentList");
					} else {
						objectMap.put("type", "document");
					}

					if (StringUtils.isNotBlank(rb.getDescription())) {
						Map<String, String> data = Maps.newHashMap();
						data.put("fieldDescription", Base64.getEncoder().encodeToString(rb.getDescription().getBytes()));
						objectMap.put("data", data);
					}

					inputs.add(objectMap);
				}

				/*if ("array".equals(bodySchema.getType()))
					inputs.add(createDocumentList(payloadParam, body, rb.getDescription()));
				else
					inputs.add(createDocument(payloadParam, body, rb.getDescription()));*/
			}
		}
	}

	private static void addResponses(List<Object> outputs, ApiResponses apiResponses, OpenAPI swagger) {
		Set<Entry<String, ApiResponse>> apiRespSet = apiResponses.entrySet();
		for (Entry<String, ApiResponse> entry : apiRespSet) {
			String code = entry.getKey();
			ApiResponse apiresp = entry.getValue();
			if (apiresp.getContent() != null && apiresp.getContent().entrySet() != null
					&& apiresp.getContent().entrySet().size() > 0) {
				int respSize = apiresp.getContent().entrySet().size();
				MediaType[] mts = new MediaType[respSize];
				MediaType mt = (MediaType) ((Entry) apiresp.getContent().entrySet().toArray()[0]).getValue();// Array(mts)[0];
				Schema bodySchema = mt.getSchema();
				List<Object> body = getBody(bodySchema, swagger);
				outputs.add(createDocument("*" + code, body, apiresp.getDescription()));
			} else {
				outputs.add(createDocument("*" + code, null, apiresp.getDescription()));
			}
		}
	}

	private static Map<String, Object> generateClientLib(String alias, Operation op, OpenAPI swagger, String method) {
		if (op == null)
			return null;
		FlowService flowService = new FlowService(op.getDescription(), op.getSummary(), com.google.common.collect.Sets.newHashSet("consumers"), Sets.newHashSet("developers"));

		//Map<String, Object> latest = flowService.getVersion();
		List<Object> inputs = flowService.getInput();
		List<Object> outputs = flowService.getOutput();
		List<Object> flowSteps = flowService.getFlowSteps();

		List<Object> query = getParameters(op, "query");

		List<Object> headers = getParameters(op, "header");

		List<Object> parameters = getParameters(op, "path");

		RequestBody rb = op.getRequestBody();
		addRequestBody(inputs, rb, swagger, true);

		ApiResponses apiResponses = op.getResponses();
		addResponses(outputs, apiResponses, swagger);

		Map<String, String> commonOutputRawResponse = Maps.newHashMap();
		commonOutputRawResponse.put("text", "rawResponse");
		commonOutputRawResponse.put("type", "string");
		outputs.add(commonOutputRawResponse);

		Map<String, String> commonOutputStatusCode = Maps.newHashMap();
		commonOutputStatusCode.put("text", "statusCode");
		commonOutputStatusCode.put("type", "string");
		outputs.add(commonOutputStatusCode);

		if (headers.size() > 0)
			inputs.add(createDocument("requestHeaders", headers, ""));
		if (parameters.size() > 0)
			inputs.add(createDocument("pathParameters", parameters, ""));
		if (query.size() > 0)
			inputs.add(createDocument("queryParameters", query, ""));

		Map<String, Object> intiMapStep = createMapStep(flowSteps, "Resolving Parameters");
		createVariables(intiMapStep, "basePath", "#{basePath}", Evaluate.EEV, "string");

		intiMapStep = createMapStep(flowSteps, "Initializing Parameters");
		createVariables(intiMapStep, "method", method, null, "string");
		createVariables(intiMapStep, "url", "#{basePath}" + alias.replaceAll(Pattern.quote("{"), "#{pathParameters/"), Evaluate.EEV, "string");
		dropVariables(intiMapStep, "basePath", "string");
		dropVariables(intiMapStep, "pathParameters", "document");

		if (!"GET".equalsIgnoreCase(method)) {
			Map<String, Object> invokeStepToJson= createInvokeStep(flowSteps,"service","packages/middleware/pub/json/toString", "Initialize");
			createPreInvokeMapping(invokeStepToJson, "copy", "document", "/payload", "documentList", "/root");
			createPostInvokeMapping(invokeStepToJson, "copy", "string", "/jsonString", "string", "/body");
		}

		Map<String, Object> invokeStepRequest= createInvokeStep(flowSteps,"service","packages/middleware/pub/client/http/request", "Initialize");

		createPreInvokeMapping(invokeStepRequest, "copy", "document", "/requestHeaders", "document","/headers");
		createPreInvokeMapping(invokeStepRequest, "copy", "document", "/queryParameters", "document", "/urlParameters");
		createPreInvokeMapping(invokeStepRequest, "copy", "string", "/url", "string", "/url");
		createPreInvokeMapping(invokeStepRequest, "copy", "string", "/method", "string", "/method");

		createPostInvokeMapping(invokeStepRequest, "copy", "document", "/respHeaders", "document", "/respHeaders");
		createPostInvokeMapping(invokeStepRequest, "copy", "string", "/respPayload", "string", "/respPayload");
		createPostInvokeMapping(invokeStepRequest, "copy", "string", "/error", "string", "/error");
		createPostInvokeMapping(invokeStepRequest, "copy", "byteList", "/bytes", "byteList", "/bytes");
		createPostInvokeMapping(invokeStepRequest, "copy", "javaObject", "/inputStream", "javaObject", "/inputStream");
		createPostInvokeMapping(invokeStepRequest, "copy", "string", "/statusCode", "string", "/statusCode");

		dropVariables(invokeStepRequest, "method", "string");
		dropVariables(invokeStepRequest, "url", "string");
		dropVariables(invokeStepRequest, "headers", "document");
		dropVariables(invokeStepRequest, "urlParameters", "document");

		if (!"GET".equalsIgnoreCase(method)) {
			createPreInvokeMapping(invokeStepRequest, "copy", "string", "/body", "string", "/payload");
			dropVariables(invokeStepRequest, "payload", "string");
		}

		Map<String, SecurityScheme> securitySchemes =
				null == swagger.getComponents().getSecuritySchemes() ? Maps.newHashMap() : swagger.getComponents().getSecuritySchemes();
		for (String securitySchemesList: securitySchemes.keySet()) {

			SecurityScheme securityScheme = securitySchemes.get(securitySchemesList);
			SecurityScheme.Type type = securityScheme.getType();

            if (SecurityScheme.Type.APIKEY.equals(type)) {
                //Map<String, String> commonApiKey = Maps.newHashMap();
                //commonApiKey.put("text", "apiKey");
                //commonApiKey.put("type", "string");
                // inputs.add(commonApiKey);
                if (SecurityScheme.In.HEADER.equals(securityScheme.getIn())) {
                    createVariables(intiMapStep, "requestHeaders/" + securityScheme.getName(), "#{apiKey}", Evaluate.EEV, "document/string");
                } else if (SecurityScheme.In.QUERY.equals(securityScheme.getIn())) {
                    createVariables(intiMapStep, "queryParameters/" + securityScheme.getName(), "#{apiKey}", Evaluate.EEV, "document/string");
                }

            } else if (SecurityScheme.Type.HTTP.equals(type)) {
                Map<String, String> commonAccessToken = Maps.newHashMap();
                commonAccessToken.put("text", "access_token");
                commonAccessToken.put("type", "string");
                inputs.add(commonAccessToken);
                createVariables(intiMapStep, "requestHeaders/Authorization", "Bearer ${access_token}", Evaluate.EEV, "document/string");
            }
			/* else if (SecurityScheme.Type.OAUTH2.equals(type)) {

				 SecurityScheme.Type type1 = securityScheme.getType();
				 OAuthFlows flows = securityScheme.getFlows();
				 OAuthFlow authorizationCode = flows.getAuthorizationCode();
				 String authorizationUrl = authorizationCode.getAuthorizationUrl();
				 String tokenUrl = authorizationCode.getTokenUrl();
				 Scopes scopes = authorizationCode.getScopes();

			 }*/
		}

		Map<String, Object> switchMapping = createSwitch(flowSteps,"switch","SWITCH", "Checking status for response");
		addData(switchMapping, "switch", "statusCode");

		Set<Entry<String, ApiResponse>> apiRespSet = apiResponses.entrySet();
		List<Object> cases = Lists.newArrayList();
		for (Entry<String, ApiResponse> entry : apiRespSet) {

			String code = entry.getKey();
			Map<String, Object> sequenceMapping = createCase(cases,"CASE", "Handling " + code + " response");
			addData(sequenceMapping, "case", code);

			List<Object> contentTypeSwitch = Lists.newArrayList();
			addContentTypeHandler(contentTypeSwitch, intiMapStep, swagger, entry);
			addChildren(sequenceMapping, contentTypeSwitch);
		}

		addChildren(switchMapping, cases);

		Map<String, Object> commonResponse = createMapStep(flowSteps, "Mapping Common Response & HTTP Status Code");

		createMapping(commonResponse, "copy", "string", "/respPayload", "string", "/rawResponse");

		return flowService.getFlow();
	}

	private static Map<String, Object> addContentTypeHandler(List<Object> contentTypeSwitch, Map<String, Object> intiMapStep, OpenAPI swagger, Entry<String, ApiResponse> entry) {
		String code = entry.getKey();
		ApiResponse apiResponse = entry.getValue();
		Map<String, Object> switchContentTypeMapping = createSwitch(contentTypeSwitch,"switch","SWITCH", "Checking content type for response");
		addData(switchContentTypeMapping, "switch", "respHeaders/content-type");

		List<Object> contentTypeCases = Lists.newArrayList();
		Content content = apiResponse.getContent();
		if (null != content && false) {
			for (Map.Entry<String, MediaType> mediaTypeEntry : apiResponse.getContent().entrySet()) {

				Map<String, Object> sequenceContentTypeMapping = createCase(contentTypeCases,"CASE", "Handling " + mediaTypeEntry.getKey() + " response");
				addData(sequenceContentTypeMapping, "case", mediaTypeEntry.getKey());

				if (mediaTypeEntry.getKey().equalsIgnoreCase("application/xml")) {

					List<Object> jsonConversionCase = Lists.newArrayList();
					Map<String, Object> invokeStepToJson = createInvokeStep(jsonConversionCase,"service","packages/middleware/pub/xml/fromXML", "Initialize");

					createPreInvokeMapping(invokeStepToJson, "copy", "string", "/respPayload", "string", "/xml");
					createPostInvokeMapping(invokeStepToJson, "copy", "document/document", "/output/root", "document/document", "/*" + code + "/root");


					addChildren(sequenceContentTypeMapping, jsonConversionCase);

				} else {
					List<Object> jsonConversionCase = Lists.newArrayList();

					List<Object> body = getBody(mediaTypeEntry.getValue().getSchema(), swagger);
					Map<String, Object> map = null;
					if (null != body) {
						map = (Map<String, Object>) body.get(0);
					} else {
						map = new java.util.HashMap<>();
						map.put("text", "common");
					}
					createVariables(intiMapStep, "rootName_" + map.get("text").toString(), map.get("text").toString(), null, "string");

					Map<String, Object> invokeStepToJson = createInvokeStep(jsonConversionCase,"service","packages/middleware/pub/json/fromJson", "Initialize");

					createPreInvokeMapping(invokeStepToJson, "copy", "string", "/respPayload", "string", "/jsonString");
					createPreInvokeMapping(invokeStepToJson, "copy", "string", "/rootName_" + map.get("text").toString(), "string", "/rootName");
					createPostInvokeMapping(invokeStepToJson, "copy", "document", "/output", "document", "/*" + code);

					addChildren(sequenceContentTypeMapping, jsonConversionCase);
				}

			}
		} else {
			{
				Map<String, Object> sequenceContentTypeMapping = createCase(contentTypeCases,"CASE", "Handling JSON response");
				addData(sequenceContentTypeMapping, "case", "#regex:.*json.*");

				List<Object> jsonConversionCase = Lists.newArrayList();
				createVariables(intiMapStep, "rootName_Error", "Error", null, "string");

				Map<String, Object> invokeStepToJson = createInvokeStep(jsonConversionCase,"service","packages/middleware/pub/json/fromJson", "Initialize");

				createPreInvokeMapping(invokeStepToJson, "copy", "string", "/respPayload", "string", "/jsonString");
				createPreInvokeMapping(invokeStepToJson, "copy", "string", "/rootName_Error", "string", "/rootName");
				createPostInvokeMapping(invokeStepToJson, "copy", "document", "/output", "document", "/*" + code);
				addChildren(sequenceContentTypeMapping, jsonConversionCase);
			}
			{
				Map<String, Object> sequenceContentTypeMapping = createCase(contentTypeCases,"CASE", "Handling XML response");
				addData(sequenceContentTypeMapping, "case", "#regex:.*xml.*");

				List<Object> jsonConversionCase = Lists.newArrayList();
				Map<String, Object> invokeStepToJson = createInvokeStep(jsonConversionCase,"service","packages/middleware/pub/xml/fromXML", "Initialize");

				createPreInvokeMapping(invokeStepToJson, "copy", "string", "/respPayload", "string", "/xml");
				createPostInvokeMapping(invokeStepToJson, "copy", "document/document", "/output/root", "document/document", "/*" + code + "/root");
				addChildren(sequenceContentTypeMapping, jsonConversionCase);
			}
		}
		addChildren(switchContentTypeMapping, contentTypeCases);
		return switchContentTypeMapping;
	}

	private static Map<String, Object> generateServerStub(Operation op, OpenAPI swagger, String servicePath,Boolean isClientRequested,String packageName,String alias) throws FileNotFoundException {
		if (op == null)
			return null;

		File file = new File(servicePath);
		FlowService flowService;
		if (file.exists()) {
			flowService = new Gson().fromJson(new FileReader(file), FlowService.class);
		} else {
			flowService = new FlowService(op.getDescription(), op.getSummary(), Sets.newHashSet("consumers"), Sets.newHashSet("developers"));
		}

		List<Object> inputs = flowService.getInput();
		inputs.clear();
		List<Object> outputs = flowService.getOutput();
		outputs.clear();

		Map<String, Object> intiMapStep = null;
		if (isClientRequested) {
			intiMapStep = createMapStep(flowService.getFlowSteps(), "Resolving Parameters");
		}

		List<Object> query = getParameters(op, "query");

		List<Object> headers = getParameters(op, "header");

		List<Object> parameters = getParameters(op, "path");

		RequestBody rb = op.getRequestBody();
		addRequestBody(inputs, rb, swagger, false);

		ApiResponses apiResponses = op.getResponses();
		addResponses(outputs, apiResponses, swagger);

		if (isClientRequested) {
			String str = "packages/" + packageName + "/client" + ServiceUtils.normalizeApiPath(alias) + "/" + op.getOperationId().replaceFirst("//", "/");
			Map<String, Object>  invokeClient = createInvokeStep(flowService.getFlowSteps(), "flow",
					str, "Invoking Client");

            if (swagger.getPaths() != null) {
                for (Map.Entry<String, PathItem> pathEntry : swagger.getPaths().entrySet()) {
                    PathItem pathItem = pathEntry.getValue();
                    for (Operation operation : pathItem.readOperations()) {
                        if (op.getParameters() != null) {
                            for (Parameter parameter : op.getParameters()) {
                                if ("query".equalsIgnoreCase(parameter.getIn())) {
                                    createPreInvokeMapping(invokeClient, "copy", "string", "/" + parameter.getName(), "document", "/queryParameters/" + parameter.getName());
                                }
                            }
                        }
                    }
                }
            }

			if (headers.size() > 0)
				inputs.add(createDocument("*requestHeaders", headers, ""));
			if (parameters.size() > 0)
				inputs.add(createDocument("*pathParameters", parameters, ""));
			if (query.size() > 0)
				inputs.addAll(query);

            createPreInvokeMapping(invokeClient, "copy", "document", "/*payload", "document", "/payload");
            //  createPreInvokeMapping(invokeClient, "copy", "string", "/apiKey", "document/string", "/requestHeaders/apiKey");
            createPreInvokeMapping(invokeClient, "copy", "document", "/*pathParameters", "document", "/pathParameters   ");

			createPostInvokeMapping(invokeClient, "copy", "string", "/rawResponse", "string", "/rawResponse");
			createPostInvokeMapping(invokeClient, "copy", "string", "/statusCode", "string", "/statusCode");

            Set<Entry<String, ApiResponse>> apiRespSet = apiResponses.entrySet();
            for (Entry<String, ApiResponse> entry : apiRespSet) {
                String code = entry.getKey();
                createVariables(intiMapStep, "basePath", "/*" + code, Evaluate.EEV, "string");
                createPostInvokeMapping(invokeClient, "copy", "document", "/*" + code, "document", "/*" + code);
            }

            Map<String, SecurityScheme> securitySchemes = swagger.getComponents().getSecuritySchemes();
            for (String securitySchemesList : securitySchemes.keySet()) {

				SecurityScheme securityScheme = securitySchemes.get(securitySchemesList);
				SecurityScheme.Type type = securityScheme.getType();

				if (SecurityScheme.Type.APIKEY.equals(type)) {
					Map<String, String> commonApiKey = Maps.newHashMap();
					commonApiKey.put("text", "apiKey");
					commonApiKey.put("type", "string");
					inputs.add(commonApiKey);

                    if (SecurityScheme.In.HEADER.equals(securityScheme.getIn())) {
                        createVariables(intiMapStep, "*requestHeaders/" + securityScheme.getName(), "#{apiKey}", Evaluate.EEV, "document/string");
                        createPreInvokeMapping(invokeClient, "copy", "document/string", "/*requestHeaders/" + securityScheme.getName(), "string", "/apiKey");

					} else if (SecurityScheme.In.QUERY.equals(securityScheme.getIn())) {
						createVariables(intiMapStep, "queryParameters/" + securityScheme.getName(), "${apiKey}", Evaluate.EEV, "document/string");
					}
				} else if (SecurityScheme.Type.HTTP.equals(type)) {
					Map<String, String> commonAccessToken = Maps.newHashMap();
					commonAccessToken.put("text", "access_token");
					commonAccessToken.put("type", "string");
					inputs.add(commonAccessToken);
					createVariables(intiMapStep, "requestHeaders/Authorization", "Bearer ${access_token}", Evaluate.EEV, "document/string");

				}
			}

			Map<String, Object> invokeStepToJson = createInvokeStep(flowService.getFlowSteps(), "service", "packages/middleware/pub/json/fromJson",
					"Converting raw Json to JSON Object");

			createPreInvokeMapping(invokeStepToJson, "copy", "string", "/rawResponse", "string", "/jsonString");
			createPostInvokeMapping(invokeStepToJson, "copy", "document", "/output", "document", "/output");

			Map<String, String> commonOutputRawResponse = Maps.newHashMap();
			commonOutputRawResponse.put("text", "rawResponse");
			commonOutputRawResponse.put("type", "string");
			outputs.add(commonOutputRawResponse);

			Map<String, String> commonOutputStatusCode = Maps.newHashMap();
			commonOutputStatusCode.put("text", "statusCode");
			commonOutputStatusCode.put("type", "string");
			outputs.add(commonOutputStatusCode);
		}
		return flowService.getFlow();
	}

	public static Map<String, Object> createMapStep(List<Object> parent,String comment) {
		Map<String, Object> map = createEntity("MAP", "map");
		parent.add(map);
		Map<String, Object> data = new HashMap();
		map.put("data", data);
		data.put("comment", comment);
		data.put("status", "enabled");
		data.put("dropList", Lists.newArrayList());
		data.put("createList", Lists.newArrayList());
		data.put("transformers", Lists.newArrayList());
		data.put("lines", Lists.newArrayList());
		return data;
	}

	public static Map<String, Object> createInvokeStep(List<Object> parent, String serviceType, String serviceFqn, String comment) {
		Map<String, Object> map = createEntity(serviceFqn, "invoke");
		parent.add(map);
		Map<String, Object> data = new HashMap();
		map.put("data", data);
		data.put("comment", comment);
		data.put("serviceType", serviceType);
		data.put("status", "enabled");
		data.put("dropList", Lists.newArrayList());
		data.put("createList", Lists.newArrayList());
		data.put("transformers", Lists.newArrayList());
		data.put("lines", Lists.newArrayList());
		return data;
	}

	/**
	 * @param parent
	 * @param serviceType
	 * @param serviceFqn
	 * @param comment
	 * @return
	 */
	public static Map<String, Object> createSwitch(List<Object> parent, String serviceType, String serviceFqn, String comment) {
		Map<String, Object> map = createEntity(serviceFqn, "switch");
		parent.add(map);
		Map<String, Object> data = new HashMap();
		map.put("data", data);
		data.put("comment", comment);
		data.put("serviceType", serviceType);
		data.put("status", "enabled");
		return map;
	}

	/**
	 * @param parent
	 * @param serviceFqn
	 * @param comment
	 * @return
	 */
	public static Map<String, Object> createCase(List<Object> parent, String serviceFqn, String comment) {
		Map<String, Object> map = createEntity(serviceFqn, "sequence");
		parent.add(map);
		Map<String, Object> data = new HashMap();
		map.put("data", data);
		data.put("comment", comment);
		data.put("status", "enabled");
		return map;
	}

	/**
	 * @param mapStep
	 * @param path
	 * @param value
	 * @param evaluate
	 * @param typePath
	 */
	public static void createVariables(Map<String, Object> mapStep, String path, String value, Evaluate evaluate, String typePath) {
		List<Object> createList = (List<Object>) mapStep.get("createList");

		if (createList == null) {
			createList = new ArrayList();
			mapStep.put("createList", createList);
		}

		Map<String, Object> createVariable = new HashMap();
		createVariable.put("path", path);
		createVariable.put("evaluate", null != evaluate ? evaluate.name() : null);
		createVariable.put("typePath", typePath);
		createVariable.put("value", value);

		createList.add(createVariable);
	}

	/**
	 * @param mapStep
	 * @param path
	 * @param typePath
	 */
	public static void dropVariables(Map<String, Object> mapStep, String path, String typePath) {
		List<Object> dropList = (List<Object>) mapStep.get("dropList");

		if (dropList == null) {
			dropList = new ArrayList();
			mapStep.put("dropList", dropList);
		}

		Map<String, Object> dropVariable = new HashMap();
		dropVariable.put("path", path);
		dropVariable.put("typePath", typePath);

		dropList.add(dropVariable);
	}

	public static enum Evaluate {
		ELV, // Evaluate Local Variable
		EGV, // Evaluate Global Variable
		EEV, // Evaluate Expression Variable
		EPV, // Evaluate Package Variable
	}

	public static void createMapping(Map<String, Object> mapStep, String op, String inTypePath, String from, String outTypePath,
									 String to) {
		List<Object> transformers = (List<Object>) mapStep.get("transformers");
		List<Object> lines = (List<Object>) mapStep.get("lines");
		if (transformers == null) {
			transformers = new ArrayList();
			lines = new ArrayList();
			mapStep.put("lines", lines);
			mapStep.put("transformers", transformers);
		}
		Map<String, Object> transformer = new HashMap();
		transformer.put("op", op);
		transformer.put("outTypePath", outTypePath);
		transformer.put("from", from);
		transformer.put("to", to);
		transformer.put("inTypePath", inTypePath);

		from = from.substring(1);
		to = to.substring(1);

		Map<String, Object> line = new HashMap();
		line.put("op", op);
		line.put("line", 0);
		line.put("INPath", from);
		line.put("outTypePath", outTypePath);
		line.put("dashedLine", false);
		line.put("inTypePath", inTypePath);
		line.put("outpJsTree", "#landing_arrow_jsTree");
		line.put("outType", outTypePath.substring(outTypePath.lastIndexOf("/") + 1));
		line.put("OUTPath", to);
		line.put("inputPath", from);
		line.put("outputPath", to);
		line.put("inpJsTree", "#launching_arrow_jsTree");
		line.put("inType", inTypePath.substring(inTypePath.lastIndexOf("/") + 1));
		transformers.add(transformer);
		lines.add(line);
	}

	public static void createPreInvokeMapping(Map<String, Object> mapStep, String op, String inTypePath, String from, String outTypePath,
											  String to) {
		List<Object> transformers = (List<Object>) mapStep.get("transformers");
		List<Object> lines = (List<Object>) mapStep.get("lines");
		if (transformers == null) {
			transformers = new ArrayList();
			lines = new ArrayList();
			mapStep.put("lines", lines);
			mapStep.put("transformers", transformers);
		}
		Map<String, Object> transformer = new HashMap();
		transformer.put("op", op);
		transformer.put("outTypePath", outTypePath);
		transformer.put("from", from);
		transformer.put("to", to);
		transformer.put("inTypePath", inTypePath);
		transformer.put("direction", "in");

		from = from.substring(1);
		to = to.substring(1);

		Map<String, Object> line = new HashMap();
		line.put("op", op);
		line.put("line", 0);
		line.put("INPath", from);
		line.put("outTypePath", outTypePath);
		line.put("dashedLine", false);
		line.put("inTypePath", inTypePath);
		line.put("inpJsTree", "#launching_arrow_jsTree");
		line.put("outpJsTree", "#landing_arrow_jsTree_function");
		line.put("outType", outTypePath.substring(outTypePath.lastIndexOf("/") + 1));
		line.put("OUTPath", to);
		line.put("inputPath", from);
		line.put("outputPath", to);
		line.put("inType", inTypePath.substring(inTypePath.lastIndexOf("/") + 1));
		line.put("direction", "in");

		transformers.add(transformer);
		lines.add(line);
	}

	public static void createPostInvokeMapping(Map<String, Object> mapStep, String op, String inTypePath, String from, String outTypePath,
											   String to) {
		List<Object> transformers = (List<Object>) mapStep.get("transformers");
		List<Object> lines = (List<Object>) mapStep.get("lines");
		if (transformers == null) {
			transformers = new ArrayList();
			lines = new ArrayList();
			mapStep.put("lines", lines);
			mapStep.put("transformers", transformers);
		}
		Map<String, Object> transformer = new HashMap();
		transformer.put("op", op);
		transformer.put("outTypePath", outTypePath);
		transformer.put("from", from);
		transformer.put("to", to);
		transformer.put("inTypePath", inTypePath);
		transformer.put("direction", "out");

		from = from.substring(1);
		to = to.substring(1);

		Map<String, Object> line = new HashMap();
		line.put("op", op);
		line.put("line", 0);
		line.put("INPath", from);
		line.put("outTypePath", outTypePath);
		line.put("dashedLine", false);
		line.put("inTypePath", inTypePath);
		line.put("inpJsTree", "#launching_arrow_jsTree_function");
		line.put("outpJsTree", "#landing_arrow_jsTree");
		line.put("outType", outTypePath.substring(outTypePath.lastIndexOf("/") + 1));
		line.put("OUTPath", to);
		line.put("inputPath", from);
		line.put("outputPath", to);
		line.put("inType", inTypePath.substring(inTypePath.lastIndexOf("/") + 1));
		line.put("direction", "out");

		transformers.add(transformer);
		lines.add(line);
	}

	public static void addChildren(Map<String, Object> parent, List<Object> children) {
		parent.put("children", children);
	}

	/**
	 * @param map
	 * @param key
	 * @param value
	 */
	public static void addData(Map<String, Object> map, String key, String value) {
		Map<String, Object> data = (Map<String, Object>)map.get("data");
		data.put(key, value);
	}

	public static String encodeBas64(String value) {
		String encoded = "";
		if (value != null && value.trim().length() > 0) {
			encoded = new String(Base64.getEncoder().encode(value.getBytes(StandardCharsets.UTF_8)));
		}
		return encoded;
	}

	private static List<Object> getBody(Schema bodySchema, OpenAPI swagger) {

		String ref = bodySchema.get$ref();
		String name = bodySchema.getName();
		String type = bodySchema.getType();
		if (ref != null) {
			// bodySchema=bodySchema.raw$ref(ref);
			name = ref.substring(ref.lastIndexOf("/") + 1);
			bodySchema = swagger.getComponents().getSchemas().get(name);
			type = bodySchema.getType();
		}
		List<Object> body = new ArrayList();
		// String name = bodySchema.getName();

		Map<String, Object> param = new HashMap();
		if (name == null)
			name = "param_" + (int) (Math.random() * 200);
		param.put("text", name);
		param.put("type", type);
		body.add(param);
		if (type == null)
			System.out.println(name);
		type = type.toUpperCase();
		switch (type) {
			case "INTEGER": {
				break;
			}
			case "NUMBER": {
				break;
			}
			case "STRING": {
				break;
			}
			case "DATE": {
				break;
			}
			case "BOOLEAN": {
				break;
			}
			case "OBJECT": {
				param.put("type", "document");
				Map<String, Schema> addPropMap = bodySchema.getProperties();
				Schema addProp = (Schema) bodySchema.getAdditionalProperties();
				if (addProp != null) {
					String subType = addProp.getType();
					if (subType != null)
						param.put("type", subType);
					break;
				}
				if (addPropMap != null) {
					Schema subType = addPropMap.get("type");
					if (subType != null) {
						param.put("type", subType.getType());
						break;
					}
				}
				if (bodySchema.getProperties() != null && bodySchema.getProperties().size() > 0) {
					Set<Entry<String, Schema>> children = bodySchema.getProperties().entrySet();
					List<Object> childParams = new ArrayList();
					addChildren(param, childParams);
					for (Entry<String, Schema> child : children) {
						Schema childSchema = child.getValue();
						childSchema.setName(child.getKey());
						List<Object> list = getBody(childSchema, swagger);
						if (list != null && list.size() > 0)
							childParams.addAll(list);
					}
					if (childParams.size() == 0)
						param.remove("children");
				}
				break;
			}
			case "ARRAY": {
				param.put("type", "documentList");
				Set<Entry<String, Schema>> children = null;
				if (bodySchema.getItems() != null) {
					String arrayRef = bodySchema.getItems().get$ref();
					if (arrayRef != null) {
						String subName = arrayRef.substring(arrayRef.lastIndexOf("/") + 1);
						Schema bodyRefSchema = swagger.getComponents().getSchemas().get(subName);
						param.put("text", subName);
						children = bodyRefSchema.getProperties().entrySet();
					} else {
						// type = bodySchema.getType();
						if (!"array,object".contains(bodySchema.getItems().getType())) {
							String subType = bodySchema.getItems().getType();
							param.put("type", subType + "List");
						} else {
							if (bodySchema.getItems() != null && bodySchema.getItems().getProperties() != null
									&& bodySchema.getItems().getProperties().size() > 0) {
								children = bodySchema.getItems().getProperties().entrySet();
							}
						}
					}
				}

				if (children != null) {
					List<Object> childParams = new ArrayList();
					addChildren(param, childParams);
					for (Entry<String, Schema> child : children) {
						Schema childSchema = child.getValue();
						childSchema.setName(child.getKey());
						List<Object> list = getBody(childSchema, swagger);
						if (list != null && list.size() > 0)
							childParams.addAll(list);
					}
					if (childParams.size() == 0)
						param.remove("children");
				}
				break;
			}
			default: {
				param.put("type", "object");
			}
		}
		return body;
	}

	private static List<Object> getParameters(Operation op, String in) {
		List<Object> document = new ArrayList();
		List<Parameter> paramlist = op.getParameters();
		if (paramlist == null)
			return document;
		Map<String, Object> data = new HashMap();
		for (Parameter parameter : paramlist) {
			if (in.equalsIgnoreCase(parameter.getIn())) {
				Map<String, Object> documentParam = new HashMap();
				String name = parameter.getName();
				String description = encodeBas64(parameter.getDescription());
				Boolean required = parameter.getRequired();
				Schema<Parameter> schema = parameter.getSchema();
				String type = schema.getType();
				// String defaultValue=schema.getDefault();
				// String enumValues[]=schema.getEnum();
				documentParam.put("text", name);
				if (supportedTypes.contains((type.toLowerCase() + ",")))
					documentParam.put("type", type.toLowerCase());
				else
					documentParam.put("type", "string");
				data.put("fieldDescription", description);
				data.put("isRequiredField", required);
				documentParam.put("data", data);
				document.add(documentParam);
			}
		}
		return document;
	}
	public static void generateJavaClass(File file,String flowRef, DataPipeline dataPipeline)throws Exception {
		if (true) {
			return ;
		}
		String flowJavaTemplatePath= MiddlewareServer.getConfigFolderPath()+"flowJava.template";
		String className=file.getName().replace(".flow", "");
		String fullCode="";
		String pkg=flowRef.replace("/"+file.getName(),"").replace("/",".");
		List<String> lines = FileUtils.readLines(new File(flowJavaTemplatePath), "UTF-8");
		for (String line: lines) {
			String codeLine=(line.replace("#flowRef",flowRef).replace("#package",pkg).replace("#className",className));
			fullCode+=codeLine+"\n";
		}
		String javaFilePath=file.getAbsolutePath().replace(className+".flow", className+".java");
		File javaFile=new File(javaFilePath);
		if (!javaFile.exists()) {
			javaFile.createNewFile();
		}
		FileOutputStream fos = new FileOutputStream(javaFile);
		fos.write(fullCode.getBytes());
		fos.flush();
		fos.close();
		String fqn=pkg.replace("package ", "").replace(";","")+"."+className+".main";
	}
}
