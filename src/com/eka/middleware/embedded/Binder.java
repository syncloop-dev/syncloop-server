package com.eka.middleware.embedded;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.apache.commons.collections.MapUtils;

import com.eka.middleware.ext.spec.Tenant;
import com.eka.middleware.flow.FlowResolver;
import com.eka.middleware.heap.HashMap;
import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.RuntimePipeline;
import com.eka.middleware.template.SnippetException;

public class Binder {
	static Tenant tenant = Tenant.getTempTenant("default");

	public static Map<String, Object> run(String apiServiceJson, String serviceName, Map<String, Object> payload)
			throws Exception {
		JsonObject mainflowJsonObject = null;
		UUID sessionId = UUID.randomUUID();
		UUID coId = UUID.randomUUID();
		RuntimePipeline rp = RuntimePipeline.create(tenant, sessionId.toString(), coId.toString(), null, serviceName,
				null);
		DataPipeline dp = rp.dataPipeLine;
		dp.putAll(payload);
		executeService(dp, apiServiceJson, mainflowJsonObject);
		final Map<String, Object> response = new HashMap<>();
		dp.getMap().forEach((k, v) -> {
			response.put(k, v);
		});
		rp.destroy();
		return response;
	}

	private static final void executeService(DataPipeline dataPipeline, String apiServiceJson,
			JsonObject mainflowJsonObject) throws SnippetException {
		try {
			InputStream is = new ByteArrayInputStream(apiServiceJson.getBytes(StandardCharsets.UTF_8));
			mainflowJsonObject = Json.createReader(is).readObject();
			FlowResolver.execute(dataPipeline, mainflowJsonObject);
		} catch (Throwable e) {
			dataPipeline.clear();
			dataPipeline.put("error", e.getMessage());
			dataPipeline.put("status", "Service error");
			throw new SnippetException(dataPipeline, "Failed to execute " + dataPipeline.getCurrentResourceName(),
					new Exception(e));
		}
	}
}
