package com.example.studylink.config;

import com.example.studylink.user.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            UserService userService,
            @Value("${spring.h2.console.path:/h2-console}") String h2ConsolePath) throws Exception {
        RequestMatcher h2ConsoleMatcher = h2ConsoleMatcher(h2ConsolePath);

        http.userDetailsService(userService)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/health", "/register", "/login", "/css/**", "/webjars/**").permitAll()
                        .requestMatchers(h2ConsoleMatcher).permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .permitAll())
                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                        .permitAll())
                .csrf(csrf -> csrf.ignoringRequestMatchers(h2ConsoleMatcher))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        return http.build();
    }

    private static RequestMatcher h2ConsoleMatcher(String h2ConsolePath) {
        String path = normalizeH2ConsolePath(h2ConsolePath);
        return new OrRequestMatcher(
                new AntPathRequestMatcher(path),
                new AntPathRequestMatcher(path + "/**"));
    }

    private static String normalizeH2ConsolePath(String h2ConsolePath) {
        String path = (h2ConsolePath == null || h2ConsolePath.isBlank()) ? "/h2-console" : h2ConsolePath;
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }
}
