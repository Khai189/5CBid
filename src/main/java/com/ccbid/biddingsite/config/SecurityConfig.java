package com.ccbid.biddingsite.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.ccbid.biddingsite.models.UserAccount;
import com.ccbid.biddingsite.repository.UserAccountRepo;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(UserAccountRepo userAccountRepo) {
        return username -> buildUserDetails(userAccountRepo, username);
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
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/h2/**", "/error").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/register", "/auth/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/auth/me").authenticated()
                .requestMatchers(HttpMethod.GET, "/bid/**", "/bidders/**", "/rec/**", "/feed/**", "/items/**")
                    .authenticated()
                .requestMatchers(HttpMethod.POST, "/bid/list").hasAnyRole("AUCTIONEER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/bid/**").hasAnyRole("BIDDER", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/bid/**").hasAnyRole("BIDDER", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/bidders/add/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults());

        http.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        http.formLogin(AbstractHttpConfigurer::disable);
        http.logout(AbstractHttpConfigurer::disable);

        return http.build();
    }

    private UserDetails buildUserDetails(UserAccountRepo userAccountRepo, String username) {
        UserAccount account = userAccountRepo.findByUsernameIgnoreCase(username)
            .orElseThrow(() -> new UsernameNotFoundException("User " + username + " not found"));

        return User.withUsername(account.getUsername())
            .password(account.getPasswordHash())
            .roles(account.getRole().name())
            .build();
    }
}
