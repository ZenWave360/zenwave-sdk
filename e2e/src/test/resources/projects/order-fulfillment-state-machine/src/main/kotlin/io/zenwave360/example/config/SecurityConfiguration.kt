package io.zenwave360.example.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.AuditorAware
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import java.util.*

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
open class SecurityConfiguration {

    @Bean
    @Throws(Exception::class)
    open fun filterChain(http: HttpSecurity): SecurityFilterChain {
        // @formatter:off
        http
            .cors(Customizer.withDefaults())
            .headers { headers -> headers.frameOptions { it.disable() } }
            .csrf { it.disable() }
            // consider disabling session management for stateless applications with
            // SessionCreationPolicy.STATELESS
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/actuator/health", "/actuator/health/**")
                    .permitAll()
                    .requestMatchers("/.well-known/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated()
            }
            .httpBasic { httpBasic -> httpBasic.realmName("API Realm") }
        // @formatter:on
        return http.build()
    }

    @Bean
    open fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOriginPatterns = listOf("*")
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        configuration.allowedHeaders = listOf("Authorization", "X-Requested-With", "Content-Type")
        configuration.allowCredentials = true
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    @Bean
    open fun springSecurityAuditorAware(): AuditorAware<String> {
        return AuditorAware { Optional.of(getCurrentUserLogin().orElse("system")) }
    }

    /**
     * Get the login of the current user.
     *
     * @return the login of the current user.
     */
    private fun getCurrentUserLogin(): Optional<String> {
        val securityContext = SecurityContextHolder.getContext()
        return Optional.ofNullable(extractPrincipal(securityContext.authentication))
    }

    private fun extractPrincipal(authentication: Authentication?): String? {
        return when {
            authentication == null -> null
            authentication.principal is UserDetails -> (authentication.principal as UserDetails).username
            authentication.principal is String -> authentication.principal as String
            else -> null
        }
    }
}
