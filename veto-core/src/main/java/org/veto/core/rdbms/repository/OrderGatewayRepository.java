package org.veto.core.rdbms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.veto.core.rdbms.bean.OrderGateway;

public interface OrderGatewayRepository extends JpaRepository<OrderGateway, Long>, JpaSpecificationExecutor<OrderGateway> {
    
    OrderGateway findByOrderId(String orderId);
    
    boolean existsByOrderId(String orderId);
}
