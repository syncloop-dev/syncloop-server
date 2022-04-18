package com.eka.middleware.flow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.eka.middleware.pub.util.Document.Function;
import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.PropertyManager;
import com.eka.middleware.service.ServiceUtils;
import com.eka.middleware.template.SnippetException;
import com.eka.middleware.template.SystemException;

public class FlowUtils {
	private static Logger LOGGER = LogManager.getLogger(FlowUtils.class);
	private static final ScriptEngineManager factory = new ScriptEngineManager();
	private static final ScriptEngine engine = factory.getEngineByName("graal.js");

	public static String placeXPathValue(String xPaths, DataPipeline dp) throws SnippetException {
		try {
			String xPathValues = xPaths;
			String params[] = extractExpressions(xPaths);// xPaths.split(Pattern.quote("}"));
			for (String param : params) {
				// if (param.contains("#{")) {
				// param = param.split(Pattern.quote("#{"))[1];// replace("#{", "");
				String val = dp.getValueByPointer(param) + "";
				String value = val.replace("\"", "");
				// System.out.println(value);
				xPathValues = xPathValues.replace("#{" + param + "}", value);// cond=evaluatedParam+"="+value;
				// }
			}
			return xPathValues;
		} catch (Exception e) {
			ServiceUtils.printException("Something went wrong while parsing xpath(" + xPaths + ")", e);
			throw new SnippetException(dp, "Something went wrong while parsing xpath(" + xPaths + ")", e);
		}
	}

	public static boolean evaluateCondition(String condition, DataPipeline dp) throws SnippetException {
		String con = null;
		try {
			con = placeXPathValue(condition, dp);
			return (boolean) engine.eval(con);
		} catch (Exception e) {
			ServiceUtils.printException("Something went wrong while parsing condition(" + condition + "), " + con, e);
			throw new SnippetException(dp, "Something went wrong while parsing condition(" + condition + "), " + con,
					e);

		}
	}

	public static void map(JsonArray transformers, DataPipeline dp) throws SnippetException {
		try {
			Map<String, List<JsonOp>> map = split(transformers);
			List<JsonOp> leaders = map.get("leaders");
			List<JsonOp> followers = map.get("followers");
			List<JsonOp> follows = new ArrayList<JsonOp>();
			boolean successful = true;
			for (JsonOp jsonValue : leaders) {
				String loop_id = jsonValue.getLoop_id();
				if (loop_id != null && loop_id.startsWith("loop_id")) {
					int index = 0;
					while (successful) {
						follows.clear();
						for (JsonOp jsonOp : followers)
							follows.add(jsonOp.clone());
						successful = transform(follows, loop_id, index + "", dp);
						index++;
					}
				} else {
					transform(jsonValue, dp);
				}
			}
		} catch (Exception e) {
			throw new SnippetException(dp, "Something went wrong while applying transformers", e);
		}
	}

	public static String[] extractExpressions(String string) {
		String expressions[] = StringUtils.substringsBetween(string, "#{", "}");
		return expressions;
	}

	public static void setValue(JsonArray createList, DataPipeline dp) throws SnippetException {
		for (JsonValue jsonValue : createList) {
			String path = jsonValue.asJsonObject().getString("path", null);
			String typePath = jsonValue.asJsonObject().getString("typePath", null);
			String value = jsonValue.asJsonObject().getString("value", null);
			String evaluate = jsonValue.asJsonObject().getString("evaluate", null);
			if (evaluate != null && evaluate.trim().length() > 0) {
				Map<String, String> map = new HashMap<String, String>();
				String expressions[] = extractExpressions(value);
				switch (evaluate) {
				case "ELV": // Evaluate Local Variable
					for (String expressionKey : expressions) {
						String expressionValue = dp.getMyConfig(expressionKey);
						map.put(expressionKey, expressionValue);
					}
					break;
				case "EGV": // Evaluate Global Variable
					for (String expressionKey : expressions) {
						String expressionValue = dp.getGlobalConfig(expressionKey);
						map.put(expressionKey, expressionValue);
					}
					break;
				case "EEV": // Evaluate Expression Variable
					for (String expressionKey : expressions) {
						String expressionValue = dp.getAsString(expressionKey);
						map.put(expressionKey, expressionValue);
					}
					break;
				case "EPV": // Evaluate Package Variable
					for (String expressionKey : expressions) {
						String expressionValue = dp.getMyPackageConfig(expressionKey);
						map.put(expressionKey, expressionValue);
					}
					break;
				}
				for (String expressionKey : expressions) {
					value = value.replace("#{" + expressionKey + "}", map.get(expressionKey));
				}
			}
			dp.setValueByPointer(path, value, typePath);
		}
	}

	public static void dropValue(JsonArray dropList, DataPipeline dp) {
		for (JsonValue jsonValue : dropList) {
			String path = jsonValue.asJsonObject().getString("path", null);
			path = ("//" + path + "//").replace("///", "").replace("//", "");
			dp.drop(path);
			// String typePath=jsonValue.asJsonObject().getString("typePath",null);
			String tokens[] = path.split("/");
			String key = tokens[tokens.length - 1];
			String parentPath = (path + "_#").replace(key + "_#", "");
			Map<String, Object> map = (Map<String, Object>) dp.getAsMap(parentPath);
			if (map != null)
				map.remove(key);
		}
	}

	private static boolean transform(List<JsonOp> followers, String loop_id, String index, DataPipeline dp)
			throws Exception {
		boolean successful = true;
		try {
			List<JsonOp> follows = getFollowersById(followers, loop_id, index);
			for (JsonOp jsonOp : follows) {
				String f_loop_id = jsonOp.getLoop_id();
				if (f_loop_id != null && f_loop_id.startsWith("loop_id")) {
					int f_index = 0;
					while (successful) {
						successful = transform(followers, loop_id, f_index + "", dp);
						f_index++;
					}
				} else
					successful = transform(jsonOp, dp);
			}

		} catch (Exception e) {
			throw new Exception("");
		}
		return successful;
	}

	private static boolean transform(JsonOp leader, DataPipeline dp) throws Exception {
		boolean successful = true;
		String op = leader.getOp();
		try {
			boolean canCopy = true;
			if (leader.getCondition() != null && leader.getCondition().trim().length() > 0)
				canCopy = evaluateCondition(leader.getCondition(), dp);
			if (canCopy) {
				String expressions[] = null;
				String function = leader.getJsFunction();
				if (function != null && function.trim().length() > 0) {
					String jsClass = "cb_" + (dp.getCurrentResource().hashCode() & 0xfffffff);
					boolean isCBAvailable = false;
					try {
						isCBAvailable = (boolean) engine.eval("(" + jsClass + "!=null);");
					} catch (Exception e) {
						isCBAvailable = false;
					}
					if (!isCBAvailable) {
						String varObj = "var " + jsClass + "={}";
						engine.eval(varObj);
					}
					String functionName = jsClass + ".jsFunc_" + leader.getId() + "_applyLogic";
					boolean isFunctionAvailable = false;
					try {
						isFunctionAvailable = (boolean) engine.eval("(" + functionName + "!=null);");
					} catch (Exception e) {
						isFunctionAvailable = false;
					}

					if (!isFunctionAvailable) {
						expressions = extractExpressions(leader.getJsFunction());
						if (expressions != null)
							for (String expressionKey : expressions)
								function = function.replace("#{" + expressionKey + "}", dp.getAsString(expressionKey));
						function = functionName + "=function(val){" + function + "};";
						engine.eval(function);
					}
					Object val = dp.getValueByPointer(leader.getFrom());
					// System.out.println(val.getClass());
					if (val.getClass().toString().contains("String"))
						val = engine.eval(functionName + "('" + val + "');");
					else
						val = engine.eval(functionName + "(" + val + ");");

					if (val != null)
						dp.setValueByPointer(leader.getTo(), val, leader.getOutTypePath());
				} else
					successful = copy(leader.getFrom(), leader.getTo(), leader.getOutTypePath(), dp);
			}
		} catch (Exception e) {
			throw new Exception(
					"Failed to perform " + op + " from '" + leader.getFrom() + "' to '" + leader.getTo() + "'");
		}
		return successful;
	}

	public static void resetJSCB(String resource) throws SystemException {
		try {
			String jsClass = "cb_" + (resource.hashCode() & 0xfffffff);
			String varObj = "var " + jsClass + "={}";
			engine.eval(varObj);
		} catch (Exception e) {
			throw new SystemException("EKA_MWS_1003", e);
		}
	}

	private static Map<String, List<JsonOp>> split(JsonArray transformers) throws Exception {
		String follow = "";
		List<JsonOp> leaders = new ArrayList<>();
		List<JsonOp> followers = new ArrayList<>();
		for (JsonValue jsonValue : transformers) {
			follow = jsonValue.asJsonObject().getString("follow", null);
			if (follow != null && follow.startsWith("loop_id"))
				followers.add(new JsonOp(jsonValue));
			else
				leaders.add(new JsonOp(jsonValue));
		}
		Map<String, List<JsonOp>> map = new HashMap<>();
		map.put("leaders", leaders);
		map.put("followers", followers);
		return map;
	}

	private static List<JsonOp> getFollowersById(List<JsonOp> followers, String loop_id, String index) {
		List<JsonOp> followersById = new ArrayList<JsonOp>();
		for (JsonOp jsonValue : followers) {
			String follow = jsonValue.getFollow();
			jsonValue.applyIndex(index, loop_id);
			if (loop_id.equals(follow))
				followersById.add(jsonValue);
		}
		return followersById;
	}

	private static boolean copyOp(JsonValue copyOp, DataPipeline dp) {
		String from = copyOp.asJsonObject().getString("from");
		String to = copyOp.asJsonObject().getString("to");
		String toTypePath = copyOp.asJsonObject().getString("outTypePath");
		return copy(from, to, toTypePath, dp);
	}

	private static boolean copy(String from, String to, String toTypePath, DataPipeline dp) {
		Object val = dp.getValueByPointer(from);
		if (val == null)
			return false;
		dp.setValueByPointer(to, val, toTypePath);
		return true;
	}

	public static void validateDocuments(DataPipeline dp, JsonValue jv) throws SnippetException {
		JsonArray jva = jv.asJsonArray();
		if (jva.isEmpty())
			return;

		/*Exception e=new Exception("Service document validation is enabled but Input/Output document is null or empty");
		ServiceUtils.printException("Service document validation is enabled but Input/Output document is null or empty", e);
		throw new SnippetException(dp, "Service document validation is enabled but Input/Output document is null or empty", e);*/

		
		final Map<String, String> mapPointers = new HashMap<>();
		final Map<String, JsonObject> mapPointerData = new HashMap<>();
		if (jva != null) {
			// outPutData=
			for (JsonValue jsonValue : jva) {
				getKeyTypePair(jsonValue, null,null, mapPointers, mapPointerData);

			}
		}
		dp.log("Document pointers:-",Level.TRACE);
		mapPointers.forEach((k, v) -> dp.log(k + " : " + v,Level.TRACE));
		dp.log("Document data:-",Level.TRACE);
		mapPointerData.forEach((k, v) -> dp.log(k + " : " + v.toString(),Level.TRACE));
		
		Set<String> keys=mapPointers.keySet();
		for (String key : keys) {
			String typePath=mapPointers.get(key);
			JsonObject data=mapPointerData.get(key);
			if(data!=null && !data.isEmpty()) {
				Map<String,Object> m=ServiceUtils.jsonToMap(data.toString());
				final Map<String,String> dataMap=new HashMap<String,String>();
				m.forEach((k,v)->{
					if(v!=null && (v+"").trim().length()>0)
						dataMap.put(k, v+"");
				});
				Function.validate(dp, key, typePath, dataMap);
			}
		}	
	}

	private static void getKeyTypePair(JsonValue jsonValue, String key,String typePath, Map<String, String> mapPointers,
			Map<String, JsonObject> mapPointerData) {
		if (key == null)
			key = jsonValue.asJsonObject().getString("text");
		else
			key += "/" + jsonValue.asJsonObject().getString("text");
		if(typePath==null)
			typePath = jsonValue.asJsonObject().getString("type");
		else
			typePath += "/"+jsonValue.asJsonObject().getString("type");
		
		JsonObject data = jsonValue.asJsonObject().getJsonObject("data");
		mapPointers.put(key, typePath);
		mapPointerData.put(key, data);
		JsonValue jv = jsonValue.asJsonObject().get("children");
		if (jv != null) {
			JsonArray jva = jv.asJsonArray();
			for (JsonValue jsonVal : jva) {
				getKeyTypePair(jsonVal, key,typePath, mapPointers, mapPointerData);
			}
		}
	}

}