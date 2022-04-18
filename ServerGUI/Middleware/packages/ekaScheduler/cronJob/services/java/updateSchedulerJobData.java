package packages.ekaScheduler.cronJob.services.java;
import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.ServiceUtils;
import com.eka.middleware.template.SnippetException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
public final class updateSchedulerJobData{
	public static final void main(DataPipeline dataPipeline) throws SnippetException{
try {
  
    Map<String,Map<String,String>> jobMapData=(Map<String,Map<String,String>>)dataPipeline.get("jobMapData");
    //dataPipeline.log("***********************"+jobMapData);
    List<Map<String,String>> jobList=dataPipeline.getAsList("jobList");
    //dataPipeline.log("***********************a"+jobMapData);
    for(Map<String,String> jobData : jobList){
      jobMapData.clear();
      //int id=(int)jobData.get("id");
      //jobData.put("id",id+"");
      dataPipeline.log("Updating schedules : "+jobData);
      Map<String,String> jd=jobMapData.get(jobData.get("id")+"");
      //dataPipeline.log("updateScheduler***********************2");
      if(jd!=null){
        //dataPipeline.log("updateScheduler***********************3");
        jd.put("cronExpression",jobData.get("cronExpression"));
        jd.put("enabled",jobData.get("enabled"));
      }else
        jobMapData.put(jobData.get("id")+"",jobData);
    }
    dataPipeline.clear();
    dataPipeline.put("msg","Success");
  } catch (Exception e) {
		dataPipeline.clear();
  		dataPipeline.put("error",e.getMessage());
    	throw new SnippetException(dataPipeline,"Snippet exception", new Exception(e));
  }
	}

}