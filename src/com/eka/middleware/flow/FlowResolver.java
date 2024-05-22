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
	JsonValue flowJsonValue = mainflowJsonObject.getJsonObject("latest").containsKey("api") ? mainflowJsonObject.getValue("/latest/api") : null;
	if (null == flowJsonValue || flowJsonValue.asJsonArray().isEmpty()) {
		flowJsonValue = mainflowJsonObject.getJsonObject("latest").containsKey("flow") ? mainflowJsonObject.getValue("/latest/flow") : null;
	}
	if (null == flowJsonValue) {
		return ;
	}
	JsonValue JsonInputValue=mainflowJsonObject.get("latest").asJsonObject().get("input");
	JsonValue JsonOutputValue=mainflowJsonObject.get("latest").asJsonObject().get("output");
	Boolean validationRequired=mainflowJsonObject.getBoolean("enableServiceDocumentValidation");
	//if(validationRequired)
		FlowUtils.validateDocuments(dp, JsonInputValue, validationRequired);
	JsonArray flows= flowJsonValue.asJsonArray();
	for (JsonValue jsonValue : flows) {
		final String type=jsonValue.asJsonObject().getString("type");
		switch(type) {
			case "try-catch":
				TCFBlock tcfBlock=new TCFBlock(jsonValue.asJsonObject());
				tcfBlock.process(dp);
			break;
			case "sequence":
			case "group":
				Scope scope=new Scope(jsonValue.asJsonObject());
				scope.process(dp);
			break;
			case "switch":
				Switch swich=new Switch(jsonValue.asJsonObject());
				swich.process(dp);
			break;
			case "ifelse":
				IfElse ifElse = new IfElse(jsonValue.asJsonObject());
				ifElse.process(dp);
				break;
			case "loop":
			case "foreach":
				Loop loop=new Loop(jsonValue.asJsonObject());
				loop.process(dp);
			break;
			case "repeat":
			case "redo":
				Repeat repeat=new Repeat(jsonValue.asJsonObject());
				repeat.process(dp);
			break;
			case "invoke":
			case "service":
				Api api=new Api(jsonValue.asJsonObject());
				api.process(dp);
			break;
			case "map":
			case "transformer":
				Transformer transformer=new Transformer(jsonValue.asJsonObject());
				transformer.process(dp);
			break;
			case "await":
				Await await=new Await(jsonValue.asJsonObject());
				await.process(dp);
			break;
			case "function":
				Function function = new Function(jsonValue.asJsonObject());
				function.process(dp);
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
/*	//if(dp.get("*multiPart")!=null)
		//outPutData.put("*multiPart", dp.get("*multiPart"));
	if(dp.get("lastErrorDump")!=null) {
		if(outPutData==null)
			outPutData=new HashMap<>();
		outPutData.put("lastErrorDump", dp.get("lastErrorDump"));
	}*/
	dp.clear();
	if(outPutData!=null)
		dp.putAll(outPutData);
	if(validationRequired)
		FlowUtils.validateDocuments(dp, JsonOutputValue, validationRequired);
}
}
