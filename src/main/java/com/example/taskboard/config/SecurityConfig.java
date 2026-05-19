package com.example.taskboard.config;

import com.example.taskboard.common.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        CookieCsrfTokenRepository csrfRepo = CookieCsrfTokenRepository.withHttpOnlyFalse();
        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
        csrfHandler.setCsrfRequestAttributeName(null);

        http
            .csrf(csrf -> csrf
                    .csrfTokenRepository(csrfRepo)
                    .csrfTokenRequestHandler(csrfHandler))
            .authorizeHttpRequests(reg -> reg
                    .requestMatchers("/login.html", "/css/**", "/js/**", "/favicon.ico").permitAll()
                    .requestMatchers("/api/auth/login").permitAll()
                    .requestMatchers("/api/**").authenticated()
                    .anyRequest().permitAll())
            .formLogin(form -> form
                    .loginPage("/login.html")
                    .loginProcessingUrl("/api/auth/login")
                    .usernameParameter("username")
                    .passwordParameter("password")
                    .successHandler((req, res, auth) -> res.setStatus(HttpStatus.NO_CONTENT.value()))
                    .failureHandler((req, res, ex) -> {
                        res.setStatus(HttpStatus.UNAUTHORIZED.value());
                        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        objectMapper.writeValue(res.getOutputStream(),
                                ApiError.of(401, "UNAUTHORIZED", "ユーザー名またはパスワードが正しくありません"));
                    })
                    .permitAll())
            .logout(logout -> logout
                    .logoutUrl("/api/auth/logout")
                    .logoutSuccessHandler((req, res, auth) -> res.setStatus(HttpStatus.NO_CONTENT.value())))
            .exceptionHandling(eh -> eh
                    .authenticationEntryPoint((req, res, ex) -> {
                        res.setStatus(HttpStatus.UNAUTHORIZED.value());
                        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        objectMapper.writeValue(res.getOutputStream(),
                                ApiError.of(401, "UNAUTHORIZED", "認証が必要です"));
                    })
                    .accessDeniedHandler((req, res, ex) -> {
                        res.setStatus(HttpStatus.FORBIDDEN.value());
                        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        objectMapper.writeValue(res.getOutputStream(),
                                ApiError.of(403, "FORBIDDEN", "この操作を行う権限がありません"));
                    }));

        return http.build();
    }
}
