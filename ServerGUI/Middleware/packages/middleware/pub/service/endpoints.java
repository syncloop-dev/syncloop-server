package packages.middleware.pub.service;
import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.ServiceUtils;
import com.eka.middleware.template.SnippetException;
import java.util.*;
public final class endpoints{
	public static final void main(DataPipeline dataPipeline) throws SnippetException{
try{
  String keyword=dataPipeline.getString("keyword");
  List<String> endpointList=ServiceUtils.searchEndpoints(keyword);
  String[] endpoints=endpointList.toArray(new String[endpointList.size()]);
  dataPipeline.put("endpoints",endpoints);
  } catch (Exception e) {
		dataPipeline.clear();
  		dataPipeline.put("error",e.getMessage());
    	throw new SnippetException(dataPipeline,"Snippet exception", new Exception(e));
  }
	}

}