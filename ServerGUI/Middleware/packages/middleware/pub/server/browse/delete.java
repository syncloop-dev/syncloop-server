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
public final class delete{
	public static final void main(DataPipeline dataPipeline) throws SnippetException{
try {
			String location = dataPipeline.getUrlPath().split("DELETE/files/")[1];
			String split[] = location.split(Pattern.quote("."));
			String ext = split[split.length - 1];
			location = ServiceUtils.getPackagesPath()
					+ location;
			dataPipeline.clear();
			//dataPipeline.put("Location", location); 
			File file = new File(location); 
			if(file.exists()) {
              	AuthAccount authAccount = dataPipeline.getCurrentRuntimeAccount();
				String loggedInUserId = authAccount.getUserId();
                //System.out.println("Logged in user: "+loggedInUserId);
                String lockedByUser=getLocakedByUser(file);
                //System.out.println("LockedBy user in user: "+lockedByUser);
              	if(!loggedInUserId.equals(lockedByUser)){
                  if(lockedByUser!=null)
                    dataPipeline.put("error", "Locked by user '"+lockedByUser+"'");
                  else
					dataPipeline.put("error", "Please lock the service first.");
                  return;
                }
                //System.out.println("LockedBy user in user: "+loggedInUserId);
                String name=file.getName();
              	String javaName=name.replace("."+ext,".java");
              	String javaClass=name.replace("."+ext,".class");
                System.out.println("Deleting "+location);
				file.delete();
                String javalocation=location.replace(name,javaName);
              	file=new File(javalocation);
              	System.out.println("Deleting "+javalocation);
              	if(file.exists())
                  file.delete();
              	String classlocation=location.replace(name,javaClass);
              	file=new File(classlocation);
              	System.out.println("Deleting "+classlocation);
                if(file.exists())
                  file.delete();
			}
			dataPipeline.clear();
			dataPipeline.put("status", "Deleted");
	} catch (Throwable e) {
			dataPipeline.clear();
			dataPipeline.put("error", e.getMessage());
			dataPipeline.setResponseStatus(500);
			dataPipeline.put("status", "Failed to delete");
			new SnippetException(dataPipeline,"Failed to delete", new Exception(e));
	}
	}
public static String getLocakedByUser(File file)throws Exception{
		byte[] data = ServiceUtils.readAllBytes(file);
		String json = new String(data);
		Map<String, Object> jsonMap = ServiceUtils.jsonToMap(json);
  		if(jsonMap.get("lockedByUser")!=null)
          return jsonMap.get("lockedByUser").toString();
  		if(jsonMap.get("latest")!=null){
          Map<String, Object> version=(Map<String, Object>)jsonMap.get("latest");
          if(version !=null && version.get("lockedByUser")==null)
            return null;
          return version.get("lockedByUser").toString();
        }else
          return null;
	}
}