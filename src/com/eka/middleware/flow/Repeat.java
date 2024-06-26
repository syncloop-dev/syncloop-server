package com.eka.middleware.flow;

import java.util.List;
import java.util.Map;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import com.eka.middleware.service.FlowBasicInfo;
import com.google.common.collect.Maps;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.ServiceUtils;
import com.eka.middleware.template.SnippetException;

public class Repeat implements FlowBasicInfo {
    private List<Scope> scopes;
    private List<TCFBlock> tcfBlocks;
    private List<Api> invokes;
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
    private String indexVar = "*index";
    private String snapshot = null;
    private String snapCondition = null;

    @Getter
    private String name;

    @Getter
    private String type;

    @Getter
    private String guid;

    private int allowedLoop = 1000;

    public Repeat(JsonObject jo) {
        repeat = jo;
        data = repeat.get("data").asJsonObject();
        condition = data.getString("condition", null);
        String status = data.getString("status", null);
        disabled = "disabled".equals(status);
        String rt = data.getString("redo", null);
        if (StringUtils.isBlank(rt)) {
            rt = data.getString("repeat", "0");
        }
        if (rt.startsWith("#{")) {
            rt = FlowUtils.extractExpressions(rt, null)[0];
        }
        boolean isNumber = NumberUtils.isParsable(rt);
        if (isNumber)
            repeatTimes = Integer.parseInt(rt);
        evaluateCondition = data.getBoolean("evaluate", false);
        comment = data.getString("comment", null);

        interval = data.getInt("interval", 0);
        String sleep = data.getString("interval", null);
        if (interval == 0 && sleep != null)
            interval = Integer.parseInt(sleep);
        repeatOn = data.getString("repeatOn", "success");
        indexVar = data.getString("indexVar", "*index");
        snapshot = data.getString("snap", null);
        if (snapshot != null && snapshot.equals("disabled"))
            snapshot = null;
        snapCondition = data.getString("snapCondition", null);

        guid = data.getString("guid",null);
        name = repeat.getString("text",null);
        type = repeat.getString("type",null);
    }

    public void process(DataPipeline dp) throws SnippetException {
        Map<String, Object> snapMeta = Maps.newHashMap();
        snapMeta.put("*indexVar", indexVar);

        if (dp.isDestroyed()) {
            throw new SnippetException(dp, "User aborted the service thread", new Exception("Service runtime pipeline destroyed manually"));
        }
        if (disabled) {
            return;
        }
        dp.addErrorStack(this);
        String snap = dp.getString("*snapshot");
        boolean canSnap = false;
        if (snap != null || snapshot != null) {
            canSnap = true;
            //snap=snapshot;
            if (snapshot != null && snapshot.equals("conditional") && snapCondition != null) {
                canSnap = FlowUtils.evaluateCondition(snapCondition, dp);
                if (canSnap)
                    dp.put("*snapshot", "enabled");
            } else
                dp.put("*snapshot", "enabled");
        }
        /*if (!canSnap)
            dp.drop("*snapshot");*/
        if (canSnap ) {
            dp.snapBefore(comment, guid);
        }
        try {
            long index = 0;
            boolean canExecute = true;
            if (repeatTimes < 0 && getCondition() != null) {
                canExecute = FlowUtils.evaluateCondition(getCondition(), dp);
                snapMeta.put("condition", getCondition());
                snapMeta.put("conditionEvaluation", canExecute);
            } else if (repeatTimes == 0) {
                canExecute = false;
            }
            snapMeta.put("repeatTimes", repeatTimes);
            snapMeta.put("repeatOn", repeatOn);
            snapMeta.put("interval", interval);
            int loopExecutedCount = 0;
            while (repeatOn != null && canExecute) {
                loopExecutedCount++;
                if (loopExecutedCount > allowedLoop) {
                    throw new RuntimeException("Loop exceeded");
                }

                dp.put(indexVar, index + "");
                index++;
                Exception throwable = null;
                try {
                    action(dp);
                    if ("error".equals(repeatOn))
                        repeatOn = null;
                } catch (Exception e) {
                    throwable = e;
                    String msg = e.getMessage();
                    if (msg.contains("packages.middleware.pub.service.exitRepeat"))
                        break;
                    if (msg.contains("packages.middleware.pub.service.continueRepeat")) {
                        repeatTimes--;
                        if (repeatTimes == 0)
                            repeatOn = null;
                        continue;
                    }
                    if ("success".equals(repeatOn)) {
                        repeatOn = null;
                        SnippetException se = null;
                        if (e instanceof SnippetException)
                            se = (SnippetException) e;
                        else
                            se = new SnippetException(dp, "Exception on step repeat(" + comment + ")", new Exception(e));
                        dp.putGlobal("lastErrorDump", ServiceUtils.getExceptionMap(se));
                        if (se.propagate)
                            throw se;
                    } else
                        dp.putGlobal("lastErrorDump", ServiceUtils.getExceptionMap(new Exception(e)));
                }
                repeatTimes--;
                if (repeatTimes == 0)
                    repeatOn = null;

                if (repeatTimes == 0 && null != throwable) {
                    throw new SnippetException(dp, throwable.getMessage(), throwable);
                }

                try {
                    Thread.sleep(interval);
                } catch (Exception e) {
                    dp.putGlobal("lastErrorDump", ServiceUtils.getExceptionMap(e));
                }

                if (repeatTimes < 0 && repeatOn != null)
                    canExecute = FlowUtils.evaluateCondition(getCondition(), dp);

                if (dp.isDestroyed())
                    throw new SnippetException(dp, "User aborted the service thread", new Exception("Service runtime pipeline destroyed manually"));
            }
            dp.putGlobal("*hasError", false);
        } catch (Exception e) {
            dp.putGlobal("*error", e.getMessage());
            dp.putGlobal("*hasError", true);
            throw e;
        } finally {
            if (canSnap) {
                dp.snapAfter(comment, guid, snapMeta);
                if (null != snapshot || null != snapCondition) {
                    dp.drop("*snapshot");
                }
            } else if (snap != null)
                dp.put("*snapshot", snap);
        }
    }

    public void action(DataPipeline dp) throws SnippetException {
        JsonArray flows = repeat.getJsonArray("children");
        for (JsonValue jsonValue : flows) {
            String type = jsonValue.asJsonObject().getString("type", null);
            JsonObject jov=jsonValue.asJsonObject().get("data").asJsonObject();
			String status=jov.getString("status",null);
			if(!"disabled".equals(status))
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
                case "group":
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
                case "ifelse":
                    IfElse ifElse = new IfElse(jsonValue.asJsonObject());
                    if (!evaluateCondition) {
                        ifElse.process(dp);
                    } else {
                        boolean canExecute = FlowUtils.evaluateCondition(ifElse.getCondition(), dp);
                        if (canExecute)
                            ifElse.process(dp);
                    }
                    break;
                case "loop":
                case "foreach":
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
                case "redo":
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
                case "service":
                    Api invoke = new Api(jsonValue.asJsonObject());
                    if (!evaluateCondition) {
                        invoke.process(dp);
                    } else {
                        boolean canExecute = FlowUtils.evaluateCondition(invoke.getCondition(), dp);
                        if (canExecute)
                            invoke.process(dp);
                    }
                    break;
                case "map":
                case "transformer":
                    Transformer transformer = new Transformer(jsonValue.asJsonObject());
                    if (!evaluateCondition) {
                        transformer.process(dp);
                    } else {
                        boolean canExecute = FlowUtils.evaluateCondition(transformer.getCondition(), dp);
                        if (canExecute)
                            transformer.process(dp);
                    }
                    break;
                case "await":
					Await await=new Await(jsonValue.asJsonObject());
					if(!evaluateCondition) {
						await.process(dp);
					}else { 
						boolean canExecute =FlowUtils.evaluateCondition(await.getCondition(),dp);
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
                        if(canExecute)
                            contextObject.process(dp);
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
