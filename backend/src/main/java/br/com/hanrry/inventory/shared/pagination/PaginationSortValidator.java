package br.com.hanrry.inventory.shared.pagination;

import br.com.hanrry.inventory.shared.exception.pagination.InvalidPaginationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;
import java.util.stream.Collectors;

public final class PaginationSortValidator {

    private PaginationSortValidator() {
    }

    public static void validateAllowedProperties(Pageable pageable, Set<String> allowedProperties) {
        if (pageable.getSort().isUnsorted()) {
            return;
        }
        for (Sort.Order order : pageable.getSort()) {
            String property = order.getProperty();
            if (property.contains(".") || !property.matches("[a-zA-Z][a-zA-Z0-9]*")) {
                throw new InvalidPaginationException(
                        "Invalid sort property '" + property + "'. Allowed: "
                                + allowedProperties.stream().sorted().collect(Collectors.joining(", "))
                );
            }
            if (!allowedProperties.contains(property)) {
                String allowed = allowedProperties.stream().sorted().collect(Collectors.joining(", "));
                throw new InvalidPaginationException(
                        "Invalid sort property '" + property + "'. Allowed: " + allowed
                );
            }
        }
    }
}
