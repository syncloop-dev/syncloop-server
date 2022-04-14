package com.eka.middleware.auth;

import java.io.File;
import java.net.URL;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.eka.middleware.server.MiddlewareServer;
import com.eka.middleware.service.ServiceUtils;
import com.eka.middleware.template.SnippetException;
import com.eka.middleware.template.SystemException;

import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.idm.PasswordCredential;

public class UserProfileManager implements IdentityManager {
	private static final Map<String, Object> users = new ConcurrentHashMap<String, Object>();
	
	public static final Map<String, Object> getUsers() throws SystemException {
		try {
			byte bytes[] = MiddlewareServer.readConfigurationFile("profiles.json");
			if (bytes!=null) {
				String json = new String(bytes);
				final Map<String, Object> map = ServiceUtils.jsonToMap(json);
				final Map<String, Object> tanent = ((Map<String, Object>)map.get("dev"));
				tanent.forEach((k,v)->{
					Map<String, Object> user = (Map<String, Object>) v;
					String pass=user.get("password").toString();
					if(!pass.startsWith("[#]")) {
						String passHash="[#]"+ServiceUtils.generateUUID(pass+k);
						user.put("password", passHash);
					}
				});
				users.putAll(map);
				json=ServiceUtils.toPrettyJson(users);
				MiddlewareServer.writeConfigurationFile("profiles.json", json.getBytes());
			}
		} catch (Exception e) {
			throw new SystemException("EKA_MWS_1001", e);
		}
		return users;
	}
	
	public UserProfileManager() throws SystemException {
		getUsers();
	}

	@Override
	public Account verify(Account account) {
		// An existing account so for testing assume still valid.
		return account;
	}

	@Override
	public Account verify(String id, Credential credential) {
		Account account = getAccount(id);
		if (account != null && verifyCredential(account, credential)) {
			return account;
		}

		return null;
	}

	@Override
	public Account verify(Credential credential) {
		// TODO Auto-generated method stub
		return null;
	}

	private boolean verifyCredential(Account account, Credential credential) {
		if (credential instanceof PasswordCredential) {
			char[] password = ((PasswordCredential) credential).getPassword();
			Map<String, Object> tanent = (Map<String, Object>) users.get("dev");
			String userId=account.getPrincipal().getName();
			Map<String, Object> user = (Map<String, Object>) tanent.get(userId);
			if (user == null) {
				return false;
			}
			char[] expectedPassword = user.get("password").toString().toCharArray();
			String pass=new String(password);
			String passHash="[#]"+ServiceUtils.generateUUID(pass+userId);
			return Arrays.equals(passHash.toCharArray(), expectedPassword);
		}
		return false;
	}

	private Account getAccount(final String id) {
		final Map<String, Object> tanent = (Map<String, Object>) users.get("dev");
		Map<String, Object> user = (Map<String, Object>) tanent.get(id);
		
		if (user!=null) {
			final Map<String, Object> profile=(Map<String, Object>) user.get("profile");
			AuthAccount authAccount=new AuthAccount(id);
			authAccount.setProfile(profile);
			return authAccount;
		}
		return null;
	}
}
