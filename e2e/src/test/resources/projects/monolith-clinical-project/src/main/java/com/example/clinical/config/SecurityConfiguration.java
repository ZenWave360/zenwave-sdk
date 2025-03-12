package com.example.clinical.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.AuditorAware;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Optional;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // @formatter:off
        http
                .cors(Customizer.withDefaults())
                .headers(headers -> headers.frameOptions(FrameOptionsConfig::disable))
                .csrf(CsrfConfigurer::disable)
                // consider disabling session management for stateless applications with SessionCreationPolicy.STATELESS
                .authorizeHttpRequests(auth ->
                        auth
                            .requestMatchers("/api/user", "/api/user/**").hasRole("ADMIN") // usermanagement
                            .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                            .requestMatchers("/.well-known/**").permitAll()
                            .anyRequest().authenticated()
                )
                .exceptionHandling((exceptions) -> exceptions
                        // this disables the default login form, use login-openapi.yml for login in Swagger UI
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                .oneTimeTokenLogin(Customizer.withDefaults())
                .formLogin(form -> form
                        .failureHandler((request, response, exception) -> response.setStatus(HttpStatus.UNAUTHORIZED.value()))
                        .successHandler((request, response, authentication) -> response.setStatus(HttpStatus.OK.value()))
                )
        ;
        // @formatter:on
        return http.build();
    }

    /**
     * Protect the Swagger UI with basic authentication.
     * @param http
     * @return
     * @throws Exception
     */
    @Bean
    @Order(1)
    public SecurityFilterChain swaggerSecurityFilterChain(HttpSecurity http) throws Exception {
        // @formatter:off
        http.securityMatcher("/swagger-ui/**", "/v3/api-docs/**", "/apis/**")
                .authorizeHttpRequests(auth ->
                        auth.anyRequest().hasRole("ADMIN")
                )
                .csrf(CsrfConfigurer::disable)
                .httpBasic(httpBasic -> httpBasic.realmName("Swagger Realm"));
        // @formatter:on
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "X-Requested-With", "Content-Type"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    AuditorAware<String> springSecurityAuditorAware() {
        return new AuditorAware<String>() {

            @Override
            public Optional<String> getCurrentAuditor() {
                return Optional.of(getCurrentUserLogin().orElse("system"));
            }

            /**
             * Get the login of the current user.
             * @return the login of the current user.
             */
            public static Optional<String> getCurrentUserLogin() {
                SecurityContext securityContext = SecurityContextHolder.getContext();
                return Optional.ofNullable(extractPrincipal(securityContext.getAuthentication()));
            }

            private static String extractPrincipal(Authentication authentication) {
                if (authentication == null) {
                    return null;
                }
                else if (authentication.getPrincipal() instanceof UserDetails springSecurityUser) {
                    return springSecurityUser.getUsername();
                }
                else if (authentication.getPrincipal() instanceof String stringPrincipal) {
                    return stringPrincipal;
                }
                return null;
            }

        };
    }

}
