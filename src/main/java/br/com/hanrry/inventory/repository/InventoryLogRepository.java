package br.com.hanrry.inventory.repository;

import br.com.hanrry.inventory.entity.InventoryLog;
import br.com.hanrry.inventory.entity.Type;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface InventoryLogRepository extends JpaRepository<InventoryLog, Long> {

    List<InventoryLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    List<InventoryLog> findByType(Type type);

    List<InventoryLog> findByBatchId(Long batchId);

}
