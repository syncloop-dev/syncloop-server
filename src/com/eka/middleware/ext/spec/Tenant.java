package com.eka.middleware.ext.spec;

import java.security.InvalidKeyException;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.Cipher;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.ThreadContext;

import com.eka.middleware.auth.AuthAccount;
import com.eka.middleware.heap.HashMap;
import com.eka.middleware.service.ServiceUtils;
import com.eka.middleware.template.TenantImpl;

public interface Tenant {
	static Map<String, Tenant> tenantMap = new HashMap();
	static final Object syncObject = new Object();
	public Map<String, Object> getProperties();
	public static Tenant getTenant(String name) {

        synchronized (syncObject) {
            try {
                if (name == null)
                    name = "default";
                if (tenantMap.get(name) == null) {
                    tenantMap.put(name, new TenantImpl(name));
                }
            } catch (Exception e) {
                ServiceUtils.printException("Failed while loading tenant.", e);
            }
        }
        return tenantMap.get(name);
    }
	public static boolean exists(String name) {
        if (name == null || name.trim().length() == 0)
            return false;
        return tenantMap.get(name) != null;
    }
	public static Tenant getTempTenant(String name){
		return new Tenant() {
			private final Map<String, Marker> logMarkerMap = new ConcurrentHashMap<String, Marker>();
			private static final Marker TENANT_MARKER = MarkerManager.getMarker("TENANT");
			final Object lock=new Object();
			public void logInfo(String serviceName, String msg) {
		        if (serviceName == null)
		            serviceName = name;
		        Logger logger = getLogger(serviceName);
		        logger.info(TENANT_MARKER, msg);
		        clearContext();
		    }

		    public void logTrace(String serviceName, String msg) {
		        if (serviceName == null)
		            serviceName = name;
		        Logger logger = getLogger(serviceName);
		        logger.trace(TENANT_MARKER, msg);
		        clearContext();
		    }

		    public void logWarn(String serviceName, String msg) {
		        if (serviceName == null)
		            serviceName = name;
		        Logger logger = getLogger(serviceName);
		        logger.warn(TENANT_MARKER, msg);
		        clearContext();
		    }

		    public void logError(String serviceName, String msg) {
		        if (serviceName == null)
		            serviceName = name;
		        Logger logger = getLogger(serviceName);
		        logger.error(TENANT_MARKER, msg);
		        clearContext();
		    }

		    public void logDebug(String serviceName, String msg) {
		        if (serviceName == null)
		            serviceName = name;
		        Logger logger = getLogger(serviceName);
		        logger.error(TENANT_MARKER, msg);
		        clearContext();
		    }

		    private void clearContext() {
		        //ThreadContext.remove("name");
		        //ThreadContext.remove("service");
		        //ThreadContext.clearAll();
		    }

		    private Marker getMarker(String serviceName) {

		        serviceName = serviceName.replace("/", ".");
		        String markerKey = name + "/logs/" + serviceName;
		        Marker logMarker = logMarkerMap.get(markerKey);
		        if (logMarker == null)
		            synchronized (lock) {
		                logMarker = logMarkerMap.get(markerKey);
		                if (logMarker == null) {
		                    logMarker = MarkerManager.getMarker(markerKey);
		                    logMarkerMap.put(markerKey, logMarker);
		                }
		            }
		        return logMarker;
		    }

		    private Logger getLogger(String serviceName) {
		        serviceName = serviceName.replace("/", ".");
		        Logger log = LogManager.getLogger();
		        ThreadContext.put("name", name);
		        ThreadContext.put("service", serviceName);
		        //LoggerContext ctx=LogManager.getContext();
		        //ctx.putObject("name", name);
		        //ctx.putObject("service", serviceName);
		        return log;
		    }
			
			@Override
			public String getName() {
				// TODO Auto-generated method stub
				return name;
			}

			@Override
			public void setTenantJWTExpiry(Date expiryDate) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public String generateJWTForTenant(UserProfile up) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void initCipher(int mode,Cipher cipher)  throws InvalidKeyException{
				// TODO Auto-generated method stub
				
			}

			@Override
			public String getID() {
				// TODO Auto-generated method stub
				return name+"_";
			}

			@Override
			public String generateJWTForTenant(AuthAccount authacc) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Map<String, Object> getProperties() {
				// TODO Auto-generated method stub
				return null;
			}
		};
	}
	
	public void logInfo(String serviceName, String msg) ;

    public void logTrace(String serviceName, String msg);

    public void logWarn(String serviceName, String msg);

    public void logError(String serviceName, String msg) ;

    public void logDebug(String serviceName, String msg);
    
    public void setTenantJWTExpiry(Date expiryDate);
    
    public String generateJWTForTenant(UserProfile up);

    public String getName();
    
    public void initCipher(int mode,Cipher cipher) throws InvalidKeyException;
    public String getID();
    public String generateJWTForTenant(AuthAccount authacc);
}
