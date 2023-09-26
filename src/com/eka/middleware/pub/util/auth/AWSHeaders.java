package com.eka.middleware.pub.util.auth;

import com.eka.middleware.pub.util.auth.aws.AWS4SignerBase;
import com.eka.middleware.pub.util.auth.aws.AWS4SignerForChunkedUpload;
import com.eka.middleware.pub.util.rest.BinaryUtils;

import java.net.URL;
import java.util.Map;
import java.util.TreeMap;

public class AWSHeaders {


    /*public static AWS4SignerForChunkedUpload build(URL endpointUrl, String method, String awsAccessKey, String awsSecretKey,
                                        String bucketName, String regionName, String service, Map<String, String> headers,
                                        byte[] payload, Map<String, String> queryParameters){*/
/*	addAwsHeaders(Map<String, String> headers,String httpMethodName,Map<String,String> queryParametes,
            String canonicalURI,String accessKeyID, String secretAccessKey, String regionName, 
            String serviceName,String payload, byte[] payloadBytes)
	*/
    public static AWS4SignerForChunkedUpload build(Map<String, String> headers, String method, Map<String, String> queryParameters,
                                                   String canonicalURI, String awsAccessKey, String awsSecretKey, String regionName, String service,
                                                   byte[] payload) throws Exception {

        URL endpointUrl = new URL(canonicalURI);
        Map<String, String> awsHeaders = new TreeMap<>();
        final int userDataBlockSize = 64 * 1024;
        String contentLengthHeader = "content-length";
        String contentHashString = AWS4SignerBase.EMPTY_BODY_SHA256;
        if ((payload == null || payload.length == 0)) {
            awsHeaders.put("x-amz-content-sha256", AWS4SignerBase.EMPTY_BODY_SHA256);
//            if (null == queryParameters || queryParameters.isEmpty()) {
//                awsHeaders.put("x-amz-content-sha256", AWS4SignerBase.EMPTY_BODY_SHA256);
//            }

        } else {
            byte[] contentHash = AWS4SignerBase.hash(payload);
            contentHashString = BinaryUtils.toHex(contentHash);
            if (headers != null && "aws-chunked".equals(headers.get("content-encoding"))) {
                contentLengthHeader = "x-amz-decoded-content-length";
                contentHashString = AWS4SignerForChunkedUpload.STREAMING_BODY_SHA256;
                long totalLength = AWS4SignerForChunkedUpload.calculateChunkedContentLength(payload.length, userDataBlockSize);
                headers.put("content-length", "" + totalLength);
            }
            awsHeaders.put("x-amz-content-sha256", contentHashString);
            awsHeaders.put(contentLengthHeader, "" + payload.length);
        }

        if (headers != null) {
            headers.forEach((k, v) -> {
                if (k != null && v != null) awsHeaders.put(k, v);
            });
        }

        AWS4SignerForChunkedUpload signer = new AWS4SignerForChunkedUpload(
                endpointUrl, method.toUpperCase(), service, regionName);
        String authorization = signer.computeSignature(awsHeaders,
                queryParameters,
                contentHashString,
                awsAccessKey,
                awsSecretKey);

        awsHeaders.put("Authorization", authorization);
        //headers.clear();
        awsHeaders.forEach((k, v) -> headers.put(k, v));
        return signer;
    }
}
