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
public final class lock{
	public static final void main(DataPipeline dataPipeline) throws SnippetException{
try{
			//System.out.println("**********************"+dataPipeline.getUrlPath());
			String location = dataPipeline.getUrlPath().split("POST/artifact/lock/")[1];
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
  			byte[] data = ServiceUtils.readAllBytes(file);
			String json = new String(data);
			Map<String, Object> jsonMap = ServiceUtils.jsonToMap(json);
  
  			Map<String, Object> profile = authAccount.getProfile();
            List<String> userGroups=(List<String>)profile.get("groups");
            //String consumers="";
            String developers=null;
            if(jsonMap.get("developers")!=null)
             	developers=(String)jsonMap.get("developers");
  			boolean isDeveloper=false;
            if(developers!=null && developers.trim().length()>2){
              developers=developers+",";
              for(String group: userGroups){
                   if(developers.contains(group+",")){
                     isDeveloper=true;
                     break;
                   }
              }
            }else
              isDeveloper=true;
  			
  			Object obj=jsonMap.get("latest");
  			Object lockedByUser=null;
  			if(obj==null){
                lockedByUser=jsonMap.get("lockedByUser");
                if(lockedByUser==null){
            		jsonMap.put("lockedByUser",loggedInUserId);
                	lockedByUser=loggedInUserId;
                }
            }else{
            	Map<String, Object> version=(Map<String, Object>)jsonMap.get("latest");
              	lockedByUser=version.get("lockedByUser");
              	if(lockedByUser==null){
            		version.put("lockedByUser",loggedInUserId);
                	lockedByUser=loggedInUserId;
                }
            }
			
			dataPipeline.clear();
  			if(lockedByUser!=null && !loggedInUserId.equals(lockedByUser))
            	dataPipeline.put("status", "Locked by another user('"+lockedByUser+"')"); 
  			else if(isDeveloper){
				java.nio.file.Files.write(file.toPath(), ServiceUtils.toJson(jsonMap).getBytes());
                dataPipeline.put("status", "Locked");
            }else{
            	dataPipeline.put("status", "Access denied. Please ask owner to add you in the developers group of this service");
            }
		} catch (Throwable e) {
			dataPipeline.clear();
			dataPipeline.put("error", e.getMessage());
			dataPipeline.setResponseStatus(500);
			dataPipeline.put("status", "Not Modified");
			new SnippetException(dataPipeline,"Failed while saving file", new Exception(e));
		}
	}

}