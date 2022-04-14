package com.eka.middleware.flow;

import java.util.List;

import javax.json.JsonArray;
import javax.json.JsonObject;

import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.template.SnippetException;

public class Invoke {
	private boolean disabled=false;
	private boolean sync=true;
	private String fqn;
	private Transformer transformer;
	private String condition;
	private String label;
	private JsonObject invoke;
	private boolean evaluateCondition;
	private String comment;
	private JsonArray transformers;
	private JsonObject data=null;
	private JsonArray createList;
	private JsonArray dropList;
	
	public Invoke(JsonObject jo) {
		invoke=jo;	
		data=invoke.get("data").asJsonObject();
		condition=data.getString("condition",null);
		String status=data.getString("status",null);
		disabled="disabled".equals(status);
		label=data.getString("label",null);
		evaluateCondition=data.getBoolean("evaluate",false);
		comment=data.getString("comment",null);
//		System.out.println(data.isNull("transformers"));
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
    	if(createList!=null)
			FlowUtils.setValue(createList, dp);
    	
		if(transformers!=null)
			FlowUtils.map(transformers, dp);
		String text=invoke.getString("text",null);
		if(text!=null && text.trim().length()>8) {
			dp.apply(text.trim()+".main");
			if(transformers!=null)
				FlowUtils.map(transformers, dp);
		}
		if(dropList!=null)
			FlowUtils.dropValue(dropList, dp);// setValue(dropList, dp);
	}
	
	public boolean isDisabled() {
		return disabled;
	}
	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}
	public boolean isSync() {
		return sync;
	}
	public void setSync(boolean sync) {
		this.sync = sync;
	}
	public String getFqn() {
		return fqn;
	}
	public void setFqn(String fqn) {
		this.fqn = fqn;
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
	public Transformer getTransformer() {
		return transformer;
	}
	public void setTransformer(Transformer transformer) {
		this.transformer = transformer;
	}
}
