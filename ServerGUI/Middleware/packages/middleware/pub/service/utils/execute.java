package packages.middleware.pub.service.utils;
import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.ServiceUtils;
import com.eka.middleware.template.SnippetException;
public final class execute{
	public static final void main(DataPipeline dataPipeline) throws SnippetException{
try{
	String fqn=dataPipeline.getString("fqn");
	dataPipeline.apply(fqn);
    dataPipeline.put("msg","Success");
  } catch (Exception e) {
		dataPipeline.clear();
  		dataPipeline.put("error","Error: "+e.getMessage());
        e.printStackTrace();
    	//throw new SnippetException(dataPipeline,"Snippet exception", new Exception(e));
  }
	}

}