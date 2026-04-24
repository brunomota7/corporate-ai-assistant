package br.com.api_core.modules.order;

import br.com.api_core.infra.security.SecurityUtils;
import br.com.api_core.modules.order.dto.OrderCreateDTO;
import br.com.api_core.modules.order.dto.OrderResponseDTO;
import br.com.api_core.modules.order.dto.OrderStatusUpdateDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final SecurityUtils securityUtils;

    public OrderController(OrderService orderService,
                           SecurityUtils securityUtils) {
        this.orderService = orderService;
        this.securityUtils = securityUtils;
    }

    @PostMapping
    public ResponseEntity<OrderResponseDTO> createOrder(
            @Valid @RequestBody OrderCreateDTO dto,
            Authentication authentication) {

        UUID userId = securityUtils.getAuthenticatedUserId(authentication);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(orderService.create(dto, userId));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponseDTO>> findAllByUser(
            Authentication authentication) {

        UUID userId = securityUtils.getAuthenticatedUserId(authentication);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(orderService.findAllByUser(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDTO> findById(
            @PathVariable UUID id,
            Authentication authentication) {

        UUID userId = securityUtils.getAuthenticatedUserId(authentication);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(orderService.findById(id, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelOrder(
            @PathVariable UUID id,
            Authentication authentication) {

        UUID userId = securityUtils.getAuthenticatedUserId(authentication);
        orderService.cancelOrder(id, userId);
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .build();
    }
}
