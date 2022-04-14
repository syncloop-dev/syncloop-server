package com.eka.middleware.flow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.template.SnippetException;

public class Loop {
	private boolean disabled=false;
	private String inputArrayPath;
	private String outPutArrayPath;
	private String condition;
	private JsonObject loop;
	private String label;
	private String comment;
	private JsonObject data;
	public Loop(JsonObject jo) {
		loop=jo;		
		data=loop.get("data").asJsonObject();
		setCondition(data.getString("condition",null));
		String status=data.getString("status",null);
		disabled="disabled".equals(status);
		setLabel(data.getString("label",null));
		comment=data.getString("comment",null);
		inputArrayPath=data.getString("inArray",null);
		outPutArrayPath=data.getString("outArray",null);
	}
	
	public void process(DataPipeline dp) throws SnippetException{
		if(disabled || inputArrayPath==null)
			return;
		inputArrayPath=("//"+inputArrayPath+"//").replace("///", "").replace("//","");
		String outKey=null;
		List<Object> outputList=null;
		String outTypePath="";
		String outputArrayParent="";
		Map<String, Object> outMap=null;
		if(outPutArrayPath!=null) {
			outPutArrayPath=("//"+outPutArrayPath+"//").replace("///", "").replace("//","");
			String outTokens[]=outPutArrayPath.split("/");
			outTypePath="";
			for (String string : outTokens) {
				outTypePath+="/document";
			}
			outKey=outTokens[outTokens.length-1];
			outputArrayParent=(outPutArrayPath+"_#").replace(outKey+"_#","");
			outputList= new ArrayList<>();
			
			
			if(outputArrayParent.trim().length()>0)
				outMap=(Map<String, Object>)dp.getAsMap(outputArrayParent);			
		}
		
		List<Object> list= dp.getAsList(inputArrayPath);
		if(list==null)
			return;
		
		String tokens[]=inputArrayPath.split("/");
		String key=tokens[tokens.length-1];
		String inputArrayParent=(inputArrayPath+"_#").replace(key+"_#","");
		Map<String, Object> map=null;
		if(inputArrayParent.trim().length()>0)
			map=(Map<String, Object>)dp.getAsMap(inputArrayParent);
//		if(map==null && dpM)
//			throw new SnippetException(dp, "Path pointer '"+inputArrayParent+"'. Please loop over parent array first", null);
		for (Object object : list) {
			if(map!=null)
				map.put(key, object);
			else
				dp.put(key, object);
			if(outPutArrayPath!=null) {
				Map<String, Object> newMap=new HashMap<String, Object>();
				if(outMap!=null)
					outMap.put(outKey, newMap);
				else
					dp.put(outKey, newMap);
				outputList.add(newMap);
			}
			
			JsonArray flows= loop.getJsonArray("children");
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
		}
//		map.put(key, list);
		
		if(map!=null)
			map.put(key, list);
		else
			dp.put(key, list);
		if(outPutArrayPath!=null) {
			if(outMap!=null)
				outMap.put(outKey, outputList);
			else
				dp.put(outKey, outputList);
		}
		
	}
	
	public boolean isDisabled() {
		return disabled;
	}
	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}
	public String getInputArrayPath() {
		return inputArrayPath;
	}
	public void setInputArrayPath(String inputArrayPath) {
		this.inputArrayPath = inputArrayPath;
	}
	public String getOutPutArrayPath() {
		return outPutArrayPath;
	}
	public void setOutPutArrayPath(String outPutArrayPath) {
		this.outPutArrayPath = outPutArrayPath;
	}

	public String getCondition() {
		return condition;
	}

	public void setCondition(String condition) {
		this.condition = condition;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}
}
