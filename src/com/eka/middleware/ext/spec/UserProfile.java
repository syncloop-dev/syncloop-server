package com.eka.middleware.ext.spec;

import java.io.Serializable;
import java.security.Principal;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface UserProfile extends Serializable{
	
	String getId();

    void setId(String id);

    String getTypedId();

    String getUsername();

    Object getAttribute(String name);

    Map<String, Object> getAttributes();

    boolean containsAttribute(String name);

    void addAttribute(String key, Object value);

    void removeAttribute(String key);

    void addAuthenticationAttribute(String key, Object value);

    void removeAuthenticationAttribute(String key);

    void addRole(String role);

    void addRoles(Collection<String> roles);

    Set<String> getRoles();

    void addPermission(String permission);

    void addPermissions(Collection<String> permissions);

    Set<String> getPermissions();

    boolean isRemembered();

    void setRemembered(boolean rme);

    String getClientName();

    void setClientName(String clientName);

    String getLinkedId();

    void setLinkedId(String linkedId);

    boolean isExpired();

    Principal asPrincipal();
    
	static UserProfile SYSTEM_PROFILE=new UserProfile() {
		
		@Override
		public void setRemembered(boolean rme) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void setLinkedId(String linkedId) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void setId(String id) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void setClientName(String clientName) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void removeAuthenticationAttribute(String key) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void removeAttribute(String key) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public boolean isRemembered() {
			// TODO Auto-generated method stub
			return false;
		}
		
		@Override
		public boolean isExpired() {
			// TODO Auto-generated method stub
			return false;
		}
		
		@Override
		public String getUsername() {
			// TODO Auto-generated method stub
			return null;
		}
		
		@Override
		public String getTypedId() {
			// TODO Auto-generated method stub
			return null;
		}
		
		@Override
		public Set<String> getRoles() {
			// TODO Auto-generated method stub
			return null;
		}
		
		@Override
		public Set<String> getPermissions() {
			// TODO Auto-generated method stub
			return null;
		}
		
		@Override
		public String getLinkedId() {
			// TODO Auto-generated method stub
			return null;
		}
		
		@Override
		public String getId() {
			// TODO Auto-generated method stub
			return "SYSTEM";
		}
		
		@Override
		public String getClientName() {
			// TODO Auto-generated method stub
			return null;
		}
		
		@Override
		public Map<String, Object> getAttributes() {
			// TODO Auto-generated method stub
			return null;
		}
		
		@Override
		public Object getAttribute(String name) {
			// TODO Auto-generated method stub
			return null;
		}
		
		@Override
		public boolean containsAttribute(String name) {
			// TODO Auto-generated method stub
			return false;
		}
		
		@Override
		public Principal asPrincipal() {
			// TODO Auto-generated method stub
			return null;
		}
		
		@Override
		public void addRoles(Collection<String> roles) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void addRole(String role) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void addPermissions(Collection<String> permissions) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void addPermission(String permission) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void addAuthenticationAttribute(String key, Object value) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void addAttribute(String key, Object value) {
			// TODO Auto-generated method stub
			
		}
	};
}
