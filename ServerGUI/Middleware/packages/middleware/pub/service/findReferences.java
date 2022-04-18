package packages.middleware.pub.service;
import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.ServiceUtils;
import com.eka.middleware.template.SnippetException;
import com.eka.middleware.server.ServiceManager;

import java.util.stream.Stream;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Map;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.util.ArrayList;
public final class findReferences{
	public static final void main(DataPipeline dataPipeline) throws SnippetException{
try {
		String serviceFqn=dataPipeline.getString("serviceFqn");
  		String packageDirectory=ServiceManager.packagePath+"packages/";
  		File dir=new File(packageDirectory);
  		List<String> list=parseDirectory(dir,serviceFqn,"|");
        dataPipeline.clear();
  		dataPipeline.put("list",list);
        //dataPipeline.log("***********************************************\n"+list);
        
  } catch (Exception e) {
		dataPipeline.clear();
  		dataPipeline.put("error",e.getMessage());
    	throw new SnippetException(dataPipeline,"Snippet exception", new Exception(e));
  }
	}
private static int searchStream(File pathFile, String word) throws Exception{

    InputStream inputStream = new FileInputStream(pathFile);
    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
    Stream<String> linesStream = bufferedReader.lines();
    List<String> list=linesStream.filter(a -> a.contains(word)).collect(Collectors.toList());
    return list.size();

}

private static List<String> parseDirectory(File dir,String keyWord,String padding) throws Exception {
   List<String> references=new ArrayList<String>();
   List<String> paths=new ArrayList<String>();
   //paths.add(padding+"packages");
   if(dir.exists()) {
		File[] list=dir.listFiles();
		for (File file : list) {
			if(file.isDirectory()) {
              paths.add((padding+"packages"+(file.getAbsolutePath().split("packages")[1])).replace("\\","/"));
              List<String> ref=parseDirectory(file,keyWord,padding+"-");
              if(ref.size()>0){
                references.addAll(paths);
			  	references.addAll(ref);
              }
              paths.clear();
			}else if(!file.getName().endsWith(".class")){
              //System.out.println(file.getAbsolutePath());
              int count=searchStream(file,keyWord);
              if(count>0){
                references.add((padding+"packages"+(file.getAbsolutePath().split("packages")[1])).replace("\\","/")+" : "+count+" times");
              }
            }
		}
	}
  return references;
}

}