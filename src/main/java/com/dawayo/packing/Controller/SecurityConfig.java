package com.dawayo.packing.Controller;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

       http
    .authorizeHttpRequests(auth -> auth
        .requestMatchers("/login", "/loginProcess", "/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
        .anyRequest().permitAll() // ✅ 여기 변경
    )
    .formLogin(login -> login.disable()) // ✅ 로그인 가로채지 않도록
    .logout(logout -> logout
        .logoutSuccessUrl("/login?logout=true")
        .deleteCookies("JSESSIONID")
        .invalidateHttpSession(true)
    )
    .csrf(csrf -> csrf.disable())
    .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }
}
