package br.com.hanrry.inventory.service;

import br.com.hanrry.inventory.dto.product.ProductRequestDTO;
import br.com.hanrry.inventory.dto.product.ProductResponseDTO;
import br.com.hanrry.inventory.entity.Product;
import br.com.hanrry.inventory.mapper.ProductMapper;
import br.com.hanrry.inventory.repository.ProductRepository;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    private final ProductRepository repository;
    private final ProductMapper mapper;
    public ProductService(ProductRepository repository, ProductMapper mapper){
        this.repository = repository;
        this.mapper = mapper;
    }

    public ProductResponseDTO createProduct(ProductRequestDTO request){

        repository.findBySku(request.sku()).ifPresent(
               p -> {
                   throw new RuntimeException();
               });
        
       Product product = mapper.toEntity(request);

       Product savedProduct = repository.save(product);

       return mapper.toDTO(savedProduct);
    }






}
