package br.com.naheroback.common.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.Arrays;
import java.util.List;

@Configuration
public class WhiteListConfig {

    @Bean
    public RequestMatcher whiteListedRoutes() {
        List<RequestMatcher> matchers = Arrays.asList(
                new AntPathRequestMatcher("/error", "GET"),
                new AntPathRequestMatcher("/auth/**", "POST"),
                new AntPathRequestMatcher("/users", "POST"),
                new AntPathRequestMatcher("/courses/**", "GET"),
                new AntPathRequestMatcher("/users/verify/**", "GET"),
                new AntPathRequestMatcher("/payment/**", "POST"),
                new AntPathRequestMatcher("/certificates/**", "GET")
        );
        return new OrRequestMatcher(matchers);
    }
}