package com.eka.middleware.pub.util;

import com.eka.middleware.service.DataPipeline;
import com.google.common.collect.Maps;

import java.util.Map;

public class AppUpdate {

    static final Map<String, Object> UpdatingStatus = Maps.newHashMap();

    public static Object getStatus(String uniqueId, DataPipeline dataPipeline) {
        return UpdatingStatus.get(String.format("%s_%s", dataPipeline.rp.getTenant().getName(), uniqueId));
    }

    public static void updateStatus(String uniqueId, String status, DataPipeline dataPipeline) {
        UpdatingStatus.put(String.format("%s_%s", dataPipeline.rp.getTenant().getName(), uniqueId), status);
    }
}
