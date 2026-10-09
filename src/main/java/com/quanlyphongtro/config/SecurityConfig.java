package com.quanlyphongtro.config;

import com.quanlyphongtro.enums.AccountStatus;
import com.quanlyphongtro.repository.AccountRepository;
import com.quanlyphongtro.security.*;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.session.*;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration
public class SecurityConfig {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  SessionRegistry sessionRegistry() {
    return new SessionRegistryImpl();
  }

  @Bean
  HttpSessionEventPublisher sessionPublisher() {
    return new HttpSessionEventPublisher();
  }

  @Bean
  UserDetailsService userDetails(AccountRepository repo) {
    return email ->
        repo.findByEmailIgnoreCase(email.trim())
            .map(
                a ->
                    User.withUsername(a.getEmail())
                        .password(a.getPasswordHash())
                        .roles(a.getRole().name())
                        .disabled(a.getStatus() != AccountStatus.ACTIVE)
                        .build())
            .orElseThrow(() -> new UsernameNotFoundException("Thông tin đăng nhập không hợp lệ"));
  }

  @Bean
  SecurityFilterChain security(
      HttpSecurity http, AccountRepository accounts, SessionRegistry registry) throws Exception {
    http.authorizeHttpRequests(
            a ->
                a.requestMatchers(
                        "/",
                        "/rooms",
                        "/rooms/*",
                        "/images/*",
                        "/css/**",
                        "/js/**",
                        "/demo-assets/**",
                        "/auth/**",
                        "/error")
                    .permitAll()
                    .requestMatchers("/admin/**")
                    .hasRole("ADMIN")
                    .requestMatchers("/landlord/**")
                    .hasRole("LANDLORD")
                    .requestMatchers("/tenant/**")
                    .hasRole("TENANT")
                    .anyRequest()
                    .authenticated())
        .formLogin(
            f ->
                f.loginPage("/auth/login")
                    .loginProcessingUrl("/auth/login")
                    .usernameParameter("email")
                    .defaultSuccessUrl("/dashboard", true)
                    .failureUrl("/auth/login?error")
                    .permitAll())
        .logout(
            l ->
                l.logoutUrl("/auth/logout")
                    .logoutSuccessUrl("/?loggedOut")
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID"))
        .sessionManagement(
            s -> s.maximumSessions(5).sessionRegistry(registry).expiredUrl("/auth/login?expired"))
        .addFilterBefore(new RateLimitFilter(), UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(new ActiveAccountFilter(accounts), AuthorizationFilter.class);
    return http.build();
  }
}
