package packages.middleware.pub.server.browse;
import java.io.File;
import java.util.regex.Pattern;

import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.ServiceUtils;
import com.eka.middleware.template.SnippetException;
public final class saveFile{
	public static final void main(DataPipeline dataPipeline) throws SnippetException{
try {
			//System.out.println("**********************"+dataPipeline.getUrlPath());
			String location = dataPipeline.getUrlPath().split("POST/files/")[1];
			//System.out.println("++++++++++++++++++++++++++++++++++++"+location);
			String split[] = location.split(Pattern.quote("."));
			String ext = split[split.length - 1];
			location = ServiceUtils.getPackagesPath()
					+ location;
			dataPipeline.clear();
			dataPipeline.put("Location", location);
			File file = new File(location);
  			//System.out.println("Saving "+location);
			if(!file.exists()) {
				file.getParentFile().mkdirs();
				file.createNewFile();
			}
			java.nio.file.Files.write(file.toPath(), dataPipeline.getBody());
			//System.out.println("++++++++++++++++++++++++++++++++++++"+location);
			dataPipeline.clear();
			dataPipeline.put("status", "Saved");
		} catch (Throwable e) {
			dataPipeline.clear();
			dataPipeline.put("error", e.getMessage());
			dataPipeline.setResponseStatus(500);
			dataPipeline.put("status", "Not Modified");
			new SnippetException(dataPipeline,"Failed while saving file", new Exception(e));
		}
	}


}