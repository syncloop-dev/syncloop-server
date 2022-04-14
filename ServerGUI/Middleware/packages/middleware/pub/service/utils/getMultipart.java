package packages.middleware.pub.service.utils;
import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.ServiceUtils;
import com.eka.middleware.template.SnippetException;
import com.eka.middleware.service.ServiceUtils;
import com.eka.middleware.template.MultiPart;
import java.util.Map;
public final class getMultipart{
	public static final void main(DataPipeline dataPipeline) throws SnippetException{
try {
  Map<String,Object> formData=null;
  if(dataPipeline.getMultiPart()!=null)
    formData=dataPipeline.getMultiPart().formData;
  if(formData!=null)
  	dataPipeline.put("formData",formData);
  else{
  	 byte[] bytes = dataPipeline.getBody();
  	 dataPipeline.put("bytes",bytes);
  }
} catch (Exception e) {
	dataPipeline.clear();
	dataPipeline.put("error", e.getMessage());
	e.printStackTrace();
}
	}

}