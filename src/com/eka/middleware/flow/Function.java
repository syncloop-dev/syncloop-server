package com.eka.middleware.flow;

import com.beust.jcommander.internal.Lists;
import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.FlowBasicInfo;
import com.eka.middleware.template.SnippetException;
import com.google.common.collect.Maps;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

public class Function implements FlowBasicInfo {

    private boolean disabled=false;
    private boolean sync=true;
    private String fqn;
    private Transformer transformer;
    private String condition;
    private String label;
    private JsonObject api;
    private boolean evaluateCondition;
    private String comment;
    private JsonArray transformers;
    private JsonObject data=null;
    private JsonArray createList;
    private JsonArray dropList;
    private String requestMethod;
    private String snapshot=null;
    private String snapCondition=null;

    @Getter
    private String name;

    @Getter
    private String type;

    @Getter
    private String guid;

    public Function(JsonObject jo) {
        api=jo;
        data=api.get("data").asJsonObject();
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
        requestMethod=data.getString("requestMethod","sync");
//		System.out.println(data.isNull("transformers"));
        if(data.containsKey("transformers") && !data.isNull("transformers"))
            transformers=data.getJsonArray("transformers");
        if(!data.isNull("createList"))
            createList=data.getJsonArray("createList");
        if(!data.isNull("dropList"))
            dropList=data.getJsonArray("dropList");

        guid = data.getString("guid",null);
        name = api.getString("text",null);
        type = api.getString("type",null);

    }

    public void process(DataPipeline dp) throws SnippetException {

        if(dp.isDestroyed()) {
            throw new SnippetException(dp, "User aborted the service thread", new Exception("Service runtime pipeline destroyed manually"));
        }
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
            if (disabled)
                return;
            dp.addErrorStack(this);

            try {

                String afn = data.getString("acn", null);
                String outputArgument = data.getString("outputArgument", null);
                String function = data.getString("function", null);

                JsonArray jsonArray = data.get("argumentsWrapper").asJsonArray();
                Class[] aClass = new Class[jsonArray.size()];

                for (int i = 0 ; i < jsonArray.size() ; i++) {
                    aClass[i] = Class.forName(jsonArray.getString(i));
                }

                FlowUtils.mapBefore(transformers, dp);

                jsonArray = data.get("arguments").asJsonArray();
                List<Object> arguments = Lists.newArrayList();
                for (int i = 0 ; i < jsonArray.size() ; i++) {
                    arguments.add(dp.get(jsonArray.getString(i)));
                }

                Class afnClass = Class.forName(afn);
                Method method = afnClass.getMethod(function, aClass);
                Object output = method.invoke(null, arguments.toArray());
                dp.map("output", output);
                FlowUtils.mapAfter(transformers, dp);

            } catch ( Exception e ) {
                e.printStackTrace();
            }

            if (createList != null)
                FlowUtils.setValue(createList, dp);
            if (dropList != null)
                FlowUtils.dropValue(dropList, dp);// setValue(dropList, dp);
            dp.putGlobal("*hasError", false);
        } catch (Exception e) {
            dp.putGlobal("*error", e.getMessage());
            dp.putGlobal("*hasError", true);
            throw e;
        } finally {
            if(canSnap) {
                dp.snapAfter(comment, guid, Maps.newHashMap());
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
