package packages.middleware.pub.json;
import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.ServiceUtils;
import com.eka.middleware.template.SnippetException;
import java.util.Map;
public final class fromJson{
	public static final void main(DataPipeline dataPipeline) throws SnippetException{
try{
  String jsonString=""; 
  jsonString=dataPipeline.getString("jsonString");
  Map<String,Object> map=ServiceUtils.jsonToMap("{\"jsonDoc\":"+jsonString+"}");
  //dataPipeline.log(jsonString);
  //System.out.println(map);
  dataPipeline.put("output",map);
  //dataPipeline.logDataPipeline();
}catch(Exception e){
	dataPipeline.clear();
  	dataPipeline.put("error",e.getMessage());
  	new SnippetException(dataPipeline,"Sneppet exception", new Exception(e));
}
	}

}