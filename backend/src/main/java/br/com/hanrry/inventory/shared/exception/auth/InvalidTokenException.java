package br.com.hanrry.inventory.shared.exception.auth;

public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException(String message) {
        super(message);
    }
}
