package br.com.hanrry.inventory.shared.exception.notification.email;

public class EmailSendException extends RuntimeException {

    public EmailSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
