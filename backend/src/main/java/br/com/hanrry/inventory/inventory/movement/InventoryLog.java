package br.com.hanrry.inventory.inventory.movement;

import br.com.hanrry.inventory.inventory.batch.Batch;
import br.com.hanrry.inventory.product.entity.Product;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.Hibernate;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tb_inventory_logs")
public class InventoryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LogType type;

    @Column(nullable = false)
    private Long quantity;

    @CreationTimestamp
    @Column(nullable = false)
    private LocalDateTime timestamp;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne
    @JoinColumn(name = "batch_id")
    private Batch batch;

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false;
        InventoryLog inventoryLog = (InventoryLog) other;
        return id != null && id.equals(inventoryLog.id);
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }

}
