package com.eka.middleware.test;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class TestConfigReader {

    private static final Properties properties;

    static {
        properties = new Properties();
        try {
            properties.load(new FileInputStream("src/com/eka/middleware/test/resources/TestConfig.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }
}

