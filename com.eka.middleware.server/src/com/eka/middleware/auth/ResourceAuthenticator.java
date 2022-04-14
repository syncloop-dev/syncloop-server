package com.eka.middleware.auth;

import java.io.File;
import java.util.List;
import java.util.Map;

import com.eka.middleware.server.ServiceManager;
import com.eka.middleware.service.ServiceUtils;

import io.undertow.security.api.SecurityContext;
import io.undertow.server.HttpServerExchange;

public class ResourceAuthenticator {
public static boolean isAllowed(final HttpServerExchange exchange) {
	return isPublic(exchange);
}

public static boolean isPublic(final HttpServerExchange exchange) {
	final SecurityContext context = exchange.getSecurityContext();
	AuthAccount authAccount= null;
	if(context!=null)
		authAccount=(AuthAccount) context.getAuthenticatedAccount();
	if(authAccount!=null)
		return true;
	String path=exchange.getRequestPath();
	if(path.toLowerCase().startsWith("/files/gui/middleware/pub/server/ui/welcome"))
		return true;
	return false;
}

public static boolean isConsumerAllowed(String resource, AuthAccount authAccount) {
	if(authAccount==null)
		return true;
	String userID=authAccount.getUserId();
	boolean canConsume=false;
	try {
		String path = (ServiceManager.packagePath + resource.replace(".main", "#main").replace(".", "/")).replace("//", "/").replace("#main", ".service"); 
		File file=new File(path);
		if(!file.exists()) {
			path=(ServiceManager.packagePath + resource.replace(".main", "#main").replace(".", "/")).replace("//", "/").replace("#main", ".sql");
			file=new File(path);
			if(!file.exists()) {
				path=(ServiceManager.packagePath + resource.replace(".main", "#main").replace(".", "/")).replace("//", "/").replace("#main", ".flow");
				file=new File(path);
				if(!file.exists())
					file=null;
			}
		}
		if(file!=null) {
			byte[] json=ServiceUtils.readAllBytes(file);
			Map<String, Object> map=ServiceUtils.jsonToMap(new String(json));
			String consumers=(String)map.get("consumers");
			Map<String, Object> profile = authAccount.getProfile();
            List<String> userGroups=(List<String>)profile.get("groups");
  			
            if(consumers==null || consumers.trim().length()==0) {
            	for(String group: userGroups){
                    if("administrators".equals(group)){
                      canConsume=true;
                      break;
                    }
               }
            }
            else{
            	consumers=consumers+",";
              for(String group: userGroups){
                   if(consumers.contains(group+",")){
                     canConsume=true;
                     break;
                   }
              }
            }
		}else
			canConsume=true;
	} catch (Exception e) {
		e.printStackTrace();
	}

	return canConsume;
}


}
