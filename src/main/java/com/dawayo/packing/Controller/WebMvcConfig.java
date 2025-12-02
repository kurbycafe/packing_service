package com.dawayo.packing.Controller;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebMvc
public class WebMvcConfig implements WebMvcConfigurer {

        @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SessionInterceptor())
                .addPathPatterns("/**")                // 전체 경로에 적용
                .excludePathPatterns(                  // 검사 제외 경로
                        "/login","/loginProcess",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/error"
                );
    }
    
    
}
