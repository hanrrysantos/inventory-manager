package br.com.hanrry.inventory.product.repository;

import br.com.hanrry.inventory.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("select distinct p from Product p left join fetch p.batches")
    List<Product> findAllWithBatches();

    Optional<Product> findBySku(String sku);
}
