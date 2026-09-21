package br.com.hanrry.inventory.shared.pagination;

import br.com.hanrry.inventory.shared.exception.pagination.InvalidPaginationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaginationBoundsValidatorTest {

    @Test
    void shouldAcceptValidPageAndSize() {
        assertDoesNotThrow(() -> PaginationBoundsValidator.validate(0, 20, 100));
        assertDoesNotThrow(() -> PaginationBoundsValidator.validate(null, null, 100));
    }

    @Test
    void shouldRejectNegativePage() {
        assertThrows(
                InvalidPaginationException.class,
                () -> PaginationBoundsValidator.validate(-1, 20, 100)
        );
    }

    @Test
    void shouldRejectSizeBelowMinimum() {
        assertThrows(
                InvalidPaginationException.class,
                () -> PaginationBoundsValidator.validate(0, 0, 100)
        );
    }

    @Test
    void shouldRejectSizeAboveMaximum() {
        assertThrows(
                InvalidPaginationException.class,
                () -> PaginationBoundsValidator.validate(0, 101, 100)
        );
    }
}
