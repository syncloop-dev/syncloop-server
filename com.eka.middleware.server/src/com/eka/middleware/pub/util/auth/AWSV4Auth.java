package com.eka.middleware.pub.util.auth;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.eka.middleware.template.SnippetException;

/**
 * Example: Signing AWS Requests with Signature Version 4 in Java.
 *
 * @reference: http://docs.aws.amazon.com/general/latest/gr/sigv4_signing.html
 * @author javaQuery
 * @date 19th January, 2016
 * @Github: https://github.com/javaquery/Examples
 */
public class AWSV4Auth {

	private static final String HMACAlgorithm = "AWS4-HMAC-SHA256";
    private static final String aws4Request = "aws4_request";
    private static String[] prepareCanonicalRequest(String canonicalURI,String httpMethodName,Map<String,String> queryParametes, 
                                                    Map<String,String> awsHeaders,String payload, byte[] payloadBytes) {
        StringBuilder canonicalURL = new StringBuilder("");

        /* Step 1.1 Start with the HTTP request method (GET, PUT, POST, etc.), followed by a newline character. */
        canonicalURL.append(httpMethodName).append("\n");

        /* Step 1.2 Add the canonical URI parameter, followed by a newline character. */
        canonicalURI = canonicalURI == null || canonicalURI.trim().isEmpty() ? "/" : canonicalURI;
        canonicalURL.append(canonicalURI).append("\n");

        /* Step 1.3 Add the canonical query string, followed by a newline character. */
        StringBuilder queryString = new StringBuilder("");
        if (queryParametes != null && !queryParametes.isEmpty()) {
            for (Map.Entry<String, String> entrySet : queryParametes.entrySet()) {
                String key = entrySet.getKey();
                String value = entrySet.getValue();
                queryString.append(key).append("=").append(encodeParameter(value)).append("&");
            }

            /* @co-author https://github.com/dotkebi @git #1 @date 16th March, 2017 */
            queryString.deleteCharAt(queryString.lastIndexOf("&"));

            queryString.append("\n");
        } else {
            queryString.append("\n");
        }
        canonicalURL.append(queryString);

        /* Step 1.4 Add the canonical headers, followed by a newline character. */
        StringBuilder signedHeaders = new StringBuilder("");
        if (awsHeaders != null && !awsHeaders.isEmpty()) {
            for (Map.Entry<String, String> entrySet : awsHeaders.entrySet()) {
                String key = entrySet.getKey();
                String value = entrySet.getValue();
                signedHeaders.append(key).append(";");
                canonicalURL.append(key).append(":").append(value).append("\n");
            }

            /* Note: Each individual header is followed by a newline character, meaning the complete list ends with a newline character. */
            canonicalURL.append("\n");
        } else {
            canonicalURL.append("\n");
        }

        /* Step 1.5 Add the signed headers, followed by a newline character. */
        String strSignedHeader = signedHeaders.substring(0, signedHeaders.length() - 1); // Remove last ";"
        canonicalURL.append(strSignedHeader).append("\n");

        /* Step 1.6 Use a hash (digest) function like SHA256 to create a hashed value from the payload in the body of the HTTP or HTTPS. */
        String payloadHex="UNSIGNED-PAYLOAD";
        if(payloadBytes!=null && payloadBytes.length>0)
          payloadHex=bytesToHex(payloadBytes);
        else if(payload!=null)
          payloadHex=generateHex(payload);
        canonicalURL.append(payloadHex);

        //if (debug) {
          //  System.out.println("##Canonical Request:\n" + canonicalURL.toString());
        //}
		String arr[]=new String[]{canonicalURL.toString(),strSignedHeader,payloadHex};
        return arr;
    }

/**
     * Task 2: Create a String to Sign for Signature Version 4.
     *
     * @param canonicalURL
     * @return
     */
    private static String prepareStringToSign(String canonicalURL,String currentDate, String regionName, String serviceName,String xAmzDate) {
        String stringToSign = "";

        /* Step 2.1 Start with the algorithm designation, followed by a newline character. */
        stringToSign = HMACAlgorithm + "\n";

        /* Step 2.2 Append the request date value, followed by a newline character. */
        stringToSign += xAmzDate + "\n";

        /* Step 2.3 Append the credential scope value, followed by a newline character. */
        stringToSign += currentDate + "/" + regionName + "/" + serviceName + "/" + aws4Request + "\n";

        /* Step 2.4 Append the hash of the canonical request that you created in Task 1: Create a Canonical Request for Signature Version 4. */
        stringToSign += generateHex(canonicalURL);

       // if (debug) {
            //dataPipeline.log("##String to sign:\n" + stringToSign);
        //}

        return stringToSign;
    }

    /**
     * Task 3: Calculate the AWS Signature Version 4.
     *
     * @param stringToSign
     * @return
     */
    private static String calculateSignature(String stringToSign, String secretAccessKey, String currentDate, String regionName, 
                                             String serviceName) {
        try {
            /* Step 3.1 Derive your signing key */
            byte[] signatureKey = getSignatureKey(secretAccessKey, currentDate, regionName, serviceName);

            /* Step 3.2 Calculate the signature. */
            byte[] signature = HmacSHA256(signatureKey, stringToSign);

            /* Step 3.2.1 Encode signature (byte[]) to Hex */
            String strHexSignature = bytesToHex(signature);
            return strHexSignature;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Task 4: Add the Signing Information to the Request. We'll return Map of
     * all headers put this headers in your request.
     *
     * @return
     */
    public static void addAwsHeaders(Map<String, String> headers,String httpMethodName,Map<String,String> queryParametes,
                                                 String canonicalURI,String accessKeyID, String secretAccessKey, String regionName, 
                                                 String serviceName,String payload, byte[] payloadBytes) {
        
		String currentDate=getDate();
      	String xAmzDate=getTimeStamp();
       
        //awsHeaders.put("x-amz-date", xAmzDate);
        headers.put("x-amz-date", xAmzDate);

        /* Execute Task 1: Create a Canonical Request for Signature Version 4. */
        String arr[] = prepareCanonicalRequest(canonicalURI,httpMethodName,queryParametes,new TreeMap<>(headers),payload,payloadBytes);
		String canonicalURL=arr[0];
        String strSignedHeader=arr[1];
      	String payloadHex=arr[2];
        /* Execute Task 2: Create a String to Sign for Signature Version 4. */
        String stringToSign = prepareStringToSign(canonicalURL,currentDate,regionName,serviceName,xAmzDate);

        /* Execute Task 3: Calculate the AWS Signature Version 4. */
        
        String signature = calculateSignature(stringToSign,secretAccessKey,currentDate,regionName,serviceName);

        if (signature != null) {
            //Map<String, String> header = new HashMap<String, String>(0);

            

            headers.put("Authorization", buildAuthorizationString(strSignedHeader,signature,accessKeyID,
                                                                  currentDate,regionName,serviceName));
          headers.put("x-amz-content-sha256", payloadHex);
          /*// if (debug) {
          System.out.println("##Signature:\n" + signature);
          System.out.println("##Header:");
                for (Map.Entry<String, String> entrySet : headers.entrySet()) {
                	System.out.println(entrySet.getKey() + " = " + entrySet.getValue());
                }
                System.out.println("================================");
           // }*/
            
            //return header;
       // } else {
           // if (debug) {
       //         System.out.println("##Signature:\n" + signature);
          //  }
            //return null;
          
        }
    }

    /**
     * Build string for Authorization header.
     *
     * @param strSignature
     * @return
     */
    private static String buildAuthorizationString(String strSignedHeader, String strSignature,String accessKeyID, 
                                                   String currentDate, String regionName, String serviceName) {
      //dataPipeline.log("*********************-------@@@&&&1");  
      return HMACAlgorithm + " "
                + "Credential=" + accessKeyID + "/" + getDate() + "/" + regionName + "/" + serviceName + "/" + aws4Request + ","
                + "SignedHeaders=" + strSignedHeader + ","
                + "Signature=" + strSignature;
    }

    /**
     * Generate Hex code of String.
     *
     * @param data
     * @return
     */
    private static String generateHex(String data) {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(data.getBytes("UTF-8"));
            byte[] digest = messageDigest.digest();
            return String.format("%064x", new java.math.BigInteger(1, digest));
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Apply HmacSHA256 on data using given key.
     *
     * @param data
     * @param key
     * @return
     * @throws Exception
     * @reference:
     * http://docs.aws.amazon.com/general/latest/gr/signature-v4-examples.html#signature-v4-examples-java
     */
    private static byte[] HmacSHA256(byte[] key, String data) throws Exception {
        String algorithm = "HmacSHA256";
        Mac mac = Mac.getInstance(algorithm);
        mac.init(new SecretKeySpec(key, algorithm));
        return mac.doFinal(data.getBytes("UTF8"));
    }

    /**
     * Generate AWS signature key.
     *
     * @param key
     * @param date
     * @param regionName
     * @param serviceName
     * @return
     * @throws Exception
     * @reference
     * http://docs.aws.amazon.com/general/latest/gr/signature-v4-examples.html#signature-v4-examples-java
     */
    private static byte[] getSignatureKey(String key, String date, String regionName, String serviceName) throws Exception {
        byte[] kSecret = ("AWS4" + key).getBytes("UTF8");
        byte[] kDate = HmacSHA256(kSecret, date);
        byte[] kRegion = HmacSHA256(kDate, regionName);
        byte[] kService = HmacSHA256(kRegion, serviceName);
        byte[] kSigning = HmacSHA256(kService, aws4Request);
        return kSigning;
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    /**
     * Convert byte array to Hex
     *
     * @param bytes
     * @return
     */
    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars).toLowerCase();
    }

public static String bytesToHex_1(byte[] bytes) { 
 StringBuffer hexString = new StringBuffer();
 for (int j=0; j<bytes.length; j++) {
 String hex=Integer.toHexString(0xff & bytes[j]);
 if(hex.length()==1) hexString.append('0');
 hexString.append(hex);
 }
 return hexString.toString();
 }

    /**
     * Get timestamp. yyyyMMdd'T'HHmmss'Z'
     *
     * @return
     */
    private static String getTimeStamp() {
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));//server timezone
        return dateFormat.format(new Date());
    }

    /**
     * Get date. yyyyMMdd
     *
     * @return
     */
    private static String getDate() {
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));//server timezone
        return dateFormat.format(new Date());
    }

    /**
     * Using {@link URLEncoder#encode(java.lang.String, java.lang.String) } instead of
     * {@link URLEncoder#encode(java.lang.String) }
     * 
     * @co-author https://github.com/dotkebi
     * @date 16th March, 2017
     * @git #1
     * @param param
     * @return 
     */
     private static String encodeParameter(String param){
         try {
             return URLEncoder.encode(param, "UTF-8");
         } catch (Exception e) {
             return URLEncoder.encode(param);
         }
     }
    
     public static void main(String[] args) throws Exception {
 		
 		Map<String, String> queryParametes = new HashMap<>();
 		
 		//queryParametes.put("MessageBody", "MessageBody");
 		queryParametes.put("Action", "GetQueueUrl");
 		queryParametes.put("QueueName", "ekmwq");
 		
 		TreeMap<String, String> awsHeaders = new TreeMap<>();
 		awsHeaders.put("host", "sqs.ap-south-1.amazonaws.com");
 	
 		
 		main("https://sqs.ap-south-1.amazonaws.com", 
 				queryParametes, awsHeaders, 
 				"ap-south-1", "sqs", "GET", "AKIAITT6PDEYBC4BLOFQ", "R8negk5pXjfQuQZW32eeB8YnRbVifG+2esa6CXPZ");
 		
 	}

 	public static void main(String urlString,
 			Map<String, String> queryParametes,
 			TreeMap<String, String> header,
 			String region,
 			String serviceName, 
 			String method, 
 			String AccessKey,
 			String SecretKey) throws SnippetException {

 		try {

 			URL url = new URL(urlString);

 			AWSV4Auth aWSV4Auth = new AWSV4Auth();

 			//TreeMap<String, String> header = new TreeMap<>(); 
 					aWSV4Auth.addAwsHeaders(header, method, queryParametes, url.getPath(), AccessKey, SecretKey, region, serviceName, null, null);
 			header.put("Accept", "application/json");
 			
 			String fullUrlIncludingParams = url.toString();
 			if (queryParametes != null) {
 				Set<String> set = queryParametes.keySet();
 				if (set.size() > 0) {
 					fullUrlIncludingParams += "?";
 					for (Object obj : set) {
 						String param = (String) obj;
 						String val = (String) queryParametes.get(param);
 						fullUrlIncludingParams += param + "=" + val + "&";
 					}
 	              fullUrlIncludingParams=(fullUrlIncludingParams+"#_").replace("&#_","");
 				}
 			}
 			header.forEach((k,v) -> System.out.println(k +" : "+ v));
 			//header.remove("host");
 			System.out.println(fullUrlIncludingParams);
 			
 			HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(fullUrlIncludingParams));
 			
 			header.entrySet().forEach(m -> builder.header(m.getKey(), m.getValue()));
 			
 			HttpRequest request = builder.method(method, HttpRequest.BodyPublishers.noBody())
 					.build();
 			
 			
 			HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
 			System.out.println(response.body());
 			
 		} catch (Exception e) {
 		}

 	}
}
