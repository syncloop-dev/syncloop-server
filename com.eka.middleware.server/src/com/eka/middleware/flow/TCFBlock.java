package com.eka.middleware.flow;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.template.SnippetException;

public class TCFBlock {
	private Scope TRY;
	private Scope CATCH;
	private Scope FINALLY;
	private String label;
	private boolean disabled=false;
	private String condition;
	private JsonObject tcfBlock;
	private String comment;
	private JsonObject data=null;
	public TCFBlock(JsonObject jo) {
		tcfBlock=jo;	
		data=tcfBlock.get("data").asJsonObject();
		condition=data.getString("condition",null);
		String status=data.getString("status",null);
		disabled="disabled".equals(status);
		label=data.getString("label",null);
		comment=data.getString("comment",null);
	}
	
	public void process(DataPipeline dp) throws SnippetException {	
		if(disabled)
			return;
		JsonArray scopes=tcfBlock.getJsonArray("children");
		for (JsonValue scope : scopes) {
			String text=scope.asJsonObject().getString("text",null);
			switch(text) {
			case "TRY":
				TRY=new Scope(scope.asJsonObject());
				break;
			case "CATCH":
				CATCH=new Scope(scope.asJsonObject());
				break;
			case "FINALLY":
				FINALLY=new Scope(scope.asJsonObject());
				break;
			}
		}
		try {
			TRY.process(dp);
		} catch (Exception e) {
			dp.put("lastErrorDump", e);
			CATCH.process(dp);
		}finally {
			FINALLY.process(dp);
		}
		
	}
	
	public Scope getTRY() {
		return TRY;
	}
	public void setTRY(Scope tRY) {
		TRY = tRY;
	}
	public Scope getCATCH() {
		return CATCH;
	}
	public void setCATCH(Scope cATCH) {
		CATCH = cATCH;
	}
	public Scope getFINALLY() {
		return FINALLY;
	}
	public void setFINALLY(Scope fINALLY) {
		FINALLY = fINALLY;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
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
	
}
