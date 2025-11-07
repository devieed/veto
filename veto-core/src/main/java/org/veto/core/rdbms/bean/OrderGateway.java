package org.veto.core.rdbms.bean;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.veto.core.VetoBean;

// 第三方支付需要的一些配置信息
@Entity
@Table(name = "order_gateway", indexes = {
        @Index(columnList = "order_id", unique = true),
})
@Setter
@Getter
public class OrderGateway implements VetoBean {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderId;
}
