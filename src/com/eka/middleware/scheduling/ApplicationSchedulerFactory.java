package com.eka.middleware.scheduling;

import com.beust.jcommander.internal.Lists;
import com.cronutils.mapper.CronMapper;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import com.eka.middleware.service.DataPipeline;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApplicationSchedulerFactory {

    private static Map<String, Scheduler> tenantSchedulers = new HashMap<>();

    public static Logger LOGGER = LogManager.getLogger(ApplicationSchedulerFactory.class);

    /**
     * @param configFile
     * @return
     * @throws SchedulerException
     */
    public static Scheduler initScheduler(final String configFile, String tenantName) throws SchedulerException {
        SchedulerFactory schedFact;

        if (StringUtils.isBlank(configFile)) {
            schedFact = new org.quartz.impl.StdSchedulerFactory();
        } else {
            schedFact = new org.quartz.impl.StdSchedulerFactory(configFile);
        }
        Scheduler tenantScheduler = schedFact.getScheduler();
        tenantSchedulers.put(tenantName, tenantScheduler);
        return tenantScheduler;
    }

    public static Scheduler getSchedulerForTenant(String tenantName) {
        return tenantSchedulers.get(tenantName);
    }

    /**
     * @throws SchedulerException
     */
    public static void startScheduler(DataPipeline dataPipeline) throws SchedulerException {
        Scheduler tenantScheduler = getSchedulerForTenant(dataPipeline.rp.getTenant().getName());

        if (null == tenantScheduler) {
            LOGGER.info("Scheduler obj null for {}", dataPipeline.rp.getTenant().getName());
            return ;
        }

        if (tenantScheduler.isInStandbyMode()) {
            tenantScheduler.start();
            LOGGER.debug("Scheduler started.");
        }
    }

    /**
     * @param dataPipeline
     * @return
     * @throws SchedulerException
     */
    public static boolean isSchedulerAlive(DataPipeline dataPipeline) throws SchedulerException {
        Scheduler scheduler = getSchedulerForTenant(dataPipeline.rp.getTenant().getName());
        if (null == scheduler) {
            LOGGER.info("Scheduler obj null for {}", dataPipeline.rp.getTenant().getName());
            return false;
        }
        return !scheduler.isInStandbyMode();
    }
    /**
     * @throws SchedulerException
     */
    public static void stopScheduler(DataPipeline dataPipeline) throws SchedulerException {
        Scheduler tenantScheduler =  getSchedulerForTenant(dataPipeline.rp.getTenant().getName());
        if (null == tenantScheduler) {
            LOGGER.info("Scheduler obj null for {}", dataPipeline.rp.getTenant().getName());
            return ;
        }
        if (tenantScheduler != null && !tenantScheduler.isInStandbyMode()) {
        	LOGGER.debug("scheduler stopped.....");
            tenantScheduler.standby();
        }
    }

    /**
     * @throws SchedulerException
     */
    public static void deleteScheduler(Scheduler scheduler) throws SchedulerException {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }

    /**
     * @return
     * @throws SchedulerException
     */
    public static SchedulerMetaData getSchedulerMetaData(Scheduler scheduler) throws SchedulerException {
        if (null == scheduler) {
            LOGGER.info("Scheduler obj null");
            return null;
        }
        return scheduler.getMetaData();
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
    private static Trigger buildCronTrigger(String triggerName, String triggerGroup, String cronExpression) {
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
     *
     * @param jobKey
     * @param scheduler
     * @throws SchedulerException
     */
    public static void removeJob(JobKey jobKey, Scheduler scheduler) throws SchedulerException {
        if (null == scheduler) {
            LOGGER.info("Scheduler obj null for");
            return ;
        }
        scheduler.unscheduleJob(TriggerKey.triggerKey(jobKey.getName(), jobKey.getGroup()));
        scheduler.deleteJob(jobKey);
    }

    public static void startJob(JobKey jobKey, Scheduler scheduler) throws SchedulerException {
        if (null == scheduler) {
            LOGGER.info("Scheduler obj null for");
            return ;
        }
        scheduler.triggerJob(jobKey);
    }

    /**
     * @param jobDetail
     * @param newCronExpression
     */
    public static void updateScheduler(JobDetail jobDetail, String newCronExpression, Scheduler scheduler) {
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
    public static <T extends Job> JobKey scheduleJob(Class<T> jobClass, String identificationName, String identificationGroup, String serviceFqn,
                                                     String cronExpression, String job_name, DataPipeline dataPipeline) throws SchedulerException {
        Scheduler scheduler = ApplicationSchedulerFactory.getSchedulerForTenant(dataPipeline.rp.getTenant().getName());

        String[] expression = cronExpression.split(" ");

       /* if (expression.length == 5) {
            if (cronExpression.equals("* * * * *")) {
                cronExpression = "0 " + expression[0] + " " + expression[1] + " " + "*" + " " + expression[3] + " " + "? *";
            } else if (expression[2].equals("*") && (!expression[4].equals("*"))) {
                cronExpression = "0 " + expression[0] + " " + expression[1] + " " + "?" + " " + expression[3] + " " + expression[4] + " *";
            } else if (!expression[2].equals("*") && (expression[4].equals("*"))) {
                cronExpression = "0 " + expression[0] + " " + expression[1] + " " + expression[2] + " " + expression[3] + " " + "? *";
            } else if (!expression[2].equals("*") && !expression[4].equals("*")) {
                cronExpression = "0 " + expression[0] + " " + expression[1] + " " + expression[2] + " " + expression[3] + " ? * " ;
            } else if (expression[2].equals("*") && expression[4].equals("*")) {
                cronExpression = "0 " + expression[0] + " " + expression[1] + " " + expression[2] + " " + expression[3] + " ? * " ;
            } else {
                cronExpression = "0 " + cronExpression + " *";
            }
        }*/
        JobDetail job = JobBuilder.newJob(jobClass)
                .withIdentity(identificationName, identificationGroup).build();

        JobDataMap jobDataMap = job.getJobDataMap();
        jobDataMap.put("identificationName", identificationName);
        jobDataMap.put("cronExpression", cronExpression);
        jobDataMap.put("serviceFqn",serviceFqn);
        jobDataMap.put("identificationGroup",identificationGroup);
        jobDataMap.put("tenantName",dataPipeline.rp.getTenant().getName());
        jobDataMap.put("job_name",job_name);

        String cron = unixToQuartz(cronExpression);
        Trigger trigger = TriggerBuilder
                .newTrigger()
                .withIdentity(identificationName, identificationGroup)
                .withSchedule(
                        CronScheduleBuilder.cronSchedule(cron))
                .build();
        scheduler.scheduleJob(job, trigger);
        return job.getKey();
    }

    public static String unixToQuartz(String cron) {
        CronParser parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX));
        Cron quartzCron = parser.parse(cron);
        CronMapper cronMapper = CronMapper.fromUnixToQuartz();

        Cron cron4jCron = cronMapper.map(quartzCron);

        return cron4jCron.asString();
    }
}
