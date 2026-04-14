package com.sellglass.user;

import com.sellglass.common.response.PageResponse;
import com.sellglass.user.dto.UserRequest;
import com.sellglass.user.dto.UserResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {

    PageResponse<UserResponse> findAll(Pageable pageable);

    UserResponse findById(UUID id);

    UserResponse create(UserRequest request);

    UserResponse update(UUID id, UserRequest request);

    void deactivate(UUID id);
}
