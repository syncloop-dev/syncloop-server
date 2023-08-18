package com.eka.middleware.server;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.eka.middleware.auth.AuthAccount;
import com.eka.middleware.auth.ResourceAuthenticator;
import com.eka.middleware.auth.Security;
import com.eka.middleware.auth.UserProfileManager;
import com.eka.middleware.auth.manager.JWT;
import com.eka.middleware.ext.spec.Cookie;
import com.eka.middleware.ext.spec.HttpServerExchange;
import com.eka.middleware.ext.spec.Tenant;
import com.eka.middleware.service.RuntimePipeline;
import com.eka.middleware.service.ServiceUtils;
import com.eka.middleware.template.MultiPart;
import com.eka.middleware.template.SnippetException;

import io.undertow.util.StatusCodes;

public class ThreadManager {

	public static Logger LOGGER = LogManager.getLogger(MiddlewareServer.class);
	
	public static final void processRequest(final HttpServerExchange exchange) {
		String tenantName=ServiceUtils.setupRequestPath(exchange);
		Cookie cookie = ServiceUtils.setupCookie(exchange, tenantName, null);
		String rqp = exchange.getRequestPath();
		processRequest(exchange, cookie);
	}

	public static final void processRequest(final HttpServerExchange exchange,final Cookie cookie) {
		//final Cookie cookie = ServiceUtils.setupCookie(exchange, null, null);
		
		//if()
		final String tenantName=ServiceUtils.getTenantName(cookie);
		String requestAddress = exchange.toString() + "@" + Integer.toHexString(System.identityHashCode(exchange));
		final String method = exchange.getRequestMethod().toString();
		String pureRequestPath = exchange.getRequestPath();
		String requestPath = method + pureRequestPath;
		AuthAccount account = null;
		try {
			account = UserProfileManager.getUserProfileManager()
					.getAccount(ServiceUtils.getCurrentLoggedInUserProfile(exchange));
		} catch (SnippetException e1) {
			exchange.send("Could not fetch the profile for the active session");
			LOGGER.info(ServiceUtils.getFormattedLogLine(exchange.getRequestPath(), requestAddress, "Error"));
		}
		if (account != null) {
			String rsrcTokens[] = requestPath.split("/");
			//String tenantName = null;
			if ("tenant".equalsIgnoreCase(rsrcTokens[1])) {
				//tenantName = rsrcTokens[2];
				requestPath = requestPath.replace("/" + rsrcTokens[1] + "/" + rsrcTokens[2], "");
				pureRequestPath = pureRequestPath.replace("/" + rsrcTokens[1] + "/" + rsrcTokens[2], "");
			}

			if (account.getUserId().equalsIgnoreCase("anonymous")) {
				if (!Security.isPublic(pureRequestPath, tenantName)) {
					LOGGER.info("User(" + account.getUserId() + ") active tenant mismatch or path not public. Make sure you are using right tenant name('"+tenantName+"'). Name is case sensitive. Clear your cookies retry with correct tenant name.");
					exchange.clearResponseHeaders();
					exchange.setStatusCode(401);
					exchange
							.send("Tenant Access Denied. Path access not allowed." /*+ pureRequestPath
									+ "\nPublic prefix paths:\n" + Security.getPublicPrefixPaths(tenantName)
									+ "\nPublic exact paths:\n" + Security.getPublicExactPaths(tenantName)*/);
					exchange.endExchange();
					return;
				}

				account.getAuthProfile().put("tenant", tenantName);
				List<String> groups = new ArrayList<String>();
//				groups.add("administrators");
				groups.add("guest");
				groups.add("default");
				account.getAuthProfile().put("groups", groups);
				if(!ServiceUtils.isApiCall(exchange))
					exchange.setResponseCookie(cookie);

			} else {
				if (tenantName != null && account.getAuthProfile() != null
						&& account.getAuthProfile().get("tenant") != null
						&& !((String) account.getAuthProfile().get("tenant")).equalsIgnoreCase(tenantName)) {
					LOGGER.info("User(" + account.getUserId() + ") active tenant mismatch");
					String profileTenantName = (String) account.getAuthProfile().get("tenant");
					exchange.clearResponseHeaders();
					String tntName=(String) account.getAuthProfile().get("tenant");
					String token=JWT.generate(exchange);
					cookie.setValue(tntName+" "+token);
					if(!ServiceUtils.isApiCall(exchange))
						exchange.setResponseCookie(cookie);
					String tenantPath="/tenant/" + profileTenantName + pureRequestPath;
					ServiceUtils.redirectRequest(exchange, tenantPath);
					//exchange.getResponseHeaders().put(Headers.STATUS, 400);
					//exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, "text/html; charset=utf-8");
					
					//exchange.send("<html><body><a href='/tenant/" + profileTenantName + pureRequestPath
					//		+ "'>Re-direct to my workspace.</a><body></html>");
					exchange.endExchange();
					return;
				}
			}
			Tenant tenant = Tenant.getTenant(tenantName);
			RuntimePipeline rp = null;
			Boolean logTransaction = true;

			if (requestPath != null && requestPath.length() > 1) {
				String resource = null;
				Tenant.getTenant(tenantName);
				Map<String, Object> pathParams = new HashMap<String, Object>();
				pathParams.put("pathParameters", "");
				
				resource = ServiceUtils.getPathService(requestPath, pathParams, tenant);

				if (resource == null) {
					exchange.clearResponseHeaders();

					String content = exchange.getAuthorizationContent(requestPath); //AuthorizationRequest.getContent(exchange, requestPath.toUpperCase());

					if (content != null) {
						exchange.putResponseHeaders("STATUS", Long.toString(200));
						exchange.send(content);
					} else {
						exchange.putResponseHeaders("STATUS", Long.toString(404));
						exchange
								.send("Server is up and running but it could not find the resource.");
					}
					return;
				}
				
				try {
					exchange.putResponseHeaders("CONTENT_TYPE", "text/plain");
					logTransaction = exchange.getQueryParameters().containsKey("logTransaction");

					String uuid = UUID.randomUUID().toString();

					rp = RuntimePipeline.create(tenant, uuid, null, exchange, resource, requestPath);

					boolean isAllowed = false;
					isAllowed = ResourceAuthenticator.isConsumerAllowed(resource, account, requestPath, method);

					if (!isAllowed && "default".equals(account.getAuthProfile().get("tenant")) && !account.getUserId().equalsIgnoreCase("anonymous")) {
						exchange.clearResponseHeaders();
						exchange.setStatusCode(StatusCodes.FOUND);
						String newUserPath = Security.gerDefaultNewUserPath(tenantName);
						if (newUserPath == null)
							newUserPath = Security.defaultTenantPage;
						exchange.putResponseHeaders("LOCATION", "/tenant/"+tenantName+newUserPath);
						exchange.endExchange();
						return;
					}

					if (!isAllowed) {
						if (logTransaction == true)
							LOGGER.info(ServiceUtils.getFormattedLogLine(rp.getSessionID(), resource, "resource"));
						String userId = account.getUserId();
						if (logTransaction == true)
							LOGGER.info(ServiceUtils.getFormattedLogLine(rp.getSessionID(),
									"User(" + userId + ") is not in a consumer group.", "permission"));
						exchange.clearResponseHeaders();
						exchange.putResponseHeaders("STATUS", Long.toString(400));
						exchange.send("Access Denied.");
						return;
					}
					if (logTransaction == true) {
						LOGGER.info(ServiceUtils.getFormattedLogLine(rp.getSessionID(), resource, "resource"));
						LOGGER.info(ServiceUtils.getFormattedLogLine(rp.getSessionID(), requestAddress, "Started"));
						LOGGER.info(ServiceUtils.getFormattedLogLine(rp.getSessionID(), rp.getCorrelationId(),
								"correlationId"));
					}
					final RuntimePipeline rpf = rp;
					if (exchange.getQueryParameters() != null)
						exchange.getQueryParameters().forEach((k, v) -> {
							if (v != null) {
								if (k.endsWith("-b64")) {
									String key = k.split("-b64")[0];
									String value = new String(Base64.getDecoder().decode(v.getFirst()));
									Map<String, Object> map = ServiceUtils.jsonToMap("{\"root\":" + value + "}");
									rpf.payload.put(key, map.get("root"));
								} else if(v.size()==1)
									rpf.payload.put(k, v.getFirst());
								else {
									final List<String> list=new ArrayList<>();
									v.forEach(x->{list.add(x);});
									rpf.payload.put(k, list);
								}
							}
						});
					// rp.payload.putAll(exchange.getQueryParameters());// put("parameters",
					// exchange.getQueryParameters());
					rp.payload.put("*requestHeaders", ServiceUtils.extractHeaders(exchange));
					((Map) rp.payload.get("*requestHeaders")).put("Authorization", "********");
					rp.payload.put("*pathParameters", pathParams.get("pathParameters"));
					final String acceptHeader = (String) (ServiceUtils.getCaseInsensitiveKey(rp.dataPipeLine.getHeaders(), "Accept") == null ? "*/*"
							: ServiceUtils.getCaseInsensitiveKey(rp.dataPipeLine.getHeaders(), "Accept"));
					final String contentType = (String) ServiceUtils.getCaseInsensitiveKey(rp.dataPipeLine.getHeaders(), "Content-Type");
					Map map = null;
					byte body[] = null;
					String content = null;

					if (contentType != null) {
						switch (contentType.toLowerCase()) {
						case "application/json":
							body = rp.dataPipeLine.getBody();
							content = new String(body);
							if (content != null && content.trim().length() > 0)
								map = ServiceUtils.jsonToMap("{\"root\":" + content + "}");
							break;
						case "application/xml":
							body = rp.dataPipeLine.getBody();
							content = new String(body);
							if (content != null && content.trim().length() > 0)
								map = ServiceUtils.xmlToMap(String.format("<root>%s</root>", content));
							break;
						case "application/yaml":
							body = rp.dataPipeLine.getBody();
							content = new String(body);
							if (content != null && content.trim().length() > 0)
								map = ServiceUtils.yamlToMap(content);
							break;
						}
						if (map != null)
							rp.dataPipeLine.put("*payload", map.get("root"));
					}

					ServiceManager.invokeJavaMethod(resource, rp.dataPipeLine);
					exchange.putResponseHeaders("Access-Control-Allow-Origin", "*");// new
					exchange.putResponseHeaders("Access-Control-Allow-Methods", "*");// new
					exchange.putResponseHeaders("Access-Control-Allow-Headers", "*");// new
																													// HttpString("Access-Control-Allow-Origin"),
																													// "*");
					exchange.putResponseHeaders("X-Frame-Options", "SAMEORIGIN");
					exchange.putResponseHeaders("X-Xss-Protection", "0");
					if (rp.payload.get("*multiPart") != null) {
						try {
							MultiPart mp = (MultiPart) rp.payload.get("*multiPart");
							ServiceUtils.startStreaming(rp, mp);
							if (logTransaction == true)
								LOGGER.info(ServiceUtils.getFormattedLogLine(rp.getSessionID(), requestAddress,
										"Ended successfully"));
							rp.destroy();
							rp = null;
						} catch (Exception e) {
							throw new SnippetException(rp.dataPipeLine, "Sending stream failed\n" + e.getMessage(), e);
						}
					} else {
						// exchange.getRequestHeaders().
						String responsePayload = null;
						if (acceptHeader.toLowerCase().contains("xml")) {
							responsePayload = rp.dataPipeLine.toXml();
							exchange.putResponseHeaders("Content-Type",
									"application/xml");
						} else if (acceptHeader.toLowerCase().contains("yaml")) {
							responsePayload = rp.dataPipeLine.toYaml();
							exchange.putResponseHeaders("Content-Type",
									"application/x-yaml");
						} else {
							responsePayload = rp.dataPipeLine.toJson();
							exchange.putResponseHeaders("Content-Type",
									"application/json");
						}
						final String resPayload = responsePayload;
						// final RuntimePipeline rpf = rp;
						exchange.send(resPayload);

						/*
						 * ExecutorService threadpool = Executors.newCachedThreadPool();
						 * 
						 * @SuppressWarnings("unchecked") Future<Long> futureTask = (Future<Long>)
						 * threadpool.submit(() -> { exchange.startBlocking();
						 * exchange.send(resPayload);
						 * LOGGER.info(ServiceUtils.getFormattedLogLine(rpf.getSessionID(),
						 * requestAddress, "Ended successfully")); }); try { Thread.sleep(10); while
						 * (!futureTask.isDone()) Thread.sleep(100); } catch (InterruptedException e) {
						 * // TODO Auto-generated catch block e.printStackTrace(); }
						 * 
						 * threadpool.shutdown();
						 */
						rp.destroy();
						rp = null;
					}
				} catch (SnippetException e) {
					exchange
							.send("RequestId: " + rp.getSessionID() + "\nInternal Server error:-\n" + e.getMessage());
					LOGGER.info(ServiceUtils.getFormattedLogLine(rp.getSessionID(), requestAddress, "Error"));

				} finally {
					if (rp != null) {
						if (logTransaction == true)
							LOGGER.info(ServiceUtils.getFormattedLogLine(rp.getSessionID(), requestAddress, "Ended"));
						rp.destroy();
						rp = null;
					}
				}
			}
		} else {
			exchange.clearResponseHeaders();
			exchange.send("Access denied");
			/*
			
			String rqp = exchange.getRequestPath();
			if (rqp != null && !rqp.startsWith("/tenant/")) {
				Cookie cookie = exchange.getRequestCookie("tenant");
				if (cookie == null)
					cookie = new CookieImpl("tenant");
				String tenantName = cookie.getValue();
				if (tenantName == null || tenantName.trim().length() == 0)
					tenantName = "default";
				// cookie = new CookieImpl("tenant", tenantName);
				cookie.setValue(tenantName);
				exchange.setResponseCookie(cookie);
				exchange.clearResponseHeaders();
				exchange.setStatusCode(StatusCodes.FOUND);
				exchange.getResponseHeaders().put(Headers.LOCATION,
						"/tenant/" + tenantName + rqp + "?" + exchange.getQueryString());
				cookie = new CookieImpl("tenant", tenantName);
				exchange.setResponseCookie(cookie);
			}*/
		}
	}

}
