package com.eka.middleware;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;

import com.eka.middleware.server.ServiceManager;
import com.eka.middleware.service.ServiceUtils;

public class test {
public static void main(String[] args) throws Exception {
	  List<Map<String,String>> artifacts=null;;
	  String buildName="";
	String packagePath=ServiceManager.packagePath;
	String buildsDirPath=packagePath+"builds/";
	String newBuildPath=buildsDirPath+buildName;
	Boolean includeDependencies=false;
	  Boolean includeGlobalProperties=false;
	  Boolean includeLocalProperties=false;
	  Boolean includeEndpoints=false;
	
	File newBuild=new File(newBuildPath);
	if(newBuild.exists());
	   // dataPipeline.put("msg","build already exists with the name '"+buildName+"'. Please try another name.");
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
	    	  IOUtils.copy(new FileInputStream(deployableAsset), new FileOutputStream(toFile));
	    	  if(deployableAssetJava.exists()) {
	    		  toFile=new File(newBuildPath+artifact.get("asset")+".java");
		    	  toFile.getParentFile().mkdirs();
		    	  toFile.createNewFile();
		    	  IOUtils.copy(new FileInputStream(deployableAssetJava), new FileOutputStream(toFile));
	    	  }
	    	  if(deployableClass.exists()) {
	    		  toFile=new File(newBuildPath+artifact.get("asset")+".class");
		    	  toFile.getParentFile().mkdirs();
		    	  toFile.createNewFile();
		    	  IOUtils.copy(new FileInputStream(deployableClass), new FileOutputStream(toFile));
	    	  }
	      }
	    }
	    
	    //String sourceFile = "zipTest";
        FileOutputStream fos = new FileOutputStream(buildsDirPath+buildName+".zip");
        ZipOutputStream zipOut = new ZipOutputStream(fos);
        File fileToZip = new File(newBuildPath);
        ServiceUtils.zipFile(fileToZip, fileToZip.getName(), zipOut);
        zipOut.close();
        fos.close();
        fileToZip.delete();
	  }
	
}

public static void parseDirectory(File dir) {
	if(dir.exists()) {
		File[] list=dir.listFiles();
		for (File file : list) {
			if(file.isDirectory()) {
				parseDirectory(file);
			}
			System.out.println(file.getAbsolutePath());
		}
	}
}
}
