package br.com.api_core.modules.product;

import br.com.api_core.domain.Product;
import br.com.api_core.domain.repository.ProductRepository;
import br.com.api_core.infra.exception.ResourceNotFoundException;
import br.com.api_core.modules.product.dto.ProductCreateDTO;
import br.com.api_core.modules.product.dto.ProductResponseDTO;
import br.com.api_core.modules.product.dto.ProductUpdateDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static br.com.api_core.support.TestUtils.getField;
import static br.com.api_core.support.TestUtils.setField;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product product;

    @BeforeEach
    void setUp() {
        product = new Product();
        setField(product, "id", UUID.randomUUID());
        product.setName("Notebook");
        product.setDescription("Notebook gamer");
        product.setPrice(new BigDecimal("3500.00"));
        product.setStockQuantity(5);
        product.setCategory("Eletrônicos");
        product.setActive(true);
    }

    @Test
    void create_shouldPersistProduct_withGeneratedSku() {
        ProductCreateDTO dto = new ProductCreateDTO(
                "Notebook", "Notebook gamer",
                new BigDecimal("3500.00"), 5, "Eletrônicos"
        );

        when(productRepository.existsBySku(anyString())).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            setField(p, "id", UUID.randomUUID());
            return p;
        });

        ProductResponseDTO response = productService.create(dto);

        assertNotNull(response.sku());
        assertTrue(response.sku().contains("NOTEBOOK"));
        assertEquals("Notebook", response.name());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void create_shouldRetrySkuGeneration_whenCollisionOccurs() {
        ProductCreateDTO dto = new ProductCreateDTO(
                "Notebook", null,
                new BigDecimal("3500.00"), 5, null
        );

        // Primeira chamada colide, segunda é única
        when(productRepository.existsBySku(anyString()))
                .thenReturn(true)
                .thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        productService.create(dto);

        // existsBySku chamado 2 vezes — colisão + sucesso
        verify(productRepository, times(2)).existsBySku(anyString());
    }

    @Test
    void findAllProducts_shouldReturnOnlyActiveProducts() {
        when(productRepository.findAllByActiveTrue()).thenReturn(List.of(product));

        List<ProductResponseDTO> result = productService.findAllProducts();

        assertEquals(1, result.size());
        assertEquals("Notebook", result.get(0).name());
        verify(productRepository).findAllByActiveTrue();
    }

    @Test
    void findProductById_shouldReturnProduct_whenActiveAndExists() {
        UUID id = (UUID) getField(product, "id");

        when(productRepository.findById(id)).thenReturn(Optional.of(product));

        ProductResponseDTO response = productService.findProductById(id);

        assertEquals("Notebook", response.name());
    }

    @Test
    void findProductById_shouldThrow_whenNotFound() {
        UUID id = UUID.randomUUID();

        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> productService.findProductById(id));
    }

    @Test
    void update_shouldOnlyUpdateNonNullFields() {
        UUID id = (UUID) getField(product, "id");

        // Atualiza só o preço — nome e descrição ficam como estão
        ProductUpdateDTO dto = new ProductUpdateDTO(null, null,
                new BigDecimal("4000.00"), null, null);

        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponseDTO response = productService.update(id, dto);

        assertEquals("Notebook", response.name());
        assertEquals("Notebook gamer", response.description());
        assertEquals(new BigDecimal("4000.00"), response.price());
    }

    @Test
    void update_shouldThrow_whenProductNotFound() {
        UUID id = UUID.randomUUID();
        ProductUpdateDTO dto = new ProductUpdateDTO("Novo nome", null, null, null, null);

        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> productService.update(id, dto));
    }

    @Test
    void delete_shouldSetActiveFalse() {
        UUID id = (UUID) getField(product, "id");

        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        productService.delete(id);

        assertFalse(product.getActive());
        verify(productRepository).save(product);
    }

    @Test
    void delete_shouldThrow_whenProductNotFound() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> productService.delete(id));
    }
}