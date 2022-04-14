package packages.middleware.pub.file;
import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.ServiceUtils;
import com.eka.middleware.template.SnippetException;
import java.io.File;
public final class verifyPath{
	public static final void main(DataPipeline dataPipeline) throws SnippetException{
 try {
		String filePathWithName=dataPipeline.getString("absolutePath");
        File file=new File(filePathWithName);
    	boolean exists=file.exists();
        dataPipeline.put("exists",exists);
  } catch (Exception e) {
		dataPipeline.clear();
  		dataPipeline.put("error",e.getMessage());
    	throw new SnippetException(dataPipeline,"Sneppet exception", new Exception(e));
  }
	}

}