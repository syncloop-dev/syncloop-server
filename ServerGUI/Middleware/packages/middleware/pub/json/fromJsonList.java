package packages.middleware.pub.json;
import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.ServiceUtils;
import com.eka.middleware.template.SnippetException;
import java.util.*;
public final class fromJsonList{
	public static final void main(DataPipeline dataPipeline) throws SnippetException{
try{
  //String jsonString=""; 
  String[] jsonList=(String[])dataPipeline.get("lines");
  List<Object> root=new ArrayList<>();
  for(String jsonString: jsonList){
    //dataPipeline.log(jsonString);
    Map<String,Object> map=ServiceUtils.jsonToMap("{\"jsonDoc\":"+jsonString+"}");
    Object obj=map.get("jsonDoc");
    root.add(obj);
  }
  dataPipeline.put("root",root);
  //dataPipeline.logDataPipeline();
}catch(Exception e){
	dataPipeline.clear();
  	dataPipeline.put("error",e.getMessage());
  	new SnippetException(dataPipeline,"Sneppet exception", new Exception(e));
}
	}

}