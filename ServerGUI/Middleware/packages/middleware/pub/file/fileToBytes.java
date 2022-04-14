package packages.middleware.pub.file;
import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.ServiceUtils;
import com.eka.middleware.template.SnippetException;
import java.io.File;
import java.io.FileOutputStream;
import org.apache.commons.io.IOUtils;
public final class fileToBytes{
	public static final void main(DataPipeline dataPipeline) throws SnippetException{
  try {
		String filePathWithName=dataPipeline.getString("filePathWithName");
        File file=new File(filePathWithName);
    	byte[] bytes=ServiceUtils.readAllBytes(file);
        dataPipeline.put("bytes",bytes);
  } catch (Exception e) {
		dataPipeline.clear();
  		dataPipeline.put("error",e.getMessage());
    	throw new SnippetException(dataPipeline,"Sneppet exception", new Exception(e));
  }
	}

}