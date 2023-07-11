package com.eka.middleware.pub.util;

import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.PropertyManager;
import com.eka.middleware.service.ServiceUtils;
import com.eka.middleware.template.SnippetException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.eka.middleware.server.MiddlewareServer;


public class AutoUpdate {

    public static final void main(DataPipeline dataPipeline) throws SnippetException {
        try {
            String url = dataPipeline.getAsString("url");
            updateTenant(url, dataPipeline);
        } catch (Exception e) {
            dataPipeline.clear();
            dataPipeline.put("error", e.getMessage());
            dataPipeline.setResponseStatus(500);
            dataPipeline.put("status", "Not Modified");
            new SnippetException(dataPipeline, "Failed while saving file", new Exception(e));
        }

    }

    private static boolean importURLAliases(String UrlAliasFilepath, DataPipeline dp) throws Exception {
        Boolean importSuccessful = true;
        Properties prop = new Properties();
        File file = new File(UrlAliasFilepath);
        if (!file.exists()) {
            dp.put("msg", "Alias file not found. Path: " + UrlAliasFilepath);
            return true;
        }
        FileInputStream aliasFIS = new FileInputStream(file);
        prop.load(aliasFIS);
        prop.forEach((k, v) -> {
            if (dp.getString("error") == null) {
                String key = (String) k;
                String value = (String) v;
                dp.put("fqn", value);
                dp.put("alias", key);
                try {
                    dp.apply("packages.middleware.pub.server.browse.registerURLAlias");
                    String msg = dp.getString("msg");
                    if (!"Saved".equals(msg))
                        dp.put("error", msg);

                } catch (Exception e) {
                    dp.put("error", e.getMessage());
                }
                dp.drop("fqn");
                dp.drop("alias");
            }
        });
        aliasFIS.close();
        if (dp.getString("error") != null)
            importSuccessful = false;
        file.delete();
        return importSuccessful;
    }

    private static void unzip(String zipFilePath, String destDir, DataPipeline dp) throws Exception {
        File dir = new File(destDir);
        String unZippedFolderPath = null;
        // create output directory if it doesn't exist
        if (!dir.exists()) dir.mkdirs();
        FileInputStream fis;
        //buffer for read and write data to file

        fis = new FileInputStream(zipFilePath);
        ZipInputStream zis = new ZipInputStream(fis);
        ZipEntry ze = zis.getNextEntry();
        while (ze != null) {
            String fileName = ze.getName();
            if (unZippedFolderPath == null)
                unZippedFolderPath = fileName;
            else {
                dp.log("Zipped entry " + fileName);
                fileName = ("#$" + fileName).replace("#$" + unZippedFolderPath, "");
                File newFile = new File(destDir + File.separator + fileName);
                new File(newFile.getParent()).mkdirs();
                byte[] buffer = new byte[1024];
                int len = zis.read(buffer);
                if (len > 0) {
                    FileOutputStream fos = new FileOutputStream(newFile);
                    while (len > 0) {
                        fos.write(buffer, 0, len);
                        //buffer = new byte[1024]
                        len = zis.read(buffer);
                    }
                    fos.flush();
                    fos.close();
                }
            }
            //close this ZipEntry
            zis.closeEntry();
            ze = zis.getNextEntry();
        }
        //close last ZipEntry
        zis.closeEntry();
        zis.close();
        fis.close();
    }

    private static void createRestorePoint(String buildName, DataPipeline dataPipeline) throws Exception {
        String packagePath = PropertyManager.getPackagePath(dataPipeline.rp.getTenant());
        String bkpDirPath = packagePath + "builds/backup/";
        File dir = new File(bkpDirPath);
        String timeStmp = System.currentTimeMillis() + "";
        if (!dir.exists())
            dir.mkdirs();
        FileOutputStream fos = new FileOutputStream(bkpDirPath + timeStmp + "_packages_" + buildName);
        ZipOutputStream zipOut = new ZipOutputStream(fos);
        File fileToZip = new File(packagePath + "packages/");
        ServiceUtils.zipFile(fileToZip, fileToZip.getName(), zipOut);
        zipOut.flush();
        zipOut.close();
        fos.flush();
        fos.close();
    }

    public static void updateTenant(String version, DataPipeline dataPipeline) throws Exception {
        String fileName = String.format("eka-distribution-tenant-v%s.zip", version);
        URL url = new URL(String.format("https://eka-distribution.s3.us-west-1.amazonaws.com/%s", fileName));

        String downloadLocation = PropertyManager.getPackagePath(dataPipeline.rp.getTenant()) + "builds/import/";

        System.out.println("package path: " + PropertyManager.getPackagePath(dataPipeline.rp.getTenant()));
        System.err.println("downloadLocation: " + downloadLocation);


        File downloadedFile = new File(downloadLocation + fileName);
        try (InputStream in = url.openStream()) {
            Files.copy(in, downloadedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("File downloaded successfully: " + fileName);
        }

        String filePath = PropertyManager.getPackagePath(dataPipeline.rp.getTenant()) + "builds/import/" + fileName;

        Boolean checkDigest = compareDigest(filePath, returnTenantUpdateUrl());
        if (checkDigest) {
            // Call the other methods in the class
            String packagePath = PropertyManager.getPackagePath(dataPipeline.rp.getTenant());
            String buildsDirPath = packagePath + "builds/import/";

            // Unzip and deploy
            String location = buildsDirPath + fileName;
            unzip(location, packagePath, dataPipeline);

            // Import URL aliases
            String urlAliasFilePath = packagePath + (("URLAlias_" + fileName + "#").replace(".zip#", ".properties"));
            boolean importSuccessful = importURLAliases(urlAliasFilePath, dataPipeline);

            // Create restore point
            createRestorePoint(fileName, dataPipeline);

            String jsonContent = readJsonFromUrl(returnTenantUpdateUrl());
            String extractedJsonString = extractJsonPart(jsonContent, "latest");
            String tenantUpdateFileLocation = PropertyManager.getPackagePath(dataPipeline.rp.getTenant()) + "builds/tenant-update.json";

            try {
                writeJsonToFile(extractedJsonString, tenantUpdateFileLocation);
                System.out.println("Extraction complete. tenant-update file updated");
            } catch (IOException e) {
                e.printStackTrace();
            }

            dataPipeline.put("status", true);
        } else {
            dataPipeline.put("status", false);
        }

    }

    public static String readJsonFromUrl(String urlString) throws IOException {
        try {
            return IOUtils.toString(new URI(urlString), StandardCharsets.UTF_8);
        } catch (URISyntaxException e) {
        }
        return null;
    }

    private static String readJsonFromFile(String filePath) throws IOException {
        return new String(Files.readAllBytes(Paths.get(filePath)));
    }


    public static String jsonValueFetch(String location, String variableName) throws IOException {
        String jsonString;
        if (isUrl(location)) {
            jsonString = readJsonFromUrl(location);
        } else {
            jsonString = readJsonFromFile(location);
        }

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode root = objectMapper.readTree(jsonString);
        return findValueByVariableName(root, variableName);
    }

    private static String findValueByVariableName(JsonNode node, String variableName) {
        String[] searchKeys = variableName.split("\\.");

        JsonNode currentNode = node;
        for (String key : searchKeys) {
            currentNode = currentNode.get(key);
            if (currentNode == null) {
                return null;
            }
        }

        return currentNode.asText();
    }


    private static boolean isUrl(String location) {
        try {
            (new URL(location)).toURI();
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    public static String checkForUpdate(DataPipeline dataPipeline) throws Exception {

        String url = returnTenantUpdateUrl();
        String filePath = PropertyManager.getPackagePath(dataPipeline.rp.getTenant()) + "builds/tenant-update.json";

        String filePathVersion = "0";

        File file = new File(filePath);

        if (file.exists()) {
            filePathVersion = jsonValueFetch(filePath, "latest.version_number");
        }
        String urlVersion = jsonValueFetch(url, "latest.version_number");

        if (Integer.parseInt(urlVersion) > Integer.parseInt(filePathVersion)) {
            return jsonValueFetch(url, "latest.version");
        }

        return null;
    }


    private static String calculateFileChecksum(String filePath) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");

        try (InputStream inputStream = new FileInputStream(filePath)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
        }

        byte[] digest = md.digest();

        StringBuilder checksumBuilder = new StringBuilder();
        for (byte b : digest) {
            checksumBuilder.append(String.format("%02x", b));
        }

        return checksumBuilder.toString();
    }


    private static String getDigestFromUrl(String url) throws IOException {
        return jsonValueFetch(url, "latest.digest");
    }


    public static Boolean compareDigest(String filePath, String url) throws Exception {
        String urlDigest = getDigestFromUrl(url);
        String fileDigest = calculateFileChecksum(filePath);
        return StringUtils.equals(urlDigest, fileDigest);
    }

    private static String extractJsonPart(String jsonString, String partName) {
        int startIndex = jsonString.indexOf("\"" + partName + "\":");
        if (startIndex == -1) {
            throw new IllegalArgumentException("JSON part not found: " + partName);
        }

        int endIndex = jsonString.indexOf("}", startIndex);
        if (endIndex == -1) {
            throw new IllegalArgumentException("Invalid JSON structure");
        }

        return "{\n" + jsonString.substring(startIndex, endIndex + 1) + "\n}";
    }

    private static void writeJsonToFile(String jsonString, String filePath) throws IOException {
        try (FileWriter fileWriter = new FileWriter(filePath)) {
            fileWriter.write(jsonString);
        }
    }


    public static String returnTenantUpdateUrl() throws Exception {

        boolean IS_COMMUNITY_VERSION = Boolean.parseBoolean(System.getProperty("COMMUNITY_DEPLOYMENT"));

        if (IS_COMMUNITY_VERSION)
            return "https://eka-distribution.s3.us-west-1.amazonaws.com/community-tenant-update.json";
        else
            return "https://eka-distribution.s3.us-west-1.amazonaws.com/tenant-update.json";
    }


}
