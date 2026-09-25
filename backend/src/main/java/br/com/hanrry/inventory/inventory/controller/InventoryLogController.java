package br.com.hanrry.inventory.inventory.controller;

import br.com.hanrry.inventory.inventory.controller.docs.InventoryLogControllerDocs;
import br.com.hanrry.inventory.inventory.dto.invetoryLog.InventoryLogResponseDTO;
import br.com.hanrry.inventory.inventory.movement.LogType;
import br.com.hanrry.inventory.inventory.service.InventoryLogService;
import br.com.hanrry.inventory.shared.config.PaginationConfig;
import br.com.hanrry.inventory.shared.dto.PageResponse;
import br.com.hanrry.inventory.shared.pagination.PaginationBoundsValidator;
import br.com.hanrry.inventory.shared.pagination.PaginationSortValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/inventory-logs")
@RequiredArgsConstructor
public class InventoryLogController implements InventoryLogControllerDocs {

    private static final Set<String> LOG_LIST_SORT_PROPERTIES = Set.of("timestamp", "id");

    private final InventoryLogService inventoryLogService;

    @GetMapping
    public ResponseEntity<PageResponse<InventoryLogResponseDTO>> findLogs(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) LogType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @PageableDefault(size = PaginationConfig.DEFAULT_PAGE_SIZE, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PaginationBoundsValidator.validate(page, size, PaginationConfig.MAX_PAGE_SIZE);
        PaginationSortValidator.validateAllowedProperties(pageable, LOG_LIST_SORT_PROPERTIES);
        return ResponseEntity.ok(inventoryLogService.findLogs(productId, batchId, type, from, to, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InventoryLogResponseDTO> findLogById(@PathVariable Long id) {
        return ResponseEntity.ok(inventoryLogService.findLogById(id));
    }
}
