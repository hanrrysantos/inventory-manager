package br.com.hanrry.inventory.auth.serviceTest;

import br.com.hanrry.inventory.auth.service.GoogleAuthService;
import br.com.hanrry.inventory.shared.exception.auth.InvalidTokenException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertThrows;

class GoogleAuthServiceTest {

    @Test
    void shouldRejectMalformedGoogleTokenAsAuthenticationFailure() throws Exception {
        GoogleIdTokenVerifier verifier = Mockito.mock(GoogleIdTokenVerifier.class);
        GoogleAuthService googleAuthService = new GoogleAuthService(verifier);
        Mockito.when(verifier.verify("malformed-token")).thenThrow(new IllegalArgumentException());

        assertThrows(
                InvalidTokenException.class,
                () -> googleAuthService.validarTokenGoogle("malformed-token")
        );
    }

    @Test
    void shouldRejectGoogleTokenWithoutVerifiedEmail() throws Exception {
        GoogleIdTokenVerifier verifier = Mockito.mock(GoogleIdTokenVerifier.class);
        GoogleIdToken idToken = Mockito.mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload()
                .setSubject("google-subject-123")
                .setEmail("user@example.com")
                .setEmailVerified(false);
        GoogleAuthService googleAuthService = new GoogleAuthService(verifier);

        Mockito.when(verifier.verify("unverified-email-token")).thenReturn(idToken);
        Mockito.when(idToken.getPayload()).thenReturn(payload);

        assertThrows(
                InvalidTokenException.class,
                () -> googleAuthService.validarTokenGoogle("unverified-email-token")
        );
    }
}
