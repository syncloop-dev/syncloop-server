package packages.middleware.pub.service;
import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.ServiceUtils;
import com.eka.middleware.template.SnippetException;
public final class sleep{
	public static final void main(DataPipeline dataPipeline) throws SnippetException{
try{
String milli=dataPipeline.getString("milli");
int delay=Integer.parseInt(milli);
Thread.sleep(delay);
}catch(Exception e){
  dataPipeline.clear();
  dataPipeline.put("error",e.getMessage());
  throw new SnippetException(dataPipeline,"Snippet exception", new Exception(e));
}
	}

}