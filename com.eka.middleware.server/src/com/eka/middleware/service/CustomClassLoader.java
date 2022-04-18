package com.eka.middleware.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.eka.middleware.server.ServiceManager;
import com.eka.middleware.template.SnippetException;

public class CustomClassLoader extends ClassLoader {
	public CustomClassLoader(String fqn, String[] paths, ClassLoader parent) {
		if (paths != null && paths.length > 0)
			try {
				loadCustomJars(fqn, paths);
			} catch (Exception e) {
				ServiceUtils.printException("Loading custom jars failed: ", e);
			}
	}

	final Map<String, byte[]> jeMap = new HashMap<String, byte[]>();

	public void loadCustomJars(String fqn, String[] paths) {
		boolean isFailed = false;
		String className = null;
		try {
			List<String> classNames = new ArrayList<String>();
			LOGGER.trace("Loading jars for class '" + fqn + "'");

			for (String path : paths) {
				if (path != null && path.length() > 0) {
					final JarFile jar = new JarFile(new File(path));
					LOGGER.trace("Loading Classes from jar '" + path + "'");
					classNames.addAll(loadClassFromJar(jar));
				}
			}

			if (isFailed)
				LOGGER.info("Loading jars for class '" + fqn + "' completed with errors");
			else
				LOGGER.trace("Loading jars for class '" + fqn + "' completed successfully");
		} catch (Throwable e) {
			isFailed = true;
			ServiceUtils.printException("Failed to find dependent class '" + className + "'", new Exception(e));
		}
	}

	private List<String> loadClassFromJar(final JarFile jar) throws Exception {
		List<String> classNames = new ArrayList<String>();
		
		Enumeration<JarEntry> entries = jar.entries();
		while (entries.hasMoreElements()) {
			JarEntry nextElement = entries.nextElement();
			String name = nextElement.getName();
			// LOGGER.debug(name);
			InputStream is = null;
			ByteArrayOutputStream bao = null;
			try {
				if (name.endsWith(".class")) {
					bao = new ByteArrayOutputStream();
					is = jar.getInputStream(nextElement);
					IOUtils.copy(is, bao);
					bao.flush();
					byte[] jarBytes = bao.toByteArray();
					bao.close();
					is.close();
					bao = null;
					is = null;
					String clsName = nextElement.getName().replace("/", ".").replace(".class", "");
					if (jarBytes == null)
						LOGGER.info("Could not load class from jar '" + clsName);
					else {
						jeMap.put(clsName, jarBytes);
						classNames.add(clsName);
						// LOGGER.info("Loading class from jar '" + clsName);
					}
				}
			} finally {
				if (is != null)
					is.close();
				if (bao != null)
					bao.close();

			}
		}
		return classNames;
	}

	public static Logger LOGGER = LogManager.getLogger(CustomClassLoader.class);

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		// TODO Auto-generated method stub

		// if(name.split(Pattern.quote(".")).length>2)
		// return null;
		try {
			byte[] classBytes = null;

			if (jeMap != null && jeMap.size() > 0)
				classBytes = jeMap.get(name);
			if (classBytes == null) {
				LOGGER.trace("Loading class '" + name + "'");
				try {
					return Class.forName(name);
				} catch (Exception e) {
					return super.loadClass(name);
				}
			} else {
				LOGGER.trace("Loading class from bytes'" + name + "'");

				return defineClass(name, classBytes, 0, classBytes.length);

			}
		} catch (Exception e) {
			ServiceUtils.printException("Class Not found '" + name + "'", e);
			throw new ClassNotFoundException(name);
		}
	}

	@Override
	public Class findClass(String name) throws ClassNotFoundException {
		// System.out.println(name);
		byte[] b;
		try {
			b = loadClassFromFile(name);
			return defineClass(name, b, 0, b.length);
		} catch (SnippetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		throw new ClassNotFoundException(name);

	}

	private byte[] loadClassFromFile(String fileName) throws SnippetException {
		// System.out.println(fileName);

		fileName = fileName.replace('.', '/') + ".class";
		// System.out.println(fileName);
		URI path = null;
		try {
			path = new File(
					(ServiceManager.packagePath + fileName.replace("packages/packages", "packages")).replace("//", "/"))
							.toURI();// ServiceManager.class.getResource(fileName).toURI();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			ServiceUtils.printException("Failed to create URI '"
					+ (ServiceManager.packagePath + fileName.replace("packages/packages", "packages")).replace("//",
							"/")
					+ "'", e);
		}
		InputStream inputStream = null;
		File file = new File(path);
		byte[] buffer = null;
		try {
			inputStream = new FileInputStream(file);

			ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			int nextValue = 0;
			try {
				while ((nextValue = inputStream.read()) != -1) {
					byteStream.write(nextValue);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			buffer = byteStream.toByteArray();
			try {
				inputStream.close();
				inputStream = null;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				ServiceUtils.printException("Failed to close inputStream", e);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			ServiceUtils.printException("Class file not found '"
					+ (ServiceManager.packagePath + fileName.replace("packages/packages", "packages")).replace("//",
							"/")
					+ "'", e);
		} finally {
			// if(file!=null && file.exists())
			// file.delete();
			if (inputStream != null)
				try {
					inputStream.close();
				} catch (IOException e) {
					ServiceUtils.printException("Failed to close inputStream in finally block", e);
				}
		}
		return buffer;
	}
}
