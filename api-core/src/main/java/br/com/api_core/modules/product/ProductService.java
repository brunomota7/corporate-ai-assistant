package br.com.api_core.modules.product;

import br.com.api_core.domain.Product;
import br.com.api_core.domain.repository.ProductRepository;
import br.com.api_core.infra.exception.ProductAlreadyExistsException;
import br.com.api_core.infra.exception.ResourceNotFoundException;
import br.com.api_core.modules.product.dto.ProductCreateDTO;
import br.com.api_core.modules.product.dto.ProductResponseDTO;
import br.com.api_core.modules.product.dto.ProductUpdateDTO;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    private String generateSku(String productName) {

        String normalizedName = Normalizer
                .normalize(productName, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("[^A-Za-z0-9]", "")
                .toUpperCase();

        String prefix = UUID.randomUUID()
                .toString()
                .substring(0, 4)
                .toUpperCase();

        String sku = prefix + "-" + normalizedName;

        return sku;
    }

    @Transactional
    public ProductResponseDTO create(ProductCreateDTO dto) {

        String sku = generateSku(dto.name());

        if (productRepository.existsBySku(sku)) {
            throw new ProductAlreadyExistsException(sku);
        }

        Product product = new Product();
        product.setSku(sku);
        product.setName(dto.name());
        product.setDescription(dto.description());
        product.setPrice(dto.price());
        product.setStockQuantity(dto.stockQuantity());
        product.setCategory(dto.category());
        product.setActive(true);

        productRepository.save(product);

        return new ProductResponseDTO(
            product.getId(),
            product.getSku(),
            product.getName(),
            product.getDescription(),
            product.getPrice(),
            product.getStockQuantity(),
            product.getCategory(),
            product.getActive()
        );
    }

    public List<ProductResponseDTO> findAllProducts() {

        return productRepository.findAll()
                .stream()
                .map(product -> new ProductResponseDTO(
                        product.getId(),
                        product.getSku(),
                        product.getName(),
                        product.getDescription(),
                        product.getPrice(),
                        product.getStockQuantity(),
                        product.getCategory(),
                        product.getActive()
                ))
                .toList();
    }

    public ProductResponseDTO findProductById(UUID id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!Boolean.TRUE.equals(product.getActive())) {
            throw new ResourceNotFoundException("Product not found");
        }

        return new ProductResponseDTO(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getCategory(),
                product.getActive()
        );
    }

    public ProductResponseDTO update(UUID id, ProductUpdateDTO dto) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        product.setName(dto.name());
        product.setDescription(dto.description());
        product.setPrice(dto.price());
        product.setStockQuantity(dto.stockQuantity());
        product.setCategory(dto.category());
        product.setUpdatedAt(LocalDateTime.now());

        productRepository.save(product);

        return new ProductResponseDTO(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getCategory(),
                product.getActive()
        );
    }

    public void delete(UUID id) {

        Product product =  productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        product.setActive(false);
        productRepository.save(product);
    }
}
