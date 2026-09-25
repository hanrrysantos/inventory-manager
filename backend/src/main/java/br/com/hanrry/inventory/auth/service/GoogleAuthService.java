package br.com.hanrry.inventory.auth.service;

import br.com.hanrry.inventory.shared.exception.auth.InvalidTokenException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private final GoogleIdTokenVerifier googleIdTokenVerifier;

    public GoogleIdToken.Payload validarTokenGoogle(String idTokenString) {
        try {
            GoogleIdToken idToken = googleIdTokenVerifier.verify(idTokenString);

            if (idToken != null && hasRequiredIdentityClaims(idToken.getPayload())) {
                return idToken.getPayload();
            }
            throw new InvalidTokenException("Invalid Google ID token");
        } catch (InvalidTokenException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidTokenException("Invalid Google ID token");
        }
    }

    private boolean hasRequiredIdentityClaims(GoogleIdToken.Payload payload) {
        return payload.getSubject() != null && !payload.getSubject().isBlank()
                && payload.getEmail() != null && !payload.getEmail().isBlank()
                && Boolean.TRUE.equals(payload.getEmailVerified());
    }
}
