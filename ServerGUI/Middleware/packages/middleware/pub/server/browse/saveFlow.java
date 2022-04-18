package packages.middleware.pub.server.browse;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Base64;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.List;
import org.apache.commons.io.FileUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.ServiceUtils;
import com.eka.middleware.template.SnippetException; 
import com.eka.middleware.auth.AuthAccount;
import java.net.URL;

import com.eka.middleware.server.MiddlewareServer;
public final class saveFlow{
	public static final void main(DataPipeline dataPipeline) throws SnippetException{
try {
			//System.out.println("**********************"+dataPipeline.getUrlPath());
			String location = dataPipeline.getUrlPath().split("POST/flow/")[1];
			//System.out.println("++++++++++++++++++++++++++++++++++++"+location);
			String split[] = location.split(Pattern.quote("."));
			String ext = split[split.length - 1];
  			String flowRef=location;
			location = ServiceUtils.getPackagesPath()
					+ location;
			dataPipeline.clear();
			dataPipeline.put("Location", location);
			File file = new File(location);
  
  			AuthAccount authAccount = dataPipeline.getCurrentRuntimeAccount();
			String loggedInUserId = authAccount.getUserId();
			String lockedByUser = null;
			boolean canUpdate=false;
			if(!file.exists()) {
				file.getParentFile().mkdirs();
				file.createNewFile();
				canUpdate=true;
			}else{
				lockedByUser = getLocakedByUser(file);
				
				canUpdate=(lockedByUser!=null && lockedByUser.equals(loggedInUserId));
			}
			
			if(canUpdate){
				String json = new String(dataPipeline.getBody());
				Map<String, Object> jsonMap = ServiceUtils.jsonToMap(json);
                Map<String, Object> version=(Map<String, Object>)jsonMap.get("latest");
				version.put("lockedByUser",loggedInUserId);
				java.nio.file.Files.write(file.toPath(), ServiceUtils.toPrettyJson(jsonMap).getBytes());
				//System.out.println("++++++++++++++++++++++++++++++++++++"+loggedInUserId);
				generateJavaClass(file,flowRef,dataPipeline);
				dataPipeline.clear();
				dataPipeline.put("status", "Saved");
			}else{
				dataPipeline.clear();
				dataPipeline.setResponseStatus(500);
				dataPipeline.put("status", "Not Modified");
				if(lockedByUser!=null)
					dataPipeline.put("error", "Resource is locked by another user('"+lockedByUser+"')");
              	else
                    dataPipeline.put("error", "Please lock the resource first.");	
			}
			/*java.nio.file.Files.write(file.toPath(), dataPipeline.getBody());
			System.out.println("++++++++++++++++++++++++++++++++++++"+location);
			dataPipeline.clear();
  			generateJavaClass(file,flowRef,dataPipeline);
			dataPipeline.put("status", "Saved");
  			*/
  			//dataPipeline.log(fullCode);
		} catch (Throwable e) {
			dataPipeline.clear();
			dataPipeline.put("error", e.getMessage());
			dataPipeline.setResponseStatus(500);
			dataPipeline.put("status", "Not Modified");
			new SnippetException(dataPipeline,"Failed while saving file", new Exception(e));
		}
	}
public static void generateJavaClass(File file,String flowRef, DataPipeline dataPipeline)throws Exception {
	String flowJavaTemplatePath=MiddlewareServer.getConfigFolderPath()+"flowJava.template";
    //System.out.println("flowJavaTemplatePath: "+flowJavaTemplatePath);
  	String className=file.getName().replace(".flow", "");
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
		String javaFilePath=file.getAbsolutePath().replace(className+".flow", className+".java");
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
	
	public static String getLocakedByUser(File file)throws Exception{
		byte[] data = ServiceUtils.readAllBytes(file);
		String json = new String(data);
		Map<String, Object> jsonMap = ServiceUtils.jsonToMap(json);
        Map<String, Object> version=(Map<String, Object>)jsonMap.get("latest");
		if(version !=null && version.get("lockedByUser")==null)
			return null;
		return version.get("lockedByUser").toString();
	}
}