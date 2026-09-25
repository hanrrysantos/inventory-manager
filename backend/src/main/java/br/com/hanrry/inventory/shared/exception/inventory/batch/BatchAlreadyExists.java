package br.com.hanrry.inventory.shared.exception.inventory.batch;

public class BatchAlreadyExists extends RuntimeException {
    public BatchAlreadyExists(String message) {
        super(message);
    }
}
