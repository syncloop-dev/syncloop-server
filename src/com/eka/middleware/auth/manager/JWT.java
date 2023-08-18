package com.eka.middleware.auth.manager;

import java.util.Date;
import java.util.Deque;

import org.apache.commons.lang3.StringUtils;

import com.eka.middleware.ext.UserProfileImpl;
import com.eka.middleware.ext.spec.HttpServerExchange;
import com.eka.middleware.ext.spec.Tenant;
import com.eka.middleware.ext.spec.UserProfile;
import com.eka.middleware.service.ServiceUtils;

public class JWT {
	public static String generate(HttpServerExchange exchange) {
		String tenantName=ServiceUtils.setupRequestPath(exchange);
		String token = "";
		UserProfile up =  new UserProfileImpl((org.pac4j.core.profile.UserProfile) exchange.getProfile());
        if (up != null) {
            Tenant tenant = Tenant.getTenant(tenantName);

            Deque<String> stringDeque = exchange.getQueryParameters().get("expiration_time");
            String expirationTimeStr = (null == stringDeque) ? null : stringDeque.pop();
            int expirationTime = 720;
            if (StringUtils.isNotBlank(expirationTimeStr)) {
                expirationTime = Integer.parseInt(expirationTimeStr);
            }

            Date expiryDate = new Date();
            expiryDate = ServiceUtils.addHoursToDate(expiryDate, expirationTime);

            tenant.setTenantJWTExpiry(expiryDate);
            token = tenant.generateJWTForTenant(up);

            token=ServiceUtils.encrypt(token, tenantName);
        }
        return token;
	}
	
}
