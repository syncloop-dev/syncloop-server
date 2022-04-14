package packages.middleware.pub.server.browse;
import java.io.File;
import java.io.FileInputStream;
import java.net.URLConnection;
import java.util.regex.Pattern;

import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.ServiceUtils;
import com.eka.middleware.template.MultiPart;
import com.eka.middleware.template.SnippetException;

import io.undertow.util.Headers;

public final class getFile{
	public static final void main(DataPipeline dataPipeline) throws SnippetException{
try {
			//System.out.println("**********************" + dataPipeline.getUrlPath());
			String location = dataPipeline.getUrlPath().split("GET/files/")[1];
			//System.out.println("++++++++++++++++++++++++++++++++++++" + location);
			String split[] = location.split(Pattern.quote("."));
			String ext = split[split.length - 1];
			location = ServiceUtils.getPackagesPath() + location;
			dataPipeline.clear();
			dataPipeline.put("Location", location);
			File file = new File(location);
			
			if(!file.exists()) {
				dataPipeline.setResponseStatus(404);
				dataPipeline.clear();
				dataPipeline.put("msg", "file not found");
				return;
			}
			
			String contentType = java.nio.file.Files.probeContentType(file.toPath());
			if (contentType == null) {
				if (ext.toLowerCase().equals("js"))
					contentType = "application/javascript";
				if (ext.toLowerCase().equals("json"))
					contentType = "application/json";
			}
			if(contentType==null) {
				URLConnection connection = file.toURL().openConnection();
			    contentType = connection.getContentType();
                connection.getInputStream().close();
				//contentType="application/";
			}
			//System.out.println("++++++++++++++++++++++++++++++++++++" + location);
			//System.out.println("++++++++++++++++++++++++++++++++++++" + contentType);
			MultiPart mp = new MultiPart(dataPipeline, new FileInputStream(file), false);
			mp.putHeader(Headers.CONTENT_TYPE_STRING, contentType);
		} catch (Throwable e) {
			dataPipeline.clear();
			dataPipeline.put("error", e.getMessage());
			// throw e;
		}
	}

}