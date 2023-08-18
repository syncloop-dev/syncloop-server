package com.eka.middleware.auth;

import java.io.File;
import java.util.List;
import java.util.Map;

import com.eka.middleware.service.PropertyManager;
import com.eka.middleware.service.ServiceUtils;

public class ResourceAuthenticator {
//public static boolean isAllowed(final HttpServerExchange exchange) {
//	return isPublic(exchange);
//}

//public static boolean isPublic(final HttpServerExchange exchange) {
//	final SecurityContext context = exchange.getSecurityContext();
//	Account authAccount= null;
//	if(context!=null)
//		authAccount=context.getAuthenticatedAccount();
//	if(authAccount!=null)
//		return true;
//	String path=exchange.getRequestPath();
//	if(path.toLowerCase().startsWith("/files/gui/middleware/pub/server/ui/welcome"))
//		return true;
//	return false;
//}

public static boolean isConsumerAllowed(String resource, AuthAccount authAccount,String requestPath, String method) {
	if(authAccount==null)
		return false;
	String userID=authAccount.getUserId();
	requestPath=requestPath.toLowerCase();
	String serviceAlias=requestPath.split("/")[1];
	boolean canConsume=false;
	try {
		String packagePath=null;
//		if(authAccount.getAuthProfile()!=null && authAccount.getAuthProfile().get("tenant")!=null)
//			packagePath=PropertyManager.getPackagePath((String) authAccount.getAuthProfile().get("tenant"));
//		else
		String tenantName=(String)authAccount.getAuthProfile().get("tenant");
		if(tenantName==null)
			tenantName="default";
		packagePath=PropertyManager.getPackagePathByTenantName(tenantName);
		String path = (packagePath + resource.replace(".main", "#main").replace(".", "/")).replace("//", "/").replace("#main", ".service"); 
		File file=new File(path);
		if(!file.exists()) {
			path=(packagePath + resource.replace(".main", "#main").replace(".", "/")).replace("//", "/").replace("#main", ".sql");
			file=new File(path);
			if(!file.exists()) {
				path=(packagePath + resource.replace(".main", "#main").replace(".", "/")).replace("//", "/").replace("#main", ".flow");
				file=new File(path);
				if(!file.exists()) {
					path=(packagePath + resource.replace(".main", "#main").replace(".", "/")).replace("//", "/").replace("#main", ".api");
					file=new File(path);
					if (!file.exists()) {
						file = null;
					}
				}
			}
		}
		if(file!=null) {
			byte[] json=ServiceUtils.readAllBytes(file);
			Map<String, Object> map=ServiceUtils.jsonToMap(new String(json));
			String consumers=(String)map.get("consumers");
			Map<String, Object> profile = authAccount.getAuthProfile();
            List<String> userGroups=(List<String>)profile.get("groups");
  			
            if(consumers==null || consumers.trim().length()==0) {
            	for(String group: userGroups){
                    if(AuthAccount.STATIC_ADMIN_GROUP.equals(group)){
                      canConsume=true;
                      break;
                    }else if(AuthAccount.STATIC_DEFAULT_GROUP.equals(group) && (ServiceUtils.isPublicFolder(requestPath) && method.toLowerCase().equals("get"))) {
                    	canConsume=true;
                        break;
                    }
               }
            }
            else{
            	consumers=consumers+",";
            	consumers=consumers.toLowerCase();
              for(String group: userGroups){
                   if(consumers.contains(group.toLowerCase()+",")){
                     canConsume=true;
                     break;
                   }
              }
            }
		}else {
			Map<String, Object> profile = authAccount.getAuthProfile();
            List<String> userGroups=(List<String>)profile.get("groups");
            canConsume=false;
            for(String group: userGroups){
                    if(AuthAccount.STATIC_ADMIN_GROUP.equals(group)){
                      canConsume=true;
                      break;
                    }else if(AuthAccount.STATIC_DEFAULT_GROUP.equals(group) && serviceAlias.equalsIgnoreCase("files") && requestPath.contains("tenant")) {
                    	canConsume=true;
                        break;
                    }
               }
		}
	} catch (Exception e) {
		e.printStackTrace();
	}
	return canConsume;
}


}
