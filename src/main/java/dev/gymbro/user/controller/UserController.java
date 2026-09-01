package dev.gymbro.user.controller;

import dev.gymbro.auth.AuthUser;
import dev.gymbro.common.error.ApiException;
import dev.gymbro.common.error.ErrorType;
import dev.gymbro.user.dto.MeResponse;
import dev.gymbro.user.repository.UserRepository;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserRepository users;

    public UserController(UserRepository users) {
        this.users = users;
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal AuthUser principal) {
        return users.findById(principal.id())
                .map(MeResponse::from)
                .orElseThrow(() -> new ApiException(ErrorType.UNAUTHENTICATED));
    }
}
