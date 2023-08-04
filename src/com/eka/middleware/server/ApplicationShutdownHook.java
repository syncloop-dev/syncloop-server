package com.eka.middleware.server;

import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.PropertyManager;
import com.eka.middleware.template.SystemException;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.io.IOException;


public class ApplicationShutdownHook implements Runnable {

    private static Integer EXIT_CODE = 0;
    public static String[] arg;

//    private static ProcessHandle lastProcess;
    private static long pid;

    /*
    @Override
    public void run() {
        if (EXIT_CODE == 1) {

            try {
                System.setProperty("jdk.httpclient.allowRestrictedHeaders", System.getProperty("jdk.httpclient.allowRestrictedHeaders"));
                System.setProperty("java.awt.headless", System.getProperty("java.awt.headless"));
                System.setProperty("CONTAINER_DEPLOYMENT", System.getProperty("CONTAINER_DEPLOYMENT"));
                System.setProperty("CONTAINER_ON_PRIM_DEPLOYMENT", System.getProperty("CONTAINER_ON_PRIM_DEPLOYMENT"));
                System.setProperty("COMMUNITY_DEPLOYMENT", System.getProperty("COMMUNITY_DEPLOYMENT"));
                System.setProperty("CORE_DEPLOYMENT", System.getProperty("CORE_DEPLOYMENT"));
                System.setProperty("com.sun.jndi.ldap.object.disableEndpointIdentification", System.getProperty("com.sun.jndi.ldap.object.disableEndpointIdentification"));

                System.out.println("Restarting WAITING...");
                Runtime.getRuntime().exec("taskkill /F /PID " + pid);
                System.out.println("Restarting...");

                //MiddlewareServer.main(arg);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void restartServer(DataPipeline dataPipeline) throws Exception {

        if (!Boolean.parseBoolean(System.getProperty("CONTAINER_ON_PRIM_DEPLOYMENT"))) {
            throw new Exception("Restart is not allow");
        }

        EXIT_CODE = 1;
        System.out.println("Shutting down...");
        System.out.println("Undertow going down...");
//        MiddlewareServer.server.stop();
        System.out.println("Undertow down...");
        System.exit(EXIT_CODE);
    }

    public static void getCurrentProcess() {
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        String processName = runtimeMXBean.getName();
        pid = Long.parseLong(processName.split("@")[0]);
        ProcessHandle processHandle = ProcessHandle.of(pid).get();
        lastProcess = processHandle;
    }*/

        @Override
        public void run() {
            if (EXIT_CODE == 1) {
                try {

                    System.out.println("Restarting...");
                    restartApplication();
                    System.out.println("Application restarted.");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

        }

        public static void restartServer(DataPipeline dataPipeline) throws Exception {
            if (!Boolean.parseBoolean(System.getProperty("CONTAINER_ON_PRIM_DEPLOYMENT"))) {
                throw new Exception("Restart is not allowed.");
            }

            if (!dataPipeline.rp.getTenant().getName().equals("default")) {
                throw new Exception("Restart is not allowed.");
            }

            System.out.println("Restarting...");
            restartApplication();
            System.out.println("Application restarted.");
        }

    private static void restartApplication() throws IOException, InterruptedException {
        String restartScriptPath = "/Users/divyansh/Desktop/Syncloop/Local_CodeBase/ekamw-distributions-main/restart-server.sh";

        ProcessBuilder processBuilder;
        if (isUnix()) {
            processBuilder = new ProcessBuilder("sh", restartScriptPath);
        } else if (isWindows()) {
            processBuilder = new ProcessBuilder("cmd", "/c", restartScriptPath);
        } else {
            throw new UnsupportedOperationException("Unsupported OS. Cannot restart the server.");
        }

        Process process = processBuilder.start();
        process.waitFor();
    }

    private static boolean isUnix() {
        String os = System.getProperty("os.name").toLowerCase();
        return (os.contains("nix") || os.contains("nux") || os.contains("mac"));
    }

    private static boolean isWindows() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("win");
    }




}
