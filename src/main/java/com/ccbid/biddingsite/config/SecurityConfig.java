package com.ccbid.biddingsite.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(
        PasswordEncoder passwordEncoder,
        @Value("${app.security.admin.username:admin}") String adminUsername,
        @Value("${app.security.admin.password:admin}") String adminPassword,
        @Value("${app.security.bidder.username:bidder}") String bidderUsername,
        @Value("${app.security.bidder.password:bidder}") String bidderPassword,
        @Value("${app.security.auctioneer.username:auctioneer}") String auctioneerUsername,
        @Value("${app.security.auctioneer.password:auctioneer}") String auctioneerPassword
    ) {
        return new InMemoryUserDetailsManager(
            User.withUsername(adminUsername)
                .password(passwordEncoder.encode(adminPassword))
                .roles("ADMIN", "BIDDER", "AUCTIONEER")
                .build(),
            User.withUsername(bidderUsername)
                .password(passwordEncoder.encode(bidderPassword))
                .roles("BIDDER")
                .build(),
            User.withUsername(auctioneerUsername)
                .password(passwordEncoder.encode(auctioneerPassword))
                .roles("AUCTIONEER")
                .build()
        );
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
        @Value("${app.security.allowed-origins:http://localhost:3000,http://localhost:5173}") List<String> allowedOrigins
    ) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.ignoringRequestMatchers(
                "/h2/**",
                "/bid/**",
                "/bidders/**",
                "/rec/**",
                "/feed/**"
            ))
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/h2/**", "/error").permitAll()
                .requestMatchers(GET, "/bid/**", "/bidders/**", "/rec/**", "/feed/**").authenticated()
                .requestMatchers(POST, "/bid/list").hasAnyRole("AUCTIONEER", "ADMIN")
                .requestMatchers(POST, "/bid/**").hasAnyRole("BIDDER", "ADMIN")
                .requestMatchers(DELETE, "/bid/**").hasAnyRole("BIDDER", "ADMIN")
                .requestMatchers("/bidders/add/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults());

        http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        http.formLogin(AbstractHttpConfigurer::disable);
        http.logout(AbstractHttpConfigurer::disable);

        return http.build();
    }
}
