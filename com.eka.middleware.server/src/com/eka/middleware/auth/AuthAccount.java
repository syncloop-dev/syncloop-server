package com.eka.middleware.auth;

import java.security.Principal;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import io.undertow.security.idm.Account;

public class AuthAccount implements Account{
	
	public String getUserId() {
		return userId;
	}

	private final String userId;
	private Map<String, Object> profile;
	
	AuthAccount(String userId){
		this.userId=userId;
	}

	private final Principal principal = new Principal() {

		@Override
		public String getName() {
			return userId;
		}		
	};
	

	@Override
	public Principal getPrincipal() {
		return principal;
	}

	@Override
	public Set<String> getRoles() {
		return Collections.emptySet();
	}

	public Map<String, Object> getProfile() {
		return profile;
	}

	public void setProfile(Map<String, Object> profile) {
		this.profile = profile;
	}

}
