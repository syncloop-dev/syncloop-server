package com.eka.middleware.flow;

import java.util.List;

import javax.json.JsonArray;
import javax.json.JsonObject;

import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.template.SnippetException;

public class Transformer {
	private boolean disabled=false;
	private String condition;
	private String label;
	private boolean evaluateCondition;
	private List<JsonOp> ops;
	private JsonObject transformer;
	private JsonObject data;
	private String comment;
	private JsonArray transformers;
	private JsonArray createList;
	private JsonArray dropList;
	public Transformer(JsonObject jo) {
		transformer=jo;		
		data=transformer.get("data").asJsonObject();
		condition=data.getString("condition",null);
		String status=data.getString("status",null);
		disabled="disabled".equals(status);
		label=data.getString("label",null);
		evaluateCondition=data.getBoolean("evaluate",false);
		comment=data.getString("comment",null);
		if(!data.isNull("transformers"))
			transformers=data.getJsonArray("transformers");
		if(!data.isNull("createList"))
			createList=data.getJsonArray("createList");
		if(!data.isNull("dropList"))
			dropList=data.getJsonArray("dropList");
	}
	
    public void process(DataPipeline dp) throws SnippetException {
    	if(disabled)
			return;
    	if(transformers!=null)
			FlowUtils.map(transformers, dp);
    	if(createList!=null)
			FlowUtils.setValue(createList, dp);
    	if(dropList!=null)
			FlowUtils.dropValue(dropList, dp);
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

	public List<JsonOp> getOps() {
		return ops;
	}

	public void setOps(List<JsonOp> ops) {
		this.ops = ops;
	}

	
}
