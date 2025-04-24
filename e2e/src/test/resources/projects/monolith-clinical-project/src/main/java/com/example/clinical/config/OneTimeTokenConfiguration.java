package com.example.clinical.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ott.OneTimeToken;
import org.springframework.security.web.authentication.ott.OneTimeTokenGenerationSuccessHandler;
import org.springframework.security.web.authentication.ott.RedirectOneTimeTokenGenerationSuccessHandler;

@Configuration
public class OneTimeTokenConfiguration {

	@Bean
	OneTimeTokenGenerationSuccessHandler oneTimeTokenGenerationSuccessHandler() {
		var redirectHandler = new RedirectOneTimeTokenGenerationSuccessHandler("/login/ott");
		return (HttpServletRequest request, HttpServletResponse response, OneTimeToken oneTimeToken) -> {
			System.out.println("One-time token generated: " + oneTimeToken.getUsername() + " "
					+ oneTimeToken.getTokenValue() + " " + oneTimeToken.getExpiresAt());
			redirectHandler.handle(request, response, oneTimeToken);
		};
	}

}
