package packages.middleware.pub.file;
import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.ServiceUtils;
import com.eka.middleware.template.SnippetException;
import java.io.BufferedReader;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
public final class readLines{
	public static final void main(DataPipeline dataPipeline) throws SnippetException{
try {
		BufferedReader bufferedReader=(BufferedReader)dataPipeline.get("bufferedReader");
		Integer number=dataPipeline.getInteger("number");
        List<String> lines=new ArrayList<String>();
        String line=null;
        while(number-->0 && (line=bufferedReader.readLine())!=null){
        	lines.add(line);
        }
        if(lines.size()==0)
        	dataPipeline.put("lines",null);
  		else
        	dataPipeline.put("lines",lines.toArray(new String[lines.size()]));
  } catch (Exception e) {
		dataPipeline.clear();
  		dataPipeline.put("error",e.getMessage());
    	throw new SnippetException(dataPipeline,"Snippet exception", new Exception(e));
  }
	}

}