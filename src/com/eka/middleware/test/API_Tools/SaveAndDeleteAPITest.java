package com.eka.middleware.test.API_Tools;


import com.eka.middleware.auth.AuthAccount;
import com.eka.middleware.server.MiddlewareServer;
import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.PropertyManager;
import com.eka.middleware.service.RuntimePipeline;
import com.eka.middleware.service.ServiceUtils;
import com.eka.middleware.template.SnippetException;
import com.eka.middleware.template.Tenant;
import com.eka.middleware.test.TestConfigReader;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SaveAndDeleteAPITest {

    private static DataPipeline dataPipeline;
    private static RuntimePipeline rp;

    @BeforeAll
    public static void setUp() throws Exception {
        PropertyManager.initConfig(new String[]{TestConfigReader.getProperty("middleware.server.test")});
        UUID coId = UUID.randomUUID();
        rp = RuntimePipeline.create(Tenant.getTempTenant("default"), "test", coId.toString(), null, "standalone",
                null);
        assert rp != null;
        dataPipeline = rp.dataPipeLine;
    }

    @Test
    public void testSaveApiFile() throws Exception {

        String loggedInUserId = null;
        File file = null;
        String location = null;
        try {

            //Mock class
            DataPipeline dataPipelineMock = mock(DataPipeline.class);
            dataPipeline.appLog("OPERATION", "SaveAPI");
            when(dataPipelineMock.getUrlPath()).thenReturn(TestConfigReader.getProperty("testApi.data")).toString();

            location = String.valueOf(dataPipelineMock.getUrlPath());

            String split[] = location.split(Pattern.quote("."));
            String ext = split[split.length - 1];
            String flowRef = location;
            location = PropertyManager.getPackagePath(dataPipeline.rp.getTenant())
                    + location;
            dataPipeline.clear();
            dataPipeline.put("Location", location);
            dataPipeline.appLog("API_LOCATION", location);
            file = new File(location);

            AuthAccount authAccount = dataPipeline.getCurrentRuntimeAccount();
            loggedInUserId = authAccount.getUserId();
            dataPipeline.appLog("LOGGED_IN_USER_ID", loggedInUserId);
            String lockedByUser = null;
            boolean canUpdate = false;
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
                dataPipeline.appLog("NEW_API_CREATED", file.getName());
                canUpdate = true;
            } else {
                lockedByUser = getLockedByUser(file);

                canUpdate = (lockedByUser != null && lockedByUser.equals(loggedInUserId));
            }

            if (canUpdate) {
                String expectedJson = "{\"latest\":{\"createdTS\":\"\",\"input\":[{\"id\":\"j2_1\",\"text\":\"hi\",\"icon\":null,\"li_attr\":{\"id\":\"j2_1\"},\"a_attr\":{\"href\":\"#\",\"id\":\"j2_1_anchor\"},\"state\":{\"loaded\":true,\"opened\":false,\"selected\":true,\"disabled\":false},\"data\":{},\"children\":[],\"type\":\"string\"}],\"output\":[{\"id\":\"j3_1\",\"text\":\"hi\",\"icon\":null,\"li_attr\":{\"id\":\"j3_1\"},\"a_attr\":{\"href\":\"#\",\"id\":\"j3_1_anchor\"},\"state\":{\"loaded\":true,\"opened\":false,\"selected\":false,\"disabled\":false},\"data\":{},\"children\":[],\"type\":\"string\"}],\"api\":[{\"id\":\"j1_1\",\"text\":\"Transformer\",\"icon\":null,\"li_attr\":{\"id\":\"j1_1\"},\"a_attr\":{\"href\":\"#\",\"id\":\"j1_1_anchor\"},\"state\":{\"loaded\":true,\"opened\":false,\"selected\":true,\"disabled\":false},\"data\":{\"guid\":\"08f2eaa8-9238-4ad4-a6c9-6039bc318bbd\",\"transformers\":[],\"lines\":[],\"dropList\":[],\"createList\":[],\"initiateList\":[]},\"children\":[],\"type\":\"transformer\"}],\"api_info\":{\"title\":\"\",\"description\":\"\"}},\"consumers\":\"\",\"developers\":\"developers,developers\",\"enableServiceDocumentValidation\":false,\"created_on\":\"1714748716591\",\"modified_on\":1714748716592}";

                when(dataPipelineMock.getBody()).thenReturn(expectedJson.getBytes());
                byte[] jsonBytes = dataPipelineMock.getBody();
                String json = new String(jsonBytes);
                Map<String, Object> jsonMap = ServiceUtils.jsonToMap(json);
                Map<String, Object> version = (Map<String, Object>) jsonMap.get("latest");
                version.put("lockedByUser", loggedInUserId);
                dataPipeline.appLog("RESOURCE_LOCKED_BY_CURRENT_USER", lockedByUser);
                java.nio.file.Files.write(file.toPath(), ServiceUtils.toPrettyJson(jsonMap).getBytes());
                //System.out.println("++++++++++++++++++++++++++++++++++++"+loggedInUserId);
                generateJavaClass(file, flowRef, dataPipeline);
                dataPipeline.clear();
                dataPipeline.put("status", "Saved");
                dataPipeline.appLog("JAVA_CLASS_SAVED_FOR_API", file.getName());

            } else {
                dataPipeline.clear();
                dataPipeline.setResponseStatus(500);
                dataPipeline.put("status", "Not Modified");
                if (lockedByUser != null) {
                    dataPipeline.appLog("RESOURCE_LOCKED_BY_ANOTHER_USER", lockedByUser);
                    dataPipeline.put("error", "Resource is locked by another user('" + lockedByUser + "')");
                } else {
                    dataPipeline.put("error", "Please lock the resource first.");
                    dataPipeline.appLog("RESOURCE_NOT_LOCKED", "Please lock the resource first.");
                }
            }

        } catch (Throwable e) {
            e.printStackTrace();
            dataPipeline.clear();
            dataPipeline.put("error", e.getMessage());
            dataPipeline.appLog("SAVE_API_ERROR", e.getMessage());
            dataPipeline.setResponseStatus(500);
            dataPipeline.put("status", "Not Modified");
            new SnippetException(dataPipeline, "Failed while saving file", new Exception(e));
        }

        assertTrue(file.exists(), "File was created");
        assertEquals("Saved", dataPipeline.get("status"), "Status is \"Saved\"");
        assertEquals(loggedInUserId, getLockedByUser(file), "File is locked by the logged-in user");
        assertFalse(location.isEmpty(), "Location should not be empty");
        assertNotEquals(0, file.length(), "File should have non-zero length");

    }

    @Test
    public void testDeleteApiFile() throws Exception {
        File file = null;
        try {
            DataPipeline dataPipelineMock = mock(DataPipeline.class);
            dataPipeline.appLog("OPERATION", "delete");
            when(dataPipelineMock.getUrlPath()).thenReturn(TestConfigReader.getProperty("testApi.data")).toString();
            String location = String.valueOf(dataPipelineMock.getUrlPath());

            if (location.equals("packages/middleware.package")) {
                throw new IllegalStateException("Cannot delete the system packages.");
            }
            dataPipeline.appLog("EXTRACTED_LOCATION_FROM_URL", location);
            String split[] = location.split(Pattern.quote("."));
            String ext = split[split.length - 1];
            location = PropertyManager.getPackagePath(dataPipeline.rp.getTenant())
                    + location;
            if (location.contains(".package")) {
                location = location.replaceAll(Pattern.quote(".package"), "");
            } else if (location.contains(".folder")) {
                location = location.replaceAll(Pattern.quote(".folder"), "");
            }
            dataPipeline.clear();
            dataPipeline.appLog("LOCATION", location);
            dataPipeline.put("Location", location);
            file = new File(location);
            if (file.exists()) {
                if (file.isDirectory()) {
                    dataPipeline.appLog("OPERATION_EXECUTED", "Delete directory");
                    deleteDirectory(dataPipeline, file);
                } else {
                    if (deleteFile(dataPipeline, location, ext, file)) return;
                    dataPipeline.appLog("OPERATION_EXECUTED", "Delete file");
                }
            } else {
                dataPipeline.log("File is not existed ------>" + location);
                dataPipeline.appLog("EXECUTION_STOPPED", "File doesnot exist");
            }
            dataPipeline.clear();
            dataPipeline.appLog("FILE_DELETED_AT_LOCATION", location);
            dataPipeline.put("status", "Deleted");
        } catch (Throwable e) {
            dataPipeline.clear();
            dataPipeline.put("error", e.getMessage());
            dataPipeline.appLog("DELETE_ERROR", e.getMessage());
            dataPipeline.setResponseStatus(500);
            dataPipeline.put("status", "Failed to delete");
            new SnippetException(dataPipeline, "Failed to delete", new Exception(e));
        }

        assertFalse(file.exists(), "File should be deleted");
        String javaFilePath = file.getAbsolutePath().replace(file.getName(), file.getName().replace(".api", ".java"));
        assertFalse(new File(javaFilePath).exists(), "Java file should be deleted");
        String classFilePath = file.getAbsolutePath().replace(file.getName(), file.getName().replace(".api", ".class"));
        assertFalse(new File(classFilePath).exists(), "Class file should be deleted");

        // Assert status after deletion
        assertEquals("Deleted", dataPipeline.get("status"), "Status should be \"Deleted\"");

    }

    private static boolean deleteFile(DataPipeline dataPipeline, String location, String ext, File file) throws Exception {
        AuthAccount authAccount = dataPipeline.getCurrentRuntimeAccount();
        String loggedInUserId = authAccount.getUserId();
        String lockedByUser = getLockedByUser(file);
        dataPipeline.appLog("LOGGED_IN_USER_ID", loggedInUserId);
        Map<String, Object> profiles = authAccount.getAuthProfile();
        List<String> groupList = (List<String>)profiles.get("groups");
        boolean isSystemAdmin = groupList.contains("system-admin");

        if(!isSystemAdmin) {
            if(!loggedInUserId.equals(lockedByUser) && !(ext.equals("jdbc") || ext.equals("properties"))){
                if(lockedByUser!=null){
                    dataPipeline.put("error", "Resource is locked by another user" + lockedByUser + "'");
                    dataPipeline.appLog("RESOURCE_LOCKED_BY_ANOTHER_USER",lockedByUser);
                }
                else{
                    dataPipeline.put("error", "Please lock the service first.");
                    dataPipeline.appLog("RESOURCE_NOT_LOCKED", "Please lock the resource first.");
                }
                return true;
            }
        }

        String name= file.getName();
        String javaName=name.replace("."+ ext,".java");
        String javaClass=name.replace("."+ ext,".class");
        file.delete();
        String javalocation= location.replace(name,javaName);
        file =new File(javalocation);
        dataPipeline.appLog("DELETING_FILE_JAVALOCATION", javalocation);
        if(file.exists())
            file.delete();
        String classlocation= location.replace(name,javaClass);
        file =new File(classlocation);
        dataPipeline.appLog("DELETING_FILE_CLASSLOCATION",classlocation);
        if(file.exists())
            file.delete();
        return false;
    }

    public static void deleteDirectory(DataPipeline dataPipeline,File directory) {

        // if the file is directory or not
        if(directory.isDirectory()) {
            File[] files = directory.listFiles();

            // if the directory contains any file
            if(files != null) {
                for(File file : files) {

                    // recursive call if the subdirectory is non-empty
                    deleteDirectory(dataPipeline,file);
                }
            }
        }

        if(directory.delete()) {
            System.out.println(directory + " is deleted");
            dataPipeline.appLog("SERVICE_EXECUTED", "Directory gets deleted");
        }
        else {
            System.out.println("Directory not deleted");
            dataPipeline.appLog("SERVICE_EXECUTED", "Directory does not gets deleted");
        }
    }

    public static void generateJavaClass(File file,String flowRef, DataPipeline dataPipeline)throws Exception {
        dataPipeline.appLog("GENERATING_JAVA_CLASS_FOR_API",file.getName());
        String flowJavaTemplatePath=MiddlewareServer.getConfigFolderPath()+"apiJava.template";
        //System.out.println("flowJavaTemplatePath: "+flowJavaTemplatePath);
        String className=file.getName().replace(".api", "");
        //URL url = new URL(flowJavaTemplatePath);
        String fullCode="";
        String pkg=flowRef.replace("/"+file.getName(),"").replace("/",".");
        List<String> lines = FileUtils.readLines(new File(flowJavaTemplatePath), "UTF-8");
        for (String line: lines) {
            String codeLine=(line.replace("#flowRef",flowRef).replace("#package",pkg).replace("#className",className));
            fullCode+=codeLine+"\n";
            //dataPipeline.log("\n");
            //dataPipeline.log(codeLine);
        }
        //dataPipeline.log("\n");
        //return fullCode;

        //System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
        //String fullCode="";//pkg+"\n"+imports+"\n"+classDef+"\n"+mainDef+"\n"+mainFunc+"\n"+mainDefClose+"\n"+classDefClose;
        //System.out.println(fullCode);
        //System.out.println(className+".service");
        String javaFilePath=file.getAbsolutePath().replace(className+".api", className+".java");
        File javaFile=new File(javaFilePath);
        if (!javaFile.exists()) {
            javaFile.createNewFile();
        }
        //System.out.println(javaFilePath);
        FileOutputStream fos = new FileOutputStream(javaFile);
        fos.write(fullCode.getBytes());
        fos.flush();
        fos.close();
        String fqn=pkg.replace("package ", "").replace(";","")+"."+className+".main";
        //dataPipeline.log("fqn: "+fqn);
        ServiceUtils.compileJavaCode(fqn, dataPipeline);
    }

    public static String getLockedByUser(File file)throws Exception{
        byte[] data = ServiceUtils.readAllBytes(file);
        String json = new String(data);
        Map<String, Object> jsonMap = ServiceUtils.jsonToMap(json);
        Map<String, Object> version=(Map<String, Object>)jsonMap.get("latest");
        if(version !=null && version.get("lockedByUser")==null)
            return null;
        return version.get("lockedByUser").toString();
    }

}

