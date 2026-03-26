package br.com.hanrry.inventory.mapper;

import br.com.hanrry.inventory.dto.product.ProductRequestDTO;
import br.com.hanrry.inventory.dto.product.ProductResponseDTO;
import br.com.hanrry.inventory.entity.Batch;
import br.com.hanrry.inventory.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "batches", ignore = true)
    @Mapping(target = "category.id", ignore = true)
    Product toEntity(ProductRequestDTO request);

    @Mapping(source = "category.name", target = "categoryName")
    @Mapping(target = "totalQuantity", expression = "java(calculateTotalQuantity(product))")
    ProductResponseDTO toDTO(Product product);

    List<ProductResponseDTO> toDTOList(List<Product> productList);

    default Long calculateTotalQuantity(Product product) {
        if (product.getBatches() == null) {
            return 0L;
        }
        long total = 0L;
        for (Batch batch : product.getBatches()) {
            if (batch.getQuantity() != null) {
                total += batch.getQuantity();
            }
        }
        return total;
    }
}
