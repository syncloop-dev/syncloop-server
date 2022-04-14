package com.eka.middleware.server;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.RTCompile;
import com.eka.middleware.service.ServiceUtils;
import com.eka.middleware.template.SnippetException;

public class ServiceManager {

	private static final Map<String, Class> classMap = new ConcurrentHashMap<String, Class>();

//	private static final Map<String, Long> lastModified = new ConcurrentHashMap<String, Long>();

	public static final String packagePath = ServiceUtils.getServerProperty("middleware.server.home.dir");

	
	public static Class compileJava(final String fqn, final DataPipeline dataPipeLine) throws Throwable{
		
		String expandFQN[] = fqn.split(Pattern.quote("."));
		String funcName = expandFQN[expandFQN.length - 1];
		String className = expandFQN[expandFQN.length - 2];
		String classFile = fqn.replace(".", "/").replace(className + "/" + funcName, className + ".java");
		String fqnClass = classFile.replace(".java", "").replace("/", ".");
		
		URI path = null;
		try {
			dataPipeLine.log((packagePath + classFile).replace("//", "/"));
			File uriFile=new File((packagePath + classFile).replace("//", "/"));
			path = uriFile.toURI(); 
			
			
		} catch (Exception e) {
			throw new SnippetException(dataPipeLine,
					e.getMessage()+"\n Path: " + packagePath + classFile, e);
		}

		File file = new File(path);
		if (!file.exists()) {
			throw new SnippetException(dataPipeLine,
					"Resource not found\n" + file.getAbsolutePath(), null);
		}
		String classID = fqn ;//+ "." + lastChangedTime;
		Class cls = classMap.get(classID);
		cls = RTCompile.getClassRef(fqnClass, file.getAbsolutePath(),true);
		classMap.put(classID, cls);
		return cls;
	}
	
	public static void invokeJavaMethod(final String fqn, final DataPipeline dataPipeLine) throws SnippetException {
		
		
//		Long lastChangedTimeSaved = lastModified.get(fqn);
//		long lastChangedTime = file.lastModified();
		String classID = fqn ;//+ "." + lastChangedTime;
		Class cls = classMap.get(classID);
		if(cls==null) synchronized (classMap){
			try {
				cls=compileJava(fqn, dataPipeLine);
			} catch (Throwable e) {
				throw new SnippetException(dataPipeLine, "Error during compilation", new Exception(e));
			}
		}
		String expandFQN[] = fqn.split(Pattern.quote("."));
		String funcName = expandFQN[expandFQN.length - 1];
		try {
/*			if (cls != null) {
				if (lastChangedTimeSaved != null) {
					
					if (lastChangedTime - lastChangedTimeSaved > 1) {
						synchronized (lastModified) {
							lastChangedTimeSaved = lastModified.get(fqn);
							if (lastChangedTime - lastChangedTimeSaved > 1) {
								cls = RTCompile.getClassRef(fqnClass, file.getAbsolutePath(),false);
								// System.out.println(sessionId);
								classMap.put(classID, cls);
								lastModified.put(fqn, lastChangedTime);
								classMap.remove(fqn + "." + lastChangedTimeSaved);
							}
						}
					}
				} else {
					synchronized (lastModified) {
						lastChangedTimeSaved = lastModified.get(fqn);
						if (lastChangedTimeSaved == null) {
							cls = RTCompile.getClassRef(fqnClass, file.getAbsolutePath(),false);// fqn.replace("."+funcName,
																							// "")
							classMap.put(classID, cls);
							lastModified.put(fqn, lastChangedTime);
						}
						// System.out.println(sessionId);
					}

				}
			} else {
				synchronized (lastModified) {
					if (cls == null) {
						cls = RTCompile.getClassRef(fqnClass, file.getAbsolutePath(),false);
						classMap.put(classID, cls);
						lastModified.put(fqn, lastChangedTime);
						// System.out.println(sessionId);
					}
				}

			}
*/
			
			Method method =cls.getMethod(funcName, DataPipeline.class);//cls.getMethods();// ;
//			Method func = null;
//			for (Method method : methods) {
//				if (method.getName().equals(funcName)) {
//					func = method;
//					break;
//				}
//			}
			if (method != null) {
				try {
					method.invoke(null, dataPipeLine);
				} catch (InvocationTargetException e) {
					Exception ex = new Exception(e.getCause());
					throw ex;
				}
				
			}
			else
				throw new Exception("Method not found '" + funcName + "'");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			throw new SnippetException(dataPipeLine,"Method '" + funcName + "' not found. " + e.getMessage(), e);
		}
	}
}
