package com.eka.middleware.flow;

import java.util.List;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.template.SnippetException;

public class Switch {
	private List<Scope> cases;
	private boolean disabled=false;
	private String condition;
	private String caseLabel;
	private String switchVariable;
	private JsonObject swich;
	private String switchXpath;
	public Switch(JsonObject jo) {
		swich=jo;		
		condition=swich.get("data").asJsonObject().getString("condition",null);
		String status=swich.get("data").asJsonObject().getString("status",null);
		disabled="disabled".equals(status);
		caseLabel=swich.get("data").asJsonObject().getString("label",null);
		switchXpath=swich.get("data").asJsonObject().getString("switch",null);
	}
	
	public void process(DataPipeline dp) throws SnippetException {
		if(disabled)
			return;
		String text=swich.get("data").asJsonObject().getString("text",null);
		JsonArray flows= swich.getJsonArray("children");
		String xPathValue=dp.getAsString(switchXpath);// FlowUtils.placeXPathValue(switchXpath, dp);
		JsonObject defaultCase=null;
		JsonObject nullCase=null;
		for (JsonValue jsonValue : flows) {
			String caseLabel=jsonValue.asJsonObject().get("data").asJsonObject().getString("case",null);
			if(caseLabel == null)
				throw new SnippetException(dp,"Case label is a required field. It can not be left empty. Use #null for null comparision use !null(empty is not null) or !empty(null is also considered empty)." , null);
			if("#null".equals(caseLabel) && xPathValue==null) {
				Scope scope=new Scope(jsonValue.asJsonObject());
				scope.process(dp);
				return;
			}else if(xPathValue.equals(caseLabel)) {
				Scope scope=new Scope(jsonValue.asJsonObject());
				scope.process(dp);
				return;
			}else if("#default".equals(caseLabel))
				defaultCase=jsonValue.asJsonObject();
		}
		if(defaultCase!=null) {
			Scope scope=new Scope(defaultCase.asJsonObject());
			scope.process(dp);
		}
	}
	
	
	public List<Scope> getCases() {
		return cases;
	}
	public void setCases(List<Scope> cases) {
		this.cases = cases;
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
		return caseLabel;
	}
	public void setLabel(String label) {
		this.caseLabel = label;
	}
	public String getSwitchVariable() {
		return switchVariable;
	}
	public void setSwitchVariable(String switchVariable) {
		this.switchVariable = switchVariable;
	}
}
