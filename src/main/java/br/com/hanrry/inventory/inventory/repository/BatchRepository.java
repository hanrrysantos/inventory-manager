package br.com.hanrry.inventory.inventory.repository;

import br.com.hanrry.inventory.inventory.batch.Batch;
import br.com.hanrry.inventory.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BatchRepository extends JpaRepository<Batch, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Batch> findByProductIdAndQuantityGreaterThanAndExpiryDateGreaterThanEqualOrderByExpiryDateAscIdAsc(
            Long productId, Long quantity, LocalDate expiryDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Batch b where b.id = :id")
    Optional<Batch> findByIdForUpdate(@Param("id") Long id);

    Optional<Batch> findByBatchNumber(String batchNumber);

    List<Batch> findByProductId(Long productId);

    List<Batch> findByExpiryDateBefore(LocalDate date);

    @Query("""
            SELECT b FROM Batch b
            JOIN b.product p
            WHERE b.expiryDate < :today
              AND (:owner IS NULL OR p.owner = :owner OR p.owner IS NULL)
            """)
    Page<Batch> findExpiredBatches(
            @Param("today") LocalDate today,
            @Param("owner") User owner,
            Pageable pageable
    );

    @Query("""
            SELECT b FROM Batch b
            JOIN b.product p
            WHERE p.id = :productId
              AND (:owner IS NULL OR p.owner = :owner OR p.owner IS NULL)
            """)
    Page<Batch> findByProductId(
            @Param("productId") Long productId,
            @Param("owner") User owner,
            Pageable pageable
    );
}
