package org.veto.core.rdbms.bean;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.veto.core.VetoBean;
import org.veto.shared.COIN_TYPE;

import java.math.BigDecimal;
import java.util.Date;

@Table(name = "orders", indexes = {
        @Index(columnList = "user_id"),
        @Index(columnList = "created_at"),
        @Index(columnList = "expire_at"),
        @Index(columnList = "updated_at"),
        @Index(columnList = "status"),
        @Index(columnList = "coin_type")
})
@Entity
@Setter
@Getter
public class Order implements VetoBean {
    @Id
    private String id;

    @Column(name = "user_id")
    private String userId;
    // 关联的外部id，如果有
    @Column(name = "outside_id")
    private String outsideId;

    @Column(name = "created_at")
    private Date createdAt;

    @Enumerated(EnumType.ORDINAL)
    private COIN_TYPE coinType;

    @Column(precision = 18, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.ORDINAL)
    private WALLET_BEHAVIOR_TYPE walletBehaviorType;

    @Column(name = "updated_at")
    private Date updatedAt;

    private Date expireAt;

    @Enumerated(EnumType.ORDINAL)
    private STATUS status;

    @PrePersist
    public void prePersist() {
        this.updatedAt = new Date();
        this.createdAt = new Date();
    }

    @PreUpdate
    public void preUpdate(){
        this.updatedAt = new Date();
    }

    public enum STATUS{
        PENDING_PAYMENT,
        ACTIVE,
        EXPIRE,
        PAYMENTING, // 正在支付
        PAYMENTED, // 支付成功
        CLOSED, // 订单关闭
    }
}
