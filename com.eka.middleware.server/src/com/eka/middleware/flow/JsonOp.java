package com.eka.middleware.flow;

import javax.json.JsonObject;
import javax.json.JsonValue;

public class JsonOp {
	private String op;
	private String from;
	private String to;
	private String condition;
	private String jsFunction;
	private String loop_id;
	private String follow;
	private String outTypePath;
	private String inTypePath;
	private JsonObject jsonop;
	private String applyFunction;
	private String jsFunctionSig;
	private int id;

	public JsonOp(JsonValue jsonValue) {
		jsonop=jsonValue.asJsonObject();
		from = jsonop.getString("from",null);
		to = jsonop.getString("to",null);
		op = jsonValue.asJsonObject().getString("op",null);
		condition = jsonop.getString("condition",null);
		jsFunction = jsonop.getString("jsFunction",null);
		loop_id = jsonop.getString("loop_Id",null);
		follow = jsonop.getString("follow",null);
        setApplyFunction(jsonop.getString("applyFunction",null));
      	setJsFunctionSig(jsonop.getString("jsFunctionSig",null));
		if(!jsonop.isNull("outTypePath"))
			outTypePath = jsonop.getString("outTypePath",null);
		id=((from+" to "+to).hashCode() & 0xfffffff);
	}
	
	private JsonOp(JsonOp jsonValue) {
		jsonop=jsonValue.getJsonop();
		loop_id = jsonValue.getLoop_id();
		from = jsonValue.getFrom();
		to = jsonValue.getTo();
		op = jsonValue.getOp();
		condition = jsonValue.getCondition();
		jsFunction = jsonValue.getJsFunction();
		follow = jsonValue.getFollow();
		outTypePath=jsonValue.getOutTypePath();
		setApplyFunction(jsonValue.getApplyFunction());
      	setJsFunctionSig(jsonValue.getJsFunctionSig());
      	id=jsonValue.getId();
	}
	
	public JsonOp clone() {
		return new JsonOp(this);
	}
	
	public String getOutTypePath() {
		return outTypePath;
	}
	
	public void setOutTypePath(String outTypePath) {
		this.outTypePath = outTypePath;
	}
	
	public JsonObject getJsonop() {
		return jsonop;
	}
	
	public void setJsonop(JsonObject jsonop) {
		this.jsonop = jsonop;
	}
	
	public void applyIndex(String index, String loopId) {
		if(from!=null) 
			from=from.replace("#{"+loopId+"}", index);
		if(to!=null) 
			to=to.replace("#{"+loopId+"}", index);
	}
	
	public String getLoop_id() {
		return loop_id;
	}

	public void setLoop_id(String loop_id) {
		this.loop_id = loop_id;
	}

	public String getFollow() {
		return follow;
	}

	public void setFollow(String follow) {
		this.follow = follow;
	}

	public String getOp() {
		return op;
	}
	public void setOp(String op) {
		this.op = op;
	}
	public String getFrom() {
		return from;
	}
	public void setFrom(String from) {
		this.from = from;
	}
	public String getTo() {
		return to;
	}
	public void setTo(String to) {
		this.to = to;
	}
	public String getCondition() {
		return condition;
	}
	public void setCondition(String condition) {
		this.condition = condition;
	}
	public String getJsFunction() {
		return jsFunction;
	}
	public void setJsFunction(String jsFunction) {
		this.jsFunction = jsFunction;
	}
	public String getInTypePath() {
		return inTypePath;
	}
	public void setInTypePath(String inTypePath) {
		this.inTypePath = inTypePath;
	}
	public String getApplyFunction() {
		return applyFunction;
	}
	public void setApplyFunction(String applyFunction) {
		this.applyFunction = applyFunction;
	}
	public String getJsFunctionSig() {
		return jsFunctionSig;
	}
	public void setJsFunctionSig(String jsFunctionSig) {
		this.jsFunctionSig = jsFunctionSig;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
}
