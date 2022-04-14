package packages.middleware.pub.client.http;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.eka.middleware.pub.util.rest.Client;
import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.template.SnippetException;
import com.eka.middleware.pub.util.auth.AWSV4Auth;
import java.util.TreeMap;
public final class restv2{
	public static final void main(DataPipeline dataPipeline) throws SnippetException{
		try {
			String method = dataPipeline.getString("method");
			String url = dataPipeline.getString("url");
//            System.out.println(url);
			Map<String, String> reqHeaders = dataPipeline.getAsMap("headers");
			Map<String, String> urlParameters = dataPipeline.getAsMap("urlParameters");
			Map<String, String> pathParameters = dataPipeline.getAsMap("uriParameters");
			Map<String, Object> respHandling = dataPipeline.getAsMap("respHandling");
			Map<String, Map<String, String>> auth = dataPipeline.getAsMap("auth");
			Map<String, Object> formData = dataPipeline.getAsMap("formData");
			Map<String, Object> binaryData = dataPipeline.getAsMap("binaryData");

			if (reqHeaders == null)
				reqHeaders = new HashMap<String, String>();
			String payload = dataPipeline.getString("payload");
			File binary = null;
            Map<String, String> headers=new HashMap<String, String>();
			//dataPipeline.log(url);

			if (pathParameters != null) {
				Set set = pathParameters.keySet();
				if (set.size() > 0) {
					for (Object obj : set) {
						String param = (String) obj;
						String val = (String) pathParameters.get(param);
						url = url.replace("{" + param + "}", val);
					}
				}
			}
			byte[] payloadBytes = null;
			InputStream payloadIS = null;
			if (binaryData != null) {
				if (binaryData.get("bytes") != null)
					payloadBytes = (byte[]) binaryData.get("bytes");
				else if (binaryData.get("inputStream") != null) {
					payloadIS = (InputStream) binaryData.get("inputStream");
				}
			}

			String fullUrlIncludingParams = url;
			if (urlParameters != null) {
				Set set = urlParameters.keySet();
				if (set.size() > 0) {
					fullUrlIncludingParams += "?";
					for (Object obj : set) {
						String param = (String) obj;
						String val = (String) urlParameters.get(param);
						fullUrlIncludingParams += param + "=" + val + "&";
					}
                  fullUrlIncludingParams=(fullUrlIncludingParams+"#_").replace("&#_","");
				}
			}
//			dataPipeline.log(fullUrlIncludingParams);
			String basicAuthUser = null;
			String basicAuthPass = null;
			//headers.put("content-length",((payload==null?0:payload.length())+(payloadBytes==null?0:payloadBytes.length))+"");
			if (auth != null && auth.get("basic") != null) {
				basicAuthUser = auth.get("basic").get("username");
				basicAuthPass = auth.get("basic").get("password");
			} else if (auth!=null && auth.get("awsSignature") != null) {
				Map<String, String> awsHeaders = new TreeMap<String, String>();
				String host = auth.get("awsSignature").get("host");
				//headers.forEach((k,v)->awsHeaders.put(k,v));
                awsHeaders.put("host", host);
                
				String AccessKey = auth.get("awsSignature").get("AccessKey");
				String SecretKey = auth.get("awsSignature").get("SecretKey");
				String region = auth.get("awsSignature").get("region");
				String service = auth.get("awsSignature").get("service");
				String signPayload = auth.get("awsSignature").get("signPayload");
				String uriTokens[] = url.split(host);
				String canonicalURI = "";
				if (uriTokens.length == 2)
					canonicalURI = url.split(host)[1];
//              dataPipeline.log("**********************************"+canonicalURI);
//				dataPipeline.log(headers.toString());
              
				if (signPayload != null && signPayload.toLowerCase().equals("true")) {
					if (payload == null)
						payload = "";
					AWSV4Auth.addAwsHeaders(awsHeaders, method, urlParameters, canonicalURI, AccessKey, SecretKey,
							region, service, payload, payloadBytes);
				} else {
					AWSV4Auth.addAwsHeaders(awsHeaders, method, urlParameters, canonicalURI, AccessKey, SecretKey,
							region, service, null, null);
				}
              headers=awsHeaders;
			}
            final Map<String,String> header=headers;
            reqHeaders.forEach((k,v)->header.put(k,v));

			respHandling = Client.invokeREST(0, fullUrlIncludingParams, method, headers, formData, payload, binary,
					basicAuthUser, basicAuthPass, respHandling, payloadBytes, payloadIS);
			// dataPipeline.put("respPayload",respHandling.get("body"));
			// if(respHandling.get("bytes")!=null)
            dataPipeline.put("statusCode", respHandling.get("statusCode"));
			dataPipeline.put("bytes", respHandling.get("bytes"));
			dataPipeline.put("respPayload", respHandling.get("respPayload"));
			dataPipeline.put("inputStream", respHandling.get("inputStream"));
			dataPipeline.put("respHeaders", headers);

		} catch (Exception e) {
			dataPipeline.clear();
			dataPipeline.put("error", e.getMessage());
			new SnippetException(dataPipeline, "Sneppet exception", new Exception(e));
		}
	}

}