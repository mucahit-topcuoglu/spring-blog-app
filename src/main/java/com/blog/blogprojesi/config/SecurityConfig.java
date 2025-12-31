package com.blog.blogprojesi.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Spring Security Yapılandırma Sınıfı
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authenticationProvider(authenticationProvider())
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // Herkese açık sayfalar
                .requestMatchers("/", "/index", "/home", "/login", "/register", "/forgot-password", "/reset-password/**").permitAll()
                .requestMatchers("/post/**").permitAll() // Post detay sayfaları herkese açık
                .requestMatchers("/topics", "/topics/**").permitAll()
                .requestMatchers("/search", "/search/**").permitAll()
                
                // Static kaynaklar
                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**", "/static/**").permitAll()
                
                // API endpointleri - yetkilendirme gerektiren
                .requestMatchers("/api/check-username", "/api/check-email").permitAll()
                .requestMatchers("/api/**").authenticated()
                
                // Yetkilendirme gerektiren sayfalar
                .requestMatchers("/write", "/write/**").authenticated()
                .requestMatchers("/profile", "/profile/**").authenticated()
                .requestMatchers("/settings", "/settings/**").authenticated()
                .requestMatchers("/my-posts", "/my-posts/**").authenticated()
                .requestMatchers("/bookmarks", "/bookmarks/**").authenticated()
                
                // Admin sayfaları
                .requestMatchers("/admin/**").hasRole("ADMIN")
                
                // Diğer tüm istekler yetkilendirme gerektirir
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/home", true)
                .failureUrl("/login?error=true")
                .usernameParameter("username")
                .passwordParameter("password")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .rememberMe(remember -> remember
                .key("blogProjesiSecretKey")
                .tokenValiditySeconds(86400 * 7) // 7 gün
                .userDetailsService(userDetailsService)
            )
            .exceptionHandling(exception -> exception
                .accessDeniedPage("/access-denied")
            );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
