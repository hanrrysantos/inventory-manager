package br.com.hanrry.inventory.inventory.repository;

import br.com.hanrry.inventory.inventory.movement.InventoryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryLogRepository extends JpaRepository<InventoryLog, Long> {
}
