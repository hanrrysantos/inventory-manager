package br.com.hanrry.inventory.product.entity;

import jakarta.persistence.*;
import br.com.hanrry.inventory.user.entity.User;
import lombok.*;
import org.hibernate.Hibernate;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tb_categories", uniqueConstraints = @UniqueConstraint(columnNames = {"owner_id", "name"}))
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    private String description;

    @OneToMany(mappedBy = "category")
    private List<Product> products = new ArrayList<>();

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false;
        Category category = (Category) other;
        return id != null && id.equals(category.id);
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
