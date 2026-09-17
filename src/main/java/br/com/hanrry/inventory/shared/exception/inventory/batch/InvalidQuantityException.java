package br.com.hanrry.inventory.shared.exception.inventory.batch;

public class InvalidQuantityException extends RuntimeException {
    public InvalidQuantityException(String message) {
        super(message);
    }
}
