package br.com.hanrry.inventory.shared.exception.auth;

public class GoogleAccountLinkRequiredException extends RuntimeException {

    public GoogleAccountLinkRequiredException() {
        super("Google account must be linked from an authenticated session");
    }
}
