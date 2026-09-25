package br.com.hanrry.inventory.shared.exception.inventory.batch;

public class BatchNotFound extends RuntimeException {
    public BatchNotFound(String message) {
        super(message);
    }
}
