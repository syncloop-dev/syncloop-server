package packages.middleware.pub.file;
import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.ServiceUtils;
import com.eka.middleware.template.SnippetException;
import java.io.BufferedWriter;
import java.util.*;
public final class writeLines{
	public static final void main(DataPipeline dataPipeline) throws SnippetException{
try {
  //dataPipeline.log("+++++++++++++++++++++++++++++++++++++++1");
		List<String> lines=dataPipeline.getAsList("lines");
  //dataPipeline.log("+++++++++++++++++++++++++++++++++++++++2");
    	BufferedWriter bufferedWriter = (BufferedWriter)dataPipeline.get("bufferedWriter");
  //dataPipeline.log("+++++++++++++++++++++++++++++++++++++++3");
        for(String line: lines){
  //dataPipeline.log(line.toString());
  			bufferedWriter.write(line);
            bufferedWriter.newLine();
        }
 //       dataPipeline.log("+++++++++++Writing Bytes+++++++++++++++++++++++");
  		//bufferedWriter.flush();
        //dataPipeline.log("-----------Writing Bytes completed-------------");
  } catch (Exception e) {
		dataPipeline.clear();
  		dataPipeline.put("error",e.getMessage());
    	throw new SnippetException(dataPipeline,"Snippet exception", new Exception(e));
  }
	}

}