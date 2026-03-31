package br.com.hanrry.inventory.service;

import br.com.hanrry.inventory.entity.Batch;
import br.com.hanrry.inventory.entity.InventoryLog;
import br.com.hanrry.inventory.entity.enums.LogType;
import br.com.hanrry.inventory.repository.InventoryLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryLogService {

    private final InventoryLogRepository logRepository;

    @Transactional
    public void createLog(Batch batch, Long quantity, LogType type) {
        InventoryLog log = new InventoryLog();

        log.setBatch(batch);
        log.setProduct(batch.getProduct());
        log.setQuantity(quantity);
        log.setType(type);

        logRepository.save(log);
    }
}