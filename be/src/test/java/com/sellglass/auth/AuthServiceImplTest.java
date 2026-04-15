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
import com.sellglass.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    @SuppressWarnings("rawtypes")
    private RedisTemplate redisTemplate;

    @Mock
    @SuppressWarnings("rawtypes")
    private ValueOperations valueOperations;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private MailService mailService;

    @InjectMocks
    private AuthServiceImpl service;

    private UUID customerId;
    private Customer customer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "frontendUrl", "http://localhost:3000");

        customerId = UUID.randomUUID();
        customer = new Customer();
        customer.setId(customerId);
        customer.setFullName("Nguyen Van A");
        customer.setEmail("a@example.com");
        customer.setPasswordHash("hashed");
    }

    @Test
    @DisplayName("register should create customer and issue tokens")
    void register_success() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Nguyen Van A");
        request.setEmail("a@example.com");
        request.setPassword("password123");

        when(customerRepository.existsByEmail("a@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> {
            Customer saved = inv.getArgument(0);
            saved.setId(customerId);
            return saved;
        });
        when(jwtTokenProvider.generateAccessToken(any(), anyString(), anyString(), anyString()))
                .thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(any(), anyString())).thenReturn("refresh-token");
        when(jwtTokenProvider.getRefreshTokenExpirySeconds()).thenReturn(3600L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        TokenResponse result = service.register(request);

        assertThat(result.getAccessToken()).isEqualTo("access-token");
        assertThat(result.getRefreshToken()).isEqualTo("refresh-token");
        verify(customerRepository).save(any(Customer.class));
        verify(valueOperations).set(eq("refresh:" + customerId), eq("refresh-token"), eq(3600L), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("register should throw CONFLICT when email exists")
    void register_emailExists() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("a@example.com");
        request.setPassword("password123");

        when(customerRepository.existsByEmail("a@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT);
        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("login should issue tokens on valid credentials")
    void login_success() {
        LoginRequest request = new LoginRequest();
        request.setEmail("a@example.com");
        request.setPassword("password123");

        when(customerRepository.findByEmail("a@example.com")).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(any(), anyString(), anyString(), anyString()))
                .thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(any(), anyString())).thenReturn("refresh-token");
        when(jwtTokenProvider.getRefreshTokenExpirySeconds()).thenReturn(3600L);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        TokenResponse result = service.login(request);

        assertThat(result.getAccessToken()).isEqualTo("access-token");
    }

    @Test
    @DisplayName("login should throw UNAUTHORIZED when email not found")
    void login_emailNotFound() {
        LoginRequest request = new LoginRequest();
        request.setEmail("missing@example.com");
        request.setPassword("password");

        when(customerRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("login should throw UNAUTHORIZED on wrong password")
    void login_wrongPassword() {
        LoginRequest request = new LoginRequest();
        request.setEmail("a@example.com");
        request.setPassword("wrong");

        when(customerRepository.findByEmail("a@example.com")).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> service.login(request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("forgotPassword should silently return when email not found (anti-enumeration)")
    void forgotPassword_unknownEmail() {
        when(customerRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        service.forgotPassword("missing@example.com");

        verify(passwordResetTokenRepository, never()).save(any());
        verify(mailService, never()).send(anyString(), anyString(), anyString(), anyMap());
    }

    @Test
    @DisplayName("forgotPassword should generate token and send email when email exists")
    void forgotPassword_success() {
        when(customerRepository.findByEmail("a@example.com")).thenReturn(Optional.of(customer));

        service.forgotPassword("a@example.com");

        verify(passwordResetTokenRepository).deleteByCustomerId(customerId);
        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(captor.capture());
        PasswordResetToken saved = captor.getValue();
        assertThat(saved.getCustomerId()).isEqualTo(customerId);
        assertThat(saved.getToken()).isNotBlank();
        assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now());

        verify(mailService).send(eq("a@example.com"), contains("Đặt lại mật khẩu"),
                eq("emails/password-reset"), anyMap());
    }

    @Test
    @DisplayName("resetPassword should throw BAD_REQUEST when token not found")
    void resetPassword_tokenNotFound() {
        when(passwordResetTokenRepository.findByToken("bad-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resetPassword("bad-token", "newPass"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    @DisplayName("resetPassword should throw BAD_REQUEST when token already used")
    void resetPassword_tokenUsed() {
        PasswordResetToken prt = new PasswordResetToken();
        prt.setToken("token");
        prt.setCustomerId(customerId);
        prt.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        prt.setUsedAt(LocalDateTime.now().minusMinutes(1));

        when(passwordResetTokenRepository.findByToken("token")).thenReturn(Optional.of(prt));

        assertThatThrownBy(() -> service.resetPassword("token", "newPass"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    @DisplayName("resetPassword should throw BAD_REQUEST when token expired")
    void resetPassword_tokenExpired() {
        PasswordResetToken prt = new PasswordResetToken();
        prt.setToken("token");
        prt.setCustomerId(customerId);
        prt.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(passwordResetTokenRepository.findByToken("token")).thenReturn(Optional.of(prt));

        assertThatThrownBy(() -> service.resetPassword("token", "newPass"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    @DisplayName("resetPassword should update password and mark token used on valid token")
    void resetPassword_success() {
        PasswordResetToken prt = new PasswordResetToken();
        prt.setToken("token");
        prt.setCustomerId(customerId);
        prt.setExpiresAt(LocalDateTime.now().plusMinutes(10));

        when(passwordResetTokenRepository.findByToken("token")).thenReturn(Optional.of(prt));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(passwordEncoder.encode("newPass")).thenReturn("new-hash");

        service.resetPassword("token", "newPass");

        assertThat(customer.getPasswordHash()).isEqualTo("new-hash");
        assertThat(prt.getUsedAt()).isNotNull();
        verify(customerRepository).save(customer);
        verify(passwordResetTokenRepository).save(prt);
    }

    @Test
    @DisplayName("logout should delete refresh token from Redis when access token valid")
    void logout_validToken() {
        when(jwtTokenProvider.validateToken("access")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken("access")).thenReturn(customerId.toString());

        service.logout("access");

        verify(redisTemplate).delete("refresh:" + customerId);
    }

    @Test
    @DisplayName("logout should be no-op when token invalid")
    void logout_invalidToken() {
        when(jwtTokenProvider.validateToken("bad")).thenReturn(false);

        service.logout("bad");

        verify(redisTemplate, never()).delete(anyString());
    }
}
