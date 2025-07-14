package Mydrive.com.file_service.config;

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
import io.jsonwebtoken.io.Decoders;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${jwt.secret}")
    private String jwtSecretKey;

    @Value("${file.service.internal.api.key}") // <--- Inject the new internal API key for THIS service
    private String fileServiceInternalApiKey;
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)throws Exception{
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize->authorize.
                        requestMatchers("/api/files/**").authenticated()
                        .requestMatchers("/api/files/internals").authenticated()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2->oauth2.jwt(jwt->jwt.decoder(jwtDecoder())))
                .sessionManagement(session->session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(internalApiKeyAuthFilter(), UsernamePasswordAuthenticationFilter.class);
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
    @Bean
    public InternalApiKeyAuthFilter internalApiKeyAuthFilter() {
        return new InternalApiKeyAuthFilter(fileServiceInternalApiKey);
    }

}

