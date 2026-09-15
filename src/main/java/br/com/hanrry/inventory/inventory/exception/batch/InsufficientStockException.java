package br.com.hanrry.inventory.inventory.exception.batch;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String message) {
        super(message);
    }
}
