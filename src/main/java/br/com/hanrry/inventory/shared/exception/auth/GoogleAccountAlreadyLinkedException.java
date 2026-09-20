package br.com.hanrry.inventory.shared.exception.auth;

public class GoogleAccountAlreadyLinkedException extends RuntimeException {

    public GoogleAccountAlreadyLinkedException() {
        super("Google account is already linked to another user");
    }
}
