package org.veto.core.rdbms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.veto.core.rdbms.bean.Order;
import org.veto.shared.COIN_TYPE;

import java.util.Date;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, String>, JpaSpecificationExecutor<Order> {
    
    List<Order> findByUserId(String userId);
    
    List<Order> findByUserIdAndStatus(String userId, Order.STATUS status);
    
    List<Order> findByStatus(Order.STATUS status);
    
    List<Order> findByStatusAndCreatedAtBefore(Order.STATUS status, Date expireTime);
    
    List<Order> findByCoinType(COIN_TYPE coinType);
    
    List<Order> findByUserIdAndCoinType(String userId, COIN_TYPE coinType);
    
    Order findByOutsideId(String outsideId);
    
    boolean existsByOutsideId(String outsideId);
    
    List<Order> findByCreatedAtBetween(Date startTime, Date endTime);
    
    List<Order> findByUserIdOrderByCreatedAtDesc(String userId);
}
