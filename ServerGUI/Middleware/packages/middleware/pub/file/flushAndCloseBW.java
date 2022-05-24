package packages.middleware.pub.file;
import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.ServiceUtils;
import com.eka.middleware.template.SnippetException;
import java.io.BufferedWriter;
public final class flushAndCloseBW{
	public static final void main(DataPipeline dataPipeline) throws SnippetException{
try {
    	BufferedWriter bufferedWriter = (BufferedWriter)dataPipeline.get("bufferedWriter");
        bufferedWriter.flush();
  		bufferedWriter.close();
        dataPipeline.log("-----------Writing Bytes completed-------------");
  } catch (Exception e) {
		dataPipeline.clear();
  		dataPipeline.put("error",e.getMessage());
    	throw new SnippetException(dataPipeline,"Snippet exception", new Exception(e));
  }
	}

}