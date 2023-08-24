package com.eka.middleware.licensing;

import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.PropertyManager;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class License {

    private static LicenseFile readLicense(DataPipeline dataPipeline) throws Exception {

        FileInputStream fileInputStream = new FileInputStream(PropertyManager.getPackagePath(dataPipeline.rp.getTenant()) + "builds/LICENSE.BIN");
        String content = IOUtils.toString(fileInputStream);
        fileInputStream.close();

        LicenseFile licenseFile = validateLicense(dataPipeline, content);

        return licenseFile;
    }

    private static LicenseFile validateLicense(DataPipeline dataPipeline, String content) throws IOException, ClassNotFoundException {
        byte[] obj = Base64.getDecoder().decode(content);

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(obj);
        ObjectInputStream inputStream = new ObjectInputStream(byteArrayInputStream);
        LicenseFile licenseFile = (LicenseFile) inputStream.readObject();

        File instanceUUID = new File(PropertyManager.getConfigFolderPath() + "INSTANCE.UUID");
        File instanceGroupUUID = new File(PropertyManager.getConfigFolderPath() + "INSTANCE-GROUP.UUID");

        if (!IOUtils.toString(new FileInputStream(instanceUUID)).equalsIgnoreCase(licenseFile.instanceUUID)) {
            throw new RuntimeException("License: Server ID is not matched.");
        }

        if (!dataPipeline.rp.getTenant().getName().equalsIgnoreCase(licenseFile.getTenant())) {
            throw new RuntimeException("License: Tenant is not matched.");
        }
        return licenseFile;
    }

    public static LicenseFile getLicenseFile(DataPipeline dataPipeline) {
        try {
            LicenseFile licenseFile = readLicense(dataPipeline);
            dataPipeline.put("instanceUUID", licenseFile.instanceUUID);
            dataPipeline.put("instanceClusterUUID", licenseFile.instanceClusterUUID);
            return licenseFile;
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isLicenseFound(DataPipeline dataPipeline) {
        boolean isExist = new File(PropertyManager.getPackagePath(dataPipeline.rp.getTenant()) + "builds/LICENSE.BIN").exists();
        if (!isExist) {
            return false;
        }

        try {
            readLicense(dataPipeline);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public static boolean updateLicenseKey(DataPipeline dataPipeline, String licenseKey) throws Exception  {
        validateLicense(dataPipeline, licenseKey);
        FileOutputStream fileOutputStream = new FileOutputStream(PropertyManager.getPackagePath(dataPipeline.rp.getTenant()) + "builds/LICENSE.BIN");
        IOUtils.write(licenseKey, fileOutputStream, StandardCharsets.UTF_8);
        return true;
    }
}
