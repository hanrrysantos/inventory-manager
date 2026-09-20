package br.com.hanrry.inventory.product.repository;

import br.com.hanrry.inventory.product.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
import br.com.hanrry.inventory.user.entity.User;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByNameIgnoreCase(String name);
    @org.springframework.data.jpa.repository.Query("select c from Category c where c.id = :id and (c.owner = :owner or c.owner is null)")
    Optional<Category> findByIdAndOwner(@org.springframework.data.repository.query.Param("id") Long id, @org.springframework.data.repository.query.Param("owner") User owner);
    @org.springframework.data.jpa.repository.Query("select c from Category c where lower(c.name) = lower(:name) and (c.owner = :owner or c.owner is null)")
    Optional<Category> findByNameIgnoreCaseAndOwner(@org.springframework.data.repository.query.Param("name") String name, @org.springframework.data.repository.query.Param("owner") User owner);
    @org.springframework.data.jpa.repository.Query("select c from Category c where lower(c.name) = lower(:name) and c.owner = :owner and c.id <> :id")
    Optional<Category> findByNameIgnoreCaseAndOwnerExcludingId(@org.springframework.data.repository.query.Param("name") String name, @org.springframework.data.repository.query.Param("owner") User owner, @org.springframework.data.repository.query.Param("id") Long id);
    @org.springframework.data.jpa.repository.Query("select c from Category c where c.owner = :owner or c.owner is null")
    List<Category> findAllByOwner(@org.springframework.data.repository.query.Param("owner") User owner);
}
