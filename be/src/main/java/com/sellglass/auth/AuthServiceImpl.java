package com.sellglass.auth;

import com.sellglass.auth.dto.LoginRequest;
import com.sellglass.auth.dto.RegisterRequest;
import com.sellglass.auth.dto.TokenResponse;
import com.sellglass.common.exception.AppException;
import com.sellglass.common.exception.ErrorCode;
import com.sellglass.customer.Customer;
import com.sellglass.customer.CustomerRepository;
import com.sellglass.mail.MailService;
import com.sellglass.security.JwtTokenProvider;
import com.sellglass.user.User;
import com.sellglass.user.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String REFRESH_KEY_PREFIX = "refresh:";
    private static final String LOGIN_ATTEMPTS_PREFIX = "login:attempts:";
    private static final String FORGOT_ATTEMPTS_PREFIX = "forgot:attempts:";
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int MAX_FORGOT_ATTEMPTS = 3;
    private static final long LOCKOUT_MINUTES = 15;

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final MailService mailService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

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
        String attemptsKey = LOGIN_ATTEMPTS_PREFIX + request.getEmail();
        checkRateLimit(attemptsKey, MAX_LOGIN_ATTEMPTS, "Quá nhiều lần đăng nhập thất bại. Vui lòng thử lại sau " + LOCKOUT_MINUTES + " phút.");

        Customer customer = customerRepository.findByEmail(request.getEmail()).orElse(null);
        if (customer == null || !passwordEncoder.matches(request.getPassword(), customer.getPasswordHash())) {
            recordFailedAttempt(attemptsKey);
            throw new AppException(ErrorCode.UNAUTHORIZED, "Invalid email or password");
        }
        redisTemplate.delete(attemptsKey);
        return issueCustomerTokens(customer);
    }

    @Override
    public TokenResponse staffLogin(LoginRequest request) {
        String attemptsKey = LOGIN_ATTEMPTS_PREFIX + "staff:" + request.getEmail();
        checkRateLimit(attemptsKey, MAX_LOGIN_ATTEMPTS, "Quá nhiều lần đăng nhập thất bại. Vui lòng thử lại sau " + LOCKOUT_MINUTES + " phút.");

        User user = userRepository.findByEmailAndIsActiveTrue(request.getEmail()).orElse(null);
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            recordFailedAttempt(attemptsKey);
            throw new AppException(ErrorCode.UNAUTHORIZED, "Invalid email or password");
        }
        redisTemplate.delete(attemptsKey);
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

    @Override
    @Transactional
    public void forgotPassword(String email) {
        String attemptsKey = FORGOT_ATTEMPTS_PREFIX + email;
        checkRateLimit(attemptsKey, MAX_FORGOT_ATTEMPTS, "Quá nhiều yêu cầu. Vui lòng thử lại sau " + LOCKOUT_MINUTES + " phút.");
        recordFailedAttempt(attemptsKey); // luôn tăng counter, bất kể email có tồn tại hay không

        Customer customer = customerRepository.findByEmail(email).orElse(null);
        if (customer == null) return; // silent — chống enumeration

        passwordResetTokenRepository.deleteByCustomerId(customer.getId());

        PasswordResetToken token = new PasswordResetToken();
        token.setCustomerId(customer.getId());
        token.setToken(UUID.randomUUID().toString().replace("-", ""));
        token.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        passwordResetTokenRepository.save(token);

        String resetUrl = frontendUrl + "/reset-password?token=" + token.getToken();
        mailService.send(email, "Đặt lại mật khẩu — Sell Glass", "emails/password-reset",
                Map.of("fullName", customer.getFullName(), "resetUrl", resetUrl, "expiresInMinutes", 30));
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken prt = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST, "Invalid or expired token"));
        if (prt.getUsedAt() != null)
            throw new AppException(ErrorCode.BAD_REQUEST, "Token already used");
        if (prt.getExpiresAt().isBefore(LocalDateTime.now()))
            throw new AppException(ErrorCode.BAD_REQUEST, "Token expired");

        Customer customer = customerRepository.findById(prt.getCustomerId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Customer not found"));
        customer.setPasswordHash(passwordEncoder.encode(newPassword));
        customerRepository.save(customer);

        prt.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(prt);
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

    private void checkRateLimit(String key, int maxAttempts, String message) {
        String value = redisTemplate.opsForValue().get(key);
        if (value != null && Integer.parseInt(value) >= maxAttempts) {
            throw new AppException(ErrorCode.TOO_MANY_REQUESTS, message);
        }
    }

    private void recordFailedAttempt(String key) {
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, LOCKOUT_MINUTES, TimeUnit.MINUTES);
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
