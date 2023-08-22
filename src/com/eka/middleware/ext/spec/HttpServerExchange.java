package com.eka.middleware.ext.spec;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Deque;
import java.util.Map;

import com.eka.middleware.service.RuntimePipeline;
import com.eka.middleware.template.SnippetException;

import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.util.HeaderMap;

public interface HttpServerExchange {
	public Map<String, String> getSessionManager();
	public void clearResponseHeaders();
	public void putResponseHeaders(String header, String value);
	public void setStatusCode(int status);
	public String getRequestURL();
	public SecurityContext getSecurityContext();
	public int getStatusCode();
	public String getHostAndPort();
	public String getRequestScheme();
	public Map<String, Object> extractHeaders();
	public FormDataParser getFormDataParser();
	public OutputStream getOutputStream();
	public InputStream getInputStream();
	public void endExchange();
	public byte[] getBody(RuntimePipeline rp)throws SnippetException;
	public Cookie getRequestCookie(String name);
	public String getRequestPath();
	public void setRequestPath(String path);
	public void setResponseCookie(Cookie c);
	public Object getProfile();
	public Map<String, Deque<String>> getQueryParameters();
	public String getRequestMethod();
	public void send(String text);
	public String getAuthorizationContent(String requestPath);
	public UserProfile getCurrentLoggedInUserProfile() throws SnippetException;
	public void clearSession();
	public HeaderMap getRequestHeaders();
	public String getSourceAddress();

	public HeaderMap getResponseHeaders();
}
