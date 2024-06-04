package com.eka.middleware.flow;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.FlowBasicInfo;
import com.eka.middleware.template.SnippetException;
import com.google.common.collect.Maps;
import lombok.Getter;

public class Loop implements FlowBasicInfo {
	private boolean disabled = false;
	private String inputArrayPath;
	private String outPutArrayPath;
	private String condition;
	private JsonObject loop;
	private String label;
	private String comment;
	private JsonObject data;
	private String indexVar = "*index";
	private String outArrayType = "document";
	private String snapshot=null;
	private String snapCondition=null;

	@Getter
	private String name;

	@Getter
	private String type;

	@Getter
	private String guid;

	private int allowedLoop = 1000;

	public Loop(JsonObject jo) {
		loop = jo;
		data = loop.get("data").asJsonObject();
		setCondition(data.getString("condition", null));
		String status = data.getString("status", null);
		disabled = "disabled".equals(status);
		setLabel(data.getString("label", null));
		comment = data.getString("comment", null);
		inputArrayPath = data.getString("inArray", null);
		outPutArrayPath = data.getString("outArray", null);
		snapshot=data.getString("snap",null);
		if(snapshot!=null && snapshot.equals("disabled"))
			snapshot=null;
		snapCondition=data.getString("snapCondition",null);
		indexVar = data.getString("indexVar", "*index");
		outArrayType = data.getString("outArrayType", "document");

		guid = data.getString("guid",null);
		name = loop.getString("text",null);
		type = loop.getString("type",null);
	}

	public void process(DataPipeline dp) throws SnippetException {
		Map<String, Object> snapMeta = Maps.newHashMap();
		snapMeta.put("*indexVar", indexVar);
		if(dp.isDestroyed()) {
			throw new SnippetException(dp, "User aborted the service thread", new Exception("Service runtime pipeline destroyed manually"));
		}
		if (disabled || inputArrayPath == null) {
			return;
		}
		dp.addErrorStack(this);

		String snap=dp.getString("*snapshot");
		boolean canSnap = false;
		if(snap!=null || snapshot!=null) {
			canSnap = true;
			//snap=snapshot;
			if(snapshot!=null && snapshot.equals("conditional") && snapCondition!=null){
				canSnap =FlowUtils.evaluateCondition(snapCondition,dp);
				if(canSnap)
					dp.put("*snapshot","enabled");
			}else
				dp.put("*snapshot","enabled");
		}
		/*if(!canSnap)
			dp.drop("*snapshot");*/
		if(canSnap ) {
			dp.snapBefore(comment, guid);
		}
		
		try {
			inputArrayPath = ("//" + inputArrayPath + "//").replace("///", "").replace("//", "");
			String outKey = null;
			List<Object> outputList = null;
			String outTypePath = "";
			String outputArrayParent = "";
			Object outVar = null;
			if (outPutArrayPath != null && outPutArrayPath.trim().length()>0) {
				outPutArrayPath = ("//" + outPutArrayPath + "//").replace("///", "").replace("//", "");
				String outTokens[] = outPutArrayPath.split("/");
				outTypePath = "";
				for (String string : outTokens) {
					outTypePath += "/document";
				}
				outKey = outTokens[outTokens.length - 1];
				outputArrayParent = (outPutArrayPath + "_#").replace(outKey + "_#", "");
				outputList = new ArrayList<>();

				if (outputArrayParent.trim().length() > 0) {
					switch (outArrayType) {
						case "document":
							outVar = dp.getAsMap(outputArrayParent);
							break;
						case "string":
							outVar = dp.getAsString(outputArrayParent);
							break;
						case "integer":
							outVar = dp.getAsInteger(outputArrayParent);
							break;
						case "number":
							outVar = dp.getAsNumber(outputArrayParent);
							break;
						case "boolean":
							outVar = dp.getAsBoolean(outputArrayParent);
							break;
						case "byte":
							outVar = dp.getAsByte(outputArrayParent);
							break;
						case "date":
							outVar = dp.getAsDate(outputArrayParent);
							break;
						case "object":
							outVar = dp.getValueByPointer(outputArrayParent);
							break;

						default:
							break;
					}

				}
			}

			List<Object> list = dp.getAsList(inputArrayPath);
			if (list == null)
				return;

			String tokens[] = inputArrayPath.split("/");
			String key = tokens[tokens.length - 1];
			String inputArrayParent = (inputArrayPath + "_#").replace(key + "_#", "");
			Map<String, Object> map = null;
			if (inputArrayParent.trim().length() > 0)
				map = (Map<String, Object>) dp.getAsMap(inputArrayParent);
//		if(map==null && dpM)
//			throw new SnippetException(dp, "Path pointer '"+inputArrayParent+"'. Please loop over parent array first", null);
			long index = 0;
			int loopExecutedCount = 0;

			for (Object object : list) {
				try {
					loopExecutedCount++;
					if (loopExecutedCount > allowedLoop) {
						throw new RuntimeException("Loop exceeded");
					}

					dp.put(indexVar, index + "");
					index++;
					if (map != null)
						map.put(key, object);
					else
						dp.put(key, object);
					JsonArray flows = loop.getJsonArray("children");
					for (JsonValue jsonValue : flows) {
						final String type = jsonValue.asJsonObject().getString("type");
						JsonObject jov=jsonValue.asJsonObject().get("data").asJsonObject();
						String status=jov.getString("status",null);
						if(!"disabled".equals(status))
							switch (type) {
								case "try-catch":
									TCFBlock tcfBlock = new TCFBlock(jsonValue.asJsonObject());
									tcfBlock.process(dp);
									break;
								case "sequence":
								case "group":
									Scope scope = new Scope(jsonValue.asJsonObject());
									scope.process(dp);
									break;
								case "switch":
									Switch swich = new Switch(jsonValue.asJsonObject());
									swich.process(dp);
									break;
								case "ifelse":
									IfElse ifElse = new IfElse(jsonValue.asJsonObject());
									ifElse.process(dp);
									break;
								case "loop":
								case "foreach":
									Loop loop = new Loop(jsonValue.asJsonObject());
									loop.process(dp);
									break;
								case "repeat":
								case "redo":
									Repeat repeat = new Repeat(jsonValue.asJsonObject());
									repeat.process(dp);
									break;
								case "invoke":
								case "service":
									Api api = new Api(jsonValue.asJsonObject());
									api.process(dp);
									break;
								case "map":
								case "transformer":
									Transformer transformer = new Transformer(jsonValue.asJsonObject());
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
								case "object":
									ContextObject contextObject = new ContextObject(jsonValue.asJsonObject());
									contextObject.process(dp);
									break;
							}
					}

					if (outKey != null) {
						Object append = dp.get(outKey);
						if (append != null && outputList != null) {
							outputList.add(append);
							dp.put(outKey, null);
						}
					}
					if (dp.isDestroyed())
						throw new SnippetException(dp, "User aborted the service thread", new Exception("Service runtime pipeline destroyed manually"));
				} catch (Exception e) {
					Exception throwable = e;
					String msg = e.getMessage();
					if (msg.contains("packages.middleware.pub.service.continueRepeat")) {
						continue;
					}
				}
			}

//		map.put(key, list);

			if (map != null)
				map.put(key, list);
			else
				dp.put(key, list);
			if (outPutArrayPath != null && outPutArrayPath.trim().length()>0 && outputList!=null && outputList.size()>0) {
				if (outVar != null && outArrayType.equals("document"))
					((Map<String,Object>)outVar).put(outKey, outputList);
				else
					dp.put(outKey, outputList);
			}
			dp.putGlobal("*hasError", false);
		} catch (Exception e) {
			dp.putGlobal("*error", e.getMessage());
			dp.putGlobal("*hasError", true);
			throw e;
		} finally {
			if(canSnap) {
				dp.snapAfter(comment, guid, snapMeta);
				if (null != snapshot || null != snapCondition) {
					dp.drop("*snapshot");
				}
			}else if(snap!=null)
				dp.put("*snapshot",snap);
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
