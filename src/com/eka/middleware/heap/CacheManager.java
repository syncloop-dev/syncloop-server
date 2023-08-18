package com.eka.middleware.heap;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.eka.middleware.ext.spec.Tenant;

public class CacheManager {
private static final Map<String, Map<String, Object>> tenantCache= new ConcurrentHashMap<String,  Map<String, Object>>();
	
	public static Map<String, Object> getCacheAsMap(Tenant tenant) {
		Map<String, Object> tenantMap=tenantCache.get(tenant.getID());
		if(tenantMap==null)
			tenantMap=new ConcurrentHashMap<String,  Object>();
		tenantCache.put(tenant.getID(), tenantMap);
		return tenantMap;
	}
}
