package com.eka.middleware.ext;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.pac4j.undertow.account.Pac4jAccount;

import com.eka.middleware.auth.manager.AuthorizationRequest;
import com.eka.middleware.auth.pac4j.AuthHandlers;
import com.eka.middleware.ext.spec.Cookie;
import com.eka.middleware.ext.spec.HttpServerExchange;
import com.eka.middleware.ext.spec.SecurityContext;
import com.eka.middleware.ext.spec.UserProfile;
import com.eka.middleware.heap.HashMap;
import com.eka.middleware.service.RuntimePipeline;
import com.eka.middleware.service.ServiceUtils;
import com.eka.middleware.template.SnippetException;

import io.undertow.io.Receiver.FullBytesCallback;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.server.handlers.form.FormParserFactory;
import io.undertow.server.handlers.form.FormParserFactory.Builder;
import io.undertow.server.session.Session;
import io.undertow.util.HeaderMap;
import io.undertow.util.HttpString;
import io.undertow.util.Sessions;
public class HTTPExchangeImpl implements HttpServerExchange {
	private final io.undertow.server.HttpServerExchange exchange;
	private final SecurityContext context;
	private final Map<String, Cookie> cookieMap=new HashMap<String, Cookie>();
	private UserProfile profile=null;
	public HTTPExchangeImpl(io.undertow.server.HttpServerExchange httpExchange) {
		exchange=httpExchange;
		context =new SecurityContextImpl(httpExchange);
	}
	public Map<String, String> getSessionManager(){
		Map<String, String> sessionManager = exchange.getAttachment(io.undertow.server.HttpServerExchange.REQUEST_ATTRIBUTES);
		return sessionManager;
	}
	@Override
	public void clearResponseHeaders() {
		exchange.getResponseHeaders().clear();
	}
	@Override
	public void putResponseHeaders(String header, String value) {
		exchange.getResponseHeaders().put(HttpString.tryFromString(header), value);
	}
	@Override
	public void setStatusCode(int status) {
		exchange.setStatusCode(status);
		
	}
	@Override
	public String getRequestURL() {
		// TODO Auto-generated method stub
		return exchange.getRequestURL();
	}
	@Override
	public SecurityContext getSecurityContext() {
		return context;
	}
	@Override
	public int getStatusCode() {
		// TODO Auto-generated method stub
		return exchange.getStatusCode();
	}
	@Override
	public String getHostAndPort() {
		// TODO Auto-generated method stub
		return exchange.getHostAndPort();
	}
	
	@Override
    public String getRequestScheme() {
    	return exchange.getRequestScheme();
    }
	
	public Map<String, Object> extractHeaders() {
		Map<String, Object> map = new HashMap<String, Object>();
		HeaderMap hm = exchange.getRequestHeaders();
		Collection<HttpString> hts = hm.getHeaderNames();
		for (HttpString httpString : hts) {
			map.put(httpString.toString(), hm.get(httpString).getFirst());
		}
		return map;
	}
	
	public FormDataParser getFormDataParser() {
		Builder builder = FormParserFactory.builder();
		final FormDataParser formDataParser = builder.build().createParser(exchange);
		return formDataParser;
	}

	@Override
	public void endExchange() {
		exchange.endExchange();
		
	}
	
	public byte[] getBody(RuntimePipeline rp) throws SnippetException {
		try {
			//final RuntimePipeline rp = RuntimePipeline.getPipeline(dataPipeLine.getSessionId());
			final Map<String, Object> payload = rp.payload;
			try {
				exchange.getRequestReceiver().setMaxBufferSize(1024);
				exchange.getRequestReceiver().receiveFullBytes(new FullBytesCallback() {
					@Override
					public void handle(io.undertow.server.HttpServerExchange exchange, byte[] body) {
						payload.put("@body", body);
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
				ServiceUtils.printException(rp.getTenant(),rp.getSessionID() + " Could not stream body thread.", e);
			}

			if (payload.get("@body") != null) {
				byte body[] = (byte[]) payload.get("@body");
				payload.remove("@body");
				return body;
			}

		} catch (Exception e) {
			e.printStackTrace();
			throw new SnippetException(rp.dataPipeLine, e.getMessage(), e);
		}
		return null;
	}
	@Override
	public OutputStream getOutputStream() {
		// TODO Auto-generated method stub
		return exchange.getOutputStream();
	}
	@Override
	public InputStream getInputStream() {
		// TODO Auto-generated method stub
		return exchange.getInputStream();
	}
	@Override
	public Cookie getRequestCookie(String name) {
		Cookie cookie=cookieMap.get(name);
		if(cookie==null && exchange.getRequestCookie(name)!=null) {
			cookie=new CookieImpl(exchange.getRequestCookie(name));
			cookieMap.put(name, cookie);
		}
		return cookie;
	}
	@Override
	public String getRequestPath() {
		// TODO Auto-generated method stub
		return exchange.getRequestPath();
	}
	@Override
	public void setRequestPath(String path) {
		// TODO Auto-generated method stub
		exchange.setRequestPath(path);
	}
	@Override
	public void setResponseCookie(Cookie c) {
		// TODO Auto-generated method stub
		exchange.setResponseCookie(((CookieImpl)c).cookie);
	}
	@Override
	public Object getProfile() {
		org.pac4j.core.profile.UserProfile up =AuthHandlers.getProfile(exchange);
		return up;
	}
	
	public Map<String, Deque<String>> getQueryParameters() {
		Map<String, Deque<String>> map= exchange.getQueryParameters();
		return map;
	}
	@Override
	public String getRequestMethod() {
		// TODO Auto-generated method stub
		return exchange.getRequestMethod().toString();
	}
	@Override
	public void send(String text) {
		exchange.getResponseSender().send(text);
		
	}
	@Override
	public String getAuthorizationContent(String requestPath) {
		// TODO Auto-generated method stub
		String content = AuthorizationRequest.getContent(exchange, requestPath.toUpperCase());
		return content;
	}
	
	public UserProfile getCurrentLoggedInUserProfile() throws SnippetException {
		if(profile!=null)
			return profile;
		final SecurityContext context = getSecurityContext();
		org.pac4j.core.profile.UserProfile up=null;
		if (context != null) {
			up = ((Pac4jAccount)context.getAuthenticatedAccount()).getProfile();
			profile= new UserProfileImpl(up);
		}
		return profile;
	}
	
	public void clearSession() {
		Map<String, String> sessionManager = exchange.getAttachment(io.undertow.server.HttpServerExchange.REQUEST_ATTRIBUTES);
		Map<String, io.undertow.server.handlers.Cookie> cookieMap = exchange.getRequestCookies();
		exchange.getConnection().terminateRequestChannel(exchange);
		Set<String> keys = cookieMap.keySet();
		for (String key : keys) {
			io.undertow.server.handlers.Cookie cookie = cookieMap.get(key);
			cookie.setDiscard(true);
		}
		cookieMap.clear();

		Session session = Sessions.getSession(exchange);
		if (session == null)
			return;
		HashSet<String> names = new HashSet<>(session.getAttributeNames());
		for (String attribute : names) {
			session.removeAttribute(attribute);
		}
		
		
		session.invalidate(exchange);
	}
	
	public HeaderMap getRequestHeaders() {
		return exchange.getRequestHeaders();
	}
	
	public String getSourceAddress() {
		return exchange.getSourceAddress().getAddress().toString();
	}
}
