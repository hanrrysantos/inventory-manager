package br.com.hanrry.inventory.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class PostgresIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ApplicationContext applicationContext;

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void shouldStartApplicationAndApplyFlywayMigrationsInCleanDatabase() {
        List<Map<String, Object>> migrations = jdbcTemplate.queryForList(
                "SELECT version, description, success FROM flyway_schema_history "
                        + "WHERE version IN ('1', '2', '3', '4') ORDER BY installed_rank"
        );

        assertEquals(4, migrations.size());
        assertEquals("1", migrations.get(0).get("version"));
        assertEquals("Create Tables", migrations.get(0).get("description"));
        assertEquals(true, migrations.get(0).get("success"));
        assertEquals("2", migrations.get(1).get("version"));
        assertEquals("Insert Default User", migrations.get(1).get("description"));
        assertEquals(true, migrations.get(1).get("success"));
        assertEquals("3", migrations.get(2).get("version"));
        assertEquals("Populate Tables", migrations.get(2).get("description"));
        assertEquals(true, migrations.get(2).get("success"));
        assertEquals("4", migrations.get(3).get("version"));
        assertEquals("Enforce Case Insensitive Ownership Uniqueness", migrations.get(3).get("description"));
        assertEquals(true, migrations.get(3).get("success"));
    }

    @Test
    void shouldPersistSeedDataFromSecondMigration() {
        assertEquals(5, countRows("tb_categories"));
        assertEquals(13, countRows("tb_products"));
        assertEquals(14, countRows("tb_batches"));
        assertEquals(2, countRows("tb_inventory_logs"));
        assertEquals("INPUT", jdbcTemplate.queryForObject(
                "SELECT type FROM tb_inventory_logs ORDER BY id LIMIT 1", String.class));
    }

    @Test
    void shouldEnforceCurrentDatabaseConstraints() {
        assertThrows(DataAccessException.class, () -> jdbcTemplate.update(
                "INSERT INTO tb_products (name, sku, min_stock, category_id) VALUES (?, ?, ?, ?)",
                "Duplicate SKU", "ARR-001", 0L, 1L
        ));

        assertThrows(DataAccessException.class, () -> jdbcTemplate.update(
                "INSERT INTO tb_batches "
                        + "(batch_number, quantity, manufacturing_date, expiry_date, price, product_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                "NEGATIVE-001", -1L, "2026-01-01", "2027-01-01", 1.00, 1L
        ));

        assertThrows(DataAccessException.class, () -> jdbcTemplate.update(
                "INSERT INTO tb_batches "
                        + "(batch_number, quantity, manufacturing_date, expiry_date, price, product_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                "NULL-DATES-001", 1L, null, null, 1.00, 1L
        ));

        assertThrows(DataAccessException.class, () -> jdbcTemplate.update(
                "INSERT INTO tb_inventory_logs (type, quantity, product_id) VALUES (?, ?, ?)",
                "INPUT", -1L, 1L
        ));

        assertThrows(DataAccessException.class, () -> jdbcTemplate.update(
                "INSERT INTO tb_batches "
                        + "(batch_number, quantity, manufacturing_date, expiry_date, price, product_id) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                "INVALID-PRODUCT-001", 1L, "2026-01-01", "2027-01-01", 1.00, 999999L
        ));

        assertThrows(DataAccessException.class, () -> jdbcTemplate.update(
                "INSERT INTO tb_products (name, sku, min_stock, category_id) VALUES (?, ?, ?, ?)",
                "Invalid category", "INVALID-CATEGORY-001", 0L, 999999L
        ));
    }

    @Test
    void shouldNotRegisterScheduledTasksInTestProfile() {
        assertEquals(0, applicationContext.getBeansOfType(
                org.springframework.scheduling.config.ScheduledTaskHolder.class).size());
    }

    private int countRows(String tableName) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
    }
}
