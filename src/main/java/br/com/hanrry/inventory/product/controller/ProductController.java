package br.com.hanrry.inventory.product.controller;

import br.com.hanrry.inventory.product.controller.docs.ProductControllerDocs;
import br.com.hanrry.inventory.product.dto.product.ProductRequestDTO;
import br.com.hanrry.inventory.product.dto.product.ProductResponseDTO;
import br.com.hanrry.inventory.product.dto.product.UpdateProdcutRequestDTO;
import br.com.hanrry.inventory.inventory.dto.batch.BatchResponseDTO;
import br.com.hanrry.inventory.inventory.service.BatchService;
import br.com.hanrry.inventory.product.service.ProductService;
import br.com.hanrry.inventory.shared.config.PaginationConfig;
import br.com.hanrry.inventory.shared.dto.PageResponse;
import br.com.hanrry.inventory.shared.pagination.PaginationBoundsValidator;
import br.com.hanrry.inventory.shared.pagination.PaginationSortValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductController implements ProductControllerDocs {

    private static final Set<String> PRODUCT_LIST_SORT_PROPERTIES = Set.of("id", "name", "sku");
    private static final Set<String> PRODUCT_BATCH_SORT_PROPERTIES = Set.of("id", "expiryDate", "batchNumber");

    private final ProductService productService;
    private final BatchService batchService;

    @GetMapping()
    public ResponseEntity<PageResponse<ProductResponseDTO>> findAllProducts(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @PageableDefault(size = PaginationConfig.DEFAULT_PAGE_SIZE, sort = "name") Pageable pageable
    ) {
        PaginationBoundsValidator.validate(page, size, PaginationConfig.MAX_PAGE_SIZE);
        PaginationSortValidator.validateAllowedProperties(pageable, PRODUCT_LIST_SORT_PROPERTIES);
        PageResponse<ProductResponseDTO> productPage = productService.findAllProducts(pageable);

        return ResponseEntity.ok().body(productPage);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> findProductById(
            @PathVariable Long id
    ){
        ProductResponseDTO product = productService.findProductById(id);

        return ResponseEntity.ok().body(product);
    }

    @GetMapping("/{productId}/batches")
    public ResponseEntity<PageResponse<BatchResponseDTO>> findBatchesByProduct(
            @PathVariable Long productId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @PageableDefault(size = PaginationConfig.DEFAULT_PAGE_SIZE, sort = "expiryDate") Pageable pageable
    ) {
        PaginationBoundsValidator.validate(page, size, PaginationConfig.MAX_PAGE_SIZE);
        PaginationSortValidator.validateAllowedProperties(pageable, PRODUCT_BATCH_SORT_PROPERTIES);
        return ResponseEntity.ok(batchService.findBatchesByProductId(productId, pageable));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<PageResponse<ProductResponseDTO>> getLowStock(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @PageableDefault(size = PaginationConfig.DEFAULT_PAGE_SIZE, sort = "name") Pageable pageable
    ) {
        PaginationBoundsValidator.validate(page, size, PaginationConfig.MAX_PAGE_SIZE);
        PaginationSortValidator.validateAllowedProperties(pageable, PRODUCT_LIST_SORT_PROPERTIES);
        return ResponseEntity.ok(productService.findLowStockProducts(pageable));
    }

    @PostMapping
    public ResponseEntity<ProductResponseDTO> createProduct(
            @Valid @RequestBody ProductRequestDTO request
    ) {
        ProductResponseDTO product = productService.createProduct(request);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(product.id()).toUri();
        return ResponseEntity.created(uri).body(product);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProdcutRequestDTO request
    ){
        ProductResponseDTO product = productService.updateProduct(id, request);
        return ResponseEntity.ok().body(product);
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long id
    ){
        productService.deleteProductById(id);
        return ResponseEntity.noContent().build();
    }
}
