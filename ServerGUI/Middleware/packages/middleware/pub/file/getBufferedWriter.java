package packages.middleware.pub.file;
import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.ServiceUtils;
import com.eka.middleware.template.SnippetException;
import java.io.BufferedWriter;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
public final class getBufferedWriter{
	public static final void main(DataPipeline dataPipeline) throws SnippetException{
try {
		String filePathWithName=dataPipeline.getString("filePathWithName");
        File file=new File(filePathWithName);
        file.getParentFile().mkdirs();
        file.createNewFile();
    	BufferedWriter bufferedWriter = Files.newBufferedWriter(Path.of(file.toURI()));
        dataPipeline.put("bufferedWriter",bufferedWriter);
  } catch (Exception e) {
		dataPipeline.clear();
  		dataPipeline.put("error",e.getMessage());
    	throw new SnippetException(dataPipeline,"Snippet exception", new Exception(e));
  }
	}

}