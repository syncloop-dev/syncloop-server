package com.eka.middleware.flow;

import java.util.List;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.apache.commons.lang3.math.NumberUtils;

import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.template.SnippetException;

public class Repeat {
	private List<Scope> scopes;
	private List<TCFBlock> tcfBlocks;
	private List<Invoke> invokes;
	private List<Repeat> repeats;
	private List<Loop> loops;
	private List<Transformer> transformers;
	private boolean disabled = false;
	private int interval;
	private boolean continueOnError;
	private int repeatTimes;
	private String condition;
	private JsonObject repeat;
	private JsonObject data = null;
	private boolean evaluateCondition;
	private String comment;
	private String repeatOn;

	public Repeat(JsonObject jo) {
		repeat = jo;
		data = repeat.get("data").asJsonObject();
		condition = data.getString("condition", null);
		String status = data.getString("status", null);
		disabled = "disabled".equals(status);
		String rt = data.getString("repeat", null);
		if(rt.startsWith("#{")) {
			rt=FlowUtils.extractExpressions(rt)[0];
		}
		boolean isNumber = NumberUtils.isParsable(rt);
		if (isNumber)
			repeatTimes = Integer.parseInt(rt);
		evaluateCondition = data.getBoolean("evaluate", false);
		comment = data.getString("comment", null);

		interval = data.getInt("interval", 0);
		repeatOn = data.getString("repeatOn", "error");
	}

	public void process(DataPipeline dp) throws SnippetException {
		if (disabled)
			return;
		while (repeatOn != null) {
			try {
				action(dp);
				if ("error".equals(repeatOn))
					repeatOn = null;
			} catch (SnippetException e) {
				if ("success".equals(repeatOn))
					repeatOn = null;
				dp.put("lastErrorDump", e);
			}
			repeatTimes--;
			if (repeatTimes == 0)
				repeatOn = null;
			
			try {
				Thread.sleep(interval);
			} catch (Exception e) {
				dp.put("lastErrorDump", e);
			}

		}
	}

	public void action(DataPipeline dp) throws SnippetException {
		JsonArray flows = repeat.getJsonArray("children");
		for (JsonValue jsonValue : flows) {
			String type = jsonValue.asJsonObject().getString("type", null);
			//System.out.println(type);
			switch (type) {
			case "try-catch":
				TCFBlock tcfBlock = new TCFBlock(jsonValue.asJsonObject());
				if (!evaluateCondition) {
					tcfBlock.process(dp);
				} else {
					boolean canExecute = FlowUtils.evaluateCondition(tcfBlock.getCondition(), dp);
					if (canExecute)
						tcfBlock.process(dp);
				}
				break;
			case "sequence":
				Scope scope = new Scope(jsonValue.asJsonObject());
				if (!evaluateCondition) {
					scope.process(dp);
				} else {
					boolean canExecute = FlowUtils.evaluateCondition(scope.getCondition(), dp);
					if (canExecute)
						scope.process(dp);
				}
				break;
			case "switch":
				Switch swich = new Switch(jsonValue.asJsonObject());
				if (!evaluateCondition) {
					swich.process(dp);
				} else {
					boolean canExecute = FlowUtils.evaluateCondition(swich.getCondition(), dp);
					if (canExecute)
						swich.process(dp);
				}
				break;
			case "loop":
				Loop loop = new Loop(jsonValue.asJsonObject());
				if (!evaluateCondition) {
					loop.process(dp);
				} else {
					boolean canExecute = FlowUtils.evaluateCondition(loop.getCondition(), dp);
					if (canExecute)
						loop.process(dp);
				}
				break;
			case "repeat":
				Repeat repeat = new Repeat(jsonValue.asJsonObject());
				if (!evaluateCondition) {
					repeat.process(dp);
				} else {
					boolean canExecute = FlowUtils.evaluateCondition(repeat.getCondition(), dp);
					if (canExecute)
						repeat.process(dp);
				}
				break;
			case "invoke":
				Invoke invoke = new Invoke(jsonValue.asJsonObject());
				if (!evaluateCondition) {
					invoke.process(dp);
				} else {
					boolean canExecute = FlowUtils.evaluateCondition(invoke.getCondition(), dp);
					if (canExecute)
						invoke.process(dp);
				}
				break;
			case "map":
				Transformer transformer = new Transformer(jsonValue.asJsonObject());
				if (!evaluateCondition) {
					transformer.process(dp);
				} else {
					boolean canExecute = FlowUtils.evaluateCondition(transformer.getCondition(), dp);
					if (canExecute)
						transformer.process(dp);
				}
				break;
			}
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

	public List<Invoke> getInvokes() {
		return invokes;
	}

	public void setInvokes(List<Invoke> invokes) {
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

	public int getInterval() {
		return interval;
	}

	public void setInterval(int interval) {
		this.interval = interval;
	}

	public boolean isContinueOnError() {
		return continueOnError;
	}

	public void setContinueOnError(boolean continueOnError) {
		this.continueOnError = continueOnError;
	}

	public int getRepeatTimes() {
		return repeatTimes;
	}

	public void setRepeatTimes(int repeatTimes) {
		this.repeatTimes = repeatTimes;
	}

	public String getCondition() {
		return condition;
	}

	public void setCondition(String condition) {
		this.condition = condition;
	}
}
