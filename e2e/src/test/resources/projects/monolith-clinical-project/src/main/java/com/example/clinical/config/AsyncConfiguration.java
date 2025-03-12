package com.example.clinical.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.task.TaskExecutionProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

@Configuration
@EnableAsync
@EnableScheduling
@Profile("!testdev & !testprod")
public class AsyncConfiguration implements AsyncConfigurer {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfiguration.class);

    private final TaskExecutionProperties taskExecutionProperties;

    public AsyncConfiguration(TaskExecutionProperties taskExecutionProperties) {
        this.taskExecutionProperties = taskExecutionProperties;
    }

    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        log.debug("Creating Async Task Executor");
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(taskExecutionProperties.getPool().getCoreSize());
        executor.setMaxPoolSize(taskExecutionProperties.getPool().getMaxSize());
        executor.setQueueCapacity(taskExecutionProperties.getPool().getQueueCapacity());
        executor.setThreadNamePrefix(taskExecutionProperties.getThreadNamePrefix());
        return new ExceptionHandlingAsyncTaskExecutor(executor);
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler();
    }

    /**
     *
     */
    private static class ExceptionHandlingAsyncTaskExecutor
            implements AsyncTaskExecutor, InitializingBean, DisposableBean {

        static final String EXCEPTION_MESSAGE = "Caught async exception";

        private final Logger log = LoggerFactory.getLogger(ExceptionHandlingAsyncTaskExecutor.class);

        private final AsyncTaskExecutor executor;

        /**
         * <p>
         * Constructor for ExceptionHandlingAsyncTaskExecutor.
         * </p>
         * @param executor a {@link org.springframework.core.task.AsyncTaskExecutor}
         * object.
         */
        public ExceptionHandlingAsyncTaskExecutor(AsyncTaskExecutor executor) {
            this.executor = executor;
        }

        /** {@inheritDoc} */
        @Override
        public void execute(Runnable task) {
            executor.execute(createWrappedRunnable(task));
        }

        private <T> Callable<T> createCallable(Callable<T> task) {
            return () -> {
                try {
                    return task.call();
                }
                catch (Exception e) {
                    handle(e);
                    throw e;
                }
            };
        }

        private Runnable createWrappedRunnable(Runnable task) {
            return () -> {
                try {
                    task.run();
                }
                catch (Exception e) {
                    handle(e);
                }
            };
        }

        /**
         * <p>
         * handle.
         * </p>
         * @param e a {@link java.lang.Exception} object.
         */
        protected void handle(Exception e) {
            log.error(EXCEPTION_MESSAGE, e);
        }

        /** {@inheritDoc} */
        @Override
        public Future<?> submit(Runnable task) {
            return executor.submit(createWrappedRunnable(task));
        }

        /** {@inheritDoc} */
        @Override
        public <T> Future<T> submit(Callable<T> task) {
            return executor.submit(createCallable(task));
        }

        /** {@inheritDoc} */
        @Override
        public void destroy() throws Exception {
            if (executor instanceof DisposableBean bean) {
                bean.destroy();
            }
        }

        /** {@inheritDoc} */
        @Override
        public void afterPropertiesSet() throws Exception {
            if (executor instanceof InitializingBean bean) {
                bean.afterPropertiesSet();
            }
        }

    }

}
