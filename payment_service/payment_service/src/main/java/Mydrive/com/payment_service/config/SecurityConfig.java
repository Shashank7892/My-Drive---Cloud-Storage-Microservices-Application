package Mydrive.com.payment_service.config;

import io.jsonwebtoken.io.Decoders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    @Value("${jwt.secret}")
    private String jwtSecretKey;
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // Disable CSRF for REST APIs
                .authorizeHttpRequests(authorize -> authorize
                        // Allow unauthenticated access for Razorpay webhook. Razorpay does NOT send JWT.
                        .requestMatchers("/api/payments/webhook").permitAll()
                        // All other API requests require authentication (JWT bearer token)
                        .anyRequest().authenticated()
                ).
                oauth2ResourceServer(oauth2->oauth2.jwt(jwt-> jwt.decoder(jwtDecoder())))
                // Configure as a JWT Resource Server
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // REST API is stateless
                );
        return http.build();
    }
    @Bean
    public JwtDecoder jwtDecoder() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecretKey);
        System.out.println(keyBytes);
        // Create a SecretKeySpec from the shared secret key bytes.
        // The algorithm "HmacSha256" must match the algorithm used by JwtService in User Service.
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "HmacSha256");
        // Build a NimbusJwtDecoder with the specified secret key
        return NimbusJwtDecoder.withSecretKey(secretKey).build();
    }
}

