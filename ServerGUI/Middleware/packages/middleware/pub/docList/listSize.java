package packages.middleware.pub.docList;
import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.ServiceUtils;
import com.eka.middleware.template.SnippetException;
import java.util.*;
public final class listSize{
	public static final void main(DataPipeline dataPipeline) throws SnippetException{
try {
		List<Object> inputList=dataPipeline.getAsList("docList");
        if(inputList!=null)
  			dataPipeline.put("size",inputList.size());
  		else
    		dataPipeline.put("size",0);
  } catch (Exception e) {
		dataPipeline.clear();
  		dataPipeline.put("error",e.getMessage());
    	throw new SnippetException(dataPipeline,"Snippet exception", new Exception(e));
  }
	}

}