package packages.middleware.pub.server.build.api;
import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.ServiceUtils;
import com.eka.middleware.template.SnippetException;
import org.apache.commons.io.IOUtils;

import com.eka.middleware.server.ServiceManager;
import com.eka.middleware.service.ServiceUtils;
import java.util.List;
import java.util.Map;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.FileUtils;
import com.eka.middleware.template.MultiPart;
public final class createBuild{
	public static final void main(DataPipeline dataPipeline) throws SnippetException{
try{
  //byte[] body=dataPipeline.getBody();
  //String json=new String(body);
  //String contentType=((String[])dataPipeline.getHeaders().get("Content-Type"))[0];
  List<Map<String,String>> artifacts=dataPipeline.getAsList("*payload");
  String buildName=dataPipeline.getString("buildName");
  Boolean includeDependencies=("true".equals(dataPipeline.getString("includeDependencies")));
  Boolean includeGlobalProperties=("true".equals(dataPipeline.getString("includeGlobalProperties")));
  Boolean includeLocalProperties=("true".equals(dataPipeline.getString("includeLocalProperties")));
  Boolean includeEndpoints=("true".equals(dataPipeline.getString("includeEndpoints")));
  
  	String packagePath=ServiceManager.packagePath;
	String buildsDirPath=packagePath+"builds/";
	String newBuildPath=buildsDirPath+buildName+"/";
  File newBuild=new File(newBuildPath);
	if(newBuild.exists())
	    dataPipeline.put("msg","build already exists with the name '"+buildName+"'. Please try another name.");
	  else{
	    newBuild.mkdirs();
	    for (Map<String,String> artifact : artifacts) {
	      File deployableAsset=new File(packagePath+artifact.get("asset")+"."+artifact.get("type"));
	      File deployableAssetJava=new File(packagePath+artifact.get("asset")+".java");
	      File deployableClass=new File(packagePath+artifact.get("asset")+".class");
	      File deployableGPAsset=null;//new File(packagePath+artifact.get("asset")+"."+artifact.get("type"));
	      File deployableLPAsset=null;//new File(packagePath+artifact.get("asset")+".java");
	      //File deployableURLAlias=new File(packagePath+artifact.get("asset")+".class");
	      if(includeGlobalProperties) {
	    	  //deployableGPAsset=new File(packagePath+artifact.get("asset")+"."+artifact.get("type"));
	      }
	      
	      if(deployableAsset.exists() && deployableAsset.isFile()){
	    	  File toFile=new File(newBuildPath+artifact.get("asset")+"."+artifact.get("type"));
	    	  toFile.getParentFile().mkdirs();
	    	  toFile.createNewFile();
              FileInputStream from=new FileInputStream(deployableAsset);
              FileOutputStream to=new FileOutputStream(toFile);
	    	  IOUtils.copy(from, to);
              to.flush();
              to.close();
              from.close();
              
	    	  if(deployableAssetJava.exists()) {
	    		  toFile=new File(newBuildPath+artifact.get("asset")+".java");
		    	  toFile.getParentFile().mkdirs();
		    	  toFile.createNewFile();
                  from=new FileInputStream(deployableAssetJava);
              	  to=new FileOutputStream(toFile);
		    	  IOUtils.copy(from, to);
                  to.flush();
                  to.close();
                  from.close();
	    	  }
	    	  if(deployableClass.exists()) {
	    		  toFile=new File(newBuildPath+artifact.get("asset")+".class");
		    	  toFile.getParentFile().mkdirs();
		    	  toFile.createNewFile();
                  from=new FileInputStream(deployableClass);
                  to=new FileOutputStream(toFile);
		    	  IOUtils.copy(from, to);
                  to.flush();
                  to.close();
                  from.close();
	    	  }
	      }
	    }
	    
	    //String sourceFile = "zipTest";
        FileOutputStream fos = new FileOutputStream(buildsDirPath+buildName+".zip");
        ZipOutputStream zipOut = new ZipOutputStream(fos);
        File fileToZip = new File(newBuildPath);
        ServiceUtils.zipFile(fileToZip, fileToZip.getName(), zipOut);
        zipOut.flush();
        zipOut.close();
        fos.flush();
        fos.close();
        //dataPipeline.log(fileToZip.getAbsolutePath());
        FileUtils.deleteDirectory(fileToZip);
        //new MultiPart(dataPipeline, new File(buildsDirPath+buildName+".zip"));
        dataPipeline.clear();
        dataPipeline.put("url","/files/builds/"+buildName+".zip");
	  }
  //dataPipeline.clear();
  dataPipeline.put("msg","Success");
}catch(Exception e){
	//dataPipeline.clear();
  	dataPipeline.put("error",e.getMessage());
}
	}

}