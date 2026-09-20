package br.com.hanrry.inventory.shared.security;

import br.com.hanrry.inventory.user.entity.User;
import br.com.hanrry.inventory.user.repository.UserRepository;
import br.com.hanrry.inventory.shared.exception.security.OwnerNotAuthenticatedException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OwnerContext {
    private final UserRepository userRepository;

    public User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            throw new OwnerNotAuthenticatedException("Authenticated owner is required");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new OwnerNotAuthenticatedException("Authenticated owner is required"));
    }
}
