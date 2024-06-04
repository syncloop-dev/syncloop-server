package com.eka.middleware.logging;

import com.eka.middleware.service.DataPipeline;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.springframework.util.StopWatch;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class AppLogger {

	private static final Logger appLogger = LogManager.getLogger("AppLogger");

	private static final ThreadLocal<LogMetaHolder> CONTEXT = new ThreadLocal<>();

	private boolean isLoggable = false;

	public AppLogger(DataPipeline dataPipeline) {
		Map<String, Object> requestLogMap = getDefaultAttributes(dataPipeline);
		LogMetaHolder logMetaHolder = new LogMetaHolder();
		logMetaHolder.startTracking();
		logMetaHolder.getMAP().putAll(requestLogMap);

		CONTEXT.set(logMetaHolder);
	}

	public AppLogger(String tenantName) {
		Map<String, Object> requestLogMap = new HashMap <>();
		requestLogMap.put("TENANT_NAME", tenantName);
		LogMetaHolder logMetaHolder = new LogMetaHolder();
		logMetaHolder.startTracking();
		logMetaHolder.getMAP().putAll(requestLogMap);

		CONTEXT.set(logMetaHolder);
	}

	public static String getTimestamp() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MMM-dd HH:mm:ss");
		return LocalDateTime.now().format(formatter);
	}

	/**
	 * @param dataPipeline
	 * @return
	 */
	private Map<String,Object> getDefaultAttributes(DataPipeline dataPipeline) {
		if (null == dataPipeline) {
			return Maps.newHashMap();
		}
		Map<String,Object> requestAttributeMap = new HashMap <>();

		requestAttributeMap.put("correlationId", dataPipeline.getCorrelationId());

		return requestAttributeMap;
	}
	
	/**
	 * @param key
	 * @param value
	 */
	public void add(String key, Object value) {
		if(StringUtils.isBlank(key) || null == value) return;
		if(null!=CONTEXT.get()) { 
			if((key.contains("_TT") || key.contains("_QT")) && null != CONTEXT.get().getMAP().get(key)) {
				long tt = (long) CONTEXT.get().getMAP().get(key);
				if(tt > (long) value)
					return;
			}
			CONTEXT.get().getMAP().put(key, value);
			if("OPERATION".equals(key)) {
				CONTEXT.get().getMAP().put(value + "_TS", getTimestamp());
			}
			isLoggable = true;
		}	
	}

	public void addMul(String key, Object value) {
		if(StringUtils.isBlank(key) || null == value) return;
		if(null!=CONTEXT.get()) {
			if((key.contains("_TT") || key.contains("_QT")) && null != CONTEXT.get().getMAP().get(key)) {
				long tt = (long) CONTEXT.get().getMAP().get(key);
				if(tt > (long) value)
					return;
			}
			if (null == CONTEXT.get().getMAP().get(key)) {
				CONTEXT.get().getMAP().put(key, value);
			} else {
				CONTEXT.get().getMAP().put(key, CONTEXT.get().getMAP().get(key) + ", " + value);
			}
			if("OPERATION".equals(key)) {
				CONTEXT.get().getMAP().put(value + "_TS", getTimestamp());
			}
			isLoggable = true;
		}
	}

	public void finish() {
		try {
			if (null == CONTEXT.get() || CONTEXT.get().getMAP().size() == 1) {
				return ;
			}
			StopWatch stopWatch = CONTEXT.get().stopTracking();
			add("TT", stopWatch.getLastTaskTimeMillis());

			appLogger.info(getFormattedLog());

			clear();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void clear() {
		CONTEXT.remove();
	}

	public String getRequestLogs() {
		return CONTEXT.get() != null ? CONTEXT.get().toString() : "";
	}

	public String getFormattedLog() {
		if (CONTEXT.get() != null) {
			StringBuilder logBuilder = new StringBuilder();
			for (Map.Entry<String, Object> entry : CONTEXT.get().getMAP().entrySet()) {
				logBuilder.append(entry.getKey()).append("=").append("\"").append(entry.getValue()).append("\"").append(" ");
			}
			return logBuilder.toString().trim();
		}
		return "";
	}


	public static void main(String[] args) {
		Configurator.initialize(null, "/Users/divyansh/Desktop/Syncloop/Local_CodeBase/ekamw-distributions-main/resources/config/log4j2.xml");

		AppLogger appLoggerInstance = new AppLogger((DataPipeline) null);

		appLoggerInstance.add("key1", "value1");
		appLoggerInstance.add("key2", "value2");
		appLoggerInstance.add("key3", "value3");

		appLoggerInstance.finish();

	}

}
