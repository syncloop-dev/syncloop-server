package com.eka.middleware.service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.eka.middleware.auth.AuthAccount;
import com.eka.middleware.template.SnippetException;

import io.undertow.security.api.AuthenticatedSessionManager;
import io.undertow.security.api.AuthenticatedSessionManager.AuthenticatedSession;
import io.undertow.security.api.SecurityContext;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import io.undertow.server.session.Session;
import io.undertow.server.session.SessionConfig;
import io.undertow.server.session.SessionManager;
import io.undertow.util.Sessions;


public class RuntimePipeline {
	private static final Map<String, RuntimePipeline> pipelines=new ConcurrentHashMap<String, RuntimePipeline>();
	private final String sessionId;
	private final String correlationId;
	private final HttpServerExchange exchange;
	public HttpServerExchange getExchange() throws SnippetException{
		if(exchange==null)
			throw new SnippetException(dataPipeLine,"RuntimePipeline was not created through direct HTTP request. This can happen if it's restored from file or propagated through messaging service like JMS or KAFKA",new Exception("Exchange not initialized."));
		return exchange;
	}

	public final DataPipeline dataPipeLine;
	public String getSessionID() {
		return sessionId;
	}
	
	public RuntimePipeline(String requestId,String correlationId, final HttpServerExchange exchange,String resource,String urlPath) {
		sessionId=requestId;
		this.exchange=exchange;
		if(correlationId==null)
			correlationId=requestId;
		this.correlationId=correlationId;
		dataPipeLine=new DataPipeline(this,resource,urlPath);
	}
	
	public AuthAccount getCurrentRuntimeAccount() throws SnippetException {
		final SecurityContext context = getExchange().getSecurityContext();
		AuthAccount authAccount= null;
		if(context!=null)
			authAccount=(AuthAccount) context.getAuthenticatedAccount();
		return authAccount;
	}
	
	public void logOut() throws SnippetException {
		final SecurityContext context = getExchange().getSecurityContext();
		//ServletRequestContext servletRequestContext = getExchange().getAttachment(ServletRequestContext.ATTACHMENT_KEY);
		clearSession();
		//SessionManager.ATTACHMENT_KEY.
		context.logout();
	}
	
	private void clearSession() {
		
		Map<String, String> sessionManager = exchange.getAttachment(HttpServerExchange.REQUEST_ATTRIBUTES);
//		ServletRequestContext.current().getServletRequest();
		
	//	AuthenticatedSessionManager sessionManager = exchange.getAttachment(AuthenticatedSessionManager.ATTACHMENT_KEY);
	//	Object obj=System.getSecurityManager().getSecurityContext();
		//exchange.REQUEST_ATTRIBUTES
		Map<String, Cookie>  cookieMap= exchange.getRequestCookies();
		exchange.getConnection().terminateRequestChannel(exchange);
		Set<String> keys= cookieMap.keySet();
		for (String key : keys) {
			Cookie cookie=cookieMap.get(key);
			cookie.setDiscard(true);
		}
		cookieMap.clear();
        
		  Session session = Sessions.getSession(exchange);
		  if (session == null) return;
		  HashSet<String> names = new HashSet<>(session.getAttributeNames());
		  for (String attribute : names) {
		    session.removeAttribute(attribute);
		  }
		}
	
	public static RuntimePipeline create(String requestId,String correlationId,final HttpServerExchange exchange,String resource,String urlPath) {
		//String md5=ServiceUtils.generateMD5(requestId+""+System.nanoTime());
		RuntimePipeline rp=pipelines.get(requestId);
		if(rp==null) {
			rp=new RuntimePipeline(requestId,correlationId,exchange,resource,urlPath);
			pipelines.put(requestId, rp);
		}else
			rp=create(requestId,correlationId,exchange,resource,urlPath);
		return rp;
	}
	
	public static RuntimePipeline getPipeline(String id) {
		RuntimePipeline rp=pipelines.get(id);
		return rp;
	}
	public void destroy() {
		pipelines.get(sessionId).payload.clear();
		pipelines.remove(sessionId);
	}
	
	public static void destroy(String sessionId) {
		pipelines.remove(sessionId);
	}
	
	public String getCorrelationId() {
		return correlationId;
	}

	public final Map<String, Object> payload=new ConcurrentHashMap<String, Object>();
}
