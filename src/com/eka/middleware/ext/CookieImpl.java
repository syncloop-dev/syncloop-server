package com.eka.middleware.ext;

import java.util.Date;

import com.eka.middleware.ext.spec.Cookie;

public class CookieImpl implements Cookie{

	public final io.undertow.server.handlers.Cookie cookie;
	
	public CookieImpl(io.undertow.server.handlers.Cookie cookie) {
		this.cookie=cookie;
	}
	
	public CookieImpl(String name, String value) {
		cookie=new io.undertow.server.handlers.CookieImpl(name, value);
	}
	
	@Override
	public String getName() {
		return cookie.getName();
	}

	@Override
	public String getValue() {
		return cookie.getValue();
	}

	@Override
	public Cookie setValue(String value) {
		cookie.setValue(value);
		return this;
	}

	@Override
	public String getPath() {
		// TODO Auto-generated method stub
		return cookie.getPath();
	}

	@Override
	public Cookie setPath(String path) {
		// TODO Auto-generated method stub
		cookie.setPath(path);
		return this;
	}

	@Override
	public String getDomain() {
		// TODO Auto-generated method stub
		return cookie.getDomain();
	}

	@Override
	public Cookie setDomain(String domain) {
		// TODO Auto-generated method stub
		cookie.setDomain(domain);
		return this;
	}

	@Override
	public Integer getMaxAge() {
		// TODO Auto-generated method stub
		return cookie.getMaxAge();
	}

	@Override
	public Cookie setMaxAge(Integer maxAge) {
		// TODO Auto-generated method stub
		cookie.setMaxAge(maxAge);
		return this;
	}

	@Override
	public boolean isDiscard() {
		// TODO Auto-generated method stub
		return cookie.isDiscard();
	}

	@Override
	public Cookie setDiscard(boolean discard) {
		// TODO Auto-generated method stub
		cookie.setDiscard(discard);
		return this;
	}

	@Override
	public boolean isSecure() {
		// TODO Auto-generated method stub
		return cookie.isSecure();
	}

	@Override
	public Cookie setSecure(boolean secure) {
		// TODO Auto-generated method stub
		cookie.setSecure(secure);
		return this;
	}

	@Override
	public int getVersion() {
		// TODO Auto-generated method stub
		return cookie.getVersion();
	}

	@Override
	public Cookie setVersion(int version) {
		// TODO Auto-generated method stub
		cookie.setVersion(version);
		return this;
	}

	@Override
	public boolean isHttpOnly() {
		// TODO Auto-generated method stub
		return cookie.isHttpOnly();
	}

	@Override
	public Cookie setHttpOnly(boolean httpOnly) {
		// TODO Auto-generated method stub
		cookie.setHttpOnly(httpOnly);
		return this;
	}

	@Override
	public Date getExpires() {
		// TODO Auto-generated method stub
		return cookie.getExpires();
	}

	@Override
	public Cookie setExpires(Date expires) {
		// TODO Auto-generated method stub
		cookie.setExpires(expires);
		return this;
	}

	@Override
	public String getComment() {
		// TODO Auto-generated method stub
		return cookie.getComment();
	}

	@Override
	public Cookie setComment(String comment) {
		// TODO Auto-generated method stub
		cookie.setComment(comment);
		return this;
	}

}
