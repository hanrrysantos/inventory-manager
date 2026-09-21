package br.com.hanrry.inventory.shared.exception.pagination;

public class InvalidPaginationException extends RuntimeException {

    public InvalidPaginationException(String message) {
        super(message);
    }
}
