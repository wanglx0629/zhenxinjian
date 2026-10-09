package cn.zhenxinjian.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.Executor;

/**
 * 定时任务调度与异步外呼线程池配置
 * 作者: wanglx
 *
 * 默认 @Scheduled 共用单线程调度器：推送任务外呼变慢会阻塞统计/清理任务（调度饥饿）。
 * 此处显式提供多线程调度池（taskScheduler，Spring 自动识别同名 Bean 承载全部 @Scheduled），
 * 并为三餐提醒推送提供独立外呼执行器（reminderPushExecutor），外呼耗时不占调度线程。
 */
@Configuration
public class ScheduleConfig {

    /** 调度池线程数：当前 3 个 @Scheduled 任务（提醒推送/统计聚合/游客清理），留一余量 */
    private static final int SCHEDULER_POOL_SIZE = 4;

    /** 外呼执行器核心线程数 */
    private static final int REMINDER_PUSH_CORE_POOL_SIZE = 2;

    /** 外呼执行器最大线程数 */
    private static final int REMINDER_PUSH_MAX_POOL_SIZE = 4;

    /** 外呼执行器队列容量 */
    private static final int REMINDER_PUSH_QUEUE_CAPACITY = 200;

    /**
     * 多线程调度池（Bean 名 taskScheduler 被 Spring 调度自动识别）
     *
     * @return 调度器
     */
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(SCHEDULER_POOL_SIZE);
        scheduler.setThreadNamePrefix("task-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        return scheduler;
    }

    /**
     * 三餐提醒推送外呼执行器（限并发异步下发订阅消息，避免阻塞调度线程）
     *
     * @return 外呼执行器
     */
    @Bean
    public Executor reminderPushExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(REMINDER_PUSH_CORE_POOL_SIZE);
        executor.setMaxPoolSize(REMINDER_PUSH_MAX_POOL_SIZE);
        executor.setQueueCapacity(REMINDER_PUSH_QUEUE_CAPACITY);
        executor.setThreadNamePrefix("reminder-push-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}