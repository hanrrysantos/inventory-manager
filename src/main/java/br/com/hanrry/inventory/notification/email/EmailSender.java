package br.com.hanrry.inventory.notification.email;

import java.util.List;

public interface EmailSender {

    void sendLowStockAlert(List<String> productNames, byte[] pdfAttachment);
}
