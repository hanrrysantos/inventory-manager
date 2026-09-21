package br.com.hanrry.inventory.inventory.controller;

import br.com.hanrry.inventory.inventory.controller.docs.BatchControllerDocs;
import br.com.hanrry.inventory.inventory.dto.batch.AddStockBatchRequestDTO;
import br.com.hanrry.inventory.inventory.dto.batch.BatchRequestDTO;
import br.com.hanrry.inventory.inventory.dto.batch.BatchResponseDTO;
import br.com.hanrry.inventory.inventory.dto.batch.ConsumeBatchRequestDTO;
import br.com.hanrry.inventory.inventory.service.BatchService;
import br.com.hanrry.inventory.shared.config.PaginationConfig;
import br.com.hanrry.inventory.shared.dto.PageResponse;
import br.com.hanrry.inventory.shared.pagination.PaginationBoundsValidator;
import br.com.hanrry.inventory.shared.pagination.PaginationSortValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import jakarta.validation.Valid;

import java.net.URI;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/batches")
@RequiredArgsConstructor
public class BatchController implements BatchControllerDocs {

    private static final Set<String> EXPIRED_BATCH_SORT_PROPERTIES = Set.of("id", "expiryDate", "batchNumber");

    private final BatchService batchService;

    @GetMapping("/expired")
    public ResponseEntity<PageResponse<BatchResponseDTO>> listExpired(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @PageableDefault(size = PaginationConfig.DEFAULT_PAGE_SIZE, sort = "expiryDate") Pageable pageable
    ) {
        PaginationBoundsValidator.validate(page, size, PaginationConfig.MAX_PAGE_SIZE);
        PaginationSortValidator.validateAllowedProperties(pageable, EXPIRED_BATCH_SORT_PROPERTIES);
        return ResponseEntity.ok(batchService.findExpiredBatches(pageable));
    }

    @PostMapping
    public ResponseEntity<BatchResponseDTO> createBatch(
            @Valid @RequestBody BatchRequestDTO request
    ){
        BatchResponseDTO batch = batchService.createBatch(request);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(batch.id()).toUri();
        return ResponseEntity.created(uri).body(batch);
    }

    @PatchMapping("/{id}/add")
    public ResponseEntity<BatchResponseDTO> addStock(
            @PathVariable Long id,
            @Valid @RequestBody AddStockBatchRequestDTO request
    ){
        BatchResponseDTO batch = batchService.addStock(id, request);
        return ResponseEntity.ok().body(batch);
    }

    @PostMapping("/consume")
    public ResponseEntity<Void> consume(
            @Valid @RequestBody ConsumeBatchRequestDTO request
    ){
        batchService.consumeStock(request);
        return ResponseEntity.noContent().build();
    }
}
