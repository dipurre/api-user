package com.diegoip.microservices.user.configuration.auth;

import com.diegoip.microservices.user.service.impl.UserDetailsServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final UserDetailsServiceImpl userDetailsService;
  private final AuthenticationFilter authenticationFilter;
  private final AuthEntryPoint exceptionHandler;

  private static final String[] SWAGGER_PATHS = {"/v3/api-docs/**", "/swagger-ui.html", "/webjars/swagger-ui/**", "/swagger-ui/**"};

  public SecurityConfig(UserDetailsServiceImpl userDetailsService, AuthenticationFilter authenticationFilter,
                        AuthEntryPoint exceptionHandler) {
    this.userDetailsService = userDetailsService;
    this.authenticationFilter = authenticationFilter;
    this.exceptionHandler = exceptionHandler;
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable).cors(withDefaults())
        .sessionManagement(
            (sessionManagement) -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests((authorizeHttpRequests) -> authorizeHttpRequests
            .requestMatchers("/h2-console/**").permitAll()
            .requestMatchers(SWAGGER_PATHS).permitAll()
            .requestMatchers(HttpMethod.POST, "/v1/users/login").permitAll()
            .requestMatchers(HttpMethod.POST, "/v1/users/register").permitAll()
            .anyRequest().authenticated())
        .addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .headers(httpSecurityHeadersConfigurer -> httpSecurityHeadersConfigurer.frameOptions(
            HeadersConfigurer.FrameOptionsConfig::sameOrigin
        ))
        .exceptionHandling((exceptionHandling) -> exceptionHandling.authenticationEntryPoint(exceptionHandler));

    return http.build();
  }

  public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
    auth.userDetailsService(userDetailsService).passwordEncoder(new BCryptPasswordEncoder());
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
    return authConfig.getAuthenticationManager();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("*"));
    config.setAllowedMethods(List.of("*"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(false);
    config.applyPermitDefaultValues();

    source.registerCorsConfiguration("/**", config);
    return source;
  }


}
