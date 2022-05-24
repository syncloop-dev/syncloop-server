package packages.ekaScheduler.cronJob.services.java;
import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.service.ServiceUtils;
import com.eka.middleware.template.SnippetException;
import com.eka.middleware.service.RuntimePipeline;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import io.timeandspace.cronscheduler.CronScheduler;
import java.time.format.DateTimeFormatter;
public final class getSchedulerJobData{
	public static final void main(DataPipeline dataPipeline) throws SnippetException{
try {
    //dataPipeline.log("***********_________________***"+jobMapData);
    dataPipeline.put("jobMapData",jobMapData);
    //jobMapData.put("active",new HashMap<String,String>());
    if(!schedulerThread.isAlive()){
      dataPipeline.log("Attempting to start the scheduler thread....");
      schedulerThread.start();
      dataPipeline.log("Scheduler thread started");
    }else
      dataPipeline.log("Scheduler thread active and running");
    dataPipeline.put("msg","Success");
  } catch (Exception e) {
		dataPipeline.clear();
  		dataPipeline.put("error",e.getMessage());
    	throw new SnippetException(dataPipeline,"Snippet exception", new Exception(e));
  }
	}
private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("uuuu/MM/dd HH:mm:ss");
private static final Map<String,Map<String,String>> jobMapData=new ConcurrentHashMap<String,Map<String,String>>();
	private static Thread schedulerThread=new Thread(new Runnable() {
	
	@Override
	public void run() {
        final String uuidThread = ServiceUtils.generateUUID("Job Scheduler thread - packages.ekaScheduler.cronJob.services.getSchedulerJobData.java" + System.nanoTime());
        final RuntimePipeline rpThread = RuntimePipeline.create(uuidThread, null, null, "packages.ekaScheduler.cronJob.services.java.getSchedulerJobData.main", "");
        final DataPipeline dpThread=rpThread.dataPipeLine;
		int counter=1;
        int logFrequency=1;
        int check=0;
        while(jobMapData!=null) {
			try {
				Set<String> jobIds=jobMapData.keySet();
                if(counter==1)
                  check++;
                if(check==logFrequency){
                	dpThread.log("Job schedule Monitor: actively monitoring....");
                    check=0;
                }
                counter++;
                if(jobIds.size()>0)
				for (final String jobId : jobIds) {
					Map<String, String> jobData=jobMapData.get(jobId);
					String cronExpression=jobData.get("cronExpression");
					final String serviceFqn = jobData.get("serviceFqn");
                    final String enabled = jobData.get("enabled");
                    String internal_status = jobData.get("internal_status");
					Instant nextInst = getNextInstant(cronExpression);
                    internal_status=internal_status==null?"":internal_status;
					ZonedDateTime now = ZonedDateTime.now();
                    if(!"Y".equals(enabled)){
                      dpThread.log("Job schedule Monitor: skipping disabled job - "+serviceFqn);
                    }else if(nextInst.toEpochMilli()-now.toInstant().toEpochMilli()<1000l && !internal_status.equals("running")) {
                        counter=1;
                        jobData.put("status","queued");
                        jobData.put("internal_status","queued");
                        final String uuid = ServiceUtils.generateUUID("Job Schedule - packages.ekaScheduler.cronJob.services.getSchedulerJobData.java.main" + System.nanoTime());
                        final RuntimePipeline rp = RuntimePipeline.create(uuid, null, null, "packages.ekaScheduler.cronJob.services.getSchedulerJobData.java.main", "");
                        final DataPipeline dp=rp.dataPipeLine;
                        dpThread.log("Job schedule Monitor: Service queued - "+serviceFqn);                       
						Duration syncPeriod = Duration.ofSeconds(5);
						CronScheduler cron = CronScheduler.create(syncPeriod);
						cron.scheduleAt(nextInst, new Runnable() {
							@Override
							public void run() {
                              try {
                                    ZonedDateTime startedAt = ZonedDateTime.now();
                                    jobData.put("status","running");
                                    jobData.put("internal_status","running");
                                    jobData.put("start_time",dtf.format(startedAt));
                                    jobData.put("end_time","");
                                    jobData.put("next_run",getNextInstant(cronExpression)+"");
                        			dp.put("jobData",jobData);
                                    dp.log("Calling startJob for "+serviceFqn+" at time: "+startedAt);
                                    ServiceUtils.execute("packages.ekaScheduler.cronJob.handler.startJob.main", dp);
                                    ZonedDateTime endedAt = ZonedDateTime.now();
                                    dp.log(serviceFqn+" ended at time: "+dtf.format(endedAt));
                                    dp.log(serviceFqn+" took : "+(endedAt.toInstant().toEpochMilli()-startedAt.toInstant().toEpochMilli())+"ms to finish the job.");
                                    if(dp.get("error")==null)
                                      dp.log(serviceFqn+": Service execution completed successfully");
                                	else
                                      dp.log(serviceFqn+": Service execution failed. "+dp.get("error"));
                                    jobData.put("status","Completed");
                        			jobData.put("internal_status","completed");
                                    jobData.put("end_time",dtf.format(endedAt));
                                } catch (Exception e) {
                                    dp.log(serviceFqn+": Service execution failed. "+e.getMessage());
                                    jobData.put("status","failed");
                        			jobData.put("internal_status","failed");
                                    e.printStackTrace();
                                }finally{
                              		rp.destroy();
                              }								
							}
						});
					}
				}
				Thread.sleep(1000);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
        rpThread.destroy();
	  }
   });

private static Instant getNextInstant(String cronExpression) {
        if(cronExpression.equals("0"))
          return ZonedDateTime.now().toInstant();
		CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX);
		CronParser parser = new CronParser(cronDefinition);

		Calendar cal = Calendar.getInstance();
		Date currTime = cal.getTime();
		ZonedDateTime now = ZonedDateTime.now();
		// Get date for last execution
		// DateTime now = DateTime.now();
		ExecutionTime executionTime = ExecutionTime.forCron(parser.parse(cronExpression));
		// DateTime lastExecution = executionTime.lastExecution(currTime));

		// Get date for next execution
		Optional<ZonedDateTime> zdt = executionTime.nextExecution(now);

		ZonedDateTime next = zdt.get();
		Instant inst = next.toInstant();

		return inst;
}
}