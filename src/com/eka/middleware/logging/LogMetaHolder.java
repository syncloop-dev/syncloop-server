package com.eka.middleware.logging;

import java.util.Map;

import org.apache.commons.lang3.time.StopWatch;

import com.google.common.collect.Maps;

public class LogMetaHolder {

    private Map<String, Object> MAP;
    private StopWatch stopWatch;

    public LogMetaHolder() {
        MAP = Maps.newHashMap();
        stopWatch = new StopWatch();
    }

    public Map<String, Object> getMAP() {
        return MAP;
    }

    public void startTracking() {
        stopWatch.start();
    }

    public StopWatch stopTracking() {
        if (stopWatch.isStarted()) {
            stopWatch.stop();
        }
        return stopWatch;
    }

}
