package com.eka.middleware.service;

import java.io.BufferedWriter;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import com.eka.middleware.ext.spec.HttpServerExchange;
import com.eka.middleware.ext.spec.SecurityContext;
import com.eka.middleware.ext.spec.Tenant;
import com.eka.middleware.ext.spec.UserProfile;
import com.eka.middleware.logging.KeyLogger;
import com.eka.middleware.pooling.ScriptEngineContextManager;
import com.eka.middleware.template.SnippetException;


public class RuntimePipeline {
	private static final Map<String, RuntimePipeline> pipelines = new ConcurrentHashMap<String, RuntimePipeline>();
	private final String sessionId;
	private final String correlationId;
	private final HttpServerExchange exchange;
	private boolean isDestroyed = false;
	private Thread currentThread;
	private BufferedWriter bw = null;
	private Tenant tenant=null;
	private String user=null;
	private Date createDate=null;
	public final Map<String, Object> payload = new ConcurrentHashMap<String, Object>();

	private final ThreadPoolExecutor executor;

	public ThreadPoolExecutor getExecutor() {
		return executor;
	}

	public boolean isExchangeInitialized() {
		if(exchange==null)
			return false;
		return true;
	}

	public HttpServerExchange getExchange() throws SnippetException {
		if (exchange == null)
			throw new SnippetException(dataPipeLine,
					"RuntimePipeline was not created through direct HTTP request. This can happen if it's restored from file or propagated through messaging service like JMS or KAFKA",
					new Exception("Exchange not initialized."));
		return exchange;
	}

	public void writeSnapshot(String resource, String json) {
		String packagePath=PropertyManager.getPackagePath(getTenant());
		try {
			if (bw == null) {
				String name = sessionId + ".snap";
				File file = new File(packagePath + "/snapshots/" + resource + "/" + name);
				file.getParentFile().mkdirs();
				file.createNewFile();
				BufferedWriter bufferedWriter = Files.newBufferedWriter(Path.of(file.toURI()));
				bw = bufferedWriter;
				bw.write("[");
			}
			bw.write(json+",");
			bw.newLine();
		} catch (Exception e) {
			ServiceUtils.printException(getTenant(),"Exception while saving snapshot.", e);
		}
	}
	
	public void redirectRequest(String path) throws SnippetException {
		HttpServerExchange exchange=getExchange();
		if(exchange==null)
			return;
		ServiceUtils.redirectRequest(exchange, path);
	}

	public boolean isDestroyed() {
		return isDestroyed;
	}

	public void setDestroyed(boolean isDestroyed) {
		this.isDestroyed = isDestroyed;
	}

	public final DataPipeline dataPipeLine;

	public String getSessionID() {
		return sessionId;
	}
	protected final KeyLogger keyLogger;

	public RuntimePipeline(Tenant tenant, String requestId, String correlationId, final HttpServerExchange exchange, String resource,
						   String urlPath) {
		//Securitycont

		executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
		this.tenant=tenant;
		currentThread = Thread.currentThread();
		sessionId = requestId;
		this.exchange = exchange;
		if (correlationId == null)
			correlationId = requestId;
		this.correlationId = correlationId;
		setCreateDate(new Date());
		try {
			setUser(getCurrentLoggedInUserProfile().getAttribute("email")+" | "+getCurrentLoggedInUserProfile().getUsername()+" | "+getCurrentLoggedInUserProfile().getId());
		} catch (Exception e) {
			setUser("System");
		}
		dataPipeLine = new DataPipeline(this, resource, urlPath);
		keyLogger = new KeyLogger(dataPipeLine);
	}

	public UserProfile getCurrentLoggedInUserProfile() throws SnippetException {
		if(!isExchangeInitialized())
			return UserProfile.SYSTEM_PROFILE;
		return exchange.getCurrentLoggedInUserProfile();
	}

	public void logOut() throws SnippetException {
		final SecurityContext context = getExchange().getSecurityContext();
		// ServletRequestContext servletRequestContext =
		// getExchange().getAttachment(ServletRequestContext.ATTACHMENT_KEY);
		clearSession();
		// SessionManager.ATTACHMENT_KEY.
		context.logout();
	}

	private void clearSession() {
		exchange.clearSession();
	}

	public static RuntimePipeline create(Tenant tenant,String requestId, String correlationId, final HttpServerExchange exchange,
										 String resource, String urlPath) {
		// String md5=ServiceUtils.generateMD5(requestId+""+System.nanoTime());
		RuntimePipeline rp = pipelines.get(requestId);
		if (rp == null) {
			rp = new RuntimePipeline(tenant,requestId, correlationId, exchange, resource, urlPath);
			pipelines.put(requestId, rp);
		}else {
			ServiceUtils.printException(tenant, "Unable to create unique runtime pipeline", null);
			return null;
		}
		return rp;
	}

	public static RuntimePipeline getPipeline(String id) {
		RuntimePipeline rp = pipelines.get(id);
		return rp;
	}

	public static List<RuntimePipeline> listActivePipelines() {
		List<RuntimePipeline> list = new ArrayList<>();
		Set<Entry<String, RuntimePipeline>>  rtSet=pipelines.entrySet();
		for (Entry<String, RuntimePipeline> entry : rtSet) {
			list.add(entry.getValue());
		}
		return list;
	}

	public void destroy() {
		if(bw!=null) try{
			bw.write("]");
			bw.flush();
			bw.close();
			bw=null;
		}catch (Exception e) {
			ServiceUtils.printException(getTenant(),"Exception while closing snapshot file.", e);
		}
		RuntimePipeline rtp = pipelines.get(sessionId);
		ScriptEngineContextManager.removeContext(dataPipeLine.getUniqueThreadName());
		ScriptEngineContextManager.removeContext(rtp.dataPipeLine.getUniqueThreadName());
		rtp.currentThread.interrupt();
		rtp.setDestroyed(true);
		pipelines.get(sessionId).payload.clear();
		pipelines.remove(sessionId);
		keyLogger.finish();
		executor.shutdown();
	}

	public static void destroy(String sessionId) {
		pipelines.get(sessionId).destroy();
	}

	public String getCorrelationId() {
		return correlationId;
	}

	public Tenant getTenant() {
		return tenant;
	}

	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	
}
