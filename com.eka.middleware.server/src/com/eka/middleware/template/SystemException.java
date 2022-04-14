package com.eka.middleware.template;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.eka.middleware.server.MiddlewareServer;
import com.eka.middleware.service.ServiceUtils;

public class SystemException extends Exception {
	public static Logger logger = LogManager.getLogger(SystemException.class);
	private static final Map<String, Object> errorMap = new HashMap<String, Object>();

	public SystemException(String code, Exception e) {
		super(e);
		if (errorMap.size()==0) {
			try {
				String errorFilePath = MiddlewareServer.getConfigFolderPath() + "/errors.json";
				URL url = new URL(errorFilePath);
				File file = new File(url.toURI());
				byte bytes[];
				bytes = ServiceUtils.readAllBytes(file);
				String json = new String(bytes);
				Map<String, Object> map = ServiceUtils.jsonToMap(json);
				errorMap.putAll(map);
			} catch (Exception er) {
				ServiceUtils.printException("Internal" + "    " + 0000 + "    " + "Error reading errors.json file", e);
			}
		}
		Map<String, Object> errMap=((Map<String, Object>)errorMap.get(code));
		if(errMap==null) {
			ServiceUtils.printException("Internal" + "    " + code + "    " + "Error is code not defined.", e);
		}else {
			String reason=errMap.get("reason").toString();
			ServiceUtils.printException("Internal" + "    " + code + "    " + reason, e);
		}
	}
}
