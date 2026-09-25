package br.com.hanrry.inventory.inventory.batch;

import br.com.hanrry.inventory.product.entity.Product;
import br.com.hanrry.inventory.inventory.movement.InventoryLog;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.Hibernate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tb_batches")
public class Batch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_number", nullable = false, unique = true)
    private String batchNumber;

    @Column(nullable = false)
    private Long quantity;

    @Column(nullable = false, name = "manufacturing_date")
    private LocalDate manufacturingDate;

    @Column(nullable = false, name = "expiry_date")
    private LocalDate expiryDate;

    @Column(nullable = false)
    private BigDecimal price;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @OneToMany(mappedBy = "batch")
    private List<InventoryLog> inventoryLogList;

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false;
        Batch batch = (Batch) other;
        return id != null && id.equals(batch.id);
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
