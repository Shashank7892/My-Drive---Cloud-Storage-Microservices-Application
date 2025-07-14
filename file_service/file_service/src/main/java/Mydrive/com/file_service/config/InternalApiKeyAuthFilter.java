package Mydrive.com.file_service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String requestApiKey = request.getHeader(API_KEY_HEADER);

        if (requestApiKey != null && !requestApiKey.isEmpty()) {
            if (requestApiKey.equals(internalApiKey)) {
                // If valid, set authentication for an internal service call.
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        "file-service-internal-user",
                        null,
                        AuthorityUtils.createAuthorityList("ROLE_INTERNAL")
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
                filterChain.doFilter(request, response);
            } else {
                // Invalid key
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Invalid Internal API Key for File Service");
            }
        } else {
            // No API key header, let other filters (like JWT) handle it
            filterChain.doFilter(request, response);
        }

    }
}
