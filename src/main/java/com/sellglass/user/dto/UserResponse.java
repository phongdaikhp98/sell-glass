package com.sellglass.user.dto;

import com.sellglass.user.Role;
import com.sellglass.user.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class UserResponse {

    private UUID id;
    private UUID branchId;
    private String fullName;
    private String email;
    private Role role;
    private boolean isActive;
    private LocalDateTime createdAt;

    public static UserResponse from(User user) {
        UserResponse response = new UserResponse();
        response.id = user.getId();
        response.branchId = user.getBranchId();
        response.fullName = user.getFullName();
        response.email = user.getEmail();
        response.role = user.getRole();
        response.isActive = user.isActive();
        response.createdAt = user.getCreatedAt();
        return response;
    }
}
