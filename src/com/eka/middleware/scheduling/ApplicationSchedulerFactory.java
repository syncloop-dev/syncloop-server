package com.eka.middleware.scheduling;

import com.beust.jcommander.internal.Lists;
import com.eka.middleware.service.DataPipeline;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.quartz.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApplicationSchedulerFactory {

    private ApplicationSchedulerFactory(String tenantName) {
        super();
    }
    @Getter @Setter
    private static Scheduler scheduler;
    public static Map<String, Scheduler> tenantSchedulers = new HashMap<>();
    /**
     * @param configFile
     * @return
     * @throws SchedulerException
     */

    public static ApplicationSchedulerFactory initScheduler(final String configFile, String tenantName) throws SchedulerException {
        SchedulerFactory schedFact;
        if (StringUtils.isBlank(configFile)) {
            schedFact = new org.quartz.impl.StdSchedulerFactory();
        } else {
            schedFact = new org.quartz.impl.StdSchedulerFactory(configFile);
        }
        Scheduler tenantScheduler = schedFact.getScheduler();
        tenantSchedulers.put(tenantName, tenantScheduler);
        ApplicationSchedulerFactory applicationSchedulerFactory = new ApplicationSchedulerFactory(tenantName);
       // applicationSchedulerFactory.setScheduler(tenantScheduler);
        applicationSchedulerFactory.setScheduler(schedFact.getScheduler());
        return applicationSchedulerFactory;
    }

    public static Scheduler getSchedulerForTenant(String tenantName) {
        return tenantSchedulers.get(tenantName);
    }

    /**
     * @throws SchedulerException
     */
    public void startScheduler() throws SchedulerException {
        scheduler.start();
    }

    /**
     * @throws SchedulerException
     */
    public void stopScheduler() throws SchedulerException {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.standby();
        }
    }

    /**
     * @throws SchedulerException
     */
    public void deleteScheduler() throws SchedulerException {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }

    /**
     * @return
     * @throws SchedulerException
     */
    public SchedulerMetaData getSchedulerMetaData() throws SchedulerException {
        return scheduler.getMetaData();
    }

    public static boolean isSchedulerAlive(DataPipeline dataPipeline) throws SchedulerException {
       Scheduler scheduler = getSchedulerForTenant(dataPipeline.rp.getTenant().getName());
        return !scheduler.isInStandbyMode();
    }


    /**
     * @return
     * @throws SchedulerException
     */
    public List<JobExecutionContext> getCurrentlyExecutingJobs(DataPipeline dataPipeline) throws SchedulerException {
        return Lists.newArrayList();
    }

    /**
     * @param jobClass
     * @param jobName
     * @param jobGroup
     * @return
     */
    static JobDetail buildJobDetail(Class<? extends Job> jobClass, String jobName, String jobGroup) {
        return JobBuilder.newJob(jobClass)
                .withIdentity(jobName, jobGroup)
                .build();
    }

    /**
     * @param triggerName
     * @param triggerGroup
     * @param cronExpression
     * @return
     */
    private Trigger buildCronTrigger(String triggerName, String triggerGroup, String cronExpression) {
        return TriggerBuilder.newTrigger()
                .withIdentity(triggerName, triggerGroup)
                .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                .build();
    }

    /**
     * @param jobDetail
     * @return
     * @throws SchedulerException
     */
    public static JobKey getKey(JobDetail jobDetail) throws SchedulerException {
        return jobDetail.getKey();
    }

    /**
     * @param jobKey
     * @throws SchedulerException
     */
    public static void removeJob(JobKey jobKey, Scheduler scheduler) throws SchedulerException {
        scheduler.unscheduleJob(TriggerKey.triggerKey(jobKey.getName(), jobKey.getGroup()));
        scheduler.deleteJob(jobKey);
    }
    public void startJob(JobKey jobKey) throws SchedulerException {
        scheduler.triggerJob(jobKey);
    }

    /**
     * @param jobDetail
     * @param newCronExpression
     */
    public void updateScheduler(JobDetail jobDetail, String newCronExpression) {
        try {
            JobKey jobKey = getKey(jobDetail);
            String jobName = jobKey.getName();
            String jobGroup = jobKey.getGroup();

            Trigger oldTrigger = scheduler.getTrigger(TriggerKey.triggerKey(jobName, jobGroup));

            if (oldTrigger != null) {
                Trigger newTrigger = buildCronTrigger(jobName, jobGroup, newCronExpression);
                scheduler.rescheduleJob(oldTrigger.getKey(), newTrigger);
            }
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param jobClass
     * @param identificationName
     * @param identificationGroup
     * @param cronExpression
     * @return
     * @throws SchedulerException
     */
    public static <T extends Job> JobKey scheduleJob(Class<T> jobClass, String identificationName, String identificationGroup,String serviceFqn, String cronExpression,String job_name,DataPipeline dataPipeline) throws SchedulerException {
        if (cronExpression.split(" ").length == 5) {
            cronExpression = cronExpression + " ? *";
        }

        JobDetail job = JobBuilder.newJob(jobClass)
                .withIdentity(identificationName, identificationGroup).build();

        JobDataMap jobDataMap = job.getJobDataMap();
        jobDataMap.put("identificationName", identificationName);
        jobDataMap.put("cronExpression", cronExpression);
        jobDataMap.put("serviceFqn",serviceFqn);
        jobDataMap.put("identificationGroup",identificationGroup);
        jobDataMap.put("tenantName",dataPipeline.rp.getTenant().getName());
        jobDataMap.put("job_name",job_name);

        Trigger trigger = TriggerBuilder
                .newTrigger()
                .withIdentity(identificationName, identificationGroup)
                .withSchedule(
                        CronScheduleBuilder.cronSchedule(cronExpression))
                .build();
        scheduler.scheduleJob(job, trigger);
        return job.getKey();
    }

}
