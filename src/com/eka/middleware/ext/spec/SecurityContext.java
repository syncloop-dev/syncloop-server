package com.eka.middleware.ext.spec;

public interface SecurityContext {
public Object getAuthenticatedAccount();
public void logout();
}
