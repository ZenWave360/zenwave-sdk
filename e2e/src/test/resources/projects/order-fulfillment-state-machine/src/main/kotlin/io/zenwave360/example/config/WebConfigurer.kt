package io.zenwave360.example.config

import org.springframework.context.annotation.Configuration
import org.springframework.format.FormatterRegistry
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
open class WebConfigurer : WebMvcConfigurer {
    /**
     * Configure the converters to use the ISO format for dates by default.
     */
    override fun addFormatters(registry: FormatterRegistry) {
        val registrar = DateTimeFormatterRegistrar()
        registrar.setUseIsoFormat(true)
        registrar.registerFormatters(registry)
    }
}
