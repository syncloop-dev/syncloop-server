package com.eka.middleware.pub.util;

import com.eka.middleware.server.Build;
import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.PropertyManager;
import com.eka.middleware.template.SnippetException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import static com.eka.middleware.pub.util.AutoUpdate.*;

public class PluginInstaller {
    public static void main(DataPipeline dataPipeline) throws SnippetException {
        try {

            String name = dataPipeline.getString("name");
            String latest_version_number = dataPipeline.getString("latest_version_number");

            downloadFile(name, latest_version_number, dataPipeline);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String extractValueFromJson(String json, String key) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(json);
        return extractValueFromNode(rootNode, key);
    }

    private static String extractValueFromNode(JsonNode node, String key) {
        if (node.isObject()) {
            JsonNode valueNode = node.get(key);
            if (valueNode != null) {
                return valueNode.isValueNode() ? valueNode.asText() : valueNode.toString();
            }
            for (JsonNode childNode : node) {
                String value = extractValueFromNode(childNode, key);
                if (value != null) {
                    return value;
                }
            }
        } else if (node.isArray()) {
            for (JsonNode childNode : node) {
                String value = extractValueFromNode(childNode, key);
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }

    public static void downloadFile( String name, String version, DataPipeline dataPipeline) throws Exception {

        String distributionRepo = Build.DISTRIBUTION_REPO;
        String jsonUrl = "syncloop-marketplace.json";
        String fileUrl = distributionRepo+"plugins/AWS_S3v0.0.1.zip";

        String json = readJsonFromUrl(distributionRepo + jsonUrl);
        String existed_latest_version_number = extractValueFromJson(json, "latest_version_number");
        String existedName = extractValueFromJson(json, "name");

        if (version.compareTo(existed_latest_version_number) <= 0 || !name.equals(existedName)) {
            System.out.println("Invalid version or name. Latest version: " + existed_latest_version_number + ", Name: " + existedName);
            return;
        }


        String fileName = name+"_" + version + ".zip";

        String filePath = PropertyManager.getPackagePath(dataPipeline.rp.getTenant()) + "builds/import/" + fileName;

        File existingFile = new File(filePath);
        if (existingFile.exists()) {
            System.out.println("The file already exists: " + fileName);
            return;
        }
        URL url = new URL(fileUrl);
        try (InputStream in = new BufferedInputStream(url.openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(filePath)) {
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
        }
        String downloadLocation = PropertyManager.getPackagePath(dataPipeline.rp.getTenant())+"builds/import/";
        File downloadedFile = new File(downloadLocation + fileName);

        try (InputStream in = url.openStream()) {
            Files.copy(in, downloadedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        System.err.println(filePath);
        Boolean checkDigest = compareDigest(filePath,distributionRepo+jsonUrl );
        System.out.println("checkDigest: " + checkDigest);

        if (checkDigest) {

            // Unzip and deploy
            String packagePath = PropertyManager.getPackagePath(dataPipeline.rp.getTenant());
            String buildsDirPath = packagePath + "builds/import/";
            String location = buildsDirPath + fileName;
            unzip(location, packagePath, dataPipeline);

            // Import URL aliases
            String urlAliasFilePath = packagePath + (("URLAlias_" + fileName + "#").replace(".zip#", ".properties"));
            boolean importSuccessful = importURLAliases(urlAliasFilePath, dataPipeline);
            createRestorePoint(fileName, dataPipeline);
            System.out.println("importSuccess " + importSuccessful);

        }else{
        dataPipeline.put("status", false);
    }
    }

    public static String getDigestFromUrl(String url) throws IOException {
        String json = readJsonFromUrl(url);
        return  extractValueFromJson(json, "digest");

    }
    public static Boolean compareDigest(String filePath, String url) throws Exception {
        String urlDigest = getDigestFromUrl(url);
        String fileDigest = calculateFileChecksum(filePath);

        return StringUtils.equals(urlDigest, fileDigest);
    }
}