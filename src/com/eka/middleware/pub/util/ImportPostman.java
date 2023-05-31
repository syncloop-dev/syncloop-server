package com.eka.middleware.pub.util;

import com.eka.middleware.heap.HashMap;
import com.eka.middleware.pub.util.postman.*;
import com.eka.middleware.server.MiddlewareServer;
import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.ServiceUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.nimbusds.jose.shaded.gson.Gson;
import com.nimbusds.jose.shaded.gson.GsonBuilder;
import com.nimbusds.jose.shaded.gson.JsonSyntaxException;
import net.minidev.json.JSONObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import javax.json.JsonReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.eka.middleware.pub.util.ImportSwagger.*;

public class ImportPostman {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static void main(String[] args) throws Exception {

        PostmanCollection postmanCollection = new Gson().fromJson(new FileReader("E:/jwt4.json"), PostmanCollection.class);
        //createFlowServices("E:\\Nature9_Work\\ekamw-distributions\\integration\\middleware\\tenants\\default\\", "", "", postmanCollection.getItem(), true);
        //createFlowServicesClient("E:\\Nature9_Work\\ekamw-distributions\\integration\\middleware\\tenants\\default\\","", "", postmanCollection.getItem());


    }
    public static List<String> createFlowServices(String tenantPath, String servicePath, String packageName, List<PostmanItems> item, boolean isClientRequested ,DataPipeline dataPipeline) throws Exception {

        ArrayList<String> list = new ArrayList<>();


        for (PostmanItems postmanItems : item) {
            if (postmanItems.getItem() != null && !postmanItems.getItem().isEmpty()) {
                String slug = ServiceUtils.toServiceSlug(postmanItems.getName());
                list.addAll(createFlowServices(tenantPath, servicePath + File.separator + slug, packageName, postmanItems.getItem(), isClientRequested,dataPipeline));
            } else {
               list.add(generateServerStub(tenantPath, servicePath, packageName, postmanItems, isClientRequested, Evaluate.EEV,dataPipeline));
            }
        }
        return list;
    }
    public static List<String> createFlowServicesClient(String folder, String servicePath, String packageName, List<PostmanItems> item,DataPipeline dataPipeline) throws Exception {

        ArrayList<String> list = new ArrayList<String>();

        for (PostmanItems postmanItems : item) {
            if (postmanItems.getItem() != null && !postmanItems.getItem().isEmpty()) {
                String slug = ServiceUtils.toServiceSlug(postmanItems.getName());
                list.addAll(createFlowServicesClient(folder, servicePath + File.separator + slug, packageName, postmanItems.getItem(),dataPipeline));
            } if (postmanItems.getRequest() != null) {
                String method = postmanItems.getRequest().getMethod();
 }
        }
        return list;
    }
    public static String generateServerStub(String folderPath, String servicePath, String packageName, PostmanItems postmanItems, boolean isClientRequested, Evaluate evaluateFrom,DataPipeline dataPipeline) throws Exception {

        String filePath = folderPath + File.separator + "packages" + File.separator + packageName
                + File.separator + "server" + File.separator + servicePath + File.separator + ServiceUtils.toServiceSlug(postmanItems.getName()) + ".flow";

        File file = new File(filePath);
        FlowService flowService;
        if (file.exists() && false) {
            flowService = new Gson().fromJson(new FileReader(file), FlowService.class);
        } else {
            flowService = new FlowService("", "", Sets.newHashSet("consumers"), Sets.newHashSet("developers"),dataPipeline);
        }
        List<Object> inputs = flowService.getInput();

        inputs.clear();

        List<Object> outputs = flowService.getOutput();
        inputs.clear();

        Map<String, Object> intiMapStep = null;
        if (isClientRequested) {
            intiMapStep = createMapStep(flowService.getFlowSteps(), "Resolving Parameters");
        }

        List<String> listOfHeaderKeys = Lists.newArrayList();
        if (postmanItems.getRequest() != null) {
            List<Object> headers = getRequestHeaders(postmanItems.getRequest().getHeader(), intiMapStep, true, null);
            List<Object> query = getRequestQuery(postmanItems.getRequest().getUrl().getQuery(), intiMapStep, true, null);
            List<Object> path = getRequestPathParameters(postmanItems.getRequest().getUrl().getPath());
            listOfHeaderKeys = listOfHeaderKeys(postmanItems.getRequest().getHeader());

            addRequestBody(inputs, postmanItems, false);
            if (headers.size() > 0)
                inputs.add(ImportSwagger.createDocument("*requestHeaders", headers, ""));
            if (path.size() > 0)
                inputs.add(ImportSwagger.createDocument("*pathParameters", path, ""));
            if (query.size() > 0) {
//                inputs.add(ImportSwagger.createDocument("*queryParameters", query, ""));
            }

            if (null != postmanItems.getRequest().getUrl().getQuery() && !postmanItems.getRequest().getUrl().getQuery().isEmpty()) {
                for (UrlQuery urlQuery : postmanItems.getRequest().getUrl().getQuery()) {
                    Map<String, Object> inputVar = Maps.newHashMap();
                    inputVar.put("text", urlQuery.getKey());
                    inputVar.put("type", "string");
                    inputs.add(inputVar);
                }
            }
        }
        addResponseBody(outputs, postmanItems, false);

        if (isClientRequested) {

            Map<String, Object> invokeClient = createInvokeStep(flowService.getFlowSteps(), "flow",
                    ("packages/" + packageName + "/client/" + servicePath.replaceAll(Pattern.quote(File.separator), "/") + "/" + ServiceUtils.toServiceSlug(postmanItems.getName())).replaceAll("//", "/"), "Invoking Client");

            if (postmanItems.getRequest() != null) {

                if (null != postmanItems.getRequest().getUrl().getQuery() && !postmanItems.getRequest().getUrl().getQuery().isEmpty()) {
                    for (UrlQuery urlQuery : postmanItems.getRequest().getUrl().getQuery()) {
                        createPreInvokeMapping(invokeClient, "copy", "string", "/" + urlQuery.getKey(), "document", "/queryParameters/" + urlQuery.getKey());
                    }
                }
            }

            createPreInvokeMapping(invokeClient, "copy", "document", "/*payload", "document", "/payload");

            createPostInvokeMapping(invokeClient, "copy", "string", "/rawResponse", "string", "/rawResponse");
            createPostInvokeMapping(invokeClient, "copy", "string", "/statusCode", "string", "/statusCode");

            if (postmanItems.getRequest() != null) {

                if (null != postmanItems.getRequest().getAuth()) {
                    if ("basic".equalsIgnoreCase(postmanItems.getRequest().getAuth().getType())) {
                        String username = null;
                        String password = null;
                        List<Basic> basicList = postmanItems.getRequest().getAuth().getBasic();
                        for (Basic basic : basicList) {
                            if ("username".equalsIgnoreCase(basic.getKey())) {
                                username = basic.getValue();
                            } else {
                                password = basic.getValue();
                            }
                        }


                        createVariables(intiMapStep, "username", postmanToSyncloopProps(username), evaluateFrom(username, evaluateFrom), "string");
                        createVariables(intiMapStep, "password", postmanToSyncloopProps(password), evaluateFrom(password, evaluateFrom), "string");

                        createPreInvokeMapping(invokeClient, "copy", "string", "/username", "document/string", "/basicAuth/username");
                        createPreInvokeMapping(invokeClient, "copy", "string", "/password", "document/string", "/basicAuth/password");
                    } else if ("bearer".equalsIgnoreCase(postmanItems.getRequest().getAuth().getType())) {
                        String token = postmanItems.getRequest().getAuth().getBearer().get(0).getValue();
                        createVariables(intiMapStep, "*requestHeaders/Authorization", "Bearer " + postmanToSyncloopProps(token), evaluateFrom(token, evaluateFrom), "document/string");

                        listOfHeaderKeys.add("Authorization");
                    } else if ("apikey".equalsIgnoreCase(postmanItems.getRequest().getAuth().getType())) {
                        String key = null;
                        String value = null;
                        String where = null;
                        List<ApiKey> apiKeyList = postmanItems.getRequest().getAuth().getApikey();

                        for (ApiKey apiKey : apiKeyList) {
                            if ("key".equalsIgnoreCase(apiKey.getKey())) {
                                key = apiKey.getValue();
                            } else if ("value".equalsIgnoreCase(apiKey.getKey())) {
                                value = apiKey.getValue();
                            } else if ("in".equalsIgnoreCase(apiKey.getKey())) {
                                where = apiKey.getValue();
                            }
                        }
                        if ("header".equalsIgnoreCase(where)) {
                            createVariables(intiMapStep, "*requestHeaders/" + key, postmanToSyncloopProps(value), evaluateFrom(value, evaluateFrom), "document/string");

                            Map<String, String> requestHeadersKey = Maps.newHashMap();
                            requestHeadersKey.put("text", key);
                            requestHeadersKey.put("type", "string");

                            Map<String, Object> inputVar = Maps.newHashMap();
                            inputVar.put("text", "*requestHeaders");
                            inputVar.put("type", "document");
                            inputVar.put("children", Lists.newArrayList(requestHeadersKey));

                            inputs.add(inputVar);

                            createPreInvokeMapping(invokeClient, "copy", "document/string", "/*requestHeaders/" + key, "document/string", "/requestHeaders/" + key);

                        } else if ("query".equalsIgnoreCase(where)) {
                            createVariables(intiMapStep, key, postmanToSyncloopProps(value), evaluateFrom(value, evaluateFrom), "string");

                            Map<String, Object> inputVar = Maps.newHashMap();
                            inputVar.put("text", key);
                            inputVar.put("type", "string");
                            inputs.add(inputVar);

                            createPreInvokeMapping(invokeClient, "copy", "string", "/" + key, "document/string", "/queryParameters/" + key);

                        }
                    } else if ("awsv4".equalsIgnoreCase(postmanItems.getRequest().getAuth().getType())) {
                        String service = null;
                        String region = null;
                        String secretKey = null;
                        String accessKey = null;

                        List<AwsSignature> signatureList = postmanItems.getRequest().getAuth().getAwsv4();
                        for (AwsSignature aws : signatureList) {
                            if ("service".equalsIgnoreCase(aws.getKey())) {
                                service = aws.getValue();
                            } else if ("region".equalsIgnoreCase(aws.getKey())) {
                                region = aws.getValue();
                            } else if ("secretKey".equalsIgnoreCase(aws.getKey())) {
                                secretKey = aws.getValue();
                            } else if ("accessKey".equalsIgnoreCase(aws.getKey())) {
                                accessKey = aws.getValue();
                            }
                        }
                        createVariables(intiMapStep, "service", postmanToSyncloopProps(service), evaluateFrom(service, evaluateFrom), "string");
                        createVariables(intiMapStep, "region", postmanToSyncloopProps(region), evaluateFrom(region, evaluateFrom), "string");
                        createVariables(intiMapStep, "secretKey", postmanToSyncloopProps(secretKey), evaluateFrom(secretKey, evaluateFrom), "string");
                        createVariables(intiMapStep, "accessKey", postmanToSyncloopProps(accessKey), evaluateFrom(accessKey, evaluateFrom), "string");

                        createPreInvokeMapping(invokeClient, "copy", "string", "/service", "document/string", "/awsAuth/service");
                        createPreInvokeMapping(invokeClient, "copy", "string", "/region", "document/string", "/awsAuth/region");
                        createPreInvokeMapping(invokeClient, "copy", "string", "/secretKey", "document/string", "/awsAuth/secretKey");
                        createPreInvokeMapping(invokeClient, "copy", "string", "/accessKey", "document/string", "/awsAuth/accessKey");


                    }
                }
            }

            for (String headerKey : listOfHeaderKeys) {
                createPreInvokeMapping(invokeClient, "copy", "document/string", "/*requestHeaders/" + headerKey,
                        "document/string", "/requestHeaders/" + headerKey);
            }

            Map<String, Object> invokeStepToJson = createInvokeStep(flowService.getFlowSteps(), "service", "packages/middleware/pub/json/fromJson",
                    "Converting raw Json to JSON Object");

            createPreInvokeMapping(invokeStepToJson, "copy", "string", "/rawResponse", "string", "/jsonString");
            createPostInvokeMapping(invokeStepToJson, "copy", "document", "/output", "document", "/output");

            Map<String, String> commonOutputRawResponse = Maps.newHashMap();
            commonOutputRawResponse.put("text", "output");
            commonOutputRawResponse.put("type", "document");
            outputs.add(commonOutputRawResponse);

        }

        String json = new Gson().toJson(flowService.getFlow());
        saveFlow(filePath, json);
        generateJavaClass(file,servicePath,dataPipeline);

        return filePath;
    }

  /*  public static String getRequestBody(PostmanItems item) throws IOException {

        String jsonSchema = null;

        PostmanItemRequest request = item.getRequest();
        if (request != null) {
            PostmanRequestItemBody requestBody = request.getBody();
            if (requestBody != null && StringUtils.isNotBlank(requestBody.getRaw())) {
                String raw = requestBody.getRaw();
                System.out.println("raw " + raw);
                Gson gson = new GsonBuilder();
                Object map = gson.fromJson(raw, Object.class);
                jsonSchema = getJsonSchema(map);
            }
        }
        return jsonSchema;
    }*/

    public static String getRequestBody(PostmanItems item) throws IOException {

        String jsonSchema = null;

        PostmanItemRequest request = item.getRequest();
        if (request != null) {
            PostmanRequestItemBody requestBody = request.getBody();
            if (requestBody != null && StringUtils.isNotBlank(requestBody.getRaw())) {
                String raw = requestBody.getRaw();
                try {
                    Gson gson = new Gson();
                    Object map = gson.fromJson(raw, Object.class);
                    jsonSchema = getJsonSchema(map);
                } catch (JsonSyntaxException ex) {
                    String[] lines = raw.split("\n");
                    int lineNumber = 1;
                    for (String line : lines) {
                        try {
                            Gson gson = new Gson();
                            gson.fromJson(line, Object.class);
                        } catch (JsonSyntaxException e) {
                            break;
                        }
                        lineNumber++;
                    }
                }
            }
        }
        return jsonSchema;
    }

    private static String getJsonSchema(JsonNode properties) throws JsonProcessingException {
        final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
        ObjectNode schema = OBJECT_MAPPER.createObjectNode();
        schema.put("type", "object");

        schema.set("properties", properties);

        ObjectMapper jacksonObjectMapper = new ObjectMapper();
        String schemaString = jacksonObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(schema);
        return schemaString;
    }

    private static ObjectNode createProperty(JsonNode jsonData) throws IOException {

        final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

        ObjectNode propObject = OBJECT_MAPPER.createObjectNode();

        Iterator<Map.Entry<String, JsonNode>> fieldsIterator = jsonData.fields();

        while (fieldsIterator.hasNext()) {
            Map.Entry<String, JsonNode> field = fieldsIterator.next();

            String fieldName = field.getKey();
            JsonNode fieldValue = field.getValue();
            JsonNodeType fieldType = fieldValue.getNodeType();

            ObjectNode property = processJsonField(fieldValue, fieldType, fieldName);
            if (!property.isEmpty()) {
                propObject.set(fieldName, property);
            }
        }
        return propObject;
    }

    private static ObjectNode processJsonField(JsonNode fieldValue, JsonNodeType fieldType, String fieldName)
            throws IOException {
        ObjectNode property = OBJECT_MAPPER.createObjectNode();

        switch (fieldType) {

            case ARRAY:
                property.put("type", "array");

                if (fieldValue.isEmpty()) {
                    break;
                }
                JsonNodeType typeOfArrayElements = fieldValue.get(0).getNodeType();
                if (typeOfArrayElements.equals(JsonNodeType.OBJECT)) {
                    ObjectNode arraySchema = OBJECT_MAPPER.createObjectNode();
                    arraySchema.put("type", "object");
                    arraySchema.set("properties", createProperty(fieldValue.get(0)));
                    property.set("items", arraySchema);
                } else {
                    property.set("items", processJsonField(fieldValue.get(0), typeOfArrayElements, fieldName));
                }

                break;
            case BOOLEAN:
                property.put("type", "boolean");
                break;

            case NUMBER:
                property.put("type", "number");
                break;

            case OBJECT:
                property.put("type", "object");
                property.set("properties", createProperty(fieldValue));
                break;

            case STRING:
                property.put("type", "string");
                break;
            default:
                break;
        }
        return property;
    }

    public static String getJsonSchema(Object jsonDocument) throws IllegalArgumentException, IOException {
        final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
        JsonNode properties = createProperty(OBJECT_MAPPER.convertValue(jsonDocument, JsonNode.class));
        return getJsonSchema(properties);
    }

    public static String addResponses(PostmanItems postmanItems) throws IOException {

        Map<String, Object> map = new HashMap<>();
        if (postmanItems.getResponse() != null) {
            if (postmanItems.getResponse().isEmpty()) {
                map.put("*200", new HashMap<>());
            } else {
                for (PostmanItemResponse response : postmanItems.getResponse()) {
                    if (response != null) {
                        int code = response.getCode();
                        String requestBody = response.getBody();
                        Object object = new HashMap<>();
                        if (StringUtils.isNotBlank(requestBody)) {
                            Gson gson = new Gson();
                            try {
                                object = gson.fromJson(requestBody, Object.class);
                            } catch (JsonSyntaxException e) {

                            }
                        }
                        map.put("*" + code, object);
                    }
                }
            }
        }
        return getJsonSchema(map);
    }

    private static void addRequestBody(List<Object> inputs, PostmanItems rbpostmanItems, boolean isClient)
            throws IOException {

        String payloadParam = "*payload";
        if (isClient) {
            payloadParam = "payload";
        }
        String requestBody = getRequestBody(rbpostmanItems);

        if (StringUtils.isNotBlank(requestBody)) {

            GetBody.NodeProperties map = new Gson().fromJson(requestBody, GetBody.NodeProperties.class);

            List<Object> objects = Lists.newArrayList(GetBody.getJstreeFromSchema(map));

            Map<String, Object> objectMap = createDocument(payloadParam, objects, "description");
            inputs.addAll(Lists.newArrayList(objectMap));
        }
    }

    private static List<Object> getRequestHeaders(List<PostmanRequestHeaders> headers, Map<String, Object> firstMap, boolean isServer, Evaluate evaluateFrom) {
        if (null == headers) {
            return Lists.newArrayList();
        }
        List<Object> document = new ArrayList<>();
        for (PostmanRequestHeaders header : headers) {
            if (StringUtils.isNotBlank(header.getValue()) && isServer) {
                createVariables(firstMap, "*requestHeaders/" + header.getKey(), postmanToSyncloopProps(header.getValue()),
                        evaluateFrom(header.getValue(), evaluateFrom), "document/string");
                //continue;
            }
            Map<String, Object> documentParam = new HashMap<>();
            Map<String, Object> data = new HashMap<>();
            documentParam.put("text", header.getKey());
            documentParam.put("type", "string");

            data.put("isRequiredField", true);
            documentParam.put("data", data);
            boolean add = document.add(documentParam);
        }
        return document;
    }

    private static List<String> listOfHeaderKeys(List<PostmanRequestHeaders> headers) {
        if (null == headers) {
            return Lists.newArrayList();
        }
        List<String> document = new ArrayList<>();
        for (PostmanRequestHeaders header : headers) {
            document.add(header.getKey());
        }
        return document;
    }

    private static List<Object> getRequestQuery(List<UrlQuery> queries, Map<String, Object> firstMap, boolean isServer, Evaluate evaluateFrom) {
        if (null == queries) {
            return Lists.newArrayList();
        }
        List<Object> document = new ArrayList<>();
        for (UrlQuery query : queries) {
            if (StringUtils.isNotBlank(query.getValue()) && isServer) {
                createVariables(firstMap, query.getKey(), postmanToSyncloopProps(query.getValue()),
                        evaluateFrom(query.getValue(), evaluateFrom), "string");
                //continue;
            }
            Map<String, Object> documentParam = new HashMap<>();
            Map<String, Object> data = new HashMap<>();
            documentParam.put("text", query.getKey());
            documentParam.put("type", "string");

            data.put("fieldDescription", encodeBas64(query.getDescription()));
            data.put("isRequiredField", true);
            documentParam.put("data", data);
            document.add(documentParam);
        }
        return document;
    }

    private static List<Object> getRequestPathParameters(List<String> path) {
        if (null == path) {
            return Lists.newArrayList();
        }

        List<Object> document = new ArrayList();
        for (String header : path) {
            if (header.indexOf("}}") < 0) {
                continue;
            }
            header = header.replaceAll(Pattern.quote("{{"), "").replaceAll("}}", "");
            Map<String, Object> documentParam = new HashMap();
            Map<String, Object> data = new HashMap();
            documentParam.put("text", header);
            documentParam.put("type", "string");
            data.put("isRequiredField", true);
            documentParam.put("data", data);
            document.add(documentParam);
        }
        return document;
    }

    private static void saveFlow(String filePath, String json) {

        File file = new File(filePath);
        boolean mkdirs = file.getParentFile().mkdirs();

        try (FileOutputStream fos = new FileOutputStream(file)) {
            IOUtils.write(json.getBytes(), fos);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getJsonSchema(String jsonDocument) throws IllegalArgumentException, IOException {
        Map<String, Object> map = OBJECT_MAPPER.readValue(jsonDocument, new TypeReference<Map<String, Object>>() {
        });
        return getJsonSchema(map);
    }

    private static void addResponseBody(List<Object> output, PostmanItems rbpostmanItems, boolean isClient)
            throws IOException {

        String responseBody = addResponses(rbpostmanItems);
        if (StringUtils.isNotBlank(responseBody)) {
            GetBody.NodeProperties map = new Gson().fromJson(responseBody, GetBody.NodeProperties.class);
            output.addAll(GetBody.getJstreeFromSchema(map));
        }
    }

    private static String generateClientLib(String folder, String servicePath, String packageName, PostmanItems postmanItems, String method, Evaluate evaluateFrom,DataPipeline dataPipeline) throws Exception {

        String filePath = folder + File.separator + "packages" + File.separator + packageName
                + File.separator + "client" + File.separator + servicePath + File.separator + ServiceUtils.toServiceSlug(postmanItems.getName()) + ".flow";

        File file = new File(filePath);
        FlowService flowService;
        if (file.exists() && false) {
            flowService = new Gson().fromJson(new FileReader(file), FlowService.class);
        } else {
            flowService = new FlowService("", "", Sets.newHashSet("consumers"), Sets.newHashSet("developers"),dataPipeline);
        }
        List<Object> inputs = flowService.getInput();
        List<Object> outputs = flowService.getOutput();
        List<Object> flowSteps = flowService.getFlowSteps();

        Map<String, Object> intiMapStep = createMapStep(flowSteps, "Resolving Parameters");

        if (postmanItems.getRequest() != null) {
            List<Object> headers = getRequestHeaders(postmanItems.getRequest().getHeader(), intiMapStep, false, evaluateFrom);
            List<Object> query = getRequestQuery(postmanItems.getRequest().getUrl().getQuery(), intiMapStep, false, evaluateFrom);
            List<Object> path = getRequestPathParameters(postmanItems.getRequest().getUrl().getPath());

            addRequestBody(inputs, postmanItems, true);

            if (headers.size() > 0)
                inputs.add(createDocument("requestHeaders", headers, ""));
            if (path.size() > 0)
                inputs.add(createDocument("pathParameters", path, ""));
            if (query.size() > 0)
                inputs.add(createDocument("queryParameters", query, ""));
        }

        createVariables(intiMapStep, "basePath", "#{basePath}", evaluateFrom, "string");

        intiMapStep = createMapStep(flowSteps, "Initializing Parameters");

        String alias = StringUtils.join(postmanItems.getRequest().getUrl().getPath(), "/");

        String updatedAlias = "";
        if (StringUtils.isNotBlank(alias)) {
            updatedAlias = postmanToSyncloopProps(alias);
        }

        createVariables(intiMapStep, "method", method, null, "string");
       // createVariables(intiMapStep, "url", "#{basePath}" + postmanToSyncloopProps(updatedAlias, "pathParameters"), Evaluate.EEV, "string");
        createVariables(intiMapStep, "url", "#{basePath}" + updatedAlias.replaceAll(Pattern.quote("{"), "#{pathParameters/").replaceAll("(#+)", "#"), Evaluate.EEV, "string");
        dropVariables(intiMapStep, "basePath", "string");
        dropVariables(intiMapStep, "pathParameters", "document");

        if (!"GET".equalsIgnoreCase(method)) {
            Map<String, Object> invokeStepToJson = createInvokeStep(flowSteps, "service", "packages/middleware/pub/json/toString", "Initialize");
            createPreInvokeMapping(invokeStepToJson, "copy", "document", "/payload", "documentList", "/root");
            createPostInvokeMapping(invokeStepToJson, "copy", "string", "/jsonString", "string", "/body");
        }

        if (null != postmanItems.getRequest().getAuth()) {
            if ("basic".equalsIgnoreCase(postmanItems.getRequest().getAuth().getType())) {
                String username = null;
                String password = null;
                List<Basic> basicList = postmanItems.getRequest().getAuth().getBasic();
                for (Basic basic : basicList) {
                    if ("username".equalsIgnoreCase(basic.getKey())) {
                        username = basic.getValue();
                    } else {
                        password = basic.getValue();
                    }
                }

                List<Object> basicAuth = Lists.newArrayList();

                Map<String, String> inputVar = Maps.newHashMap();
                inputVar.put("text", "username");
                inputVar.put("type", "string");
                basicAuth.add(inputVar);

                inputVar = Maps.newHashMap();
                inputVar.put("text", "password");
                inputVar.put("type", "string");
                basicAuth.add(inputVar);

                Map<String, Object> inputAuthVar = Maps.newHashMap();
                inputAuthVar.put("text", "basicAuth");
                inputAuthVar.put("type", "document");
                inputAuthVar.put("children", basicAuth);
                inputs.add(inputAuthVar);

                //createVariables(intiMapStep, "username", postmanToSyncloopProps(username), evaluateFrom(username, evaluateFrom), "string");
                //createVariables(intiMapStep, "password", postmanToSyncloopProps(password), evaluateFrom(password, evaluateFrom), "string");
                Map<String, Object> invokeStepToJson1 = createInvokeStep(flowSteps, "service", "packages/Wrapper/Authorization/Basic", "Initialize");

                createPreInvokeMapping(invokeStepToJson1, "copy", "document/string", "/basicAuth/username", "string", "/username");
                createPreInvokeMapping(invokeStepToJson1, "copy", "document/string", "/basicAuth/password", "string", "/password");

                createPostInvokeMapping(invokeStepToJson1, "copy", "string", "/Authorization", "document/string", "/requestHeaders/Authorization");

            } else if ("bearer".equalsIgnoreCase(postmanItems.getRequest().getAuth().getType())) {
                //String token = postmanItems.getRequest().getAuth().getBearer().get(0).getValue();
                //createVariables(intiMapStep, "requestHeaders/Authorization", "Bearer " + postmanToSyncloopProps(token), evaluateFrom(token, evaluateFrom), "document/string");

                Map<String, Object> inputVar = Maps.newHashMap();
                inputVar.put("text", "requestHeaders");
                inputVar.put("type", "document");

                Map<String, String> inputVarAuth = Maps.newHashMap();
                inputVarAuth.put("text", "Authorization");
                inputVarAuth.put("type", "string");
                inputVar.put("children", Lists.newArrayList(inputVarAuth));

                inputs.add(inputVar);

            } else if ("apikey".equalsIgnoreCase(postmanItems.getRequest().getAuth().getType())) {
                String key = null;
                String value = null;
                String where = null;
                List<ApiKey> apiKeyList = postmanItems.getRequest().getAuth().getApikey();

                for (ApiKey apiKey : apiKeyList) {
                    if ("key".equalsIgnoreCase(apiKey.getKey())) {
                        key = apiKey.getValue();
                    } else if ("value".equalsIgnoreCase(apiKey.getKey())) {
                        value = apiKey.getValue();
                    } else if ("in".equalsIgnoreCase(apiKey.getKey())) {
                        where = apiKey.getValue();
                    }
                }
                if ("header".equalsIgnoreCase(where)) {
                    //createVariables(intiMapStep, "requestHeaders/"+key , postmanToSyncloopProps(value), evaluateFrom(value, evaluateFrom), "document/string");

                    Map<String, String> requestHeadersKey = Maps.newHashMap();
                    requestHeadersKey.put("text", key);
                    requestHeadersKey.put("type", "string");

                    Map<String, Object> inputVar = Maps.newHashMap();
                    inputVar.put("text", "requestHeaders");
                    inputVar.put("type", "document");
                    inputVar.put("children", Lists.newArrayList(requestHeadersKey));

                    inputs.add(inputVar);

                } else if ("query".equalsIgnoreCase(where)) {
                    //createVariables(intiMapStep, "queryParameters/"+key, postmanToSyncloopProps(value), evaluateFrom(value, evaluateFrom), "document/string");

                    Map<String, String> requestHeadersKey = Maps.newHashMap();
                    requestHeadersKey.put("text", key);
                    requestHeadersKey.put("type", "string");

                    Map<String, Object> inputVar = Maps.newHashMap();
                    inputVar.put("text", "queryParameters");
                    inputVar.put("type", "document");
                    inputVar.put("children", Lists.newArrayList(requestHeadersKey));

                    inputs.add(inputVar);
                }
            } else if ("awsv4".equalsIgnoreCase(postmanItems.getRequest().getAuth().getType())) {
                String service = null;
                String region = null;
                String secretKey = null;
                String accessKey = null;

                List<AwsSignature> signatureList = postmanItems.getRequest().getAuth().getAwsv4();
                for (AwsSignature aws : signatureList) {
                    if ("service".equalsIgnoreCase(aws.getKey())) {
                        service = aws.getValue();
                    } else if ("region".equalsIgnoreCase(aws.getKey())) {
                        region = aws.getValue();
                    } else if ("secretKey".equalsIgnoreCase(aws.getKey())) {
                        secretKey = aws.getValue();
                    } else if ("accessKey".equalsIgnoreCase(aws.getKey())) {
                        accessKey = aws.getValue();
                    }
                }
//                createVariables(intiMapStep, "service", postmanToSyncloopProps(service), evaluateFrom(service, evaluateFrom), "string");
//                createVariables(intiMapStep, "region", postmanToSyncloopProps(region), evaluateFrom(region, evaluateFrom), "string");
//                createVariables(intiMapStep, "secretKey", postmanToSyncloopProps(secretKey), evaluateFrom(secretKey, evaluateFrom), "string");
//                createVariables(intiMapStep, "accessKey", postmanToSyncloopProps(accessKey), evaluateFrom(accessKey, evaluateFrom), "string");

                List<Object> awsAuth = Lists.newArrayList();
                Map<String, String> serviceMap = Maps.newHashMap();
                serviceMap.put("text", "service");
                serviceMap.put("type", "string");
                awsAuth.add(serviceMap);

                Map<String, String> regionMap = Maps.newHashMap();
                regionMap.put("text", "region");
                regionMap.put("type", "string");
                awsAuth.add(regionMap);

                Map<String, String> accessKeyMap = Maps.newHashMap();
                accessKeyMap.put("text", "accessKey");
                accessKeyMap.put("type", "string");
                awsAuth.add(accessKeyMap);

                Map<String, String> secretMap = Maps.newHashMap();
                secretMap.put("text", "secretKey");
                secretMap.put("type", "string");
                awsAuth.add(secretMap);

                Map<String, Object> inputVar = Maps.newHashMap();
                inputVar.put("text", "awsAuth");
                inputVar.put("type", "document");
                inputVar.put("children", awsAuth);

                inputs.add(inputVar);

                Map<String, Object> invokeStepToJson = createInvokeStep(flowSteps, "service", "packages/Wrapper/AWS/credential/CreateSigner", "Initialize");

                if (!"GET".equalsIgnoreCase(method)) {
                    createPreInvokeMapping(invokeStepToJson, "copy", "string", "/body", "string", "/payload");
                }

                createPreInvokeMapping(invokeStepToJson, "copy", "document/string", "/awsAuth/service", "string", "/service");
                createPreInvokeMapping(invokeStepToJson, "copy", "document/string", "/awsAuth/region", "string", "/region");
                createPreInvokeMapping(invokeStepToJson, "copy", "document/string", "/awsAuth/secretKey", "string", "/secret");
                createPreInvokeMapping(invokeStepToJson, "copy", "document/string", "/awsAuth/accessKey", "string", "/accessKey");

                createPreInvokeMapping(invokeStepToJson, "copy", "string", "/url", "string", "/url");
                createPreInvokeMapping(invokeStepToJson, "copy", "string", "/method", "string", "/method");
                createPreInvokeMapping(invokeStepToJson, "copy", "document", "/queryParameters", "document", "/queryParameters");
                createPreInvokeMapping(invokeStepToJson, "copy", "document", "/requestHeaders", "document", "/headers");
                createPostInvokeMapping(invokeStepToJson, "copy", "document", "/headers", "document", "/requestHeaders");
            } else if ("jwt".equalsIgnoreCase(postmanItems.getRequest().getAuth().getType())) {
                String payload = null;
                String isSecretBase64Encoded = null;
                String secret = null;
                String algorithm = null;
                String addTokenTo = null;
                String headerPrefix = null;
                String queryParamKey = null;
                Map<String, Object> header = new HashMap<>();
                List<JwtBearer> jwtList = postmanItems.getRequest().getAuth().getJwt();
                for (JwtBearer jwt : jwtList) {
                    if ("payload".equalsIgnoreCase(jwt.getKey())) {
                        payload = jwt.getValue();
                    } else if ("isSecretBase64Encoded".equalsIgnoreCase(jwt.getKey())) {
                        isSecretBase64Encoded = jwt.getValue();
                    } else if ("secret".equalsIgnoreCase(jwt.getKey())) {
                        secret = jwt.getValue();
                    } else if ("algorithm".equalsIgnoreCase(jwt.getKey())) {
                        algorithm = jwt.getValue();
                    } else if ("addTokenTo".equalsIgnoreCase(jwt.getKey())) {
                        addTokenTo = jwt.getValue();
                    } else if ("headerPrefix".equalsIgnoreCase(jwt.getKey())) {
                        headerPrefix = jwt.getValue();
                    } else if ("queryParamKey".equalsIgnoreCase(jwt.getKey())) {
                        queryParamKey = jwt.getValue();
                    } else if ("header".equalsIgnoreCase(jwt.getKey())) {

                        ObjectMapper objectMapper = new ObjectMapper();
                        try {
                            header = objectMapper.readValue(jwt.getValue(), new TypeReference<Map<String, Object>>() {
                            });

                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
                    }

                }

                createVariables(intiMapStep, "payload", payload, null, "string");
                createVariables(intiMapStep, "secret", secret, null, "string");
                createVariables(intiMapStep, "algorithm", algorithm, null, "string");
                createVariables(intiMapStep, "headerPrefix", headerPrefix, null, "string");
                createVariables(intiMapStep, "header/key", header.toString(), null, "document/string");

                Map<String, Object> invokeStepToJson = createInvokeStep(flowSteps, "service", "packages/Wrapper/Authorization/JwtToken", "Initialize");

                createPreInvokeMapping(invokeStepToJson, "copy", "string", "/payload", "string", "/payload");
                createPreInvokeMapping(invokeStepToJson, "copy", "string", "/secret", "string", "/secret");
                createPreInvokeMapping(invokeStepToJson, "copy", "string", "/algorithm", "string", "/algorithm");
                createPreInvokeMapping(invokeStepToJson, "copy", "string", "/headerPrefix", "string", "/headerPrefix");
                createPreInvokeMapping(invokeStepToJson, "copy", "document", "/header", "document", "/header");

            }
        }


        Map<String, Object> invokeStepRequest = createInvokeStep(flowSteps, "service", "packages/middleware/pub/client/http/request", "Initialize");

        createPreInvokeMapping(invokeStepRequest, "copy", "document", "/requestHeaders", "document", "/headers");
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


        Map<String, Object> switchMapping = createSwitch(flowSteps, "switch", "SWITCH", "Checking status for response");
        addData(switchMapping, "switch", "statusCode");

        List<PostmanItemResponse> response = postmanItems.getResponse();

        List<Object> cases = Lists.newArrayList();
        if (null != response) {
            if (response.isEmpty()) {
                PostmanItemResponse postmanItemResponse = new PostmanItemResponse();
                postmanItemResponse.setCode(200);
                response.add(postmanItemResponse);
            }
            for (PostmanItemResponse resp : response) {

                String code = resp.getCode() + "";
                Map<String, Object> sequenceMapping = createCase(cases, "CASE", "Handling " + code + " response");
                addData(sequenceMapping, "case", code);

                List<Object> contentTypeSwitch = Lists.newArrayList();
                addContentTypeHandler(contentTypeSwitch, intiMapStep, resp);
                addChildren(sequenceMapping, contentTypeSwitch);
            }
        }
        addChildren(switchMapping, cases);

        Map<String, Object> commonResponse = createMapStep(flowSteps, "Mapping Common Response & HTTP Status Code");
        createMapping(commonResponse, "copy", "string", "/respPayload", "string", "/rawResponse");

        addResponseBody(outputs, postmanItems, true);

        Map<String, String> commonOutputRawResponse = Maps.newHashMap();
        commonOutputRawResponse.put("text", "rawResponse");
        commonOutputRawResponse.put("type", "string");
        outputs.add(commonOutputRawResponse);

        Map<String, String> commonOutputStatusCode = Maps.newHashMap();
        commonOutputStatusCode.put("text", "statusCode");
        commonOutputStatusCode.put("type", "string");
        outputs.add(commonOutputStatusCode);

        // Add multiple consumer groups
        String json = new Gson().toJson(flowService.getFlow());
        Map<String, Object> map = new Gson().fromJson(json, Map.class);

        List<String> consumerGroups = new ArrayList<>();
        consumerGroups.add("consumers");
        map.put("consumers", StringUtils.join(consumerGroups, ","));


        List<String> developerGroups = new ArrayList<>();
        developerGroups.add("developers");
        map.put("developers", StringUtils.join(developerGroups, ","));

        JSONObject jsonObject = new JSONObject(map);
        String updatedJson = jsonObject.toString();
        saveFlow(filePath, updatedJson);
        generateJavaClass(file,servicePath,dataPipeline);
        return filePath;
    }

    public static Map<String, Object> addContentTypeHandler(List<Object> contentTypeSwitch, Map<String, Object> intiMapStep, PostmanItemResponse postmanItemResponse) {

        String code = postmanItemResponse.getCode() + "";

        Map<String, Object> switchContentTypeMapping = createSwitch(contentTypeSwitch, "switch", "SWITCH", "Checking content type for response");
        addData(switchContentTypeMapping, "switch", "respHeaders/content-type");

        List<Object> contentTypeCases = com.beust.jcommander.internal.Lists.newArrayList();
        String body = postmanItemResponse.getBody();

        if (null != body) {
            {
                Map<String, Object> sequenceContentTypeMapping = createCase(contentTypeCases, "CASE", "Handling JSON response");
                addData(sequenceContentTypeMapping, "case", "#regex:.*json.*");

                List<Object> jsonConversionCase = com.beust.jcommander.internal.Lists.newArrayList();
                createVariables(intiMapStep, "rootName_Error", "Error", null, "string");

                Map<String, Object> invokeStepToJson = createInvokeStep(jsonConversionCase, "service", "packages/middleware/pub/json/fromJson",
                        "Converting raw Json to JSON Object");

                createPreInvokeMapping(invokeStepToJson, "copy", "string", "/respPayload", "string", "/jsonString");
                createPreInvokeMapping(invokeStepToJson, "copy", "string", "/rootName_Error", "string", "/rootName");
                createPostInvokeMapping(invokeStepToJson, "copy", "document", "/output", "document", "/*" + code);
                addChildren(sequenceContentTypeMapping, jsonConversionCase);
            }
            {
                Map<String, Object> sequenceContentTypeMapping = createCase(contentTypeCases, "CASE", "Handling XML response");
                addData(sequenceContentTypeMapping, "case", "#regex:.*xml.*");

                List<Object> jsonConversionCase = com.beust.jcommander.internal.Lists.newArrayList();
                Map<String, Object> invokeStepToJson = createInvokeStep(jsonConversionCase, "service", "packages/middleware/pub/xml/fromXML", "Initialize");

                createPreInvokeMapping(invokeStepToJson, "copy", "string", "/respPayload", "string", "/xml");
                createPostInvokeMapping(invokeStepToJson, "copy", "document/document", "/output/root", "document/document", "/*" + code + "/root");
                addChildren(sequenceContentTypeMapping, jsonConversionCase);
            }
        } else {
            {
                Map<String, Object> sequenceContentTypeMapping = createCase(contentTypeCases, "CASE", "Handling JSON response");
                addData(sequenceContentTypeMapping, "case", "#regex:.*json.*");

                List<Object> jsonConversionCase = com.beust.jcommander.internal.Lists.newArrayList();
                createVariables(intiMapStep, "rootName_Error", "Error", null, "string");

                Map<String, Object> invokeStepToJson = createInvokeStep(jsonConversionCase, "service", "packages/middleware/pub/json/fromJson", "Converting raw Json to JSON Object");

                createPreInvokeMapping(invokeStepToJson, "copy", "string", "/respPayload", "string", "/jsonString");
                createPreInvokeMapping(invokeStepToJson, "copy", "string", "/rootName_Error", "string", "/rootName");
                createPostInvokeMapping(invokeStepToJson, "copy", "document", "/output", "document", "/*" + code);
                addChildren(sequenceContentTypeMapping, jsonConversionCase);
            }
            {
                Map<String, Object> sequenceContentTypeMapping = createCase(contentTypeCases, "CASE", "Handling XML response");
                addData(sequenceContentTypeMapping, "case", "#regex:.*xml.*");

                List<Object> jsonConversionCase = com.beust.jcommander.internal.Lists.newArrayList();
                Map<String, Object> invokeStepToJson = createInvokeStep(jsonConversionCase, "service", "packages/middleware/pub/xml/fromXML", "Initialize");

                createPreInvokeMapping(invokeStepToJson, "copy", "string", "/respPayload", "string", "/xml");
                createPostInvokeMapping(invokeStepToJson, "copy", "document/document", "/output/root", "document/document", "/*" + code + "/root");
                addChildren(sequenceContentTypeMapping, jsonConversionCase);
            }
        }
        addChildren(switchContentTypeMapping, contentTypeCases);
        return switchContentTypeMapping;
    }

    private static String postmanToSyncloopProps(String str) {
        return str.replaceAll(Pattern.quote("{{"), "#{").replaceAll(Pattern.quote("}}"), "}");
    }

    private static String postmanToSyncloopProps(String str, String parentDocument) {
        return str.replaceAll(Pattern.quote("{{"), "#{" + parentDocument + "/").replaceAll(Pattern.quote("}}"), "}");
    }

    private static Evaluate evaluateFrom(String key, Evaluate evaluateFrom) {
        Evaluate markEvaluation = evaluateFrom;
        if (!key.contains("{{")) {
            markEvaluation = null;
        }
        return markEvaluation;
    }


    public static void generateJavaClass(File file,String flowRef, DataPipeline dataPipeline)throws Exception {
        String flowJavaTemplatePath=MiddlewareServer.getConfigFolderPath()+"flowJava.template";
        //String flowJavaTemplatePath = file+"";

        //System.out.println("flowJavaTemplatePath: "+flowJavaTemplatePath);
        String className=file.getName().replace(".flow", "");
        //URL url = new URL(flowJavaTemplatePath);
        String fullCode="";
        String pkg=flowRef.replace("/"+file.getName(),"").replace("/",".");
        List<String> lines = FileUtils.readLines(new File(flowJavaTemplatePath), "UTF-8");
        for (String line: lines) {
            String codeLine=(line.replace("#flowRef",flowRef).replace("#package",pkg).replace("#className",className));
            fullCode+=codeLine+"\n";
            //dataPipeline.log("\n");
            //dataPipeline.log(codeLine);
        }
        //dataPipeline.log("\n");
        //return fullCode;

        //System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
        //String fullCode="";//pkg+"\n"+imports+"\n"+classDef+"\n"+mainDef+"\n"+mainFunc+"\n"+mainDefClose+"\n"+classDefClose;
        //System.out.println(fullCode);
        //System.out.println(className+".service");
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

        dataPipeline.log("fqn: "+fqn);
       // ServiceUtils.compileJavaCode(fqn,dataPipeline);
    }
}