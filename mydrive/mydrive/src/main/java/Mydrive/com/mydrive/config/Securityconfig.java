package Mydrive.com.mydrive.config;

import Mydrive.com.mydrive.serviceimpl.UserdetailServiceAdapter;
import io.jsonwebtoken.io.Decoders;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter; // Used for filter ordering

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity // Enables Spring Security's web security features
@EnableMethodSecurity // Enables @PreAuthorize, @PostAuthorize, etc. on methods
@RequiredArgsConstructor // Lombok constructor for final fields
public class Securityconfig {

    @Value("${jwt.secret}")
    private String jwtSecretKey;

    @Value("${internal.api.key}")
    private String internalApiKey; // Injected from application.properties

    @Autowired
    private UserdetailServiceAdapter userdetailServiceAdapter; // Assumed to be your UserDetailsService implementation

    // Defines the UserDetailsService bean, used by DaoAuthenticationProvider
    @Bean
    public UserDetailsService userDetailsService() {
        return userdetailServiceAdapter;
    }

    // Configures the security filter chain for HTTP requests
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable()) // Disable CSRF as JWTs are stateless and don't rely on sessions
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints: No authentication required
                        .requestMatchers("/api/users/check", "/api/users/register", "/api/users/login").permitAll()

                        // Internal Microservice Endpoints: Require Internal API Key (or a valid user JWT if a service calls on behalf of a user)
                        // The InternalApiKeyAuthFilter will check the X-Internal-API-Key header.
                        // If no internal key is found, the request proceeds to be handled by oauth2ResourceServer for JWTs.
                        .requestMatchers("/api/users/internal/**").authenticated()
                        .requestMatchers("/api/users/{userId}/update-plan").authenticated() // Example: could be called by payment service

                        // All other /api/users/** endpoints require user authentication (via JWT)
                        .requestMatchers("/api/users/**").authenticated()

                        // Any other request not explicitly matched above also requires authentication
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        // Configure Spring Security to act as an OAuth2 Resource Server
                        // It will look for "Authorization: Bearer <token>" headers and validate the JWT.
                        .jwt(jwt -> jwt.decoder(jwtDecoder())) // Uses the jwtDecoder() bean for validation
                )
                .sessionManagement(session -> session
                        // Make the session stateless, as JWTs are self-contained and don't require server-side sessions
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // DaoAuthenticationProvider handles username/password authentication for the /login endpoint
                .authenticationProvider(authenticationProvider())

                // Add the custom InternalApiKeyAuthFilter to the Spring Security filter chain.
                // It runs *before* standard authentication filters like BearerTokenAuthenticationFilter.
                // If it successfully authenticates, other authentication mechanisms for that request are skipped.
                .addFilterBefore(internalApiKeyAuthFilter(), UsernamePasswordAuthenticationFilter.class)

                .build(); // Build the SecurityFilterChain
    }

    // Defines the PasswordEncoder bean, used for hashing passwords
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Defines the InternalApiKeyAuthFilter bean, injecting the API key
    @Bean
    public InternalApiKeyAuthFilter internalApiKeyAuthFilter() {
        return new InternalApiKeyAuthFilter(internalApiKey); // Pass the injected key to the filter
    }

    // Defines the JwtDecoder bean, responsible for decoding and verifying JWTs
    @Bean
    public JwtDecoder jwtDecoder() {
        // Creates a SecretKeySpec from the base64-encoded secret for HMAC-SHA256
        byte[] keyBytesForDecoder = Decoders.BASE64.decode(jwtSecretKey);
        // --- END CRITICAL CHANGE ---

        String base64EncodedKeyBytesForDecoder = java.util.Base64.getEncoder().encodeToString(keyBytesForDecoder);
        System.out.println(">>> JwtDecoder: Bytes used by SecretKeySpec (Base64): " + base64EncodedKeyBytesForDecoder);
        SecretKeySpec secretKey = new SecretKeySpec(keyBytesForDecoder,"HmacSha256");
        // Builds a NimbusJwtDecoder with the specified secret key
        return NimbusJwtDecoder.withSecretKey(secretKey).build();
    }

    // Defines the AuthenticationProvider bean, specifically DaoAuthenticationProvider for user details
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userDetailsService()); // Sets the UserDetailsService
        authenticationProvider.setPasswordEncoder(passwordEncoder()); // Sets the PasswordEncoder
        return authenticationProvider;
    }

    // Exposes the AuthenticationManager bean, needed for manual authentication (e.g., at login endpoint)
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}