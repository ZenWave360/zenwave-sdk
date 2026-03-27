package io.zenwave360.example.config

import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class JacksonConfiguration {
    /**
     * Support for Java date and time API.
     * @return the corresponding Jackson module.
     */
    @Bean
    open fun javaTimeModule(): JavaTimeModule {
        return JavaTimeModule()
    }

    @Bean
    open fun jdk8TimeModule(): Jdk8Module {
        return Jdk8Module()
    }
}
