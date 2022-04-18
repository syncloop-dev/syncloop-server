package packages.middleware.pub.server.browse;
import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.ServiceUtils;
import com.eka.middleware.template.SnippetException;
import java.io.InputStream;
import java.io.File;
import org.apache.commons.io.IOUtils;
import java.io.FileOutputStream;
public final class uploadFile{
	public static final void main(DataPipeline dataPipeline) throws SnippetException{
try{
  
  String folder=dataPipeline.getString("dest");
  InputStream is=dataPipeline.getFile("file");
  String fileName=dataPipeline.getFileName("file");
  String location = ServiceUtils.getPackagesPath()+folder+"/dependency/jars/"+fileName;
  File dir=new File(ServiceUtils.getPackagesPath()+folder+"/dependency/jars/");
  if(!dir.exists())
    dir.mkdirs();
  IOUtils.copy(is,new FileOutputStream(new File(location)));
  dataPipeline.clear();
  dataPipeline.put("status", "Saved");
  
}catch(Exception e){
	dataPipeline.clear();
	dataPipeline.put("error", e.getMessage());
	dataPipeline.setResponseStatus(500);
	dataPipeline.put("status", "Not Modified");
	new SnippetException(dataPipeline,"Failed while saving file", new Exception(e));
}
	}

}