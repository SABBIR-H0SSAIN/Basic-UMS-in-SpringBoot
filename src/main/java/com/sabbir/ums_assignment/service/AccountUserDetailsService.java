package com.sabbir.ums_assignment.service;

import com.sabbir.ums_assignment.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountUserDetailsService implements UserDetailsService {
    private final AccountRepository repository;
    @Override
    public UserDetails loadUserByUsername(String username) {
        var account = repository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
        return User.withUsername(account.getUsername()).password(account.getPassword())
                .roles(account.getRole()).build();
    }
}
