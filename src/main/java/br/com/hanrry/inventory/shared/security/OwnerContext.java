package br.com.hanrry.inventory.shared.security;

import br.com.hanrry.inventory.user.entity.User;
import br.com.hanrry.inventory.user.repository.UserRepository;
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
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) return null;
        return userRepository.findByEmail(authentication.getName()).orElse(null);
    }
}
