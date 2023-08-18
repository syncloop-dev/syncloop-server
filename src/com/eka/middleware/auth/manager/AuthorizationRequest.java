package com.eka.middleware.auth.manager;

import com.eka.middleware.ext.HTTPExchangeImpl;

import io.undertow.server.HttpServerExchange;

public class AuthorizationRequest {
public static String getContent(final HttpServerExchange exchange, String requestPath) {
	
	requestPath=requestPath.substring(0, 7);
	HTTPExchangeImpl exch=new HTTPExchangeImpl(exchange);
	switch (requestPath) {
	case "GET/JWT":
		return JWT.generate(exch);
	case "GET/OIDC":
		return JWT.generate(exch);
	case "GET/SAML":
		return JWT.generate(exch);
	default:
		break;
	}
	return null;
}
}
//SecurityHandler.build(DemoHandlers.protectedIndex, config, "OidcClient"))