package com.eka.middleware.ext;

import java.security.Principal;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.eka.middleware.ext.spec.UserProfile;

public class UserProfileImpl implements UserProfile {
	public final org.pac4j.core.profile.UserProfile profile;
	public UserProfileImpl(org.pac4j.core.profile.UserProfile up) {
		profile=up;
	}

	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return profile.getId();
	}

	@Override
	public void setId(String id) {
		// TODO Auto-generated method stub
		profile.setId(id);
	}

	@Override
	public String getTypedId() {
		// TODO Auto-generated method stub
		return profile.getTypedId();
	}

	@Override
	public String getUsername() {
		// TODO Auto-generated method stub
		return profile.getUsername();
	}

	@Override
	public Object getAttribute(String name) {
		// TODO Auto-generated method stub
		return profile.getAttribute(name);
	}

	@Override
	public Map<String, Object> getAttributes() {
		// TODO Auto-generated method stub
		return profile.getAttributes();
	}

	@Override
	public boolean containsAttribute(String name) {
		// TODO Auto-generated method stub
		return profile.containsAttribute(name);
	}

	@Override
	public void addAttribute(String key, Object value) {
		// TODO Auto-generated method stub
		profile.addAttribute(key, value);
	}

	@Override
	public void removeAttribute(String key) {
		// TODO Auto-generated method stub
		profile.removeAttribute(key);
	}

	@Override
	public void addAuthenticationAttribute(String key, Object value) {
		// TODO Auto-generated method stub
		profile.addAuthenticationAttribute(key, value);
	}

	@Override
	public void removeAuthenticationAttribute(String key) {
		// TODO Auto-generated method stub
		profile.removeAuthenticationAttribute(key);
	}

	@Override
	public void addRole(String role) {
		// TODO Auto-generated method stub
		profile.addRole(role);
	}

	@Override
	public void addRoles(Collection<String> roles) {
		// TODO Auto-generated method stub
		profile.addRoles(roles);
	}

	@Override
	public Set<String> getRoles() {
		// TODO Auto-generated method stub
		return profile.getRoles();
	}

	@Override
	public void addPermission(String permission) {
		// TODO Auto-generated method stub
		profile.addPermission(permission);
	}

	@Override
	public void addPermissions(Collection<String> permissions) {
		// TODO Auto-generated method stub
		profile.addPermissions(permissions);
	}

	@Override
	public Set<String> getPermissions() {
		// TODO Auto-generated method stub
		return profile.getPermissions();
	}

	@Override
	public boolean isRemembered() {
		// TODO Auto-generated method stub
		return profile.isRemembered();
	}

	@Override
	public void setRemembered(boolean rme) {
		// TODO Auto-generated method stub
		profile.setRemembered(rme);
	}

	@Override
	public String getClientName() {
		// TODO Auto-generated method stub
		return profile.getClientName();
	}

	@Override
	public void setClientName(String clientName) {
		// TODO Auto-generated method stub
		profile.setClientName(clientName);
	}

	@Override
	public String getLinkedId() {
		// TODO Auto-generated method stub
		return profile.getLinkedId();
	}

	@Override
	public void setLinkedId(String linkedId) {
		// TODO Auto-generated method stub
		profile.setLinkedId(linkedId);
	}

	@Override
	public boolean isExpired() {
		// TODO Auto-generated method stub
		return profile.isExpired();
	}

	@Override
	public Principal asPrincipal() {
		// TODO Auto-generated method stub
		return profile.asPrincipal();
	}

}
