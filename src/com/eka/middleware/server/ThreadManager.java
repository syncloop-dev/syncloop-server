package com.eka.middleware.server;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.beust.jcommander.internal.Lists;
import com.eka.middleware.licensing.License;
import com.eka.middleware.licensing.LicenseFile;
import com.eka.middleware.logging.AppLogger;
import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.FlowMeta;
import com.google.common.collect.Maps;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.eka.middleware.auth.AuthAccount;
import com.eka.middleware.auth.ResourceAuthenticator;
import com.eka.middleware.auth.Security;
import com.eka.middleware.auth.UserProfileManager;
import com.eka.middleware.auth.manager.AuthorizationRequest;
import com.eka.middleware.auth.manager.JWT;
import com.eka.middleware.service.RuntimePipeline;
import com.eka.middleware.service.ServiceUtils;
import com.eka.middleware.template.MultiPart;
import com.eka.middleware.template.SnippetException;
import com.eka.middleware.template.Tenant;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
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
			exchange.getResponseSender().send("Could not fetch the profile for the active session");
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

				boolean isPathAllow = Security.isPublic(pureRequestPath, tenantName);

				AppLogger appLogger = new AppLogger(tenantName);
				appLogger.add("OPERATION", "UI_CROSS_COMM");
				appLogger.add("USER_ID", "ANONYMOUS");
				appLogger.add("PURE_REQUEST_PATH", pureRequestPath);
				appLogger.add("IS_PATH_ALLOW", isPathAllow);
				appLogger.finish();

				if (!isPathAllow) {
					LOGGER.info("User(" + account.getUserId() + ") active tenant mismatch or path not public. Make sure you are using right tenant name('"+tenantName+"'). Name is case sensitive. Clear your cookies retry with correct tenant name.");
					exchange.getResponseHeaders().clear();
					exchange.setStatusCode(401);
					exchange.getResponseHeaders().put(io.undertow.util.Headers.CONTENT_TYPE, "text/html"); // Change "text/plain" to your desired content type
					exchange.getResponseSender()
							.send("Tenant Access Denied. Path access not allowed. <br /><a href='/'>Click Here</a> to login again. <script>\n" +
									"\tfunction deleteAllCookies() {\n" +
									"\t\tconst cookies = document.cookie.split(\";\");\n" +
									"\n" +
									"\t\tfor (let i = 0; i < cookies.length; i++) {\n" +
									"\t\t\tconst cookie = cookies[i];\n" +
									"\t\t\tconst eqPos = cookie.indexOf(\"=\");\n" +
									"\t\t\tconst name = eqPos > -1 ? cookie.substr(0, eqPos) : cookie;\n" +
									"\t\t\tdocument.cookie = `${name}=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;`;\n" +
									"\t\t}\n" +
									"\t}\n" +
									"\tdeleteAllCookies();\n" +
									"</script>\n" /*+ pureRequestPath
									+ "\nPublic prefix paths:\n" + Security.getPublicPrefixPaths(tenantName)
									+ "\nPublic exact paths:\n" + Security.getPublicExactPaths(tenantName)*/);


					exchange.endExchange();
					return;
				}

				account.getAuthProfile().put("tenant", tenantName);
				List<String> groups = new ArrayList<String>();
//				groups.add("administrators");
				groups.add("guest");
				account.getAuthProfile().put("groups", groups);
				if(!ServiceUtils.isApiCall(exchange))
					exchange.setResponseCookie(cookie);

			} else {
				if (tenantName != null && account.getAuthProfile() != null
						&& account.getAuthProfile().get("tenant") != null
						&& !((String) account.getAuthProfile().get("tenant")).equalsIgnoreCase(tenantName)) {
					LOGGER.info("User(" + account.getUserId() + ") active tenant mismatch");
					String profileTenantName = (String) account.getAuthProfile().get("tenant");
					exchange.getResponseHeaders().clear();
					String tntName=(String) account.getAuthProfile().get("tenant");
					String token=JWT.generate(exchange);
					cookie.setValue(tntName+" "+token);
					if(!ServiceUtils.isApiCall(exchange))
						exchange.setResponseCookie(cookie);
					String tenantPath="/tenant/" + profileTenantName + pureRequestPath;
					ServiceUtils.redirectRequest(exchange, tenantPath);
					//exchange.getResponseHeaders().put(Headers.STATUS, 400);
					//exchange.getResponseHeaders().add(Headers.CONTENT_TYPE, "text/html; charset=utf-8");

					//exchange.getResponseSender().send("<html><body><a href='/tenant/" + profileTenantName + pureRequestPath
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
					exchange.getResponseHeaders().clear();

					String content = AuthorizationRequest.getContent(exchange, requestPath.toUpperCase());

					if (content != null) {
						exchange.getResponseHeaders().add(Headers.STATUS, 200);
						exchange.getResponseSender().send(content);
					} else {
						exchange.getResponseHeaders().add(Headers.STATUS, 404);
						exchange.getResponseSender()
								.send("{\"status\": \"404\", \"message\": \"URL not found\"}");
					}
					return;
				}

				try {
					exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
					logTransaction = exchange.getQueryParameters().containsKey("logTransaction");

					String uuid = UUID.randomUUID().toString();

					rp = RuntimePipeline.create(tenant, uuid, null, exchange, resource, requestPath);

					boolean isInLicense = true || ResourceAuthenticator.isEnterpriseLicenseValid(rp.dataPipeLine);
					if (!isInLicense) {
						LicenseFile licenseFile = License.getLicenseFile(rp.dataPipeLine);
						exchange.getResponseHeaders().clear();
						exchange.getResponseHeaders().put(Headers.STATUS, 401);
						exchange.getResponseSender().send(String.format("Your Enterprise License \"%s\" has expired on %s.", licenseFile.getLicenseName(),
								licenseFile.getExpiry()));
						return ;
					}
					boolean isAllowed = ResourceAuthenticator.isConsumerAllowed(resource, account, requestPath, method);

					if (!isAllowed && "default".equals(account.getAuthProfile().get("tenant")) && !account.getUserId().equalsIgnoreCase("anonymous")) {
						exchange.getResponseHeaders().clear();
						exchange.setStatusCode(StatusCodes.FOUND);
						String newUserPath = Security.gerDefaultNewUserPath(tenantName);
						if (newUserPath == null)
							newUserPath = Security.defaultTenantPage;
						exchange.getResponseHeaders().put(Headers.LOCATION, "/tenant/"+tenantName+newUserPath);
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
						exchange.getResponseHeaders().clear();
						exchange.getResponseHeaders().put(Headers.STATUS, 400);
						exchange.getResponseSender().send("Access Denied.");
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
								//map = ServiceUtils.xmlToMap(String.format("<root>%s</root>", content));
                                map = ServiceUtils.xmlToMapParser(String.format("<root>%s</root>", content));
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
					exchange.getResponseHeaders().put(HttpString.tryFromString("Access-Control-Allow-Origin"), "*");// new
					exchange.getResponseHeaders().put(HttpString.tryFromString("Access-Control-Allow-Methods"), "*");// new
					exchange.getResponseHeaders().put(HttpString.tryFromString("Access-Control-Allow-Headers"), "*");// new
																													// HttpString("Access-Control-Allow-Origin"),
																													// "*");
					exchange.getResponseHeaders().put(HttpString.tryFromString("X-Frame-Options"), "SAMEORIGIN");
					exchange.getResponseHeaders().put(HttpString.tryFromString("X-Xss-Protection"), "0");
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
							exchange.getResponseHeaders().put(HttpString.tryFromString("Content-Type"),
									"application/xml");
						} else if (acceptHeader.toLowerCase().contains("yaml")) {
							responsePayload = rp.dataPipeLine.toYaml();
							exchange.getResponseHeaders().put(HttpString.tryFromString("Content-Type"),
									"application/x-yaml");
						} else {
							responsePayload = rp.dataPipeLine.toJson();
							exchange.getResponseHeaders().put(HttpString.tryFromString("Content-Type"),
									"application/json");
						}
						final String resPayload = responsePayload;
						// final RuntimePipeline rpf = rp;
						exchange.getResponseSender().send(resPayload);

						/*
						 * ExecutorService threadpool = Executors.newCachedThreadPool();
						 *
						 * @SuppressWarnings("unchecked") Future<Long> futureTask = (Future<Long>)
						 * threadpool.submit(() -> { exchange.startBlocking();
						 * exchange.getResponseSender().send(resPayload);
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
					/*exchange.getResponseSender()
							.send("RequestId: " + rp.getSessionID() + "\nInternal Server error:-\n" + e.getMessage());
					LOGGER.info(ServiceUtils.getFormattedLogLine(rp.getSessionID(), requestAddress, "Error"));*/

					String requestId = rp.getSessionID();
					String errorName = "Internal Server error";
					String errorDetail = e.getMessage();
					List<FlowMeta> errorStack = e.getErrorStack();

					Map<String, Object> errorMap = Maps.newHashMap();
					errorMap.put("request_id", requestId);
					errorMap.put("error_name", errorName);
					errorMap.put("error_detail", errorDetail);

					List<String> errorTrace = Lists.newArrayList();
					for (FlowMeta flowMeta: errorStack) {
						errorTrace.add(String.format("%s(%s:%s)", flowMeta.getName(), flowMeta.getResource(), flowMeta.getGuid()));
					}

					errorMap.put("stackTrace", errorTrace);

					exchange.setStatusCode(500);

					String acceptHeader = exchange.getRequestHeaders().getFirst("Accept");
					if (acceptHeader != null && acceptHeader.toLowerCase().contains("xml"))  { //JSON CASE
						exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/xml");

						String xmlError = null;
						try {
							xmlError = ServiceUtils.toXml(errorMap, "error");
						} catch (Exception ex) {
							xmlError = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
									"<root>\n" +
									"    <error>\n" +
									"        <request_id>" + requestId + "</request_id>\n" +
									"        <error_name>" + errorName + "</error_name>\n" +
									"        <error_detail>" + errorDetail + errorStack + "</error_detail>\n" +
									"    </error>\n" +
									"</root>";
						}

						exchange.getResponseSender().send(xmlError);
					} else {
						exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");

						String jsonError = null;
						try {
							Map<String, Object> error = Maps.newHashMap();
							error.put("error", errorMap);
							jsonError = ServiceUtils.toJson(error);
						} catch (Exception ex) {
							jsonError = String.format(
									"{\"error\": {\"request_id\": \"%s\", \"error_name\": \"%s\", \"error_detail\": \"%s\"}}",
									requestId, errorName, errorDetail
							);
						}
						exchange.getResponseSender().send(jsonError);
					}

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
			exchange.getResponseHeaders().clear();
			exchange.getResponseSender().send("Access denied");
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
				exchange.getResponseHeaders().clear();
				exchange.setStatusCode(StatusCodes.FOUND);
				exchange.getResponseHeaders().put(Headers.LOCATION,
						"/tenant/" + tenantName + rqp + "?" + exchange.getQueryString());
				cookie = new CookieImpl("tenant", tenantName);
				exchange.setResponseCookie(cookie);
			}*/
		}
	}

}
