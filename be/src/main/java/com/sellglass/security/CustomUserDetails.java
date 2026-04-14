package com.sellglass.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Getter
public class CustomUserDetails implements UserDetails {

    private final UUID userId;
    private final String email;
    private final String password;
    private final String role;
    private final UUID branchId;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(UUID userId, String email, String password, String role, UUID branchId,
                             Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.email = email;
        this.password = password;
        this.role = role;
        this.branchId = branchId;
        this.authorities = authorities;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public static CustomUserDetails ofStaff(UUID userId, String email, String password, String role, UUID branchId) {
        List<GrantedAuthority> auths = List.of(() -> "ROLE_" + role);
        return new CustomUserDetails(userId, email, password, role, branchId, auths);
    }

    public static CustomUserDetails ofCustomer(UUID userId, String email, String password) {
        List<GrantedAuthority> auths = List.of(() -> "ROLE_CUSTOMER");
        return new CustomUserDetails(userId, email, password, "CUSTOMER", null, auths);
    }
}
