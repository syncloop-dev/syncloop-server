package packages.middleware.pub.server.browse;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Base64;
import java.util.Map;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.ServiceUtils;
import com.eka.middleware.template.SnippetException; 
import com.eka.middleware.auth.AuthAccount;

public final class saveService{
	public static final void main(DataPipeline dataPipeline) throws SnippetException{
	try {
			//System.out.println("**********************"+dataPipeline.getUrlPath());
			String location = dataPipeline.getUrlPath().split("POST/service/")[1];
			//System.out.println("++++++++++++++++++++++++++++++++++++"+location);
			String split[] = location.split(Pattern.quote("."));
			String ext = split[split.length - 1];
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
                //System.out.println("***********"+json);
				Map<String, Object> jsonMap = ServiceUtils.jsonToMap(json);
				jsonMap.put("lockedByUser",loggedInUserId);
				java.nio.file.Files.write(file.toPath(), ServiceUtils.toPrettyJson(jsonMap).getBytes());
				//System.out.println("++++++++++++++++++++++++++++++++++++"+loggedInUserId);
				generateJavaClass(file,dataPipeline);
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
		} catch (Throwable e) {
			dataPipeline.clear();
			dataPipeline.put("error", e.getMessage());
			dataPipeline.logException(e);
			dataPipeline.setResponseStatus(500);
			// throw e;
		}
	}
private static ObjectMapper om=new ObjectMapper();
	public static void generateJavaClass(File file,DataPipeline dataPipeline)throws Exception {
		byte[] data = ServiceUtils.readAllBytes(file);
		String json = new String(data);
		Map<String, Object> jsonMap = ServiceUtils.jsonToMap(json);
		
		String pkg=jsonMap.get("package").toString();
		pkg=new String(Base64.getDecoder().decode(pkg));
		
		String imports=jsonMap.get("imports").toString();
		imports=new String(Base64.getDecoder().decode(imports));
		
		String className=file.getName().replace(".service", "");
		
		String classDef="public final class "+className+"{";
		
		String mainDef="	public static final void main(DataPipeline dataPipeline) throws SnippetException{";
		
		String mainFunc=jsonMap.get("main").toString();
		mainFunc=new String(Base64.getDecoder().decode(mainFunc));
		
		String mainDefClose="	}";
		
		String staticWorkspace=jsonMap.get("staticWorkspace").toString();
		if(staticWorkspace!=null && staticWorkspace.trim().length()>0)
			staticWorkspace=new String(Base64.getDecoder().decode(staticWorkspace));
		else
			staticWorkspace="";
		
		String classDefClose="}";
		
		//System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
		String fullCode=pkg+"\n"+imports+"\n"+classDef+"\n"+mainDef+"\n"+mainFunc+"\n"+mainDefClose+"\n"+staticWorkspace+"\n"+classDefClose;
		//System.out.println(fullCode);
		//System.out.println(className+".service");
		String javaFilePath=file.getAbsolutePath().replace(className+".service", className+".java");
		File javaFile=new File(javaFilePath);
		if (!javaFile.exists()) {
			javaFile.createNewFile();
		}
		System.out.println(javaFilePath);
		FileOutputStream fos = new FileOutputStream(javaFile);
		fos.write(fullCode.getBytes());
		fos.flush();
		fos.close();
		String fqn=pkg.replace("package ", "").replace(";","")+"."+className+".main";
		ServiceUtils.compileJavaCode(fqn, dataPipeline);
	}
	
	public static String getLocakedByUser(File file)throws Exception{
		byte[] data = ServiceUtils.readAllBytes(file);
		String json = new String(data);
		Map<String, Object> jsonMap = ServiceUtils.jsonToMap(json);
		if(jsonMap !=null && jsonMap.get("lockedByUser")==null)
			return null;
		return jsonMap.get("lockedByUser").toString();
	}
}