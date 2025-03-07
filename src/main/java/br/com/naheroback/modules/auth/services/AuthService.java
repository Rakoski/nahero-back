package br.com.naheroback.modules.auth.services;

import br.com.naheroback.common.exceptions.custom.UnauthorizedException;
import br.com.naheroback.modules.auth.entities.AuthenticatedUser;
import br.com.naheroback.modules.auth.useCases.login.LoginRequest;
import br.com.naheroback.modules.auth.useCases.login.LoginResponse;
import br.com.naheroback.modules.auth.useCases.refreshToken.RefreshTokenRequest;
import br.com.naheroback.modules.auth.useCases.refreshToken.RefreshTokenResponse;
import br.com.naheroback.modules.user.entities.Role;
import br.com.naheroback.modules.user.entities.User;
import br.com.naheroback.modules.user.repositories.UserRepository;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final LoginResponse loginUserResponse;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RefreshTokenResponse refreshTokenResponse;

    @Value("${auth.jwt.access-token.expiration}")
    private Integer accessTokenExpirationTime;

    @Value("${auth.jwt.refresh-token.expiration}")
    private Integer refreshTokenExpirationTime;

    public LoginResponse login(LoginRequest loginRequest) {
        try {
            var token = new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password());
            var authentication = this.authenticationManager.authenticate(token);

            AuthenticatedUser authenticatedUser = (AuthenticatedUser) authentication.getPrincipal();
            List<String> permissions = authenticatedUser.getRoles();

            return this.generateTokens(authenticatedUser.getId(), permissions, authenticatedUser.getUser());
        } catch (RuntimeException e) {
            throw new UnauthorizedException();
        }
    }

    public RefreshTokenResponse refresh(RefreshTokenRequest refreshTokenRequest) {
        DecodedJWT decodedJWT = this.jwtService.decodeAndValidateToken(refreshTokenRequest.refreshToken());

        if (this.jwtService.isTokenExpired(decodedJWT)) {
            throw new UnauthorizedException();
        }

        if (!this.jwtService.isRefreshToken(decodedJWT)) {
            throw new UnauthorizedException();
        }

        Integer userId = Integer.parseInt(decodedJWT.getSubject());
        User user = this.userRepository.findById(userId).orElseThrow(UnauthorizedException::new);
        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .toList();

        var accessToken = this.jwtService.generateToken(user.getId(), roles, accessTokenExpirationTime, "access");

        return refreshTokenResponse.toPresentation(accessToken);
    }

    public LoginResponse generateTokens(Integer userId, List<String> permissions, User user) {
        var accessToken = this.jwtService.generateToken(userId, permissions, accessTokenExpirationTime, "access");
        var refreshToken = this.jwtService.generateToken(userId, permissions, refreshTokenExpirationTime, "refresh");

        return loginUserResponse.toPresentation(accessToken, refreshToken, user);
    }

    public static User getUserFromToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser)) {
            throw new UnauthorizedException();
        }

        return authenticatedUser.getUser();
    }
}