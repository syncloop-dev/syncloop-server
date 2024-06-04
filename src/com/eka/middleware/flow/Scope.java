package com.eka.middleware.flow;

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

public class Scope implements FlowBasicInfo {
	private JsonObject data=null;
	private String snapshot=null;
	private String snapCondition=null;

	@Getter
	private String name;

	@Getter
	private String type;

	@Getter
	private String guid;

	public Scope(JsonObject jo) {
		scope=jo;		
		data=scope.get("data").asJsonObject();
		condition=data.getString("condition",null);
		String status=data.getString("status",null);
		disabled="disabled".equals(status);
		label=data.getString("label",null);
		evaluateCondition=data.getBoolean("evaluate",false);
		comment=data.getString("comment",null);
		snapshot=data.getString("snap",null);
		if(snapshot!=null && snapshot.equals("disabled"))
			snapshot=null;
		snapCondition=data.getString("snapCondition",null);

		guid = data.getString("guid",null);
		name = scope.getString("text",null);
		type = scope.getString("type",null);
	}
	
	public void process(DataPipeline dp) throws SnippetException{
		Map<String, Object> snapMeta = Maps.newHashMap();
		snapMeta.put("GROUP_SEQUENCE_CONDITION_EVAL", evaluateCondition);
		if(dp.isDestroyed()) {
			throw new SnippetException(dp, "User aborted the service thread", new Exception("Service runtime pipeline destroyed manually"));
		}
		if(disabled) {
			return;
		}
		dp.addErrorStack(this);
		String snap=dp.getString("*snapshot");
		boolean canSnap = false;
		if(snap!=null || snapshot!=null) {
			canSnap = true;
			//snap=snapshot;
			if(snapshot!=null && snapshot.equals("conditional") && snapCondition!=null) {
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
			JsonArray flows= scope.getJsonArray("children");
			for (JsonValue jsonValue : flows) {
				String type=jsonValue.asJsonObject().getString("type",null);
				JsonObject jov=jsonValue.asJsonObject().get("data").asJsonObject();
				String status=jov.getString("status",null);
				if(!"disabled".equals(status))
					switch(type) {
						case "try-catch":
							TCFBlock tcfBlock=new TCFBlock(jsonValue.asJsonObject());
							if(!evaluateCondition) {
								tcfBlock.process(dp);
							}else {
								boolean canExecute =FlowUtils.evaluateCondition(tcfBlock.getCondition(),dp);
								snapMeta.put("GROUP_SEQUENCE_CAN_CONDITION_EXEC", canExecute);
								if(canExecute)
									tcfBlock.process(dp);
							}
							break;
						case "sequence":
						case "group":
							Scope scope=new Scope(jsonValue.asJsonObject());
							if(!evaluateCondition) {
								scope.process(dp);
							}else {
								boolean canExecute =FlowUtils.evaluateCondition(scope.getCondition(),dp);
								snapMeta.put("canExecute", canExecute);
								if(canExecute)
									scope.process(dp);
							}
							break;
						case "switch":
							Switch swich=new Switch(jsonValue.asJsonObject());
							if(!evaluateCondition) {
								swich.process(dp);
							}else {
								boolean canExecute =FlowUtils.evaluateCondition(swich.getCondition(),dp);
								snapMeta.put("canExecute", canExecute);
								if(canExecute)
									swich.process(dp);
							}
							break;
						case "ifelse":
							IfElse ifElse = new IfElse(jsonValue.asJsonObject());
							if(!evaluateCondition) {
								ifElse.process(dp);
							}else {
								boolean canExecute =FlowUtils.evaluateCondition(ifElse.getCondition(),dp);
								snapMeta.put("canExecute", canExecute);
								if(canExecute)
									ifElse.process(dp);
							}
							break;
						case "loop":
						case "foreach":
							Loop loop=new Loop(jsonValue.asJsonObject());
							if(!evaluateCondition) {
								loop.process(dp);
							}else {
								boolean canExecute =FlowUtils.evaluateCondition(loop.getCondition(),dp);
								snapMeta.put("canExecute", canExecute);
								if(canExecute)
									loop.process(dp);
							}
							break;
						case "repeat":
						case "redo":
							Repeat repeat=new Repeat(jsonValue.asJsonObject());
							if(!evaluateCondition) {
								repeat.process(dp);
							}else {
								boolean canExecute =FlowUtils.evaluateCondition(repeat.getCondition(),dp);
								snapMeta.put("canExecute", canExecute);
								if(canExecute)
									repeat.process(dp);
							}
							break;
						case "invoke":
						case "service":
							Api api=new Api(jsonValue.asJsonObject());
							if(!evaluateCondition) {
								api.process(dp);
							}else {
								boolean canExecute =FlowUtils.evaluateCondition(api.getCondition(),dp);
								snapMeta.put("canExecute", canExecute);
								if(canExecute)
									api.process(dp);
							}
							break;
						case "map":
						case "transformer":
							Transformer transformer=new Transformer(jsonValue.asJsonObject());
							if(!evaluateCondition) {
								transformer.process(dp);
							}else {
								boolean canExecute =FlowUtils.evaluateCondition(transformer.getCondition(),dp);
								snapMeta.put("canExecute", canExecute);
								if(canExecute)
									transformer.process(dp);
							}
							break;
						case "await":
							Await await=new Await(jsonValue.asJsonObject());
							if(!evaluateCondition) {
								await.process(dp);
							}else {
								boolean canExecute =FlowUtils.evaluateCondition(await.getCondition(),dp);
								snapMeta.put("canExecute", canExecute);
								if(canExecute)
									await.process(dp);
							}
							break;
						case "function":
							Function function = new Function(jsonValue.asJsonObject());
							if(!evaluateCondition) {
								function.process(dp);
							}else {
								boolean canExecute =FlowUtils.evaluateCondition(function.getCondition(),dp);
								snapMeta.put("canExecute", canExecute);
								if(canExecute)
									function.process(dp);
							}
							break;

						case "object":
							ContextObject contextObject = new ContextObject(jsonValue.asJsonObject());
							if(!evaluateCondition) {
								contextObject.process(dp);
							}else {
								boolean canExecute =FlowUtils.evaluateCondition(contextObject.getCondition(),dp);
								snapMeta.put("canExecute", canExecute);
								if(canExecute)
									contextObject.process(dp);
							}
							break;
					}
			}
			dp.putGlobal("*hasError", false);
		} catch (Exception e) {
			dp.putGlobal("*error", e.getMessage());
			dp.putGlobal("*hasError", false);
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
	
	public List<Scope> getScopes() {
		return scopes;
	}
	public void setScopes(List<Scope> scopes) {
		this.scopes = scopes;
	}
	public List<TCFBlock> getTcfBlocks() {
		return tcfBlocks;
	}
	public void setTcfBlocks(List<TCFBlock> tcfBlocks) {
		this.tcfBlocks = tcfBlocks;
	}
	public List<Api> getInvokes() {
		return invokes;
	}
	public void setInvokes(List<Api> invokes) {
		this.invokes = invokes;
	}
	public List<Repeat> getRepeats() {
		return repeats;
	}
	public void setRepeats(List<Repeat> repeats) {
		this.repeats = repeats;
	}
	public List<Loop> getLoops() {
		return loops;
	}
	public void setLoops(List<Loop> loops) {
		this.loops = loops;
	}
	public List<Transformer> getTransformers() {
		return transformers;
	}
	public void setTransformers(List<Transformer> transformers) {
		this.transformers = transformers;
	}
	public boolean isDisabled() {
		return disabled;
	}
	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
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
	public boolean isEvaluateCondition() {
		return evaluateCondition;
	}
	public void setEvaluateCondition(boolean evaluateCondition) {
		this.evaluateCondition = evaluateCondition;
	}
	private List<Scope> scopes;
	private List<TCFBlock> tcfBlocks;
	private List<Api> invokes;
	private List<Repeat> repeats;
	private List<Loop> loops;
	private List<Transformer> transformers;	
	private boolean disabled=false;
	private String condition;
	private String label;
	private boolean evaluateCondition;
	private JsonObject scope;
	private String comment;
}
