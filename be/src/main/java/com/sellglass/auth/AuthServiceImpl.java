package com.sellglass.auth;

import com.sellglass.auth.dto.LoginRequest;
import com.sellglass.auth.dto.RegisterRequest;
import com.sellglass.auth.dto.TokenResponse;
import com.sellglass.common.exception.AppException;
import com.sellglass.common.exception.ErrorCode;
import com.sellglass.customer.Customer;
import com.sellglass.customer.CustomerRepository;
import com.sellglass.security.JwtTokenProvider;
import com.sellglass.user.User;
import com.sellglass.user.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String REFRESH_KEY_PREFIX = "refresh:";

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.CONFLICT, "Email already registered");
        }
        Customer customer = new Customer();
        customer.setFullName(request.getFullName());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        customer = customerRepository.save(customer);

        return issueCustomerTokens(customer);
    }

    @Override
    public TokenResponse login(LoginRequest request) {
        Customer customer = customerRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED, "Invalid email or password"));
        if (!passwordEncoder.matches(request.getPassword(), customer.getPasswordHash())) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Invalid email or password");
        }
        return issueCustomerTokens(customer);
    }

    @Override
    public TokenResponse staffLogin(LoginRequest request) {
        User user = userRepository.findByEmailAndIsActiveTrue(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED, "Invalid email or password"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Invalid email or password");
        }
        return issueStaffTokens(user);
    }

    @Override
    public TokenResponse refresh(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Invalid or expired refresh token");
        }
        Claims claims = jwtTokenProvider.parseToken(refreshToken);
        String type = claims.get("type", String.class);
        if (!"refresh".equals(type)) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Not a refresh token");
        }

        String userId = claims.get("userId", String.class);
        String storedToken = redisTemplate.opsForValue().get(REFRESH_KEY_PREFIX + userId);
        if (!refreshToken.equals(storedToken)) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Refresh token revoked or invalid");
        }

        String subject = claims.getSubject();
        if (subject.startsWith("customer:")) {
            String email = subject.substring("customer:".length());
            Customer customer = customerRepository.findByEmail(email)
                    .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Customer not found"));
            return issueCustomerTokens(customer);
        } else if (subject.startsWith("staff:")) {
            String email = subject.substring("staff:".length());
            User user = userRepository.findByEmailAndIsActiveTrue(email)
                    .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "User not found"));
            return issueStaffTokens(user);
        }

        throw new AppException(ErrorCode.UNAUTHORIZED, "Unknown token subject");
    }

    @Override
    public void logout(String accessToken) {
        if (!jwtTokenProvider.validateToken(accessToken)) {
            return;
        }
        String userId = jwtTokenProvider.getUserIdFromToken(accessToken);
        redisTemplate.delete(REFRESH_KEY_PREFIX + userId);
    }

    private TokenResponse issueCustomerTokens(Customer customer) {
        String subject = "customer:" + customer.getEmail();
        String accessToken = jwtTokenProvider.generateAccessToken(
                customer.getId(), customer.getEmail(), "CUSTOMER", subject);
        String refreshToken = jwtTokenProvider.generateRefreshToken(customer.getId(), subject);

        redisTemplate.opsForValue().set(
                REFRESH_KEY_PREFIX + customer.getId(),
                refreshToken,
                jwtTokenProvider.getRefreshTokenExpirySeconds(),
                TimeUnit.SECONDS
        );
        return new TokenResponse(accessToken, refreshToken, jwtTokenProvider.getRefreshTokenExpirySeconds());
    }

    private TokenResponse issueStaffTokens(User user) {
        String subject = "staff:" + user.getEmail();
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().name(), subject);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), subject);

        redisTemplate.opsForValue().set(
                REFRESH_KEY_PREFIX + user.getId(),
                refreshToken,
                jwtTokenProvider.getRefreshTokenExpirySeconds(),
                TimeUnit.SECONDS
        );
        return new TokenResponse(accessToken, refreshToken, jwtTokenProvider.getRefreshTokenExpirySeconds());
    }
}
