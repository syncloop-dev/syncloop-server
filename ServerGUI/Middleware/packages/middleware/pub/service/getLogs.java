package packages.middleware.pub.service;
import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.ServiceUtils;
import com.eka.middleware.template.SnippetException;
import java.io.File;
import com.eka.middleware.server.ServiceManager;
public final class getLogs{
	public static final void main(DataPipeline dataPipeline) throws SnippetException{

String resource=dataPipeline.getAsString("*pathParameters/resource");
String name=dataPipeline.getAsString("*pathParameters/id");
dataPipeline.clear();
File file = null;
if(name==null || name.equals("list")){
  file=new File(ServiceManager.packagePath + "/snapshots/" + resource);
  String files[]=file.list();
  dataPipeline.put("files",files);
}else{
  if(!name.endsWith("snap"))
    name+=".snap";
  file=new File(ServiceManager.packagePath + "/snapshots/" + resource + "/" + name);
  dataPipeline.setBody(file);
}
	}

}