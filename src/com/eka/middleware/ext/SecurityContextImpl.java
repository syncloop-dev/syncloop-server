package com.eka.middleware.ext;

import com.eka.middleware.ext.spec.SecurityContext;

import io.undertow.server.HttpServerExchange;

public class SecurityContextImpl implements SecurityContext{

	final io.undertow.security.api.SecurityContext context;
	public SecurityContextImpl(HttpServerExchange exchange) {
		context=exchange.getSecurityContext();
	}
	@Override
	public Object getAuthenticatedAccount() {
		return context.getAuthenticatedAccount();
	}
	@Override
	public void logout() {
		context.logout();
	}

}
