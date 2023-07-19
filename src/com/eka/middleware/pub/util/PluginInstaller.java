package com.eka.middleware.pub.util;

import com.eka.middleware.server.Build;
import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.PropertyManager;
import com.google.common.collect.Lists;
import com.nimbusds.jose.shaded.gson.Gson;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

import static com.eka.middleware.pub.util.AutoUpdate.*;

public class PluginInstaller {

    public static void downloadFile(String pluginId, String version, DataPipeline dataPipeline) throws Exception {

        MarketPlace marketPlace = getMarketPlace();
        //List<Plugins> plugins = marketPlace.getPlugins();

        Optional<Plugins> pluginObj = marketPlace.getPlugins().parallelStream().filter(f -> f.getUnique_id().equals(pluginId)).findAny();

        if (!pluginObj.isPresent()) {
            throw new Exception("Invalid Plugin");
        }

        Plugins plugin = pluginObj.get();

        int existed_latest_version_number = plugin.getLatest_version_number();
        // Read version number from plugin-package.json

        int versionNumber = getVersionNumberFromFile(dataPipeline);
        if (existed_latest_version_number <= versionNumber) {
            return;
        }

        String fileName = String.format("%sv%s.zip", plugin.getName_slug(), version);
        String filePath = PropertyManager.getPackagePath(dataPipeline.rp.getTenant()) + "builds/import/" + fileName;

        URL url = new URL(Build.DISTRIBUTION_REPO + "plugins/" + fileName);
        String downloadLocation = PropertyManager.getPackagePath(dataPipeline.rp.getTenant()) + "builds/import/";
        File downloadedFile = new File(downloadLocation + fileName);

        InputStream in = url.openStream();
        Files.copy(in, downloadedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        Boolean checkDigest = compareDigest(filePath, plugin.getDigest());

        if (checkDigest) {

            String packagePath = PropertyManager.getPackagePath(dataPipeline.rp.getTenant());
            String buildsDirPath = packagePath + "builds/import/";
            String location = buildsDirPath + fileName;
            unzip(location, packagePath, dataPipeline);

            // Import URL aliases
            String urlAliasFilePath = packagePath + (("URLAlias_" + fileName + "#").replace(".zip#", ".properties"));
            boolean importSuccessful = importURLAliases(urlAliasFilePath, dataPipeline);
            createRestorePoint(fileName, dataPipeline);

            // Update in plugin-package.json

            updatePackagePlugin(plugin, dataPipeline);

        } else {
            dataPipeline.put("status", false);
        }
    }
    private static MarketPlace getMarketPlace() throws IOException {
        String jsonUrl = "syncloop-marketplace.json";

        String json = readJsonFromUrl(Build.DISTRIBUTION_REPO + jsonUrl);

        return new Gson().fromJson(json, MarketPlace.class);

    }
    private static Boolean compareDigest(String filePath, String url) throws Exception {
        String fileDigest = calculateFileChecksum(filePath);
        return StringUtils.equals(url, fileDigest);
    }

    /**
     * @param plugins
     * @param dataPipeline
     * @throws IOException
     */
    private static void updatePackagePlugin(Plugins plugins, DataPipeline dataPipeline) throws IOException {
        PluginPackage pluginPackage = new PluginPackage();
        File file = new File(PropertyManager.getPackagePath(dataPipeline.rp.getTenant()) + "builds/plugin-package.json");
        if (!file.exists()) {
            pluginPackage = new PluginPackage();
            pluginPackage.setPlugins(Lists.newArrayList(plugins));
        } else {
            pluginPackage = new Gson().fromJson(new FileReader(file), PluginPackage.class);
            Optional<Plugins> pluginObj = pluginPackage.getPlugins().parallelStream().filter(f -> f.getUnique_id().equals(plugins.getUnique_id())).findAny();

            if (pluginObj.isPresent()) {
                Plugins oldPlugin = pluginObj.get();
                pluginPackage.getPlugins().remove(oldPlugin);
                pluginPackage.getPlugins().add(plugins);
            } else {
                pluginPackage.setPlugins(Lists.newArrayList(plugins));
            }
        }
        IOUtils.write(new Gson().toJson(pluginPackage), new FileOutputStream(file), StandardCharsets.UTF_8);
    }

    private static int getVersionNumberFromFile(DataPipeline dataPipeline) throws IOException {

        File file = new File(PropertyManager.getPackagePath(dataPipeline.rp.getTenant()) + "builds/plugin-package.json");

        if (file.exists()) {
            PluginPackage pluginPackage = new Gson().fromJson(new FileReader(file), PluginPackage.class);
            List<Plugins> plugins = pluginPackage.getPlugins();

            if (!plugins.isEmpty()) {
                return plugins.get(0).getLatest_version_number();
            }
        }

        return 0;
    }

    public static Boolean checkForUpdate( List<Plugins> plugins,DataPipeline dataPipeline) throws Exception {
        int versionNumberFromFile = getVersionNumberFromFile(dataPipeline);

        for (Plugins plugin : plugins) {
            if (plugin.getLatest_version_number() > versionNumberFromFile) {
                return true;
            }
        }
        return false;
    }
    public static boolean arePluginsInstalled(List<Plugins> plugins, DataPipeline dataPipeline) throws IOException {

        String filePath = PropertyManager.getPackagePath(dataPipeline.rp.getTenant()) + "builds/plugin-package.json";
        File file = new File(filePath);

        if (file.exists()) {

            PluginPackage pluginPackage = new Gson().fromJson(new FileReader(file), PluginPackage.class);

            List<Plugins> installedPlugins = pluginPackage.getPlugins();

            for (Plugins plugin : plugins) {
                if (installedPlugins.stream().anyMatch(p -> p.getUnique_id().equals(plugin.getUnique_id()))) {
                    return true;
                }
            }
        }

        return false;
    }
}