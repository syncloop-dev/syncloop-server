package packages.middleware.pub.file;
import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.ServiceUtils;
import com.eka.middleware.template.SnippetException;
import java.io.File;
import java.io.FileOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
public final class stringToFile{
	public static final void main(DataPipeline dataPipeline) throws SnippetException{
try {
		String string=dataPipeline.getString("string");
  		byte[] bytes=string.getBytes();
  		byte[] oldBytes=null;
  		String filePathWithName=dataPipeline.getString("filePathWithName");
        Boolean append=dataPipeline.getAsBoolean("append");
  		File file=new File(filePathWithName);
  		if(append){
        	oldBytes=ServiceUtils.readAllBytes(file);
            bytes=ArrayUtils.addAll(bytes, oldBytes);
            
        }
  		String size=bytes.length+"";
  		FileOutputStream fo=new FileOutputStream(file);
		IOUtils.write(bytes, fo);
  		fo.flush();
  		fo.close();
        dataPipeline.put("size",size);
	} catch (Exception e) {
		dataPipeline.clear();
  		dataPipeline.put("error",e.getMessage());
    	throw new SnippetException(dataPipeline,"Sneppet exception", new Exception(e));
	}
	}

}