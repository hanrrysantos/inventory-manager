package br.com.hanrry.inventory.inventory.service;

import br.com.hanrry.inventory.inventory.batch.Batch;
import br.com.hanrry.inventory.inventory.dto.invetoryLog.InventoryLogResponseDTO;
import br.com.hanrry.inventory.inventory.mapper.InventoryLogMapper;
import br.com.hanrry.inventory.inventory.movement.InventoryLog;
import br.com.hanrry.inventory.inventory.movement.LogType;
import br.com.hanrry.inventory.inventory.repository.InventoryLogRepository;
import br.com.hanrry.inventory.shared.dto.PageResponse;
import br.com.hanrry.inventory.shared.exception.inventory.log.InventoryLogNotFoundException;
import br.com.hanrry.inventory.shared.security.OwnerContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class InventoryLogService {

    private final InventoryLogRepository logRepository;
    private final InventoryLogMapper inventoryLogMapper;
    private final OwnerContext ownerContext;

    @Transactional
    public void createLog(Batch batch, Long quantity, LogType type) {
        InventoryLog log = new InventoryLog();

        log.setBatch(batch);
        log.setProduct(batch.getProduct());
        log.setQuantity(quantity);
        log.setType(type);

        logRepository.save(log);
    }

    @Transactional(readOnly = true)
    public PageResponse<InventoryLogResponseDTO> findLogs(
            Long productId,
            Long batchId,
            LogType type,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    ) {
        var owner = ownerContext == null ? null : ownerContext.currentUser();
        Page<InventoryLog> page = logRepository.findFiltered(
                productId,
                batchId,
                type,
                from,
                to,
                owner,
                pageable
        );
        return PageResponse.from(page, inventoryLogMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public InventoryLogResponseDTO findLogById(Long id) {
        var owner = ownerContext == null ? null : ownerContext.currentUser();
        InventoryLog log = logRepository.findByIdAndOwner(id, owner)
                .orElseThrow(() -> new InventoryLogNotFoundException("Inventory log not found with id: " + id));
        return inventoryLogMapper.toDTO(log);
    }
}
