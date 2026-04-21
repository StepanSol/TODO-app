package com.example.todo_service.service;

import com.example.todo_service.model.User;
import com.example.todo_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService{

    private final UserRepository userRepository;

    public String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        Jwt jwt = (Jwt) authentication.getPrincipal();

        return jwt.getSubject();
    }

    public void createOrUpdateUser(Jwt jwt) {

        String keycloakId = jwt.getSubject();

        User user = userRepository.findByKeycloakId(keycloakId)
                .orElseGet(User::new);

        user.setKeycloakId(keycloakId);
        user.setEmail(jwt.getClaimAsString("email"));
        user.setUsername(jwt.getClaimAsString("preferred_username"));
        user.setFullName(jwt.getClaimAsString("name"));

        userRepository.save(user);
    }

    public User getByKeycloakId(String keycloakId) {
        return userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
