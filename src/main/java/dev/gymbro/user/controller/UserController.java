package dev.gymbro.user.controller;

import dev.gymbro.auth.AuthUser;
import dev.gymbro.user.dto.MeResponse;
import dev.gymbro.user.service.UserService;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal AuthUser principal) {
        return userService.getMe(principal.id());
    }
}
