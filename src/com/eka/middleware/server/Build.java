package com.eka.middleware.server;

import com.eka.middleware.service.DataPipeline;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Build {

    public static Logger LOGGER = LogManager.getLogger(Build.class);
    private static final String DISTRIBUTION_NAME = "eka-distribution-%s.zip";
    private static final String TENANT_DISTRIBUTION_NAME = "eka-distribution-tenant-%s.zip";
    private static final String TENANT_COMMUNITY_DISTRIBUTION_NAME = "eka-distribution-community-tenant-%s.zip";
    private static final String COMMUNITY_DISTRIBUTION_NAME = "eka-distribution-community-%s.zip";
    public static final String DISTRIBUTION_REPO = "https://repo.syncloop.com/";

    /**
     * @param version
     */
    public static void download(String version) {
        try {
            BootBuild.bootBuild(version);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param dataPipeline
     * @param version
     * @throws Exception
     */
    public static void updateTenant(DataPipeline dataPipeline,  String version) throws Exception {

        String directoryPrefix = "./";

        if (Boolean.parseBoolean(System.getProperty("CONTAINER_DEPLOYMENT"))) {
            directoryPrefix = "/eka/";
        }

        updateTenant(directoryPrefix + "integration/middleware/tenants/" + dataPipeline.rp.getTenant().getName() + File.separator, version);
    }

    /**
     * @param tenantDirPath
     * @param version
     * @throws Exception
     */
    public static void updateTenant(String tenantDirPath, String version) throws Exception {
        String tenantDistributionName = String.format(TENANT_DISTRIBUTION_NAME, version);
        if (Boolean.parseBoolean(System.getProperty("COMMUNITY_DEPLOYMENT"))) {
            tenantDistributionName = String.format(TENANT_COMMUNITY_DISTRIBUTION_NAME, version);
        }

        LOGGER.info("Build Downloading...");
        InputStream in = new URL(DISTRIBUTION_REPO + tenantDistributionName).openStream();
        Files.copy(in, Paths.get(tenantDirPath + tenantDistributionName), StandardCopyOption.REPLACE_EXISTING);
        LOGGER.info("Build Download Completed.");

        File patchFile = new File(tenantDirPath + tenantDistributionName);

        LOGGER.info("Unzipping build...");
        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(new FileInputStream(patchFile));
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            File newFile = BootBuild.newFile(new File(tenantDirPath), zipEntry);
            if (zipEntry.isDirectory()) {
                if (!newFile.isDirectory() && !newFile.mkdirs()) {
                    throw new IOException("Failed to create directory " + newFile);
                }
            } else {
                // fix for Windows-created archives
                File parent = newFile.getParentFile();
                if (!parent.isDirectory() && !parent.mkdirs()) {
                    throw new IOException("Failed to create directory " + parent);
                }

                // write file content
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
        patchFile.delete();
    }

    private static class BootBuild {
        /**
         *
         */
        private static void bootBuild(String version) throws Exception {

            String distributionName = String.format(DISTRIBUTION_NAME, version);
            if (Boolean.parseBoolean(System.getProperty("COMMUNITY_DEPLOYMENT"))) {
                distributionName = String.format(COMMUNITY_DISTRIBUTION_NAME, version);;
            }

            File eka = new File("./eka/version");
            if (eka.exists()) {
                LOGGER.info("Build already existed.");
            } else {
                if (new File("./" + distributionName).exists()) {
                    LOGGER.info("Found Build.");
                } else {
                    LOGGER.info("Build Downloading...");
                    InputStream in = new URL(DISTRIBUTION_REPO + distributionName).openStream();
                    Files.copy(in, Paths.get(distributionName), StandardCopyOption.REPLACE_EXISTING);
                    LOGGER.info("Build Download Completed.");
                }

                LOGGER.info("Unzipping build...");
                byte[] buffer = new byte[1024];
                ZipInputStream zis = new ZipInputStream(new FileInputStream("./" + distributionName));
                ZipEntry zipEntry = zis.getNextEntry();
                while (zipEntry != null) {
                    File newFile = newFile(new File(""), zipEntry);
                    if (zipEntry.isDirectory()) {
                        if (!newFile.isDirectory() && !newFile.mkdirs()) {
                            throw new IOException("Failed to create directory " + newFile);
                        }
                    } else {
                        // fix for Windows-created archives
                        File parent = newFile.getParentFile();
                        if (!parent.isDirectory() && !parent.mkdirs()) {
                            throw new IOException("Failed to create directory " + parent);
                        }

                        // write file content
                        FileOutputStream fos = new FileOutputStream(newFile);
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                        fos.close();
                    }
                    zipEntry = zis.getNextEntry();
                }
                zis.closeEntry();
                zis.close();
            }
        }

        /**
         * @param destinationDir
         * @param zipEntry
         * @return
         * @throws IOException
         */
        private static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
            File destFile = new File(destinationDir, zipEntry.getName());

            String destDirPath = destinationDir.getCanonicalPath();
            String destFilePath = destFile.getCanonicalPath();

            return destFile;
        }
    }
}
