package com.eka.middleware.sdk.api;

import com.eka.middleware.flow.Function;
import com.eka.middleware.service.DataPipeline;

public class FunctionInvoker {

    private FunctionInvoker() {
        super();
    }

    /**
     * @param dataPipeline
     */
    public static void exec(DataPipeline dataPipeline) {
        Function function = (Function)dataPipeline.get("*function_ref");
        function.exec(dataPipeline);
        dataPipeline.put("*function_ref", null);
    }
}