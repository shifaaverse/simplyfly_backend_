package com.springboot.simplyfly.config;

import com.springboot.simplyfly.service.MyUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.transaction.annotation.EnableTransactionManagement;

// Enables transaction management in the application.
@EnableTransactionManagement
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    // Role names used to control access to APIs.
    private static final String ADMIN = "ADMIN";
    private static final String AIRLINE = "AIRLINE";
    private static final String PASSENGER = "PASSENGER";

    // Used to load user details during authentication.
    private final MyUserDetailsService myUserDetailsService;

    // Used to validate JWT tokens in incoming requests.
    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain defaultFilterChain(HttpSecurity http) throws Exception {

        // Step 1: Configure security rules for incoming HTTP requests.
        http
                // Disable CSRF because this application uses JWT authentication.
                // The token is sent in the Authorization header (no cookies), so a CSRF attack is not possible.
                .csrf(AbstractHttpConfigurer::disable) // NOSONAR
                .authorizeHttpRequests(authorize -> authorize

                        // Allow browser preflight requests.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Spring error page - otherwise a 403 is reported as 401
                        .requestMatchers("/error").permitAll()

                        // Passenger sign up API
                        .requestMatchers(HttpMethod.POST, "/api/passenger/add").permitAll()

                        // Airline submits registration, Admin approves it later
                        .requestMatchers(HttpMethod.POST, "/api/airline/register").permitAll()

                        // Admin onboard API (permitAll to add) (After addition - denyAll)
                        .requestMatchers(HttpMethod.POST, "/api/admin/add").permitAll()

                        // Forgot password APIs (OTP demo)
                        .requestMatchers(HttpMethod.POST, "/api/auth/forgot-password").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/reset-password").permitAll()

                        // Login API
                        .requestMatchers(HttpMethod.GET, "/api/auth/login").authenticated()

                        // User details API
                        .requestMatchers(HttpMethod.GET, "/api/auth/user-details").authenticated()

                        // Public search APIs
                        .requestMatchers(HttpMethod.GET, "/api/airport/all").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/flight/search").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/flight/routes").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/flight/status").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/flight/seats/{instanceId}").permitAll()

                        // Role based APIs
                        // Only ADMIN users can access admin APIs.
                        .requestMatchers("/api/admin/**").hasAuthority(ADMIN)

                        // Only AIRLINE users can access airline APIs.
                        .requestMatchers("/api/airline/**").hasAuthority(AIRLINE)

                        // Only PASSENGER users can access passenger APIs.
                        .requestMatchers("/api/passenger/**").hasAuthority(PASSENGER)

                        // All remaining APIs require authentication.
                        .anyRequest().authenticated()
                );

        // Add jwt filter before Spring's Auth Filter
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        // Enable HTTP Basic authentication.
        http.httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public AuthenticationProvider getProvider() {

        // Step 2: Create the authentication provider using the user details service.
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider(myUserDetailsService);

        // Set BCrypt password encoding for password verification.
        authenticationProvider.setPasswordEncoder(getEncoder());

        return authenticationProvider;
    }

    @Bean
    public PasswordEncoder getEncoder() {

        // Step 3: Create the password encoder.
        return new BCryptPasswordEncoder();
    }
}