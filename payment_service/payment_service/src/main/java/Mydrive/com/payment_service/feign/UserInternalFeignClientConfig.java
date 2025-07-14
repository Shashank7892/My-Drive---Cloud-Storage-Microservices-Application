package Mydrive.com.payment_service.feign;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserInternalFeignClientConfig {
    @Value("${user_service.internal.api.key}")
    private String internalApiKey;

    @Bean
    public RequestInterceptor internalServiceApiKeyInterceptor() {
        return requestTemplate -> {
            // Apply the internal API key header for all requests made by this Feign client.
            // The header name 'X-Internal-API-Key' must exactly match what your User Service's
            // InternalApiKeyAuthFilter is looking for.
            requestTemplate.header("X-Internal-API-Key", internalApiKey);
        };
    }
}
