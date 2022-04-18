package packages.ekaScheduler.cronJob.api;
import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.ServiceUtils;
import com.eka.middleware.template.SnippetException;
import com.eka.middleware.service.RuntimePipeline;

import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;


import org.quartz.Scheduler;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

public final class createJobSchedule{
	public static final void main(DataPipeline dataPipeline) throws SnippetException{
	try{
       SchedulerFactory schedFact = new StdSchedulerFactory("D:\\ekamw\\lib\\quartz.properties");
       Scheduler sched = schedFact.getScheduler();
      
       JobDetail job = JobBuilder.newJob(SimpleJob.class)
      .withIdentity("myJob", "group1")
      .usingJobData("fqn", "packages.ekaScheduler.cronJob.api.createJobSchedule.main")
      .usingJobData("myFloatValue", 3.141f)
      .build();
      
      Trigger trigger = TriggerBuilder.newTrigger().withIdentity("myTrigger", "group1").
        startNow().withSchedule(SimpleScheduleBuilder.
        simpleSchedule().withIntervalInSeconds(40).repeatForever()).build();
	  JobKey jk=trigger.getJobKey();
      
      sched.scheduleJob(job, trigger);
      sched.start();
      //sched.pauseJob(jk);*/
    }catch(Exception e){
      dataPipeline.put("error",e.getMessage());
    }

	}
class SimpleJob implements Job {
    public void execute(JobExecutionContext context) throws JobExecutionException {  
      try{
      JobDataMap dataMap = context.getJobDetail().getJobDataMap();
      String serviceFqn = dataMap.getString("fqn");
      String uuid = ServiceUtils.generateUUID("Create Job Schedule - "+serviceFqn + "" + System.nanoTime());
	  RuntimePipeline rp = RuntimePipeline.create(uuid, null, null, serviceFqn, "");
      DataPipeline dp=rp.dataPipeLine;
      dp.log("Scheduler JOB executing******************************************");
      dp.log(serviceFqn);
        }catch(Exception e){
          e.printStackTrace();
    }
    }
}
}