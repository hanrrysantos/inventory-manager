package br.com.hanrry.inventory.shared.pagination;

import br.com.hanrry.inventory.shared.exception.pagination.InvalidPaginationException;

public final class PaginationBoundsValidator {

    private PaginationBoundsValidator() {
    }

    public static void validate(Integer page, Integer size, int maxSize) {
        if (page != null && page < 0) {
            throw new InvalidPaginationException("page must be >= 0");
        }
        if (size != null && (size < 1 || size > maxSize)) {
            throw new InvalidPaginationException("size must be between 1 and " + maxSize);
        }
    }
}
