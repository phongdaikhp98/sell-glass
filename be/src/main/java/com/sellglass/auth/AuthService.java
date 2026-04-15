package com.sellglass.auth;

import com.sellglass.auth.dto.LoginRequest;
import com.sellglass.auth.dto.RegisterRequest;
import com.sellglass.auth.dto.TokenResponse;

public interface AuthService {

    TokenResponse register(RegisterRequest request);

    TokenResponse login(LoginRequest request);

    TokenResponse staffLogin(LoginRequest request);

    TokenResponse refresh(String refreshToken);

    void logout(String accessToken);

    void forgotPassword(String email);

    void resetPassword(String token, String newPassword);
}
