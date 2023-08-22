package com.eka.middleware.template;

import java.security.InvalidKeyException;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.ThreadContext;
import org.pac4j.core.config.Config;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.util.Pac4jConstants;
import org.pac4j.jwt.config.signature.SecretSignatureConfiguration;
import org.pac4j.jwt.profile.JwtGenerator;

import com.eka.middleware.auth.AuthAccount;
import com.eka.middleware.auth.Security;
import com.eka.middleware.auth.UserProfileManager;
import com.eka.middleware.ext.spec.Tenant;
import com.eka.middleware.ext.spec.UserProfile;
import com.eka.middleware.heap.HashMap;
import com.eka.middleware.service.PropertyManager;
import com.eka.middleware.service.ServiceUtils;

public class TenantImpl implements Tenant {
    private final String name;
    private final String id;
    public final SecretSignatureConfiguration secConf;
    public final SecretKeySpec KEY;// =ServiceUtils.setKey(JWT_MASALA);
    private static Map<String, Tenant> tenantMap = Tenant.tenantMap;
    public final JwtGenerator jwtGenerator;
    private static final Object syncObject = new Object();
    private final Map<String, Marker> logMarkerMap = new ConcurrentHashMap<String, Marker>();
    final Object lock = new Object();
    private static Logger LOGGER = LogManager.getLogger();
    private static final Marker TENANT_MARKER = MarkerManager.getMarker("TENANT");
    private Map<String, Object> properties = new HashMap();
    private Config JWTAuthClientConfig = null;

    public Map<String, Object> getProperties(){
    	return properties;
    }
    public String getID() {
    	return id;
    }

    public static boolean exists(String name) {
        if (name == null || name.trim().length() == 0)
            return false;
        return tenantMap.get(name) != null;
    }

    private TenantImpl(String name, String id) {
        this.name = name;
        this.id = null;
        secConf = null;
        KEY = null;
        jwtGenerator = null;
    }

    public TenantImpl(String name) throws Exception {
        this.name = name;
        String key = PropertyManager.getGlobalProperties(name).getProperty(Security.PRIVATE_PROPERTY_KEY_NAME);
        if (key == null)
            throw new Exception("Tenant public key not found or tenant not found. Tenant name: " + name);
        key = Base64.getEncoder().encodeToString(key.substring(0, 32).getBytes());
        this.id = key;
        secConf = new SecretSignatureConfiguration(id);
        KEY = ServiceUtils.getKey(id);
        jwtGenerator = new JwtGenerator(secConf);
        Date expiryDate = new Date();
        expiryDate = ServiceUtils.addHoursToDate(expiryDate, 8);
        jwtGenerator.setExpirationTime(expiryDate);
        properties.put("secConf", secConf);
        properties.put("KEY", KEY);
        properties.put("JWTAuthClientConfig", JWTAuthClientConfig);
    }

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
        logger.debug(TENANT_MARKER, msg);
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

    public String getName() {
        return name;
    }

	@Override
	public void setTenantJWTExpiry(Date expiryDate) {
		// TODO Auto-generated method stub
		jwtGenerator.setExpirationTime(expiryDate);
	}

	@Override
	public String generateJWTForTenant(UserProfile up) {
		// TODO Auto-generated method stub
		String id=(String)up.getAttribute("email");
		final var profile = new CommonProfile();
		if(id==null)
        	id=up.getId();
        AuthAccount authacc=UserProfileManager.getAccount(id, up);
		profile.setId(id);
        profile.addAttribute(Pac4jConstants.USERNAME, up.getId());
        profile.addAttribute("tenant", authacc.getAuthProfile().get("tenant"));
        profile.addAttribute("groups", authacc.getAuthProfile().get("groups"));
        return jwtGenerator.generate(profile);
	}
	
	@Override
	public String generateJWTForTenant(AuthAccount authacc) {
		final var profile = new CommonProfile();
		String id=(String) authacc.getAuthProfile().get("emailID");
		profile.setId(id);
		authacc.getAuthProfile().forEach((k,v)->{
	        profile.addAttribute(k, v);
		});
        return jwtGenerator.generate(profile);
	}

	@Override
	public void initCipher(int mode,Cipher cipher) throws InvalidKeyException {
		// TODO Auto-generated method stub
		cipher.init(mode, KEY);
	}



}
