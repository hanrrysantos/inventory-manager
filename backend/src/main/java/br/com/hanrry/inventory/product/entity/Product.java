package br.com.hanrry.inventory.product.entity;

import br.com.hanrry.inventory.inventory.batch.Batch;
import jakarta.persistence.*;
import br.com.hanrry.inventory.user.entity.User;
import lombok.*;
import org.hibernate.Hibernate;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tb_products", uniqueConstraints = @UniqueConstraint(columnNames = {"owner_id", "sku"}))
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String sku;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(name = "min_stock", nullable = false)
    private Long minStock;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    @BatchSize(size = 100)
    private List<Batch> batches = new ArrayList<>();

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false;
        Product product = (Product) other;
        return id != null && id.equals(product.id);
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
