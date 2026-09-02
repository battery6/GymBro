package dev.gymbro.user.service;

import dev.gymbro.common.error.ApiException;
import dev.gymbro.common.error.ErrorType;
import dev.gymbro.user.dto.MeResponse;
import dev.gymbro.user.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public MeResponse getMe(Long userId) {
        return userRepository.findById(userId)
                .map(MeResponse::from)
                .orElseThrow(() -> new ApiException(ErrorType.UNAUTHENTICATED));
    }
}