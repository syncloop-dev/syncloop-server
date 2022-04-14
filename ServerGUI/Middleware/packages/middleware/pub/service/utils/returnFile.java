package packages.middleware.pub.service.utils;
import java.io.File;
import java.io.FileInputStream;
import java.net.URLConnection;
import java.util.regex.Pattern;

import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.ServiceUtils;
import com.eka.middleware.template.MultiPart;
import com.eka.middleware.template.SnippetException;
import java.io.InputStream;

import io.undertow.util.Headers;
public final class returnFile{
	public static final void main(DataPipeline dataPipeline) throws SnippetException{
try {
  byte[] fileData = (byte[])dataPipeline.get("fileData");
  String fileName = dataPipeline.getString("fileName");
  Object inputStream = dataPipeline.get("inputStream");
  String body = dataPipeline.getString("body");
  MultiPart mp = null;
  boolean octet=false;
  
  if(fileName!=null)
  	octet=true;

  if(inputStream!=null)
    mp = new MultiPart(dataPipeline, (InputStream)inputStream, fileName, octet);
  else
  if(fileName!=null)
  	mp = new MultiPart(dataPipeline, fileData, fileName);
  else if(fileData!=null){
    mp = new MultiPart(dataPipeline, fileData);
  	mp.putHeader(Headers.CONTENT_TYPE_STRING, "application/octet-stream");
  }else if(body!=null){
  	dataPipeline.setBody(body.getBytes());
  }
} catch (Throwable e) {
	dataPipeline.clear();
	dataPipeline.put("error", e.getMessage());
	e.printStackTrace();
}
	}

}