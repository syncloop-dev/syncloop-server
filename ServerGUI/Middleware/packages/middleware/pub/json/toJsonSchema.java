package packages.middleware.pub.json;
import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.ServiceUtils;
import com.eka.middleware.template.SnippetException;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.HashMap;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.eka.middleware.template.MultiPart;
public final class toJsonSchema{
	public static final void main(DataPipeline dataPipeline) throws SnippetException{
try{
  //Boolean isArray=dataPipeline.getAsBoolean("isArray");
  Map<String, Object> payloadMap=new HashMap<String, Object>();
  payloadMap.put("*payload",dataPipeline.get("*payload"));
  /*if(isArray){
  	List<Map<String, Object>> docList=dataPipeline.getAsList("*payload");
    payloadMap.put("*payload",docList);
  }else
    payloadMap.put("*payload",dataPipeline.getAsList("*payload");
    */
  String jsonSchema=getJsonSchema(payloadMap);
  MultiPart mp = new MultiPart(dataPipeline, jsonSchema);
  //mp.putHeader(Headers.CONTENT_TYPE_STRING, "application/octet-stream");
}catch(Exception e){
  dataPipeline.clear();
  dataPipeline.put("error", e.getMessage());
  //e.printStackTrace();
}
	}
private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
private static String getJsonSchema(JsonNode properties) throws JsonProcessingException {
		ObjectNode schema = OBJECT_MAPPER.createObjectNode();
		schema.put("type", "object");

		schema.set("properties", properties);

		ObjectMapper jacksonObjectMapper = new ObjectMapper();
		String schemaString = jacksonObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(schema);
		return schemaString;
	}

	private static ObjectNode createProperty(JsonNode jsonData) throws IOException {
		ObjectNode propObject = OBJECT_MAPPER.createObjectNode();

		Iterator<Entry<String, JsonNode>> fieldsIterator = jsonData.fields();

		while (fieldsIterator.hasNext()) {
			Entry<String, JsonNode> field = fieldsIterator.next();

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

			// Get first element of the array
			JsonNodeType typeOfArrayElements = fieldValue.get(0).getNodeType();
			if (typeOfArrayElements.equals(JsonNodeType.OBJECT)) {
				property.set("items", createProperty(fieldValue.get(0)));
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

	public static String getJsonSchema(String jsonDocument) throws IllegalArgumentException, IOException {
		Map<String, Object> map = OBJECT_MAPPER.readValue(jsonDocument, new TypeReference<Map<String, Object>>() {});
		return getJsonSchema(map);
	}

	public static String getJsonSchema(Map<String, Object> jsonDocument) throws IllegalArgumentException, IOException {

		JsonNode properties = createProperty(OBJECT_MAPPER.convertValue(jsonDocument, JsonNode.class));
		return getJsonSchema(properties);

	}
}