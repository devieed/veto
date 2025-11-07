package org.veto.core.rdbms.bean;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.veto.core.VetoBean;
import org.veto.shared.COIN_TYPE;

import java.math.BigDecimal;
import java.util.Date;

// 记录用户的总消费能力
@Entity
@Table(name = "user_consumptions", indexes = {
        @Index(columnList = "user_id,coin_type,wallet_behavior_type", unique = true),
        @Index(columnList = "coin_type"),
        @Index(columnList = "behavior_24h"),
        @Index(columnList = "behavior_48h"),
        @Index(columnList = "wallet_behavior_type"),
        @Index(columnList = "updated_at")
})
@Setter
@Getter
public class UserConsumption implements VetoBean {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Enumerated(EnumType.ORDINAL)
    private COIN_TYPE coinType;
    // 行为类型
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "wallet_behavior_type")
    private WALLET_BEHAVIOR_TYPE walletBehaviorType;
    // 总数
    @Column(precision = 18, scale = 2)
    private BigDecimal total;
    // 24小时行为量
    @Column(precision = 18, scale = 2, name = "behavior_24h")
    private BigDecimal behavior24h;

    @Column(precision = 18, scale = 2, name = "behavior_48h")
    private BigDecimal behavior48h;

    private Date updatedAt;

    @PrePersist
    public void prePersist() {
        this.updatedAt = new Date();
    }
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = new Date();
    }
}
