package br.com.hanrry.inventory.controller;

import br.com.hanrry.inventory.dto.batch.BatchRequestDTO;
import br.com.hanrry.inventory.dto.batch.BatchResponseDTO;
import br.com.hanrry.inventory.dto.batch.ConsumeBatchRequestDTO;
import br.com.hanrry.inventory.service.BatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/batches")
@RequiredArgsConstructor
public class BatchController {

    private final BatchService batchService;

    @PostMapping
    public ResponseEntity<BatchResponseDTO> createBatch(
            @RequestBody BatchRequestDTO request
    ){
        BatchResponseDTO batch = batchService.createBatch(request);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(batch.id()).toUri();
        return ResponseEntity.created(uri).body(batch);
    }

    @PatchMapping("/{id}/add")
    public ResponseEntity<BatchResponseDTO> addStock(
            @PathVariable Long id,
            @RequestBody BatchRequestDTO request
    ){
        BatchResponseDTO batch = batchService.addStock(id, request);
        return ResponseEntity.ok().body(batch);
    }

    @PostMapping("/consume")
    public ResponseEntity<Void> consume(
            @RequestBody ConsumeBatchRequestDTO request
    ){
        batchService.consumeStock(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/expired")
    public ResponseEntity<List<BatchResponseDTO>> listExpired(
    ){
        List<BatchResponseDTO> batchesList = batchService.findExpiredBatches();
        return ResponseEntity.ok().body(batchesList);
    }
}