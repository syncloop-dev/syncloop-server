package packages.middleware.pub.docList;
import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.ServiceUtils;
import com.eka.middleware.template.SnippetException;
import java.util.*;
public final class addDoc{
	public static final void main(DataPipeline dataPipeline) throws SnippetException{
try {
		List<Object> inputList=dataPipeline.getAsList("docList");
        if(inputList==null)
          inputList=new ArrayList();
        Map<String,Object> inputDoc=dataPipeline.getAsMap("addDoc");
  		if(inputDoc!=null && inputDoc.size()>0)
            inputList.add(inputDoc);
        List<Object> addDocList=dataPipeline.getAsList("addDocList");
        if(addDocList!=null && addDocList.size()>0)
          inputList.add(addDocList);
  		dataPipeline.put("docList",inputList);
  } catch (Exception e) {
		dataPipeline.clear();
  		dataPipeline.put("error",e.getMessage());
    	throw new SnippetException(dataPipeline,"Snippet exception in addDoc service", new Exception(e));
  }
	}

}