package com.example.clinical.config;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import java.util.Arrays;

@Configuration
@EnableAspectJAutoProxy
public class LoggingAspectConfiguration {

    @Bean
    @Profile({ Constants.SPRING_PROFILE_LOCAL, Constants.SPRING_PROFILE_TEST, Constants.SPRING_PROFILE_DEVELOPMENT, })
    public LoggingAspect loggingAspect(Environment env) {
        return new LoggingAspect(env);
    }

    @Aspect
    public static class LoggingAspect {

        private final Environment env;

        public LoggingAspect(Environment env) {
            this.env = env;
        }

        /**
         * Pointcut that matches all repositories, services and Web REST endpoints.
         */
        @Pointcut("within(@org.springframework.stereotype.Repository *)"
                + " || within(@org.springframework.stereotype.Service *)"
                + " || within(@org.springframework.web.bind.annotation.RestController *)")
        public void springBeanPointcut() {
            // Method is empty as this is just a Pointcut, the implementations are in the
            // advices.
        }

        /**
         * Pointcut that matches all Spring beans in the application's main packages.
         */
        @Pointcut("within(com.example.clinical.core.implementation..*)"
                + " || within(com.example.clinical.infrastructure..*)" + " || within(com.example.clinical.adapters..*)")
        public void applicationPackagePointcut() {
            // Method is empty as this is just a Pointcut, the implementations are in the
            // advices.
        }

        /**
         * Retrieves the {@link org.slf4j.Logger} associated to the given
         * {@link org.aspectj.lang.JoinPoint}.
         * @param joinPoint join point we want the logger for.
         * @return {@link org.slf4j.Logger} associated to the given
         * {@link org.aspectj.lang.JoinPoint}.
         */
        private Logger logger(JoinPoint joinPoint) {
            return LoggerFactory.getLogger(joinPoint.getSignature().getDeclaringTypeName());
        }

        /**
         * Advice that logs methods throwing exceptions.
         * @param joinPoint join point for advice.
         * @param e exception.
         */
        @AfterThrowing(pointcut = "applicationPackagePointcut() && springBeanPointcut()", throwing = "e")
        public void logAfterThrowing(JoinPoint joinPoint, Throwable e) {
            if (env.acceptsProfiles(Profiles.of(Constants.SPRING_PROFILE_DEVELOPMENT))) {
                logger(joinPoint).error("Exception in {}() with cause = '{}' and exception = '{}'",
                        joinPoint.getSignature().getName(), e.getCause() != null ? e.getCause() : "NULL",
                        e.getMessage(), e);
            }
            else {
                logger(joinPoint).error("Exception in {}() with cause = {}", joinPoint.getSignature().getName(),
                        e.getCause() != null ? String.valueOf(e.getCause()) : "NULL");
            }
        }

        /**
         * Advice that logs when a method is entered and exited.
         * @param joinPoint join point for advice.
         * @return result.
         * @throws Throwable throws {@link IllegalArgumentException}.
         */
        @Around("applicationPackagePointcut() && springBeanPointcut()")
        public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
            Logger log = logger(joinPoint);
            if (log.isDebugEnabled()) {
                log.debug("Enter: {}() with argument[s] = {}", joinPoint.getSignature().getName(),
                        Arrays.toString(joinPoint.getArgs()));
            }
            try {
                Object result = joinPoint.proceed();
                if (log.isDebugEnabled()) {
                    log.debug("Exit: {}() with result = {}", joinPoint.getSignature().getName(), result);
                }
                return result;
            }
            catch (IllegalArgumentException e) {
                log.error("Illegal argument: {} in {}()", Arrays.toString(joinPoint.getArgs()),
                        joinPoint.getSignature().getName());
                throw e;
            }
        }

    }

}
