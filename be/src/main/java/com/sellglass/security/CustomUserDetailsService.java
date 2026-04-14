package com.sellglass.security;

import com.sellglass.customer.CustomerRepository;
import com.sellglass.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Loads user details based on subject prefix in JWT claims.
 * Subject format: "staff:{email}" for internal users, "customer:{email}" for customers.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;

    /**
     * username here is the JWT subject (e.g. "staff:john@example.com" or "customer:jane@example.com")
     */
    @Override
    public UserDetails loadUserByUsername(String subject) throws UsernameNotFoundException {
        if (subject.startsWith("staff:")) {
            String email = subject.substring("staff:".length());
            return userRepository.findByEmailAndIsActiveTrue(email)
                    .map(user -> CustomUserDetails.ofStaff(
                            user.getId(),
                            user.getEmail(),
                            user.getPasswordHash(),
                            user.getRole().name(),
                            user.getBranchId()))
                    .orElseThrow(() -> new UsernameNotFoundException("Staff not found: " + email));
        }

        if (subject.startsWith("customer:")) {
            String email = subject.substring("customer:".length());
            return customerRepository.findByEmail(email)
                    .map(customer -> CustomUserDetails.ofCustomer(
                            customer.getId(),
                            customer.getEmail(),
                            customer.getPasswordHash()))
                    .orElseThrow(() -> new UsernameNotFoundException("Customer not found: " + email));
        }

        throw new UsernameNotFoundException("Unknown subject format: " + subject);
    }
}
