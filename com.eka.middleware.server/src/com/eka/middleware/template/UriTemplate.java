package com.eka.middleware.template;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.undertow.util.PathTemplate;

public class UriTemplate {

	public static Logger LOGGER = LogManager.getLogger(UriTemplate.class);
	
	/**
	 * Construct a new {@link UriTemplate} with the given URI String.
	 * 
	 * @param uriTemplate
	 *            the URI template string
	 */
	
	final PathTemplate template;
	public UriTemplate(String uriTemplate) {
//		LOGGER.info("uriTemplate in '/"+uriTemplate+"'");
		template = PathTemplate.create("/"+uriTemplate);
	}

	public Map<String, String> matcher(String uri) {
//		LOGGER.info("Request in '/"+uri+"'");
		Map<String, String> result = new HashMap<String, String>();
		template.matches("/"+uri, result);
		return result;
	}

	public static void main(String[] args) throws Exception {
		// UriTemplate template = new UriTemplate("http://feeling");
		//
		// if (template.toString().equalsIgnoreCase("http://feeling")) {
		// System.out.println("Yes, we have a match.");
		// } else {
		// Map results = template.match("http://feeling");
		// System.out.println("Result size: " + results.size());
		//
		// }
		
		//TEST
//		UriTemplate template1 = new UriTemplate("/middleware/packages/{packageName}/reload");
//		Map results1 = template1.matcher("/middleware/packages/com.github.test/reload");
//		System.out.println("Result size: " + results1.size());
//		System.out.println("Results: " + results1);

//		System.out.println("Done");
		InetAddress localhost = InetAddress.getLocalHost();
        //System.out.println("System IP Address : " + (localhost.getHostAddress()).trim());
	}
}
