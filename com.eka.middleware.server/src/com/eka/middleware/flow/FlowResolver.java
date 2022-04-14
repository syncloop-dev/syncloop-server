package com.eka.middleware.flow;

import java.util.HashMap;
import java.util.Map;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.template.SnippetException;
import com.eka.middleware.template.SystemException;

public class FlowResolver {
public static void execute(DataPipeline dp,JsonObject mainflowJsonObject) throws SnippetException{
	JsonValue flowJsonValue = mainflowJsonObject.getValue("/latest/flow");
	JsonValue JsonInputValue=mainflowJsonObject.get("latest").asJsonObject().get("input");
	JsonValue JsonOutputValue=mainflowJsonObject.get("latest").asJsonObject().get("output");
	Boolean validationRequired=mainflowJsonObject.getBoolean("enableServiceDocumentValidation");
	if(validationRequired)
		FlowUtils.validateDocuments(dp, JsonInputValue);
	JsonArray flows= flowJsonValue.asJsonArray();
	for (JsonValue jsonValue : flows) {
		final String type=jsonValue.asJsonObject().getString("type");
		switch(type) {
			case "try-catch":
				TCFBlock tcfBlock=new TCFBlock(jsonValue.asJsonObject());
				tcfBlock.process(dp);
			break;
			case "sequence":
				Scope scope=new Scope(jsonValue.asJsonObject());
				scope.process(dp);
			break;
			case "switch":
				Switch swich=new Switch(jsonValue.asJsonObject());
				swich.process(dp);
			break;
			case "loop":
				Loop loop=new Loop(jsonValue.asJsonObject());
				loop.process(dp);
			break;
			case "repeat":
				Repeat repeat=new Repeat(jsonValue.asJsonObject());
				repeat.process(dp);
			break;
			case "invoke":
				Invoke invoke=new Invoke(jsonValue.asJsonObject());
				invoke.process(dp);
			break;
			case "map":
				Transformer transformer=new Transformer(jsonValue.asJsonObject());
				transformer.process(dp);
			break;		
		}	
	}
	
	//JsonValue flowOutputvalue=mainflowJsonObject.get("latest").asJsonObject().get("output");
	JsonArray flowOutputArray=JsonOutputValue.asJsonArray();
	
	if(flowOutputArray.isEmpty())
		flowOutputArray=null;
	Map<String, Object> outPutData=null;
	
	if(flowOutputArray!=null) {
		outPutData=new HashMap<>();
		for (JsonValue jsonValue : flowOutputArray) {
			String key=jsonValue.asJsonObject().getString("text");
			Object val=dp.get(key);
			if(val!=null)
				outPutData.put(key, dp.get(key));
		}
	}
	//if(dp.get("*multiPart")!=null)
		//outPutData.put("*multiPart", dp.get("*multiPart"));
	dp.clear();
	if(outPutData!=null)
		dp.putAll(outPutData);
	if(validationRequired)
		FlowUtils.validateDocuments(dp, JsonOutputValue);
}
}
