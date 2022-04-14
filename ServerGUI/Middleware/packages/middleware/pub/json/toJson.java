package packages.middleware.pub.json;
import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.ServiceUtils;
import com.eka.middleware.template.SnippetException;
import java.util.Map;
import java.util.Set;
public final class toJson{
	public static final void main(DataPipeline dataPipeline) throws SnippetException{
try{
  Map<String,Object> map=dataPipeline.getAsMap("root");
  Boolean wrapIt=dataPipeline.getAsBoolean("enableRootWrapper");
  String jsonString=""; 
  jsonString=ServiceUtils.toJson(map);
  dataPipeline.put("jsonString",jsonString);
  
}catch(Exception e){
	dataPipeline.clear();
  	dataPipeline.put("error",e.getMessage());
  	new SnippetException(dataPipeline,"Sneppet exception", new Exception(e));
}

	}

}