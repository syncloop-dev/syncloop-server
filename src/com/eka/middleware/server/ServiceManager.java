package com.eka.middleware.server;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import com.eka.middleware.heap.CacheManager;
import com.eka.middleware.pooling.ScriptEngineContextManager;
import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.PropertyManager;
import com.eka.middleware.service.RTCompile;
import com.eka.middleware.service.ServiceUtils;
import com.eka.middleware.template.SnippetException;

public class ServiceManager {

	private static final Map<String, Class> classMap = new ConcurrentHashMap<String, Class>();

	private static final Map<String, Long> lastModified = new ConcurrentHashMap<String, Long>();

	// public static final String packagePath =
	// ServiceUtils.getServerProperty("middleware.server.home.dir");

	private static String getClassFilePath(final String fqn, final DataPipeline dataPipeLine) {
		String expandFQN[] = fqn.split(Pattern.quote("."));
		String funcName = expandFQN[expandFQN.length - 1];
		String className = expandFQN[expandFQN.length - 2];
		String classFile = fqn.replace(".", "/").replace(className + "/" + funcName, className + ".java");
		// String fqnClass = classFile.replace(".java", "").replace("/", ".");
		return classFile;
	}

	private static File getTenantFile(final DataPipeline dataPipeLine, String classFile) throws SnippetException {
		URI path = null;
		final String packagePath = PropertyManager.getPackagePath(dataPipeLine.rp.getTenant());
		try {

			File uriFile = new File((packagePath + classFile).replace("//", "/"));
			path = uriFile.toURI();

		} catch (Exception e) {
			throw new SnippetException(dataPipeLine, e.getMessage() + "\n Path: " + packagePath + classFile, e);
		}

		File file = new File(path);
		if (!file.exists()) {
			throw new SnippetException(dataPipeLine, "Resource not found\n" + file.getAbsolutePath(),
					new Exception("Resource not found\n" + file.getAbsolutePath()));
		}

		return file;
	}

	public static Class compileJava(final String fqn, final DataPipeline dataPipeLine) throws Throwable {

		String classFile = getClassFilePath(fqn, dataPipeLine);
		String fqnClass = classFile.replace(".java", "").replace("/", ".");

		final String tid = dataPipeLine.rp.getTenant().getID();

		File file = getTenantFile(dataPipeLine, classFile);

		String classID = tid + "-" + fqn;// + "." + lastChangedTime;
		Class cls = classMap.get(classID);
		cls = RTCompile.getClassRef(fqnClass, file.getAbsolutePath(), true, dataPipeLine);
		lastModified.put(classID, file.lastModified());
		classMap.put(classID, cls);
		return cls;
	}

	public static void reload(final String fqn, final DataPipeline dataPipeLine, boolean forceJavaCompile) {
		final String tid = dataPipeLine.rp.getTenant().getID();
		String classID = tid + "-" + fqn;// + "." + lastChangedTime;
		if (forceJavaCompile) {
			try {
				compileJava(fqn, dataPipeLine);
			} catch (Throwable e) {
				ServiceUtils.printException(dataPipeLine, "Re-compilation failed", new Exception(e));
			}
		} else
			classMap.remove(classID);
	}

	private static Object obj = new Object();

	public static void invokeJavaMethod(final String fqn, final DataPipeline dataPipeLine) throws SnippetException {
		String classFile = getClassFilePath(fqn, dataPipeLine);
		File file = getTenantFile(dataPipeLine, classFile);
		long lastChangedTime = file.lastModified();
		final String tid = dataPipeLine.rp.getTenant().getID();
		String classID = tid + "-" + fqn;// + "." + lastChangedTime;
		Long lastChangedTimeSaved = lastModified.get(classID);
		Class cls = classMap.get(classID);
		Map<String, Object> chache=CacheManager.getCacheAsMap(dataPipeLine.rp.getTenant());
		Boolean resetEnabled=(Boolean)chache.get("ekamw.promote.runtime.service.reload");
		if (cls == null || (lastChangedTimeSaved==null || (lastChangedTimeSaved < lastChangedTime && (resetEnabled==null || resetEnabled))))
			synchronized (obj) {
				lastChangedTimeSaved = lastModified.get(classID);
				if (null == cls || lastChangedTimeSaved==null || lastChangedTimeSaved < lastChangedTime) {
					try {
						ScriptEngineContextManager.clear();
						cls = compileJava(fqn, dataPipeLine);
					} catch (Throwable e) {
						e.printStackTrace();
						throw new SnippetException(dataPipeLine, "Error during compilation", new Exception(e));
					}
				}
			}
		String expandFQN[] = fqn.split(Pattern.quote("."));
		String funcName = expandFQN[expandFQN.length - 1];
		try {

			Method method = cls.getMethod(funcName, DataPipeline.class);// cls.getMethods();// ;
			if (method != null) {
				try {
					method.invoke(null, dataPipeLine);
				} catch (InvocationTargetException e) {
					Exception ex = null;
					if (fqn.contains("packages.middleware.pub.service.exitRepeat"))
						ex = new Exception("packages.middleware.pub.service.exitRepeat");
					else
						ex = new Exception(e.getCause());
					throw ex;
				}
			} else
				throw new Exception("Method not found '" + funcName + "'");
		} catch (Exception e) {
			if (e.getMessage().contains("packages.middleware.pub.service.exitRepeat")) {
				throw new SnippetException(dataPipeLine, e.getMessage(), e);
			} else if (e instanceof SnippetException)
				throw (SnippetException) e;
			else if (e.getCause() instanceof SnippetException)
				throw (SnippetException) e.getCause();
			else
				throw new SnippetException(dataPipeLine, "Service error : " + fqn, e);
		}
	}
}
