package br.com.api_core.modules.product;

import br.com.api_core.modules.product.dto.ProductCreateDTO;
import br.com.api_core.modules.product.dto.ProductResponseDTO;
import br.com.api_core.modules.product.dto.ProductUpdateDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ProductResponseDTO> create(
            @Valid @RequestBody ProductCreateDTO dto) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(productService.create(dto));
    }

    @GetMapping
    public ResponseEntity<List<ProductResponseDTO>> findAllProducts() {

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(productService.findAllProducts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> findProductById(
            @PathVariable UUID id) {

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(productService.findProductById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ProductResponseDTO> update(
            @PathVariable UUID id,
            @Valid @RequestBody ProductUpdateDTO dto) {

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(productService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id) {

        productService.delete(id);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }
}
