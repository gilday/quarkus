package io.quarkus.quartz.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.QuarkusUnitTest;

public class InjectJobTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Starter.class, Service.class, CountDownLatchProducer.class)
                    .addAsResource(new StringAsset("quarkus.scheduler.start-mode=forced"),
                            "application.properties"));

    @Test
    public void testSimpleScheduledJobs() throws InterruptedException {
        CountDownLatch latch = Arc.container().instance(CountDownLatch.class).get();
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Latch count: " + latch.getCount());
    }

    @ApplicationScoped
    public static class CountDownLatchProducer {
        @Produces
        @Singleton
        CountDownLatch LATCH = new CountDownLatch(2);
    }

    @ApplicationScoped
    public static class Service {

        @Inject
        CountDownLatch latch;

        public void execute() {
            latch.countDown();
        }

    }

    public static class Starter {

        void onStart(@Observes StartupEvent event, Scheduler quartz) throws SchedulerException {
            JobDetail job = JobBuilder.newJob(ServiceJob.class)
                    .withIdentity("myJob", "myGroup")
                    .build();
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("myTrigger", "myGroup")
                    .startNow()
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withIntervalInSeconds(1)
                            .repeatForever())
                    .build();
            quartz.scheduleJob(job, trigger);
        }

    }

    public static class ServiceJob implements Job {

        @Inject
        Service service;

        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            service.execute();
        }
    }

}
