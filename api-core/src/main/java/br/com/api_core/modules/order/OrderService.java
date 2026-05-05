package br.com.api_core.modules.order;

import br.com.api_core.domain.Order;
import br.com.api_core.domain.OrderItem;
import br.com.api_core.domain.Product;
import br.com.api_core.domain.User;
import br.com.api_core.domain.enums.OrderStatus;
import br.com.api_core.domain.repository.OrderItemRepository;
import br.com.api_core.domain.repository.OrderRepository;
import br.com.api_core.domain.repository.ProductRepository;
import br.com.api_core.domain.repository.UserRepository;
import br.com.api_core.infra.exception.OrderCancellationNotAllowedException;
import br.com.api_core.infra.exception.OrderNotFoundException;
import br.com.api_core.infra.exception.ProductNotFoundException;
import br.com.api_core.infra.exception.ResourceNotFoundException;
import br.com.api_core.infra.messaging.EventPublisher;
import br.com.api_core.infra.messaging.dto.OrderCancelledEventDTO;
import br.com.api_core.infra.messaging.dto.OrderConfirmedEventDTO;
import br.com.api_core.infra.messaging.dto.OrderStatusChangedEventDTO;
import br.com.api_core.modules.order.dto.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final EventPublisher eventPublisher;

    public OrderService(OrderRepository orderRepository,
                        ProductRepository productRepository,
                        UserRepository userRepository,
                        EventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    private OrderResponseDTO toResponseDTO(Order order) {

        return new OrderResponseDTO(
                order.getId(),
                order.getUser().getId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getNotes(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getItems()
                        .stream()
                        .map(this::toOrderItemResponseDTO)
                        .toList()
        );
    }

    private OrderItemResponseDTO toOrderItemResponseDTO(OrderItem item) {

        return new OrderItemResponseDTO(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getQuantity(),
                item.getUnitPrice()
        );
    }

    @Transactional
    public OrderResponseDTO create(OrderCreateDTO dto, UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));

        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);
        order.setNotes(dto.notes());

        BigDecimal total = BigDecimal.ZERO;

        for (OrderItemCreateDTO itemDTO : dto.items()) {

            Product product = productRepository.findByIdAndActiveTrue(itemDTO.productId())
                    .orElseThrow(() -> new ProductNotFoundException(itemDTO.productId().toString()));

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setQuantity(itemDTO.quantity());
            item.setUnitPrice(product.getPrice());

            BigDecimal itemTotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(itemDTO.quantity()));
            total = total.add(itemTotal);

            order.getItems().add(item);
        }

        order.setTotalAmount(total);

        orderRepository.save(order);

        List<OrderConfirmedEventDTO.OrderItemDTO> itemDTOs = order.getItems().stream()
                .map(item -> new OrderConfirmedEventDTO.OrderItemDTO(
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getUnitPrice()
                ))
                .toList();

        eventPublisher.publishOrderConfirmed(new OrderConfirmedEventDTO(
                order.getId().toString(),
                user.getId().toString(),
                user.getName(),
                user.getEmail(),
                order.getTotalAmount(),
                order.getNotes(),
                itemDTOs
        ));

        return toResponseDTO(order);
    }

    public List<OrderResponseDTO> findAllByUser(UUID userId) {

        return orderRepository.findByUserId(userId)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public List<OrderResponseDTO> findAll() {

        return orderRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public OrderResponseDTO findById(UUID id, UUID userId) {

        Order order = orderRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new OrderNotFoundException(id.toString()));

        return toResponseDTO(order);
    }

    @Transactional
    public OrderResponseDTO updateStatus(UUID id, OrderStatusUpdateDTO dto) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id.toString()));

        String oldStatus = order.getStatus().name();
        order.setStatus(dto.status());
        orderRepository.save(order);

        eventPublisher.publishOrderStatusChanged(new OrderStatusChangedEventDTO(
                order.getId().toString(),
                order.getUser().getId().toString(),
                order.getUser().getName(),
                order.getUser().getEmail(),
                oldStatus,
                dto.status().name()
        ));

        return toResponseDTO(order);
    }

    @Transactional
    public void cancelOrder(UUID id, UUID userId) {

        Order order = orderRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new OrderNotFoundException(id.toString()));

        if (!order.getStatus().equals(OrderStatus.PENDING)) {
            throw new OrderCancellationNotAllowedException(order.getStatus().toString());
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        eventPublisher.publishOrderCancelled(new OrderCancelledEventDTO(
                order.getId().toString(),
                order.getUser().getId().toString(),
                order.getUser().getName(),
                order.getUser().getEmail(),
                order.getTotalAmount()
        ));
    }
}
