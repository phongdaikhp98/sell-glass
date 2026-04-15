package com.sellglass.user;

import com.sellglass.common.exception.AppException;
import com.sellglass.common.exception.ErrorCode;
import com.sellglass.user.dto.UserRequest;
import com.sellglass.user.dto.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl service;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        user.setFullName("Nguyen Van A");
        user.setEmail("staff@example.com");
        user.setPasswordHash("hashed");
        user.setRole(Role.STAFF);
        user.setActive(true);
    }

    private UserRequest buildRequest() {
        UserRequest request = new UserRequest();
        request.setFullName("Nguyen Van A");
        request.setEmail("staff@example.com");
        request.setPassword("password123");
        request.setRole(Role.STAFF);
        return request;
    }

    @Test
    @DisplayName("findById should return user when exists")
    void findById_success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserResponse result = service.findById(userId);

        assertThat(result.getId()).isEqualTo(userId);
        assertThat(result.getRole()).isEqualTo(Role.STAFF);
    }

    @Test
    @DisplayName("findById should throw NOT_FOUND when user missing")
    void findById_notFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(userId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("create should save user with encoded password")
    void create_success() {
        when(userRepository.existsByEmail("staff@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(userId);
            return u;
        });

        UserResponse result = service.create(buildRequest());

        assertThat(result.getEmail()).isEqualTo("staff@example.com");
        assertThat(result.getRole()).isEqualTo(Role.STAFF);
        verify(passwordEncoder).encode("password123");
    }

    @Test
    @DisplayName("create should throw CONFLICT when email already in use")
    void create_emailConflict() {
        when(userRepository.existsByEmail("staff@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.create(buildRequest()))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("update should change name and role without re-encoding password when blank")
    void update_noPasswordChange() {
        UserRequest request = new UserRequest();
        request.setFullName("Updated Name");
        request.setEmail("staff@example.com");
        request.setPassword(""); // blank = keep current
        request.setRole(Role.BRANCH_MANAGER);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse result = service.update(userId, request);

        assertThat(result.getFullName()).isEqualTo("Updated Name");
        assertThat(result.getRole()).isEqualTo(Role.BRANCH_MANAGER);
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("update should re-encode password when non-blank password provided")
    void update_withPasswordChange() {
        UserRequest request = new UserRequest();
        request.setFullName("Name");
        request.setEmail("staff@example.com");
        request.setPassword("newPassword");
        request.setRole(Role.STAFF);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPassword")).thenReturn("new-hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        service.update(userId, request);

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        verify(passwordEncoder).encode("newPassword");
    }

    @Test
    @DisplayName("update should throw NOT_FOUND when user missing")
    void update_notFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(userId, buildRequest()))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("deactivate should set isActive to false")
    void deactivate_success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        service.deactivate(userId);

        assertThat(user.isActive()).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("deactivate should throw NOT_FOUND when user missing")
    void deactivate_notFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivate(userId))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
        verify(userRepository, never()).save(any());
    }
}
