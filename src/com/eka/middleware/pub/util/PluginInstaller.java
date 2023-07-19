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
import java.util.Map;
import java.util.Optional;

import static com.eka.middleware.pub.util.AutoUpdate.*;

public class PluginInstaller {

    /**
     * @param dataPipeline
     * @return
     * @throws Exception
     */
    public static MarketPlace getMarketPlace(DataPipeline dataPipeline) throws Exception {
        MarketPlace marketPlace = getMarketPlace();

        marketPlace
                .getPlugins().stream().forEach(plugin -> {
                   try {
                       Plugins installedPlugin = getInstalledPlugin(plugin.getUnique_id(), dataPipeline);
                       plugin.setInstalled(null != installedPlugin);
                       if (plugin.isInstalled()
                               && plugin.getLatest_version_number() > installedPlugin.getLatest_version_number()) {
                           plugin.setInstalled(true);
                       }
                   } catch (Exception e) {
                        e.printStackTrace();
                   }
                });

        return marketPlace;
    }

    public static void installPlugin(String pluginId, String version, DataPipeline dataPipeline) throws Exception {

        MarketPlace marketPlace = getMarketPlace();
        Optional<Plugins> pluginObj = marketPlace.getPlugins().parallelStream().filter(f -> f.getUnique_id().equals(pluginId)).findAny();

        if (!pluginObj.isPresent()) {
            dataPipeline.put("message", "Plugin not found!");
            dataPipeline.put("status", false);
            throw new Exception("Plugin not found!");
        }

        Plugins plugin = pluginObj.get();

        int existed_latest_version_number = plugin.getLatest_version_number();

        Plugins installedPlugin = getInstalledPlugin(pluginId, dataPipeline);
        if (null != installedPlugin && existed_latest_version_number <= installedPlugin.getLatest_version_number()) {
            dataPipeline.put("message", "Plugin already installed");
            dataPipeline.put("status", false);
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

            updatePackagePlugin(plugin, dataPipeline);
            dataPipeline.put("status", true);
        } else {
            dataPipeline.put("message", "Checksum Mismatched");
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
        PluginPackage pluginPackage = getInstalledPlugins(dataPipeline);
        File file = new File(PropertyManager.getPackagePath(dataPipeline.rp.getTenant()) + "builds/plugin-package.json");

        Optional<Plugins> pluginObj = pluginPackage.getPlugins().parallelStream().filter(f -> f.getUnique_id().equals(plugins.getUnique_id())).findAny();

        if (pluginObj.isPresent()) {
            Plugins oldPlugin = pluginObj.get();
            pluginPackage.getPlugins().remove(oldPlugin);
            pluginPackage.getPlugins().add(plugins);
        } else {
            pluginPackage.setPlugins(Lists.newArrayList(plugins));
        }
        IOUtils.write(new Gson().toJson(pluginPackage), new FileOutputStream(file), StandardCharsets.UTF_8);
    }

    private static PluginPackage getInstalledPlugins(DataPipeline dataPipeline) throws IOException {
        PluginPackage pluginPackage = new PluginPackage();
        File file = new File(PropertyManager.getPackagePath(dataPipeline.rp.getTenant()) + "builds/plugin-package.json");

        if (!file.exists()) {
            pluginPackage = new PluginPackage();
            pluginPackage.setPlugins(Lists.newArrayList());
        } else {
            pluginPackage = new Gson().fromJson(new FileReader(file), PluginPackage.class);
        }
        return pluginPackage;
    }

    /**
     * @param uniqueId
     * @param dataPipeline
     * @return
     * @throws IOException
     */
    private static Plugins getInstalledPlugin(String uniqueId, DataPipeline dataPipeline) throws IOException {
        PluginPackage pluginPackage = getInstalledPlugins(dataPipeline);
        Optional<Plugins> pluginObj = pluginPackage.getPlugins().parallelStream().filter(f -> f.getUnique_id().equals(uniqueId)).findAny();
        if (pluginObj.isPresent()) {
            Plugins plugin = pluginObj.get();
            return plugin;
        } else {
            return null;
        }
    }


}