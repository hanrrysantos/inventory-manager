package br.com.hanrry.inventory.controller;

import br.com.hanrry.inventory.dto.batch.BatchRequestDTO;
import br.com.hanrry.inventory.dto.batch.BatchResponseDTO;
import br.com.hanrry.inventory.service.BatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/batches")
@RequiredArgsConstructor
public class BatchController {

    private final BatchService batchService;

    @PostMapping
    public ResponseEntity<BatchResponseDTO> createBatch(@RequestBody BatchRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(batchService.createBatch(request));
    }

    @PatchMapping("/{id}/add")
    public ResponseEntity<BatchResponseDTO> addStock(@PathVariable Long id, @RequestParam Long quantity) {
        return ResponseEntity.ok(batchService.addStockToBatch(id, quantity));
    }

    @PostMapping("/consume")
    public ResponseEntity<Void> consume(@RequestParam Long productId, @RequestParam Long quantity) {
        batchService.consumeStock(productId, quantity);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/expired")
    public ResponseEntity<List<BatchResponseDTO>> listExpired() {
        return ResponseEntity.ok(batchService.findExpiredBatches());
    }
}