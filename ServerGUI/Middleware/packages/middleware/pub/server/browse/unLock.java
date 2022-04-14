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
public final class unLock{
	public static final void main(DataPipeline dataPipeline) throws SnippetException{
try {
			System.out.println("**********************"+dataPipeline.getUrlPath());
			String location = dataPipeline.getUrlPath().split("POST/artifact/unlock/")[1];
			System.out.println("++++++++++++++++++++++++++++++++++++"+location);
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
  			byte[] data = ServiceUtils.readAllBytes(file);
			String json = new String(data);
			Map<String, Object> jsonMap = ServiceUtils.jsonToMap(json);
  			Map<String, Object> profile = authAccount.getProfile();
            List<String> userGroups=(List<String>)profile.get("groups");
  			String developers=null;
            if(jsonMap.get("developers")!=null)
             	developers=(String)jsonMap.get("developers");
  			boolean isDeveloper=false;
            if(developers!=null){
              developers=developers+",";
              for(String group: userGroups){
                   if(developers.contains(group+",")){
                     isDeveloper=true;
                     break;
                   }
              }
            }else
              isDeveloper=true;
  			dataPipeline.clear();
  			if(isDeveloper){
              Object obj=jsonMap.get("latest");
              if(obj==null){
                  jsonMap.put("lockedByUser",null);
              }else{
                  Map<String, Object> version=(Map<String, Object>)jsonMap.get("latest");
                  version.put("lockedByUser",null);
              }
              java.nio.file.Files.write(file.toPath(), ServiceUtils.toJson(jsonMap).getBytes());
              dataPipeline.put("status", "UnLocked");
            }else
              dataPipeline.put("status", "Access denied.");
		} catch (Throwable e) {
			dataPipeline.clear();
			dataPipeline.put("error", e.getMessage());
			dataPipeline.setResponseStatus(500);
			dataPipeline.put("status", "Not Modified");
			new SnippetException(dataPipeline,"Failed while saving file", new Exception(e));
		}
	}

}