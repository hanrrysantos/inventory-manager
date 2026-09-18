package br.com.hanrry.inventory.entityTest;

import br.com.hanrry.inventory.inventory.batch.Batch;
import br.com.hanrry.inventory.inventory.movement.InventoryLog;
import br.com.hanrry.inventory.inventory.movement.LogType;
import br.com.hanrry.inventory.product.entity.Category;
import br.com.hanrry.inventory.product.entity.Product;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class EntityEqualityTest {

    @Test
    void shouldCompareProductsByIdWithoutComparingRelationships() {
        Product first = product(1L, "First category");
        Product second = product(1L, "Second category");

        assertEquals(first, second);
    }

    @Test
    void shouldNotCompareTransientProductsAsEqual() {
        assertNotEquals(new Product(), new Product());
    }

    @Test
    void shouldCompareCategoriesByIdWithoutComparingProducts() {
        Category first = category(1L, "First category");
        Category second = category(1L, "Second category");

        assertEquals(first, second);
    }

    @Test
    void shouldCompareBatchesByIdWithoutComparingRelationships() {
        Batch first = batch(1L, "First product");
        Batch second = batch(1L, "Second product");

        assertEquals(first, second);
    }

    @Test
    void shouldCompareInventoryLogsByIdWithoutComparingRelationships() {
        InventoryLog first = log(1L, "First product");
        InventoryLog second = log(1L, "Second product");

        assertEquals(first, second);
    }

    private Product product(Long id, String categoryName) {
        Product product = new Product();
        product.setId(id);
        product.setName("Product");
        product.setSku("SKU-001");
        product.setMinStock(10L);
        product.setCategory(category(id, categoryName));
        return product;
    }

    private Category category(Long id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setDescription("Description");
        return category;
    }

    private Batch batch(Long id, String productName) {
        Batch batch = new Batch();
        batch.setId(id);
        batch.setBatchNumber("BATCH-001");
        batch.setQuantity(10L);
        batch.setManufacturingDate(LocalDate.of(2026, 1, 1));
        batch.setExpiryDate(LocalDate.of(2027, 1, 1));
        batch.setPrice(BigDecimal.TEN);
        Product product = product(id, "Category");
        product.setName(productName);
        batch.setProduct(product);
        return batch;
    }

    private InventoryLog log(Long id, String productName) {
        InventoryLog log = new InventoryLog();
        log.setId(id);
        log.setType(LogType.INPUT);
        log.setQuantity(10L);
        log.setTimestamp(LocalDateTime.of(2026, 1, 1, 10, 0));
        Product product = product(id, "Category");
        product.setName(productName);
        log.setProduct(product);
        return log;
    }
}
