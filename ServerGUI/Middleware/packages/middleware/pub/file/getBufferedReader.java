package packages.middleware.pub.file;
import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.ServiceUtils;
import com.eka.middleware.template.SnippetException;
import java.io.BufferedReader;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
public final class getBufferedReader{
	public static final void main(DataPipeline dataPipeline) throws SnippetException{
try {
		String filePathWithName=dataPipeline.getString("filePathWithName");
        File file=new File(filePathWithName);
    	BufferedReader bufferedReader = Files.newBufferedReader(Path.of(file.toURI()));
        dataPipeline.put("bufferedReader",bufferedReader);
  } catch (Exception e) {
		dataPipeline.clear();
  		dataPipeline.put("error",e.getMessage());
    	throw new SnippetException(dataPipeline,"Sneppet exception", new Exception(e));
  }
	}

}