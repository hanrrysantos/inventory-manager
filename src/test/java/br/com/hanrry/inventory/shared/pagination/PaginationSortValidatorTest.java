package br.com.hanrry.inventory.shared.pagination;

import br.com.hanrry.inventory.shared.exception.pagination.InvalidPaginationException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaginationSortValidatorTest {

    private static final Set<String> ALLOWED = Set.of("id", "name", "sku");

    @Test
    void shouldAcceptAllowedSortProperties() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by("name").ascending());

        assertDoesNotThrow(() -> PaginationSortValidator.validateAllowedProperties(pageable, ALLOWED));
    }

    @Test
    void shouldRejectDisallowedSortProperty() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by("category").ascending());

        assertThrows(
                InvalidPaginationException.class,
                () -> PaginationSortValidator.validateAllowedProperties(pageable, ALLOWED)
        );
    }

    @Test
    void shouldRejectNestedSortProperty() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by("category.name").ascending());

        assertThrows(
                InvalidPaginationException.class,
                () -> PaginationSortValidator.validateAllowedProperties(pageable, ALLOWED)
        );
    }
}
