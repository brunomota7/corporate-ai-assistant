package br.com.api_core.modules.order;

import br.com.api_core.domain.Order;
import br.com.api_core.domain.OrderItem;
import br.com.api_core.domain.Product;
import br.com.api_core.domain.User;
import br.com.api_core.domain.enums.OrderStatus;
import br.com.api_core.domain.enums.Role;
import br.com.api_core.domain.repository.OrderRepository;
import br.com.api_core.domain.repository.ProductRepository;
import br.com.api_core.domain.repository.UserRepository;
import br.com.api_core.infra.exception.OrderCancellationNotAllowedException;
import br.com.api_core.infra.exception.OrderNotFoundException;
import br.com.api_core.infra.exception.ProductNotFoundException;
import br.com.api_core.modules.order.dto.OrderCreateDTO;
import br.com.api_core.modules.order.dto.OrderItemCreateDTO;
import br.com.api_core.modules.order.dto.OrderResponseDTO;
import br.com.api_core.modules.order.dto.OrderStatusUpdateDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static br.com.api_core.support.TestUtils.setField;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrderService orderService;

    private User user;
    private Product product;
    private UUID userId;
    private UUID productId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        productId = UUID.randomUUID();

        user = new User();
        user.setName("Jhon");
        user.setEmail("jhon@teste.com");
        user.setRole(Role.ROLE_USER);
        setField(user, "id", userId);

        product = new Product();
        product.setName("Produto Teste");
        product.setPrice(new BigDecimal("50.00"));
        product.setStockQuantity(10);
        product.setActive(true);
        setField(product, "id", productId);
    }

    @Test
    void create_shouldPersistOrderWithCorrectTotal() {
        OrderItemCreateDTO itemDTO = new OrderItemCreateDTO(productId, 2);
        OrderCreateDTO dto = new OrderCreateDTO("Entrega rápida", List.of(itemDTO));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(productRepository.findByIdAndActiveTrue(productId)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponseDTO response = orderService.create(dto, userId);

        // total = 50.00 * 2 = 100.00
        assertEquals(new BigDecimal("100.00"), response.totalAmount());
        assertEquals(OrderStatus.PENDING, response.status());
        assertEquals("Entrega rápida", response.notes());
    }

    @Test
    void create_shouldSnapshotProductPrice() {
        OrderItemCreateDTO itemDTO = new OrderItemCreateDTO(productId, 1);
        OrderCreateDTO dto = new OrderCreateDTO(null, List.of(itemDTO));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(productRepository.findByIdAndActiveTrue(productId)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order order = inv.getArgument(0);
            assertEquals(new BigDecimal("50.00"), order.getItems().get(0).getUnitPrice());
            return order;
        });

        orderService.create(dto, userId);

        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void create_shouldThrow_whenProductNotFound() {
        OrderItemCreateDTO itemDTO = new OrderItemCreateDTO(productId, 1);
        OrderCreateDTO dto = new OrderCreateDTO(null, List.of(itemDTO));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(productRepository.findByIdAndActiveTrue(productId)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> orderService.create(dto, userId));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void findById_shouldReturnOrder_whenOwnershipValid() {
        Order order = buildOrder(userId);

        when(orderRepository.findByIdAndUserId(order.getId(), userId))
                .thenReturn(Optional.of(order));

        OrderResponseDTO response = orderService.findById(order.getId(), userId);

        assertEquals(order.getId(), response.id());
    }

    @Test
    void findById_shouldThrow_whenOrderNotFound() {
        UUID orderId = UUID.randomUUID();

        when(orderRepository.findByIdAndUserId(orderId, userId))
                .thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class,
                () -> orderService.findById(orderId, userId));
    }

    @Test
    void cancelOrder_shouldSetStatusCancelled_whenPending() {
        Order order = buildOrder(userId);
        order.setStatus(OrderStatus.PENDING);

        when(orderRepository.findByIdAndUserId(order.getId(), userId))
                .thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.cancelOrder(order.getId(), userId);

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void cancelOrder_shouldThrow_whenStatusIsNotPending() {
        Order order = buildOrder(userId);
        order.setStatus(OrderStatus.SHIPPED);

        when(orderRepository.findByIdAndUserId(order.getId(), userId))
                .thenReturn(Optional.of(order));

        assertThrows(OrderCancellationNotAllowedException.class,
                () -> orderService.cancelOrder(order.getId(), userId));

        verify(orderRepository, never()).save(any());
    }

    @Test
    void updateStatus_shouldChangeStatus_whenOrderExists() {
        Order order = buildOrder(userId);
        order.setStatus(OrderStatus.PENDING);
        OrderStatusUpdateDTO dto = new OrderStatusUpdateDTO(OrderStatus.PROCESSING);

        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponseDTO response = orderService.updateStatus(order.getId(), dto);

        assertEquals(OrderStatus.PROCESSING, response.status());
    }

    private Order buildOrder(UUID ownerId) {
        User owner = new User();
        setField(owner, "id", ownerId);

        Order order = new Order();
        setField(order, "id", UUID.randomUUID());
        order.setUser(owner);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(BigDecimal.TEN);
        order.setItems(new ArrayList<>());
        return order;
    }
}