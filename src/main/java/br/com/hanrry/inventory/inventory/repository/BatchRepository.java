package br.com.hanrry.inventory.inventory.repository;

import br.com.hanrry.inventory.inventory.batch.Batch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BatchRepository extends JpaRepository<Batch, Long> {

    List<Batch> findByProductIdAndQuantityGreaterThanOrderByExpiryDateAsc(Long productId, Long quantity);

    Optional<Batch> findByBatchNumber(String batchNumber);

    List<Batch> findByProductId(Long productId);

    List<Batch> findByExpiryDateBefore(LocalDate date);
}
