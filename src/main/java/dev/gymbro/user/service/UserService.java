package dev.gymbro.user.service;

import dev.gymbro.common.error.ApiException;
import dev.gymbro.common.error.ErrorType;
import dev.gymbro.user.dto.MeResponse;
import dev.gymbro.user.repository.UserRepository;
import org.springframework.stereotype.Service;

/** Serves the authenticated user their own profile. */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Loads the profile for an id that came from a valid access token. A missing
     * row means the account was deleted after the token was issued, so this is
     * reported as {@link ErrorType#UNAUTHENTICATED} rather than a 404.
     */
    public MeResponse getMe(Long userId) {
        return userRepository.findById(userId)
                .map(MeResponse::from)
                .orElseThrow(() -> new ApiException(ErrorType.UNAUTHENTICATED));
    }
}