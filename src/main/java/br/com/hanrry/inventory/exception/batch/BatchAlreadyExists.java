package br.com.hanrry.inventory.exception.batch;

public class BatchAlreadyExists extends RuntimeException {
    public BatchAlreadyExists(String message) {
        super(message);
    }
}
