package br.com.hanrry.inventory.inventory.repository;

import br.com.hanrry.inventory.inventory.movement.InventoryLog;
import br.com.hanrry.inventory.inventory.movement.LogType;
import br.com.hanrry.inventory.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface InventoryLogRepository extends JpaRepository<InventoryLog, Long> {

    @Query("""
            SELECT l FROM InventoryLog l
            JOIN l.product p
            WHERE (:productId IS NULL OR p.id = :productId)
              AND (:batchId IS NULL OR l.batch.id = :batchId)
              AND (:type IS NULL OR l.type = :type)
              AND (:from IS NULL OR l.timestamp >= :from)
              AND (:to IS NULL OR l.timestamp <= :to)
              AND (:owner IS NULL OR p.owner = :owner OR p.owner IS NULL)
            """)
    Page<InventoryLog> findFiltered(
            @Param("productId") Long productId,
            @Param("batchId") Long batchId,
            @Param("type") LogType type,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("owner") User owner,
            Pageable pageable
    );

    @Query("""
            SELECT l FROM InventoryLog l
            JOIN l.product p
            WHERE l.id = :id
              AND (:owner IS NULL OR p.owner = :owner OR p.owner IS NULL)
            """)
    Optional<InventoryLog> findByIdAndOwner(@Param("id") Long id, @Param("owner") User owner);
}
