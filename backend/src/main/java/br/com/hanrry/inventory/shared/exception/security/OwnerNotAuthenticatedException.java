package br.com.hanrry.inventory.shared.exception.security;

public class OwnerNotAuthenticatedException extends RuntimeException {
    public OwnerNotAuthenticatedException(String message) {
        super(message);
    }
}
