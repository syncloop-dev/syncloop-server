package com.eka.middleware.sdk.api;

import com.beust.jcommander.internal.Lists;
import com.eka.middleware.sdk.api.outline.*;
import com.nimbusds.jose.shaded.gson.Gson;
import test.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.List;

public class SyncloopFunctionScanner {


    public static void main(String[] args) {

       System.out.println(new Gson( ).toJson(addClass(Test.class)));


    }


    public static List<ServiceOutline> addClass(Class aClass) {

        List<ServiceOutline> serviceOutlines = Lists.newArrayList();
        Method[] methods = aClass.getDeclaredMethods();

        for (Method method: methods) {
            SyncloopFunction methodExport = method.getAnnotation(SyncloopFunction.class);

            if (null == methodExport || !Modifier.isStatic(method.getModifiers())) {
                continue;
            }

            //TODO Generic Type, Collection & Primitive Type is pending.

            String[] parametersName = methodExport.in();
            String outputParameterName = methodExport.out();
            String methodName = method.getName();
            Class returnType = method.getReturnType();
            String packageName = method.getDeclaringClass().getPackage().getName();
            String className = method.getDeclaringClass().getName();
            Parameter[] parameters = method.getParameters();
            Class[] parametersTypes = new Class[parameters.length];
            String[] parametersTypesStr = new String[parameters.length];

            for (int i = 0 ; i < parameters.length ; i++) {
                parametersTypes[i] = parameters[i].getType();
                parametersTypesStr[i] = parameters[i].getType().getName();
            }

            DataOutline dataOutline = new DataOutline();
            dataOutline.setAcn(className);
            dataOutline.setFunction(methodName);
            dataOutline.setArguments(parametersName);
            dataOutline.setReturnWrapper(returnType.getName());
            dataOutline.setArgumentsWrapper(parametersTypesStr);
            dataOutline.setOutputArguments(outputParameterName);

            ApiInfoOutline apiInfoOutline = new ApiInfoOutline();
            apiInfoOutline.setTitle(methodExport.title());
            apiInfoOutline.setDescription(methodExport.description());

            LatestOutline latestOutline = new LatestOutline();
            latestOutline.setApi_info(apiInfoOutline);
            latestOutline.setData(dataOutline);

            List<IOOutline> input = Lists.newArrayList();
            for (int i = 0 ; i < parameters.length ; i++) {
                IOOutline ioOutline = new IOOutline();
                ioOutline.setText(parametersName[i]);
                ioOutline.setType(parameters[i].getType().getSimpleName().toLowerCase());
                input.add(ioOutline);
            }

            IOOutline in = new IOOutline();
            in.setText("in");
            in.setType("document");
            in.setChildren(input);

            latestOutline.setInput(Lists.newArrayList(in));

            List<IOOutline> output = Lists.newArrayList();
            IOOutline ioOutline = new IOOutline();
            ioOutline.setText(outputParameterName);
            ioOutline.setType(returnType.getSimpleName().toLowerCase());
            output.add(ioOutline);

            IOOutline out = new IOOutline();
            out.setText("out");
            out.setType("document");
            out.setChildren(output);

            latestOutline.setOutput(Lists.newArrayList(out));

            ServiceOutline serviceOutline = new ServiceOutline();
            serviceOutline.setLatest(latestOutline);

            serviceOutlines.add(serviceOutline);
        }

        return serviceOutlines;
    }

}
