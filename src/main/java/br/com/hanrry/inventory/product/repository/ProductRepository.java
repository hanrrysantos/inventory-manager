package br.com.hanrry.inventory.product.repository;

import br.com.hanrry.inventory.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import br.com.hanrry.inventory.user.entity.User;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("select distinct p from Product p left join fetch p.batches")
    List<Product> findAllWithBatches();

    Optional<Product> findBySku(String sku);
    @Query("select p from Product p where p.id = :id and (p.owner = :owner or p.owner is null)")
    Optional<Product> findByIdAndOwner(@org.springframework.data.repository.query.Param("id") Long id, @org.springframework.data.repository.query.Param("owner") User owner);
    @Query("select p from Product p where p.sku = :sku and (p.owner = :owner or p.owner is null)")
    Optional<Product> findBySkuAndOwner(@org.springframework.data.repository.query.Param("sku") String sku, @org.springframework.data.repository.query.Param("owner") User owner);
    @Query("select p from Product p where p.owner = :owner or p.owner is null")
    List<Product> findAllByOwner(@org.springframework.data.repository.query.Param("owner") User owner);

    @Query("select p from Product p where p.owner = :owner or p.owner is null")
    Page<Product> findAllByOwner(
            @org.springframework.data.repository.query.Param("owner") User owner,
            Pageable pageable
    );
    @Query("select distinct p from Product p left join fetch p.batches where p.owner = :owner or p.owner is null")
    List<Product> findAllWithBatchesByOwner(@org.springframework.data.repository.query.Param("owner") User owner);
}
