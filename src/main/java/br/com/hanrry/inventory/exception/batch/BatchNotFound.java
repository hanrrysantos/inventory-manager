package br.com.hanrry.inventory.exception.batch;

public class BatchNotFound extends RuntimeException {
    public BatchNotFound(String message) {
        super(message);
    }
}
