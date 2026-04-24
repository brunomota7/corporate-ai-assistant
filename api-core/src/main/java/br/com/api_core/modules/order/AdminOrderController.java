package br.com.api_core.modules.order;

import br.com.api_core.modules.order.dto.OrderResponseDTO;
import br.com.api_core.modules.order.dto.OrderStatusUpdateDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<List<OrderResponseDTO>> findAll() {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(orderService.findAll());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponseDTO> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody OrderStatusUpdateDTO dto) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(orderService.updateStatus(id, dto));
    }
}
