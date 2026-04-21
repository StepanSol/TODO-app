package com.example.todo_service.controller;

import com.example.todo_service.model.User;
import com.example.todo_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public User me(@AuthenticationPrincipal Jwt jwt) {
        return userService.getByKeycloakId(jwt.getSubject());
    }
}
