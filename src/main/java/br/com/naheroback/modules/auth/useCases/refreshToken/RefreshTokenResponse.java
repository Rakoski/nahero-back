package br.com.naheroback.modules.auth.useCases.refreshToken;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenResponse {
    public String accessToken;

    @Autowired
    private ModelMapper modelMapper;

    public RefreshTokenResponse toPresentation(String accessToken) {
        RefreshTokenResponse response = new RefreshTokenResponse();
        response.accessToken = modelMapper.map(accessToken, String.class);
        return response;
    }
}

