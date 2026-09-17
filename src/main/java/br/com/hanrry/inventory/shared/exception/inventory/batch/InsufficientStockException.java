package br.com.hanrry.inventory.shared.exception.inventory.batch;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String message) {
        super(message);
    }
}
