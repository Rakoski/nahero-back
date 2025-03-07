package br.com.naheroback.common.configs;

import br.com.naheroback.common.exceptions.custom.UnauthorizedException;
import br.com.naheroback.modules.auth.services.CustomUserDetailsService;
import br.com.naheroback.modules.auth.services.JwtService;
import com.auth0.jwt.interfaces.DecodedJWT;
import br.com.naheroback.common.exceptions.GlobalExceptionHandler;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtSecurityFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailService;
    private final GlobalExceptionHandler globalExceptionHandler;
    private final RequestMatcher whiteListedRoutes;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws IOException {
        try {
            String token = extractTokenFromHeader(request);
            boolean isRouteWhitelisted = whiteListedRoutes.matches(request);

            if (tokenIsNull(token, request, response, filterChain, isRouteWhitelisted)) {
                return;
            }

            processRequest(request, response, filterChain, token);
        } catch (Exception e) {
            if (isUnauthorizedException(e)) {
                globalExceptionHandler.sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Unauthorized", request.getRequestURI());
            } else {
                log.error("Internal server error: ", e);
                logger.error("Internal server error: ", e);
                globalExceptionHandler.sendErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", request.getRequestURI());
            }
        }
    }

    private boolean tokenIsNull(String token, HttpServletRequest request, HttpServletResponse response, FilterChain filterChain, boolean isRouteWhitelisted) throws IOException, ServletException {
        if (Objects.isNull(token)) {
            if (isRouteWhitelisted) {
                filterChain.doFilter(request, response);
            } else {
                globalExceptionHandler.sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Unauthorized", request.getRequestURI());
            }
            return true;
        }
        return false;
    }

    private void processRequest(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain, String token) throws IOException, ServletException {
        DecodedJWT decodedJWT = jwtService.decodeAndValidateToken(token);
        String userId = decodedJWT.getSubject();
        var authenticatedUser = this.customUserDetailService.loadUserById(userId);
        var authentication = new UsernamePasswordAuthenticationToken(authenticatedUser, null, authenticatedUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    private boolean isUnauthorizedException(Exception e) {
        return Objects.equals(e.getMessage(),
                "Request processing failed: br.com.naheroback.common.exceptions.custom.UnauthorizedException: Unauthorized")
                || e instanceof UnauthorizedException || e instanceof BadCredentialsException;
    }

    private String extractTokenFromHeader(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");
        if (Objects.isNull(authHeader) || !authHeader.startsWith("Bearer "))
            return null;
        return authHeader.substring(7);
    }
}
