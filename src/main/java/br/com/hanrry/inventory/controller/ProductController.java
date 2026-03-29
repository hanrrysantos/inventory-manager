package br.com.hanrry.inventory.controller;

import br.com.hanrry.inventory.controller.docs.ProductControllerDocs;
import br.com.hanrry.inventory.dto.product.ProductRequestDTO;
import br.com.hanrry.inventory.dto.product.ProductResponseDTO;
import br.com.hanrry.inventory.dto.product.UpdateProdcutRequestDTO;
import br.com.hanrry.inventory.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductController implements ProductControllerDocs {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponseDTO> createProduct(
            @RequestBody ProductRequestDTO request
    ) {
        ProductResponseDTO product = productService.createProduct(request);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(product.id()).toUri();
        return ResponseEntity.created(uri).body(product);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> findProductById(
            @PathVariable Long id
    ){
        ProductResponseDTO product = productService.findProductById(id);

        return ResponseEntity.ok().body(product);
    }

    @GetMapping()
    public ResponseEntity<List<ProductResponseDTO>> findAllProducts(){
        List<ProductResponseDTO> productList = productService.findAllProducts();

        return ResponseEntity.ok().body(productList);
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<ProductResponseDTO>> getLowStock(){
        List<ProductResponseDTO> productList = productService.getLowStockProducts();

        return ResponseEntity.ok().body(productList);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> updateProduct(
            @PathVariable Long id,
            @RequestBody UpdateProdcutRequestDTO request
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