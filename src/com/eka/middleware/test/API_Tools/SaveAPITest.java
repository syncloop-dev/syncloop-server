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

public class SaveAPITest {

    @Test
    public void testSaveApiFile() throws Exception {

        PropertyManager.initConfig(new String[]{TestConfigReader.getProperty("middleware.server.test")});
        UUID coId = UUID.randomUUID();
        RuntimePipeline rp = RuntimePipeline.create(Tenant.getTempTenant("default"), "test", coId.toString(), null, "standalone",
                null);
        assert rp != null;
        DataPipeline dataPipeline = rp.dataPipeLine;

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

