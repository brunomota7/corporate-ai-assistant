package br.com.api_core.domain.repository;

import br.com.api_core.domain.Order;
import br.com.api_core.domain.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByUserId(UUID userId);
    Order findByIdAndUserId(UUID id, UUID userId);
    List<Order> findAllByStatus(OrderStatus status);
}
