package Mydrive.com.mydrive.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class InternalApiKeyAuthFilter extends OncePerRequestFilter {

    private final String internalApiKey;
    private static final String API_KEY_HEADER = "X-Internal-API-Key";

    public InternalApiKeyAuthFilter(String internalApiKey) {
        this.internalApiKey = internalApiKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestApiKey = request.getHeader(API_KEY_HEADER);
        System.out.println("Filter: Received X-Internal-API-Key: " + requestApiKey); // Using System.out
        System.out.println("Filter: Expected Internal API Key: " + internalApiKey);

        // If the internal API key header is present
        if (requestApiKey != null && !requestApiKey.isEmpty()) {
            // Validate the key
            if (requestApiKey.equals(internalApiKey)) {
                // If valid, set authentication in SecurityContextHolder
                // We're creating a simple authentication token representing an internal service call.
                // The principal could be "internal-service-user" or similar.
                // The "ROLE_INTERNAL" authority signifies it's an authenticated internal call.
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        "internal-service-user",
                        null, // Credentials are not needed once authenticated
                        AuthorityUtils.createAuthorityList("ROLE_INTERNAL")
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
                // Continue the filter chain
                filterChain.doFilter(request, response);
            } else {
                // If key is present but invalid, reject the request
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 Unauthorized
                response.getWriter().write("Invalid Internal API Key");
                // DO NOT continue the filter chain, terminate here
            }
        } else {
            // If the internal API key header is NOT present, let the request proceed to other filters (e.g., BearerTokenAuthenticationFilter for JWTs)
            filterChain.doFilter(request, response);
        }
    }
}