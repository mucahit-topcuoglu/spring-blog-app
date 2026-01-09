package com.blog.blogprojesi.config;

import com.blog.blogprojesi.entity.Role;
import com.blog.blogprojesi.entity.User;
import com.blog.blogprojesi.service.AdminLogService;
import com.blog.blogprojesi.service.LoginAttemptService;
import com.blog.blogprojesi.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.io.IOException;

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
    private final LoginAttemptService loginAttemptService;
    private final AdminLogService adminLogService;
    private final UserService userService;

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    /**
     * Admin Security Filter Chain - Öncelikli
     */
    @Bean
    @Order(1)
    public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/admin/**")
            .authenticationProvider(authenticationProvider())
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/admin/login").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
            )
            .formLogin(form -> form
                .loginPage("/admin/login")
                .loginProcessingUrl("/admin/login")
                .successHandler(adminAuthenticationSuccessHandler())
                .failureHandler(adminAuthenticationFailureHandler())
                .usernameParameter("username")
                .passwordParameter("password")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/admin/logout"))
                .logoutSuccessUrl("/admin/login?logout=true")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .exceptionHandling(exception -> exception
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.sendRedirect("/admin/login?error=access_denied");
                })
            );

        return http.build();
    }

    /**
     * User Security Filter Chain
     */
    @Bean
    @Order(2)
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
                
                // Diğer tüm istekler yetkilendirme gerektirir
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler(defaultAuthenticationSuccessHandler())
                .failureHandler(defaultAuthenticationFailureHandler())
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

    /**
     * Admin başarılı giriş handler
     */
    @Bean
    public AuthenticationSuccessHandler adminAuthenticationSuccessHandler() {
        return new AuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                                Authentication authentication) throws IOException, ServletException {
                String username = authentication.getName();
                String ip = getClientIp(request);

                // Login denemesini kaydet
                loginAttemptService.recordLoginAttempt(username, ip, true, true);

                // Admin logunu kaydet
                userService.findByUsername(username).ifPresent(user -> {
                    if (user.getRole() == Role.ADMIN) {
                        adminLogService.logLogin(user, ip);
                    }
                });

                response.sendRedirect("/admin/dashboard");
            }
        };
    }

    /**
     * Admin başarısız giriş handler
     */
    @Bean
    public AuthenticationFailureHandler adminAuthenticationFailureHandler() {
        return new AuthenticationFailureHandler() {
            @Override
            public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                                AuthenticationException exception) throws IOException, ServletException {
                String username = request.getParameter("username");
                String ip = getClientIp(request);

                // Login denemesini kaydet
                loginAttemptService.recordLoginAttempt(username, ip, false, true);

                // Kullanıcı kilitli mi kontrol et
                if (loginAttemptService.isBlocked(username)) {
                    response.sendRedirect("/admin/login?blocked=true");
                } else {
                    int remaining = loginAttemptService.getRemainingAttempts(username);
                    response.sendRedirect("/admin/login?error=true&remaining=" + remaining);
                }
            }
        };
    }

    /**
     * Varsayılan başarılı giriş handler
     */
    @Bean
    public AuthenticationSuccessHandler defaultAuthenticationSuccessHandler() {
        return new AuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                                Authentication authentication) throws IOException, ServletException {
                String username = authentication.getName();
                String ip = getClientIp(request);

                // Login denemesini kaydet
                loginAttemptService.recordLoginAttempt(username, ip, true, false);

                response.sendRedirect("/home");
            }
        };
    }

    /**
     * Varsayılan başarısız giriş handler
     */
    @Bean
    public AuthenticationFailureHandler defaultAuthenticationFailureHandler() {
        return new AuthenticationFailureHandler() {
            @Override
            public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                                AuthenticationException exception) throws IOException, ServletException {
                String username = request.getParameter("username");
                String ip = getClientIp(request);

                // Login denemesini kaydet
                loginAttemptService.recordLoginAttempt(username, ip, false, false);

                // Kullanıcı kilitli mi kontrol et
                if (loginAttemptService.isBlocked(username)) {
                    response.sendRedirect("/login?blocked=true");
                } else {
                    response.sendRedirect("/login?error=true");
                }
            }
        };
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
