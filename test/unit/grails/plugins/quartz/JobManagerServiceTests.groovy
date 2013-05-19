package grails.plugins.quartz

import grails.test.mixin.TestFor
import org.junit.*
import org.quartz.*
import org.quartz.impl.StdSchedulerFactory

/**
 * Tests for JobManagerService
 *
 * @author Vitalii Samolovskikh aka Kefir
 */
@TestFor(JobManagerService)
class JobManagerServiceTests {
    Scheduler scheduler

    /**
     * Create the quartz scheduler and prepare a few jobs and triggers.
     */
    @Before
    public void createScheduler() {
        scheduler = StdSchedulerFactory.getDefaultScheduler()

        scheduler.start()
        service.quartzScheduler = scheduler

        JobDetail job1 = JobBuilder.newJob(SimpleTestJob.class).withIdentity(new JobKey("job1", "group1")).build()
        JobDetail job2 = JobBuilder.newJob(SimpleTestJob.class).withIdentity(new JobKey("job2", "group1")).build()
        JobDetail job3 = JobBuilder.newJob(SimpleTestJob.class).withIdentity(new JobKey("job1", "group2")).build()
        JobDetail job4 = JobBuilder.newJob(SimpleTestJob.class)
                .withIdentity(new JobKey("job2", "group2"))
                .storeDurably()
                .build()

        Trigger trigger1 = TriggerBuilder.newTrigger()
                .withIdentity(new TriggerKey("trigger1", "tgroup1"))
                .build()

        Trigger trigger2 = TriggerBuilder.newTrigger()
                .withIdentity(new TriggerKey("trigger2", "tgroup1"))
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInMinutes(2).repeatForever())
                .startNow()
                .build()

        Trigger trigger3 = TriggerBuilder.newTrigger()
                .withIdentity(new TriggerKey("trigger3", "tgroup2"))
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInMinutes(3).repeatForever())
                .startNow()
                .build()

        Trigger trigger4 = TriggerBuilder.newTrigger()
                .withIdentity(new TriggerKey("trigger4", "tgroup2"))
                .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInMinutes(4).repeatForever())
                .startNow()
                .build()

        scheduler.scheduleJob(job1, trigger1)
        scheduler.scheduleJob(job2, trigger2)
        scheduler.scheduleJobs([(job3): [trigger3, trigger4]], false)
        scheduler.addJob(job4, false)
    }

    /**
     * Shutdown the scheduler
     */
    @After
    public void shutdownScheduler() {
        scheduler.shutdown()
    }

    @Test
    public void testGetAllJobs() {
        Map<String, ? extends List<? extends JobDescriptor>> jobs = service.getAllJobs()

        assertNotNull(jobs)

        assert jobs instanceof Map
        assert jobs.size() == 2

        assert jobs.containsKey("group1")
        //assert jobs['group1'].size() == 1
        assert jobs['group1']*.name.contains('job2')

        assert jobs.containsKey("group2")
        assert jobs['group2'].size() == 2
        assert jobs['group2']*.name.contains('job1')
        assert jobs['group2']*.name.contains('job2')
    }

    @Test
    public void testGetJobs() {
        assert service.getJobs('group1')*.name.contains('job2')

        def names = service.getJobs('group2')*.name
        assert names.contains('job1')
        assert names.contains('job2')
    }

    @Test
    public void testGetRunningJobs() {
        service.getRunningJobs()
    }

    @Test
    public void testPauseAndResumeJob() {
        service.pauseJob('group1', 'job2')
        service.resumeJob('group1', 'job2')
    }

    @Test
    public void PauseAndResumeTrigger() {
        service.pauseTrigger('tgroup1', 'trigger1')
        service.resumeTrigger('tgroup1', 'trigger1')
    }

    @Test
    public void testPauseAndResumeJobGroup() {
        service.pauseJobGroup('group1');
        service.resumeJobGroup('group1');
    }

    @Test
    public void testPauseAndResumeTriggerGroup() {
        service.pauseTriggerGroup('tgroup1');
        service.resumeTriggerGroup('tgroup1');
    }

    @Test
    public void testRemoveJob() {
        assertTrue(service.getJobs('group1')*.name.contains('job2'))
        service.removeJob('group1', 'job2')
        assertFalse(service.getJobs('group1')*.name.contains('job2'))
    }

    @Test
    public void testUnscheduleJob() {
        def key = new JobKey('job2', 'group1')
        assert scheduler.getTriggersOfJob(key)?.size() > 0
        service.unscheduleJob('group1', 'job2')
        List<? extends Trigger> triggers = scheduler.getTriggersOfJob(key)
        assert triggers == null || triggers.size() == 0
    }

    @Test
    public void testInterruptJob(){
        service.interruptJob('group1', 'job2')
    }

    @Test
    public void testPauseAndResumeAll(){
        service.pauseAll()
        service.resumeAll()
    }
}
