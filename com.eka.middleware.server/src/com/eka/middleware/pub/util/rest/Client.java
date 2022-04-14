package com.eka.middleware.pub.util.rest;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.net.ssl.HostnameVerifier;

import org.apache.commons.io.IOUtils;

public class Client {
	static HostnameVerifier allHostsValid = null;
	private static final HttpClient.Builder clientBuilder = HttpClient.newBuilder();
	private static final HttpClient httpClient = clientBuilder.version(HttpClient.Version.HTTP_1_1)
			.connectTimeout(Duration.ofSeconds(10)).build();

	public static Map<String, Object> invokeREST(int itr, String url, String method, Map<String, String> headers,
			Map<String, Object> formData, String payload, File binary, String basicAuthUser, String basicAuthPass,
			Map<String, Object> respHandling, byte[] payloadBytes, InputStream payloadIS) throws Exception {

		String contentType = null;
		Boolean enableStreaming = false;
		if (respHandling != null) {
			contentType = (String) respHandling.get("contentType");
			if(respHandling.get("enableStreaming")!=null)
			enableStreaming = (boolean) respHandling.get("enableStreaming");
		}

		// System.out.println(" ****************************************");
		HttpRequest.Builder requestBuilder = HttpRequest.newBuilder();
		HttpRequest request = null;// HttpRequest.newBuilder().method(method.toUpperCase());
		method = method.toUpperCase();
		// System.setProperty("jdk.httpclient.allowRestrictedHeaders", "Connection");
		requestBuilder.uri(URI.create(url));
		//headers.remove("Connection");
		//headers.remove("Host");
		//headers.remove("Content-Length");
		headers.forEach((k, v) -> requestBuilder.setHeader(k, v));
		// requestBuilder.setHeader("Transfer-Encoding", "chunked");
		// System.out.println(" ****************************************");
		switch (method) {
		case "GET":
			requestBuilder.GET();
			break;
		case "POST":
			if (formData != null) {
				requestBuilder.POST(addFormData(formData, headers));
			} else if (payload != null)
				requestBuilder.POST(HttpRequest.BodyPublishers.ofString(payload));
			else if (payloadBytes != null)
				requestBuilder.POST(HttpRequest.BodyPublishers.ofByteArray(payloadBytes));
			else if (payloadIS != null) {
				byte[] bytes = IOUtils.toByteArray(payloadIS);
				requestBuilder.POST(HttpRequest.BodyPublishers.ofByteArray(bytes));
			} else
				requestBuilder.POST(HttpRequest.BodyPublishers.noBody());
			break;
		case "DELETE":
			requestBuilder.DELETE();
			break;
		case "PUT":
			if (formData != null) {
				requestBuilder.PUT(addFormData(formData, headers));
			} else if (payload != null)
				requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(payload));
			else if (payloadBytes != null) {
				requestBuilder.PUT(HttpRequest.BodyPublishers.ofByteArray(payloadBytes));
				// requestBuilder.setHeader("Content-Length", payloadBytes.length+"");
				// requestBuilder.setHeader("connection", "keep-alive");
			} else if (payloadIS != null) {
				byte[] bytes = IOUtils.toByteArray(payloadIS);
				requestBuilder.PUT(HttpRequest.BodyPublishers.ofByteArray(bytes));
			} else
				requestBuilder.PUT(HttpRequest.BodyPublishers.noBody());
			break;
		}

		if (basicAuthUser != null)
			addBasicAuth(clientBuilder, basicAuthUser, basicAuthPass);

		HttpResponse response = null;
		request = requestBuilder.build();
		// System.out.println(" ****************************************");
		// System.out.println(request.headers().map()+"
		// ****************************************");
		if (enableStreaming)
			response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
		else if ("bytes".equals(contentType))
			response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
		else
			response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

		HttpHeaders respheaders = response.headers();
		/*
		 * for (Map.Entry<String, String> entrySet : headers.entrySet()) {
		 * System.out.println(entrySet.getKey() + " = " + entrySet.getValue()); }
		 */
		headers.clear();
		respheaders.map().forEach((k, v) -> headers.put(k, v.toString()));
		
		Map<String, Object> respMap = new HashMap<String, Object>();
		respMap.put("statusCode",response.statusCode()+"");
		if (enableStreaming)
			respMap.put("inputStream", response.body());
		else if ("bytes".equals(contentType)) {
			respMap.put("bytes", response.body());
		} else
			respMap.put("respPayload", response.body());
		
		return respMap;
	}

	private static void addBasicAuth(HttpClient.Builder clientBuilder, String username, String password) {
		clientBuilder.authenticator(new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(username, password.toCharArray());
			}
		});
	}

	private static HttpRequest.BodyPublisher addFormData(Map<String, Object> data, Map<String, String> headers)
			throws Exception {
		var builder = new StringBuilder();
		String boundary = new BigInteger(35, new java.util.Random()).toString();
		headers.put("Content-Type", "multipart/form-data;boundary=" + boundary);
		byte[] separator = ("--" + boundary + "\r\nContent-Disposition: form-data; name=")
				.getBytes(StandardCharsets.UTF_8);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		for (Map.Entry<String, Object> entry : data.entrySet()) {
			baos.write(separator);
			if (builder.length() > 0)
				baos.write("&".getBytes(StandardCharsets.UTF_8));
			if (entry.getValue() instanceof File) {
				File file = (File) entry.getValue();
				var path = Path.of(file.toURI());
				String fileName = file.getName();
				String mimeType = Files.probeContentType(path);
				baos.write(("\"" + entry.getKey() + "\"; filename=\"" + fileName + "\"\r\nContent-Type: " + mimeType
						+ "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
				baos.write(Files.readAllBytes(path));
				baos.write("\r\n".getBytes(StandardCharsets.UTF_8));
			} else {
				baos.write(("\"" + entry.getKey() + "\"\r\n\r\n" + entry.getValue() + "\r\n")
						.getBytes(StandardCharsets.UTF_8));
//	      baos.write(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8));
//	      baos.write("=".getBytes(StandardCharsets.UTF_8));
//	      baos.write(URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8));
			}
		}
		baos.write(("--" + boundary + "--").getBytes(StandardCharsets.UTF_8));
		HttpRequest.BodyPublisher bp = HttpRequest.BodyPublishers.ofByteArray(baos.toByteArray());
		baos.close();
		return bp;
	}
}
