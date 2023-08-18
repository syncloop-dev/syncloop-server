package com.eka.middleware.auth.pac4j;

import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pac4j.core.client.Client;
import org.pac4j.core.config.Config;
import org.pac4j.core.exception.http.HttpAction;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.core.util.Pac4jConstants;
import org.pac4j.http.client.indirect.FormClient;
import org.pac4j.undertow.account.Pac4jAccount;
import org.pac4j.undertow.context.UndertowSessionStore;
import org.pac4j.undertow.context.UndertowWebContext;
import org.pac4j.undertow.handler.SecurityHandler;
import org.pac4j.undertow.http.UndertowHttpActionAdapter;

import com.eka.middleware.auth.AuthAccount;
import com.eka.middleware.auth.Security;
import com.eka.middleware.auth.manager.JWT;
import com.eka.middleware.ext.HTTPExchangeImpl;
import com.eka.middleware.ext.spec.Cookie;
import com.eka.middleware.ext.spec.Tenant;
import com.eka.middleware.server.ThreadManager;
import com.eka.middleware.service.ServiceUtils;
//import com.eka.middleware.template.Tenant;

import io.undertow.security.api.SecurityContext;
import io.undertow.security.idm.Account;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.StatusCodes;

public class AuthHandlers {

	private static final Logger LOGGER = LogManager.getLogger(AuthHandlers.class);

	public static HttpHandler indexHandler() {
		return exchange -> {
			ThreadManager.processRequest(new HTTPExchangeImpl(exchange));
		};
	}

	public static HttpHandler defaultWelcomePageHandler(final String path) {
		return exchange -> {
			HTTPExchangeImpl exch=new HTTPExchangeImpl(exchange);
			List<UserProfile> profiles = getProfiles(exchange);
			String p=path;
			String tenantName=ServiceUtils.setupRequestPath(exch);
			Cookie cookie=ServiceUtils.setupCookie(exch, tenantName, null);
			ServiceUtils.manipulateHeaders(exch);
			String token = ServiceUtils.getToken(cookie);
			if(token!=null || profiles!=null) {
				p=Security.defaultWorkspacePage;
			}
			
			exchange.getResponseHeaders().clear();
			exchange.setStatusCode(StatusCodes.FOUND);
			exchange.getResponseHeaders().put(Headers.LOCATION, p);
			//Cookie cookie = new CookieImpl("tenant", "default");
			//exchange.setResponseCookie(cookie);
			exchange.endExchange();
		};
	}

	private static void sendEnd(final HttpServerExchange exchange, final StringBuilder sb) {
		exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, "text/html; charset=utf-8");
		exchange.getResponseSender().send(sb.toString());
		exchange.endExchange();
	}

	public static HttpHandler logoutHandler = new HttpHandler() {
		public void handleRequest(final HttpServerExchange exchange) throws Exception {
			//LogoutHandler lh=new LogoutHandler(AuthConfigFactory.getLogoutConfig(),"/?defaulturlafterlogout");
			HTTPExchangeImpl exch=new HTTPExchangeImpl(exchange);
			Cookie ck=exch.getRequestCookie("tenant");
			ck.setValue(null);
			ck.setDiscard(true);
			//lh.handleRequest(exchange);
			//exchange.setRequestCookie(ck);
			//exchange.setResponseCookie(ck);
			Date expiry=new Date(System.currentTimeMillis());
			exchange.requestCookies().forEach(k->k.setMaxAge(-27788353));
			exchange.responseCookies().forEach(k->k.setMaxAge(-27788353));
			ServiceUtils.redirectRequest(exch, Security.defaultLoginPage);
			//ServiceUtils.clearSession(exch);
			exchange.endExchange();
		}
	};
	
	public static HttpHandler mainHandler = new HttpHandler() {
		public void handleRequest(final HttpServerExchange exchange) throws Exception {
			final SecurityContext context = exchange.getSecurityContext();
			HTTPExchangeImpl exch=new HTTPExchangeImpl(exchange);
			//AuthAccount acc=(AuthAccount) context.getAuthenticatedAccount();
			List<UserProfile> profiles = getProfiles(exchange);
			String tenantName=ServiceUtils.setupRequestPath(exch);
			Cookie cookie=ServiceUtils.setupCookie(exch, tenantName, null);
			ServiceUtils.manipulateHeaders(exch);
			if (profiles != null) {
				AuthAccount acc = ServiceUtils.getCurrentLoggedInAuthAccount(exch);
				String token=ServiceUtils.getToken(cookie);
				if(token==null) {
					token=JWT.generate(exch);
					cookie=ServiceUtils.setupCookie(exch, (String)acc.getAuthProfile().get("tenant"), token);
				}
				ThreadManager.processRequest(exch,cookie);
			} else {
				HttpHandler hh = this;
				HeaderMap headers = exchange.getRequestHeaders();
				HeaderValues hv = headers.get(HttpString.tryFromString("Authorization"));
				String authorization = null;
				boolean isBearer = false;
				boolean isBasic = false;

				if (hv != null) {
					authorization = exchange.getRequestHeaders().get(HttpString.tryFromString("Authorization"))
							.getFirst();
					isBearer = authorization.startsWith("Bearer ");
					isBasic = authorization.startsWith("Basic ");
				}
				String token = ServiceUtils.getToken(cookie);
				if (isBearer)
					token = authorization.replace("Bearer ", "");

				if (token == null) {
					Config cfg = Security.loginExactPathsMap.get(tenantName);
					if(cfg==null) {
						exchange.getResponseSender().send("Security has not been initialized for tenant '"+tenantName+"'.");
						exchange.endExchange();
						return;
					}
					hh = SecurityHandler.build(hh, cfg);

				} else {
					try {
						token = ServiceUtils.decrypt(token, tenantName);
					} catch (Exception e) {
						cookie.setExpires(new Date(System.currentTimeMillis()-1000));
						cookie.setDiscard(true);
						if(!ServiceUtils.isApiCall(exch))
							exch.setResponseCookie(cookie);
						exchange.setStatusCode(401);
						exchange.getResponseSender().send("Token expired. Please reload/relogin page.");
						exchange.endExchange();
						return;
					}

					exchange.getRequestHeaders().put(HttpString.tryFromString("Authorization"), token);
					Config cfg = AuthConfigFactory.getJWTAuthClientConfig(Tenant.getTenant(tenantName));
					hh = SecurityHandler.build(hh, cfg);
				}
				hh.handleRequest(exchange);
			}

		}
	};

	public static HttpHandler notProtectedIndex = exchange -> {
		HTTPExchangeImpl exch=new HTTPExchangeImpl(exchange);
		String tenantName=ServiceUtils.setupRequestPath(exch);
		Cookie cookie=ServiceUtils.setupCookie(exch, tenantName, null);
		ThreadManager.processRequest(exch,cookie);
	};

	public static HttpHandler authenticatedJsonHandler = exchange -> {
		StringBuilder sb = new StringBuilder();
		sb.append("{\"username\":\"");
		sb.append(getProfile(exchange).getId());
		sb.append("\"}");

		exchange.getResponseHeaders().add(HttpString.tryFromString("Content-Type"), "application/json");
		sendEnd(exchange, sb);
	};

	private static Pac4jAccount getAccount(final HttpServerExchange exchange) {
		final SecurityContext securityContext = exchange.getSecurityContext();
		if (securityContext != null) {
			final Account account = securityContext.getAuthenticatedAccount();
			if (account instanceof Pac4jAccount) {
				return (Pac4jAccount) account;
			}
		}
		return null;
	}

	public static Pac4jAccount getAuthAccount(final HttpServerExchange exchange) {
		final SecurityContext securityContext = exchange.getSecurityContext();
		if (securityContext != null) {
			final Pac4jAccount account = (Pac4jAccount) securityContext.getAuthenticatedAccount();
			return account;
		}
		return null;
	}

	private static List<UserProfile> getProfiles(final HttpServerExchange exchange) {
		final Pac4jAccount account = getAccount(exchange);
		if (account != null) {
			return account.getProfiles();
		}
		return null;
	}

	public static UserProfile getProfile(final HttpServerExchange exchange) {
		final Pac4jAccount account = getAccount(exchange);
		if (account != null) {
			return account.getProfile();
		}
		return null;
	}

	public static HttpHandler loginFormHandler(final Config config) {
		return exchange -> {
			FormClient formClient = (FormClient) config.getClients().findClient("FormClient").get();
			StringBuilder sb = new StringBuilder();

			LOGGER.info("Callback Url ::: {}", formClient.getCallbackUrl());

			sb.append("<html><body>");
			sb.append("<form action=\"").append(formClient.getCallbackUrl())
					.append("?client_name=FormClient\" method=\"GET\">");
			sb.append("<input type=\"text\" name=\"username\" value=\"\" />");
			sb.append("<p />");
			sb.append("<input type=\"password\" name=\"password\" value=\"\" />");
			sb.append("<p />");
			sb.append("<input type=\"submit\" name=\"submit\" value=\"Submit\" />");
			sb.append("</form>");
			sb.append("</body></html>");

			sendEnd(exchange, sb);
		};
	}

	public static HttpHandler forceLoginHandler(final Config config) {
		return exchange -> {
			final UndertowWebContext context = new UndertowWebContext(exchange);
			final UndertowSessionStore sessionStore = new UndertowSessionStore(exchange);
			final String clientName = context.getRequestParameter(Pac4jConstants.DEFAULT_CLIENT_NAME_PARAMETER).get();
			final Client client = config.getClients().findClient(clientName).get();
			HttpAction action;
			try {
				action = client.getRedirectionAction(context, sessionStore).get();
			} catch (final HttpAction e) {
				action = e;
			}
			UndertowHttpActionAdapter.INSTANCE.adapt(action, context);
		};
	}
}
